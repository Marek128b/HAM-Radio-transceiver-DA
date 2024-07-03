#include <Arduino.h>

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
  tft.fillScreen(TFT_BLACK);

  tft.setCursor(0, 0);
  tft.setTextColor(TFT_RED);
  tft.setTextSize(5);
  tft.print("Counter example:");

  Serial.println("end setup");
}
void loop()
{
  static int counter = 0;
  tft.setTextColor(TFT_RED, TFT_BLACK); //by setting the text background color you can update the text without flickering 
  tft.setTextSize(6);
  tft.setCursor(0, 40); //position at textSize * 8
  tft.print(counter);
  counter++;

  delay(10);
  //tft.fillRect(0, 80, 480, 64, TFT_BLACK); //bad method causing noticeable flickering
}