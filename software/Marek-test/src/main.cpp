#include <Arduino.h>
#include "lut.h"
#include <ArduinoJson.h>
#include <SPI.h>
#include "Free_Fonts.h"
#include <TFT_eSPI.h> // Hardware-specific library
#include <SPI.h>
#include <SD.h>
#include <SD_MMC.h>
#include "soc/soc.h"
#include "soc/sdmmc_reg.h"
#include <Encoder.h>
#include <addressable7segment.h>
#include <Adafruit_NeoPixel.h>
#include <si5351.h>
#include <Wire.h>

// #########################################
// FIRMWARE VERSION v0.1.4
// #########################################

String FW_version = "v0.1.4";

TFT_eSPI tft = TFT_eSPI();                     // Invoke custom library
TFT_eSprite spriteAmpTEMP = TFT_eSprite(&tft); // Create Sprite object "spriteAmpTEMP" with pointer to "tft" object
TFT_eSprite spriteStep = TFT_eSprite(&tft);    // Create Sprite object "spriteStep" with pointer to "tft" object
TFT_eSprite spriteFO = TFT_eSprite(&tft);      // Create Sprite object "spriteFO" with pointer to "tft" object

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

// Allocate the JSON document
JsonDocument doc;
JsonDocument docSend;
DeserializationError desError;
String name = "Georg";
String call = "OE8GKE";
/*
Serial2:
RX: 17
TX: 16
*/

// calibration: 155989 on 22.10.2024
unsigned long frequency = 14074000; // in Hz
unsigned int freqInc = 1000;
#define IF_Freq_lower 15995000
#define IF_Freq_upper 15997500
Si5351 si5351;
si5351_drive si5351Level = SI5351_DRIVE_2MA;
/*
| Setting | Scope Vpp |  dBm  |
|---------|-----------|-------|
|  2 mA   |    0.77   |  +7   |
|  4 mA   |    1.35   | +12   |
|  6 mA   |    1.83   | +14   |
|  8 mA   |    2.09   | +15.5 |
*/
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
long lastEncoderVal = 0;
#define encPinA 41
#define encPinB 40
#define encSW 39
Encoder encoder(encPinA, encPinB);
bool interrupt_encoder_switch_executed = false;

#define Ptt_btn 4
#define RXTX_switch_pin 37
bool rxtx_status = 1;
bool last_rxtx_status = 0;

#define Vbat_pin 5
unsigned long long lastVbatMeasurementMs = 0;
#define VbatMeasureInterval 2000
float voltage = 0;

// NTC temperature
#define NTC_in_pin 6
unsigned long long lastNTCMeasurementMs = 0;
#define NTCMeasureInterval 1000
float temperaturePA = 0;

// ##############################################################################################################################
static IRAM_ATTR void enc_cb(void *arg);
void encSW_read();
void rxtx_switch();
void printRxTxState();
void printFreq(unsigned long frequency);
void printStep(unsigned int freqInc);
void printVFO_BFO(unsigned long frequency);
void updateFrequencies(unsigned long frequency);
void updateVoltage();
void printVoltage();
void updateNTCTemperature();
float NTC_ADC2Temperature(unsigned int adc_value)
{
  return (float)NTC_table[adc_value] / (float)10;
}
void IRAM_ATTR handleBT();
// ##############################################################################################################################

