#include <Arduino.h>
#include <Adafruit_NeoPixel.h>
#ifdef __AVR__
#include <avr/power.h> // Required for 16 MHz Adafruit Trinket
#endif

#ifndef __addressable7Segment__
#define __addressable7Segment__

class addressableSegment
{
private:
    int PIXEL_PIN;
    int PIXEL_COUNT;
    Adafruit_NeoPixel *pixels;

    void showWithDP(char nr, int SLocation, boolean dp, int brightness);

public:
    addressableSegment(int pinNr, int segmentCount);
    void begin();

    void showInt(int nr, int location, int brightness);
    void showChar(char ch, int location, int brightness);
    void printString(String str, int SLocation, int brightness);
    void printInt(int nr, int SLocation, int brightness);
    void printInt(unsigned int nr, int SLocation, int brightness);
    void printInt(unsigned long nr, int SLocation, int brightness);
    void SegOff();
    void printDouble(double f, int pres, int SLocation, int brightness);
};

#endif