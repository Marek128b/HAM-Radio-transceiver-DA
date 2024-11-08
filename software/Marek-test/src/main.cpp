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
#include <Adafruit_NeoPixel.h>
#include <si5351.h>
#include <Wire.h>

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

//calibration: 156210
unsigned long frequency = 14000000; // in Hz
unsigned int freqInc = 1000;
#define IF_Freq 15995200
#define PLLB_FREQ 87000000000ULL
Si5351 si5351;
int32_t freq_correction = 156210; // Replace with your calculated ppm error
/*
9 - SCL
8 - SDA
*/

#define PIN 38      // Which pin the NeoPixels are connected to
#define NUMPIXELS 1 // How many NeoPixels there are in a strip
Adafruit_NeoPixel indicator(NUMPIXELS, PIN, NEO_GRB + NEO_KHZ800);

// init  Encoder object and pins
static IRAM_ATTR void enc_cb(void *arg);
ESP32Encoder encoder(true, enc_cb);
long lastEncoderVal = 0;
#define encPinA 40
#define encPinB 41
#define encSW 39
bool interrupt_encoder_executed = false;
bool interrupt_encoder_switch_executed = false;

// ##############################################################################################################################
static IRAM_ATTR void enc_cb(void *arg);
IRAM_ATTR void encSW_ISR();
void printFreq(unsigned long frequency);
void printStep(unsigned int freqInc);
void printVFO_BFO(unsigned long frequency);
// ##############################################################################################################################

void setup()
{
  pinMode(backlight_led, OUTPUT);
  analogWrite(backlight_led, 255);

  indicator.setPixelColor(0, indicator.Color(0, 30, 0));
  indicator.show();

  // Use this initializer if you're using a 3.5" TFT 480x320
  tft.init(); // initialize a ILI9488_DRIVER chip
  tft.setRotation(3);
  tft.fillScreen(TFT_BLACK); // sets Background color in RGB565 format

  // error correction = frequency error / wanted frequency
  if (!si5351.init(SI5351_CRYSTAL_LOAD_8PF, freq_correction, SI5351_XTAL_FREQ))
  {
    Serial.println("Device not found on I2C bus!");
    indicator.setPixelColor(0, indicator.Color(30, 0, 10));
    indicator.show();
  }
  else
  {
    Serial.println("Found Si5351 on I2C bus");
    indicator.setPixelColor(0, indicator.Color(0, 0, 10));
    indicator.show();
  }

  ESP32Encoder::useInternalWeakPullResistors = puType::up;
  // use pin encPinA and encPinB for the first encoder
  encoder.attachHalfQuad(encPinA, encPinB);

  attachInterrupt(encSW, encSW_ISR, FALLING);

  tft.setTextColor(tft.color565(77, 238, 234), TFT_BLACK); // by setting the text background color you can update the text without flickering
  tft.setFreeFont(FF7);
  tft.setTextSize(1);
  // tft.drawString(String(millis()), 0, 64);
  char strBuffer[32];
  snprintf(strBuffer, sizeof(strBuffer), "Marek 20m VFO test");
  tft.drawString(strBuffer, 0, 0); // prints the millis to position 0,0 and with the font #7 which looks good for texts
  // prints the frequency to the display
  printFreq(frequency);
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
    case 250:
      freqInc = 10000;
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

    interrupt_encoder_executed = false;
  }

  // Set CLK0 to output 2MHz
  Serial.println("SI5351_CLK0 = 16MHz - 14MHz");
  si5351.set_ms_source(SI5351_CLK2, SI5351_PLLB);
  si5351.set_freq_manual((frequency * 100) + IF_Freq * 100, PLLB_FREQ, SI5351_CLK0);

  // Set CLK2 to hear Signal
  Serial.println("SI5351_CLK2 = 16MHz - 2.7kHz");
  si5351.set_ms_source(SI5351_CLK2, SI5351_PLLB);
  si5351.set_freq_manual(IF_Freq * 100 - (2700 * 100), PLLB_FREQ, SI5351_CLK2);

  // Query a status update and wait a bit to let the Si5351 populate the
  // status flags correctly.
  si5351.update_status();
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
  Serial.println("Step sice = " + freqInc);
  tft.setTextColor(tft.color565(0, 255, 50), TFT_BLACK); // by setting the text background color you can update the text without flickering
  tft.setFreeFont(FF8);
  tft.setTextSize(1);
  char strBuffer[32];
  snprintf(strBuffer, sizeof(strBuffer), "step: %dHz  ", freqInc);
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
  tft.drawString(strBuffer, 0, 218); // prints the millis to position 0,99 170+2*24

  float bfo_freq = ((float)IF_Freq - (float)2700) / (float)1000000;

  tft.setTextColor(TFT_CYAN, TFT_BLACK); // by setting the text background color you can update the text without flickering
  snprintf(strBuffer, sizeof(strBuffer), "BFO: %.4fMHz  ", bfo_freq);
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