void setup()
{
  Serial.begin(115200);
  Serial2.begin(115200, SERIAL_8N1, 17, 16); // Serial for BT
  Serial.print("Firmware Version: ");
  Serial.println(FW_version);

  Serial2.print("Firmware Version: ");
  Serial2.println(FW_version);

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

  spriteAmpTEMP.setColorDepth(8);      // Create an 8-bit sprite
  spriteAmpTEMP.createSprite(480, 35); // creating a sprite with dimensions 480x35 pixels.

  spriteStep.setColorDepth(8);      // Create an 8-bit sprite
  spriteStep.createSprite(480, 45); // creating a sprite with dimensions 480x45 pixels.

  spriteFO.setColorDepth(8);      // Create an 8-bit sprite
  spriteFO.createSprite(480, 90); // creating a sprite with dimensions 480x90 pixels.

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
    si5351.set_pll(SI5351_PLL_FIXED, SI5351_PLLA);   // sets the pll A
    si5351.drive_strength(SI5351_CLK0, si5351Level); // VFO
    si5351.drive_strength(SI5351_CLK2, si5351Level); // BFO
    indicator.setPixelColor(0, indicator.Color(0, 0, 10));
    indicator.show();
  }

  pinMode(encSW, INPUT);

  // NTC input
  pinMode(NTC_in_pin, INPUT);

  // Ptt button and switch
  pinMode(Ptt_btn, INPUT_PULLUP);
  pinMode(RXTX_switch_pin, OUTPUT);
  digitalWrite(RXTX_switch_pin, !rxtx_status);

  // Vbat pin config
  pinMode(Vbat_pin, INPUT);
  updateVoltage();

  tft.setTextColor(tft.color565(77, 238, 234), TFT_BLACK); // by setting the text background color you can update the text without flickering
  tft.setFreeFont(FF7);
  tft.setTextSize(1);
  // tft.drawString(String(millis()), 0, 64);
  char strBuffer[16];
  snprintf(strBuffer, sizeof(strBuffer), "FunkY %s", FW_version); // prints the heading with the current firmware version
  tft.drawString(strBuffer, 0, 0);                                // prints the millis to position 0,0 and with the font #7 which looks good for text
  printRxTxState();
  updateFrequencies(frequency);
  printFreq(frequency); // prints the frequency to the display
  printStep(freqInc);
  printVFO_BFO(frequency);
  updateNTCTemperature();
}

// ##############################################################################################################################

void loop()
{
  if (millis() - lastVbatMeasurementMs >= VbatMeasureInterval)
  {
    lastVbatMeasurementMs = millis();
    updateVoltage();
  }

  if (millis() - lastNTCMeasurementMs >= NTCMeasureInterval)
  {
    lastNTCMeasurementMs = millis();
    updateNTCTemperature();
  }

  // read switch state and update "interrupt_encoder_switch_executed"
  encSW_read();

  if (interrupt_encoder_switch_executed == true)
  {
    interrupt_encoder_switch_executed = false;
    Serial.println("Encoder switch executed");

    // Define the sequence of frequency increments
    const int freqSteps[] = {100, 250, 500, 1000, 2500, 5000, 10000};
    const int numSteps = sizeof(freqSteps) / sizeof(freqSteps[0]);

    // Find the current position in the array
    int index = -1;
    for (int i = 0; i < numSteps; i++)
    {
      if (freqInc == freqSteps[i])
      {
        index = i;
        break;
      }
    }

    // If freqInc is not in the array, reset to default (optional safeguard)
    if (index == -1)
    {
      freqInc = freqSteps[0]; // Set to first step
    }
    else
    {
      // Move to the next step in sequence (looping back if needed)
      freqInc = freqSteps[(index + 1) % numSteps];
    }

    printStep(freqInc);
  }

  if (lastEncoderVal != encoder.read())
  {
    Serial.println("Encoder count = " + encoder.read());

    int enc_diff = lastEncoderVal - encoder.read();

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

    lastEncoderVal = encoder.read();
  }

  if (digitalRead(Ptt_btn) != last_rxtx_status)
  {
    last_rxtx_status = digitalRead(Ptt_btn);
    rxtx_switch();
  }

  handleBT();
}

// ##############################################################################################################################
void updateVoltage()
{
  // Serial.println("Vbat adc value");
  // Serial.println(analogRead(Vbat_pin));
  // Serial.println("Vbat Voltage");
  voltage = (float)(((float)analogRead(Vbat_pin)) / (float)4095) * 3.3 * (12.2 / 2.2) + 0.6;
  // Serial.println(voltage);
  printVoltage();
}

