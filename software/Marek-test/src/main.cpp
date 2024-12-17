#include <Arduino.h>
#include <ArduinoJson.h>
#include <SPI.h>
#include "Free_Fonts.h"
#include <TFT_eSPI.h> // Hardware-specific library
#include <SPI.h>
#include <SD.h>
#include <SD_MMC.h>
#include "soc/soc.h"
#include "soc/sdmmc_reg.h"
#include <ESP32Encoder.h>
#include <addressable7segment.h>
#include <Adafruit_NeoPixel.h>
#include <si5351.h>
#include <Wire.h>

// #########################################
// FIRMWARE VERSION v0.0.4
// #########################################

String FW_version = "v0.0.4";

TFT_eSPI tft = TFT_eSPI();    // Invoke custom library
const int backlight_led = 46; // backlight of LCD
/*
TFT_MISO = 18
TFT_MOSI = 11
TFT_SCLK = 10
TFT_CS = 14
TFT_DC = 12
TFT_RST = 13
TOUCH_CS = 3
SPI_FREQUENCY = 55000000
SPI_TOUCH_FREQUENCY = 2500000
TFT_BL = 46
TFT_BACKLIGHT_ON = 1
*/

// calibration: 155989 on 22.10.2024
unsigned long frequency = 14000000; // in Hz
unsigned int freqInc = 1000;
#define IF_Freq 15995200
Si5351 si5351;
int32_t freq_correction = 155989; // Replace with your calculated ppm error
/*
9 - SCL
8 - SDA
*/

#define PIN 38      // Which pin the NeoPixels are connected to
#define NUMPIXELS 1 // How many NeoPixels there are in a strip
Adafruit_NeoPixel indicator(NUMPIXELS, PIN, NEO_GRB + NEO_KHZ800);

addressableSegment oneWireDisplay(48, 7); // on pin 48, 7 Segments
#define SegmentBrightness 255

// init  Encoder object and pins
static IRAM_ATTR void enc_cb(void *arg);
ESP32Encoder encoder(true, enc_cb);
long lastEncoderVal = 0;
#define encPinA 40
#define encPinB 41
#define encSW 39
bool interrupt_encoder_executed = false;
bool interrupt_encoder_switch_executed = false;

#define Ptt_btn 4
#define RXTX_switch_pin 5
bool rxtx_status = 1;

// ##############################################################################################################################
static IRAM_ATTR void enc_cb(void *arg);
IRAM_ATTR void encSW_ISR();
IRAM_ATTR void rxtx_switch();
void printRxTxState();
void printFreq(unsigned long frequency);
void printStep(unsigned int freqInc);
void printVFO_BFO(unsigned long frequency);
void updateFrequencies(unsigned long frequency);
// ##############################################################################################################################

