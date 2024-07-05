// Note Display not working with ESP32 platformio version 6.7.0, only tested working in version 5.3.0
#include <Arduino.h>
#include "Free_Fonts.h"
#include <TFT_eSPI.h> // Hardware-specific library
#include <SPI.h>

TFT_eSPI tft = TFT_eSPI();    // Invoke custom library
const int backlight_led = 26; // backlight of LCD
/*
TFT_MISO = 12
TFT_MOSI = 11
TFT_SCLK = 13
TFT_CS = 33
TFT_DC = 36
TFT_RST = 35
TOUCH_CS = 21
SPI_FREQUENCY = 55000000
SPI_TOUCH_FREQUENCY = 2500000
TFT_BL = 26
TFT_BACKLIGHT_ON = 1
*/

void setup(void)
{
  pinMode(backlight_led, OUTPUT);

  Serial.begin(115200);
  Serial.println("Hello! ILI9488 TFT Test");

  // Use this initializer if you're using a 3.5" TFT 480x320
  tft.init(); // initialize a ILI9488_DRIVER chip
  tft.setRotation(3);
  tft.fillScreen(TFT_BLACK); // sets Background color in RGB565 format

  tft.setCursor(0, 32);
  tft.setTextColor(TFT_PINK);
  // tft.setTextFont(FONT2);     // sets the font of the texts
  tft.setFreeFont(FSB24); // Select Free Serif 24 point font
  tft.setTextSize(1);
  tft.print("Millis example:");

  Serial.println("end setup");
}
void loop()
{
  static unsigned long long mill;

  tft.setTextColor(TFT_WHITE, TFT_BLACK); // by setting the text background color you can update the text without flickering
  tft.setTextSize(1);
  tft.setCursor(0, 75); // position at textSize * 8
  // tft.drawString(String(millis()), 0, 64);
  mill = millis();
  tft.drawString(String(mill), 0, 75, 7); // prints the millis to position 0,75 and with the font #7 which looks good for texts

 // delay(1); // short delay
  // tft.fillRect(0, 42, 480, 46, TFT_BLUE); //bad method causing noticeable flickering
}