void printVoltage()
{
  if (voltage < 3.5 * 3)
  {
    tft.setTextColor(tft.color565(255, 81, 69), TFT_BLACK); // by setting the text background color you can update the text without flickering
  }
  else if (voltage > 3.7 * 3 && voltage <= 3.8 * 3)
  {
    tft.setTextColor(tft.color565(255, 150, 0), TFT_BLACK); // by setting the text background color you can update the text without flickering
  }
  else if (voltage > 3.8 * 3 && voltage <= 3.9 * 3)
  {
    tft.setTextColor(tft.color565(255, 255, 0), TFT_BLACK); // by setting the text background color you can update the text without flickering
  }
  else if (voltage > 3.9 * 3 && voltage <= 4.0 * 3)
  {
    tft.setTextColor(tft.color565(150, 255, 0), TFT_BLACK); // by setting the text background color you can update the text without flickering
  }
  else
  {
    tft.setTextColor(tft.color565(0, 255, 0), TFT_BLACK); // by setting the text background color you can update the text without flickering
  }
  tft.setFreeFont(FF7);
  tft.setTextSize(1);
  char strBuffer[32];
  snprintf(strBuffer, sizeof(strBuffer), "%.1fV  ", voltage); // prints the Battery Voltage
  tft.drawString(strBuffer, 290, 0);                          // prints the millis to position 29,0 and with the font #7 which looks good for text
}

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
  spriteStep.setTextColor(tft.color565(0, 255, 50), TFT_BLACK); // by setting the text background color you can update the text without flickering
  spriteStep.setFreeFont(FF8);
  spriteStep.setTextSize(1);
  char strBuffer[16];
  snprintf(strBuffer, sizeof(strBuffer), "Step: %dHz   ", freqInc);
  spriteStep.drawString(strBuffer, 0, 0);

  spriteStep.pushSprite(0, 160);
}

void printVFO_BFO(unsigned long frequency)
{
  spriteFO.setTextColor(tft.color565(255, 255, 0), TFT_BLACK); // by setting the text background color you can update the text without flickering
  spriteFO.setFreeFont(FF8);
  spriteFO.setTextSize(1);

  float vfo_freq = ((float)(frequency * 100) + (float)(IF_Freq_lower * 100)) / (float)100000000;

  char strBuffer[32];
  snprintf(strBuffer, sizeof(strBuffer), "VFO: %.5fMHz  ", vfo_freq);
  Serial.print("SI5351_CLK0 (VFO) = ");
  Serial.println(strBuffer);
  spriteFO.drawString(strBuffer, 0, 0); // prints the millis to position 0,99 170+2*24

  float bfo_freq = ((float)IF_Freq_upper + 200) / (float)1000000;

  spriteFO.setTextColor(TFT_CYAN, TFT_BLACK); // by setting the text background color you can update the text without flickering
  snprintf(strBuffer, sizeof(strBuffer), "BFO: %.4fMHz  ", bfo_freq);
  Serial.print("SI5351_CLK2 (BFO) = ");
  Serial.println(strBuffer);
  spriteFO.drawString(strBuffer, 0, 50); // prints the millis to position 0,99 218+24*2

  spriteFO.pushSprite(0, 218);
}

// variables to keep track of the timing of recent interrupts
unsigned long encoder_switch_time = 0;
unsigned long last_encoder_switch_time = 0;
void encSW_read()
{
  if (!digitalRead(encSW)) // Detect release
  {
    if (millis() - last_encoder_switch_time > 500)
    {
      interrupt_encoder_switch_executed = true;
      last_encoder_switch_time = millis();
    }
  }
  else // Detect press
  {
    encoder_switch_time = millis();
  }
}

void rxtx_switch()
{
  rxtx_status = !digitalRead(Ptt_btn);
  digitalWrite(RXTX_switch_pin, rxtx_status);
  Serial.print("RXTX pin: ");
  Serial.println(rxtx_status);
  printRxTxState();
}

void printRxTxState()
{
  tft.setTextColor(!rxtx_status ? tft.color565(0, 255, 0) : tft.color565(255, 0, 0), TFT_BLACK); // by setting the text background color you can update the text without flickering
  tft.setFreeFont(FF7);
  tft.setTextSize(1);
  // tft.drawString(String(millis()), 0, 64);
  char strBuffer[32];
  snprintf(strBuffer, sizeof(strBuffer), "%s", !rxtx_status ? "RX" : "TX"); // prints the RX and TX status
  tft.drawString(strBuffer, 430, 0);                                        // prints the millis to position 430,0 and with the font #7 which looks good for text
}