void setup()
{
  Serial.begin(115200);
  pinMode(backlight_led, OUTPUT);
  analogWrite(backlight_led, 255);
  oneWireDisplay.begin();
  oneWireDisplay.printDouble(0, 1, 0, SegmentBrightness); // Double, amount of digits after comma, starting pos, brightness

  indicator.setPixelColor(0, indicator.Color(0, 30, 0));
  indicator.show();

  // Use this initializer if you're using a 3.5" TFT 480x320
  tft.init(); // initialize a ILI9488_DRIVER chip
  tft.setRotation(3);
  tft.fillScreen(TFT_BLACK); // sets Background color in RGB565 format

  // error correction = frequency error / wanted frequency
  if (!si5351.init(SI5351_CRYSTAL_LOAD_8PF, 0, 0))
  {
    Serial.println("Device not found on I2C bus!");
    indicator.setPixelColor(0, indicator.Color(30, 0, 10));
    indicator.show();
  }
  else
  {
    Serial.println("Found Si5351 on I2C bus"); // if the si5351 ic is found set the drive strength for both CLK0 and CLK2
    si5351.set_correction(freq_correction, SI5351_PLL_INPUT_XO);
    si5351.set_pll(SI5351_PLL_FIXED, SI5351_PLLA);        // sets the pll A
    si5351.drive_strength(SI5351_CLK0, SI5351_DRIVE_6MA); // VFO
    si5351.drive_strength(SI5351_CLK2, SI5351_DRIVE_6MA); // BFO
    indicator.setPixelColor(0, indicator.Color(0, 0, 10));
    indicator.show();
  }

  ESP32Encoder::useInternalWeakPullResistors = puType::up;
  // use pin encPinA and encPinB for the first encoder
  encoder.attachHalfQuad(encPinA, encPinB);

  attachInterrupt(encSW, encSW_ISR, FALLING);

  // Ptt button and switch
  pinMode(Ptt_btn, INPUT_PULLUP);
  attachInterrupt(Ptt_btn, rxtx_switch, CHANGE);
  pinMode(RXTX_switch_pin, OUTPUT);

  tft.setTextColor(tft.color565(77, 238, 234), TFT_BLACK); // by setting the text background color you can update the text without flickering
  tft.setFreeFont(FF7);
  tft.setTextSize(1);
  // tft.drawString(String(millis()), 0, 64);
  char strBuffer[32];
  snprintf(strBuffer, sizeof(strBuffer), "FunkY 20m  %s", FW_version); // prints the heading with the current firmware version
  tft.drawString(strBuffer, 0, 0);                                     // prints the millis to position 0,0 and with the font #7 which looks good for text
  printRxTxState();
  updateFrequencies(frequency);
  printFreq(frequency); // prints the frequency to the display
  printStep(freqInc);
  printVFO_BFO(frequency);
}

// ##############################################################################################################################

void loop()
{
  if (interrupt_encoder_switch_executed)
  {
    Serial.println("encoder switch executed");

    switch (freqInc)
    {
    case 100:
      freqInc = 10000;
      break;
    case 250:
      freqInc = 100;
      break;
    case 500:
      freqInc = 250;
      break;
    case 1000:
      freqInc = 500;
      break;
    case 2500:
      freqInc = 1000;
      break;
    case 5000:
      freqInc = 2500;
      break;
    case 10000:
      freqInc = 5000;
      break;
    default:
      freqInc = 10000;
      break;
    }

    printStep(freqInc);
    interrupt_encoder_switch_executed = false;
  }

  if (interrupt_encoder_executed)
  {
    Serial.println("Encoder count = " + String((int32_t)encoder.getCount() / 2));

    int enc_diff = ((int32_t)encoder.getCount() / 2) - lastEncoderVal;
    lastEncoderVal = ((int32_t)encoder.getCount() / 2);

    if (enc_diff > 0)
    {
      frequency = frequency + freqInc;
    }
    else
    {
      frequency = frequency - freqInc;
    }

    if (frequency > 14350000)
    {
      frequency = 14000000;
    }
    else if (frequency < 14000000)
    {
      frequency = 14350000;
    }

    printFreq(frequency);
    printVFO_BFO(frequency);
    updateFrequencies(frequency);
    printRxTxState();

    interrupt_encoder_executed = false;
  }
}

// ##############################################################################################################################
void printFreq(unsigned long frequency)
{
  Serial.println("Freq = " + frequency / 1000);

  tft.setTextColor(TFT_WHITE, TFT_BLACK); // by setting the text background color you can update the text without flickering
  tft.setFreeFont(FF8);
  tft.setTextSize(1);
  char strBuffer[32];
  snprintf(strBuffer, sizeof(strBuffer), "%.2fkHz", (float)frequency / 1000);
  tft.drawString(strBuffer, 0, 74, 8); // prints the millis to position 0,74 with font 7 (not supporting text)
}

void printStep(unsigned int freqInc)
{
  Serial.println("Step size = " + freqInc);
  tft.setTextColor(tft.color565(0, 255, 50), TFT_BLACK); // by setting the text background color you can update the text without flickering
  tft.setFreeFont(FF8);
  tft.setTextSize(1);
  char strBuffer[32];
  snprintf(strBuffer, sizeof(strBuffer), "Step: %dHz  ", freqInc);
  tft.drawString(strBuffer, 0, 170); // prints the millis to position 0,99 74+24*2*2
}

void printVFO_BFO(unsigned long frequency)
{
  tft.setTextColor(tft.color565(255, 255, 0), TFT_BLACK); // by setting the text background color you can update the text without flickering
  tft.setFreeFont(FF8);
  tft.setTextSize(1);

  float vfo_freq = ((float)(frequency * 100) + (float)(IF_Freq * 100)) / (float)100000000;

  char strBuffer[32];
  snprintf(strBuffer, sizeof(strBuffer), "VFO: %.5fMHz  ", vfo_freq);
  Serial.print("SI5351_CLK0 (VFO) = ");
  Serial.println(strBuffer);
  tft.drawString(strBuffer, 0, 218); // prints the millis to position 0,99 170+2*24

  float bfo_freq = ((float)IF_Freq + (float)2700) / (float)1000000;

  tft.setTextColor(TFT_CYAN, TFT_BLACK); // by setting the text background color you can update the text without flickering
  snprintf(strBuffer, sizeof(strBuffer), "BFO: %.4fMHz  ", bfo_freq);
  Serial.print("SI5351_CLK2 (BFO) = ");
  Serial.println(strBuffer);
  tft.drawString(strBuffer, 0, 266); // prints the millis to position 0,99 218+24*2
}

static IRAM_ATTR void enc_cb(void *arg)
{
  interrupt_encoder_executed = true;
}

// variables to keep track of the timing of recent interrupts
unsigned long encoder_switch_time = 0;
unsigned long last_encoder_switch_time = 0;
IRAM_ATTR void encSW_ISR()
{
  encoder_switch_time = millis();
  if (encoder_switch_time - last_encoder_switch_time > 250)
  {
    interrupt_encoder_switch_executed = true;
    last_encoder_switch_time = encoder_switch_time;
  }
}

IRAM_ATTR void rxtx_switch()
{
  rxtx_status = digitalRead(Ptt_btn);
  digitalWrite(RXTX_switch_pin, rxtx_status);
  printRxTxState();
}

void printRxTxState()
{
  tft.setTextColor(rxtx_status ? tft.color565(0, 255, 0) : tft.color565(255, 0, 0), TFT_BLACK); // by setting the text background color you can update the text without flickering
  tft.setFreeFont(FF7);
  tft.setTextSize(1);
  // tft.drawString(String(millis()), 0, 64);
  char strBuffer[32];
  snprintf(strBuffer, sizeof(strBuffer), "%s", rxtx_status ? "RX" : "TX"); // prints the heading with the current firmware version
  tft.drawString(strBuffer, 400, 0);                                       // prints the millis to position 0,0 and with the font #7 which looks good for text
}

void updateFrequencies(unsigned long frequency)
{
  // Set CLK0 to output VFO
  // si5351.set_ms_source(SI5351_CLK0, SI5351_PLLA);
  // si5351.output_enable(SI5351_CLK0, 1);
  si5351.set_freq((frequency * 100) + (IF_Freq * 100), SI5351_CLK0); // VFO (frequency * 100) + (IF_Freq * 100)

  // Set CLK2 to hear Signal
  // si5351.set_ms_source(SI5351_CLK2, SI5351_PLLB);
  // si5351.output_enable(SI5351_CLK2, 1);
  si5351.set_freq((IF_Freq * 100) + (2700 * 100), SI5351_CLK2); // BFO (IF_Freq * 100) + (2700 * 100)

  // Query a status update and wait a bit to let the Si5351 populate the
  // status flags correctly.
  si5351.update_status();

  // Print Frequency: 14 350 . 000
  oneWireDisplay.printDouble((double)((double)frequency / (double)1000), 2, 0, SegmentBrightness); // Double, amount of digits after comma, starting pos, brightness
}