void updateFrequencies(unsigned long frequency)
{
  // Set CLK0 to output VFO
  // si5351.set_ms_source(SI5351_CLK0, SI5351_PLLA);
  // si5351.output_enable(SI5351_CLK0, 1);
  si5351.set_freq((frequency * 100) + (IF_Freq_lower * 100), SI5351_CLK0); // VFO (frequency * 100) + (IF_Freq * 100)

  // Set CLK2 to hear Signal
  // si5351.set_ms_source(SI5351_CLK2, SI5351_PLLB);
  // si5351.output_enable(SI5351_CLK2, 1);
  si5351.set_freq((IF_Freq_upper * 100) + (200 * 100), SI5351_CLK2); // BFO (IF_Freq * 100) + (2700 * 100)

  // Query a status update and wait a bit to let the Si5351 populate the
  // status flags correctly.
  si5351.update_status();

  // Print Frequency: 14 350 . 000
  oneWireDisplay.printDouble((double)((double)frequency / (double)1000), 2, 0, SegmentBrightness); // Double, amount of digits after comma, starting pos, brightness
}

void updateNTCTemperature()
{
  spriteAmpTEMP.setTextColor(tft.color565(255, 144, 18), TFT_BLACK); // by setting the text background color you can update the text without flickering
  spriteAmpTEMP.setFreeFont(FF7);
  spriteAmpTEMP.setTextSize(1);
  // tft.drawString(String(millis()), 0, 64);
  char strBuffer[32];
  snprintf(strBuffer, sizeof(strBuffer), "Amplifier Temp: %.1f°C  ", NTC_ADC2Temperature(analogRead(NTC_in_pin))); // prints the RX and TX status
  spriteAmpTEMP.drawString(strBuffer, 0, 0);                                                                       // prints the millis to position 430,0 and with the font #7 which looks good for text

  temperaturePA = NTC_ADC2Temperature(analogRead(NTC_in_pin));
  spriteAmpTEMP.pushSprite(0, 35);
}

// Transmits data via Serial to ESP for BT
void IRAM_ATTR handleBT()
{
  doc.clear();
  // Serial2.println("in bt func");

  if (Serial2.available() > 0) // receiver
  {
    String input = "";
    input = Serial2.readString();

    Serial.println(input);
    // Serial2.println(input);
    // Serial2.println("---");
    desError = deserializeJson(doc, input);

    if (desError)
    {
      docSend.clear();
      docSend["op"] = true;
      docSend["frequency"] = 0.00;
      docSend["voltage"] = 1.00;
      docSend["name"] = name;
      docSend["call"] = call;
      docSend["temperature"] = 0.0;

      String out;
      serializeJson(docSend, out);

      Serial.println("ERROR returned: ");
      Serial.println(desError.c_str());

      Serial.print("Transmitting: ");

      // Generate the minified JSON and send it to the Serial port.
      serializeJson(docSend, out);
      Serial.print(out);
      Serial2.println(out);
    }

    if (doc["op"]) // set value code
    {
      frequency = doc["frequency"].as<float>() * (float)1000000;
      updateFrequencies(frequency);
      printFreq(frequency);
      printVFO_BFO(frequency);

      if ((doc["name"].as<String>()) != "null")
      {
        name = doc["name"].as<String>();
      }
      if ((doc["call"].as<String>()) != "null")
      {
        call = doc["call"].as<String>();
      }
    }
    else if (doc["op"] == 0) // get value code
    {
      docSend.clear();
      docSend["op"] = false;
      if (doc["frequency"].as<String>() != "null")
      {
        docSend["frequency"] = (float)frequency / (float)1000000;
      }
      if (doc["voltage"].as<String>() != "null")
      {
        docSend["voltage"] = voltage;
      }
      if (doc["name"].as<String>() != "null")
      {
        docSend["name"] = name;
      }
      if (doc["call"].as<String>() != "null")
      {
        docSend["call"] = call;
      }
      if (doc["temperature"].as<String>() != "null")
      {
        docSend["temperature"] = temperaturePA;
      }

      String out;
      serializeJson(docSend, out);

      Serial.print("Transmitting: ");
      Serial.print(out);
      Serial2.println(out);
    }
  }
}
