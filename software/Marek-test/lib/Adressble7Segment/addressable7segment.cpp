#include <Arduino.h>
#include <addressable7segment.h>
#include <Adafruit_NeoPixel.h>
#ifdef __AVR__
#include <avr/power.h> // Required for 16 MHz Adafruit Trinket
#endif

addressableSegment::addressableSegment(int pinNr, int segmentCount)
{
    this->PIXEL_PIN = pinNr;
    this->PIXEL_COUNT = segmentCount * 3;

    this->pixels = new Adafruit_NeoPixel(this->PIXEL_COUNT, this->PIXEL_PIN, NEO_RGB + NEO_KHZ800);
}

void addressableSegment::begin()
{
    pixels->begin(); // Initialize NeoPixel strip object (REQUIRED)
    pixels->show();  // Initialize all pixels to 'off'
    SegOff();
}

void addressableSegment::showInt(int nr, int location, int brightness)
{
    int a = 0;
    int b = 1;
    int c = 2;
    int l = location;
    while (l > 0)
    {
        a += 3;
        b += 3;
        c += 3;
        l--;
    }
    // Serial.print("\nA: "); Serial.println(a); Serial.print("B: "); Serial.println(b); Serial.print("C: "); Serial.println(c);
    switch (nr)
    {
    case 0:
        pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // A, B, C
        pixels->setPixelColor(b, pixels->Color(brightness, brightness, brightness)); // D, E, F
        pixels->setPixelColor(c, pixels->Color(0, 0, 0));                            // G, DPr, DPl
        pixels->show();                                                             // Send the updated pixel colors to the hardware.
        break;
    case 1:
        pixels->setPixelColor(a, pixels->Color(0, brightness, brightness)); // A, B, C
        pixels->setPixelColor(b, pixels->Color(0, 0, 0));                   // D, E, F
        pixels->setPixelColor(c, pixels->Color(0, 0, 0));                   // G, DPr, DPl
        pixels->show();                                                    // Send the updated pixel colors to the hardware.
        break;
    case 2:
        pixels->setPixelColor(a, pixels->Color(brightness, brightness, 0)); // A, B, C
        pixels->setPixelColor(b, pixels->Color(brightness, brightness, 0)); // D, E, F
        pixels->setPixelColor(c, pixels->Color(brightness, 0, 0));          // G, DPr, DPl
        pixels->show();                                                    // Send the updated pixel colors to the hardware.
        break;
    case 3:
        pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // A, B, C
        pixels->setPixelColor(b, pixels->Color(brightness, 0, 0));                   // D, E, F
        pixels->setPixelColor(c, pixels->Color(brightness, 0, 0));                   // G, DPr, DPl
        pixels->show();                                                             // Send the updated pixel colors to the hardware.
        break;
    case 4:
        pixels->setPixelColor(a, pixels->Color(0, brightness, brightness)); // A, B, C
        pixels->setPixelColor(b, pixels->Color(0, 0, brightness));          // D, E, F
        pixels->setPixelColor(c, pixels->Color(brightness, 0, 0));          // G, DPr, DPl
        pixels->show();                                                    // Send the updated pixel colors to the hardware.
        break;
    case 5:
        pixels->setPixelColor(a, pixels->Color(brightness, 0, brightness)); // A, B, C
        pixels->setPixelColor(b, pixels->Color(brightness, 0, brightness)); // D, E, F
        pixels->setPixelColor(c, pixels->Color(brightness, 0, 0));          // G, DPr, DPl
        pixels->show();                                                    // Send the updated pixel colors to the hardware.
        break;
    case 6:
        pixels->setPixelColor(a, pixels->Color(brightness, 0, brightness));          // A, B, C
        pixels->setPixelColor(b, pixels->Color(brightness, brightness, brightness)); // D, E, F
        pixels->setPixelColor(c, pixels->Color(brightness, 0, 0));                   // G, DPr, DPl
        pixels->show();                                                             // Send the updated pixel colors to the hardware.
        break;
    case 7:
        pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // A, B, C
        pixels->setPixelColor(b, pixels->Color(0, 0, 0));                            // D, E, F
        pixels->setPixelColor(c, pixels->Color(0, 0, 0));                            // G, DPr, DPl
        pixels->show();                                                             // Send the updated pixel colors to the hardware.
        break;
    case 8:
        pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // A, B, C
        pixels->setPixelColor(b, pixels->Color(brightness, brightness, brightness)); // D, E, F
        pixels->setPixelColor(c, pixels->Color(brightness, 0, 0));                   // G, DPr, DPl
        pixels->show();                                                             // Send the updated pixel colors to the hardware.
        break;
    case 9:
        pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // A, B, C
        pixels->setPixelColor(b, pixels->Color(brightness, 0, brightness));          // D, E, F
        pixels->setPixelColor(c, pixels->Color(brightness, 0, 0));                   // G, DPr, DPl
        pixels->show();                                                             // Send the updated pixel colors to the hardware.
        break;
    }
}

void addressableSegment::showChar(char ch, int location, int brightness)
{
    int a = 0;
    int b = 1;
    int c = 2;
    int l = location;
    while (l > 0)
    {
        a += 3;
        b += 3;
        c += 3;
        l--;
    }
    // Serial.print("\nA: "); Serial.println(a); Serial.print("B: "); Serial.println(b); Serial.print("C: "); Serial.println(c);
    switch (ch)
    {
    case '0':
        pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // A, B, C
        pixels->setPixelColor(b, pixels->Color(brightness, brightness, brightness)); // D, E, F
        pixels->setPixelColor(c, pixels->Color(0, 0, 0));                            // G, DPr, DPl
        pixels->show();                                                             // Send the updated pixel colors to the hardware.
        break;
    case '1':
        pixels->setPixelColor(a, pixels->Color(0, brightness, brightness)); // A, B, C
        pixels->setPixelColor(b, pixels->Color(0, 0, 0));                   // D, E, F
        pixels->setPixelColor(c, pixels->Color(0, 0, 0));                   // G, DPr, DPl
        pixels->show();                                                    // Send the updated pixel colors to the hardware.
        break;
    case '2':
        pixels->setPixelColor(a, pixels->Color(brightness, brightness, 0)); // A, B, C
        pixels->setPixelColor(b, pixels->Color(brightness, brightness, 0)); // D, E, F
        pixels->setPixelColor(c, pixels->Color(brightness, 0, 0));          // G, DPr, DPl
        pixels->show();                                                    // Send the updated pixel colors to the hardware.
        break;
    case '3':
        pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // A, B, C
        pixels->setPixelColor(b, pixels->Color(brightness, 0, 0));                   // D, E, F
        pixels->setPixelColor(c, pixels->Color(brightness, 0, 0));                   // G, DPr, DPl
        pixels->show();                                                             // Send the updated pixel colors to the hardware.
        break;
    case '4':
        pixels->setPixelColor(a, pixels->Color(0, brightness, brightness)); // A, B, C
        pixels->setPixelColor(b, pixels->Color(0, 0, brightness));          // D, E, F
        pixels->setPixelColor(c, pixels->Color(brightness, 0, 0));          // G, DPr, DPl
        pixels->show();                                                    // Send the updated pixel colors to the hardware.
        break;
    case '5':
        pixels->setPixelColor(a, pixels->Color(brightness, 0, brightness)); // A, B, C
        pixels->setPixelColor(b, pixels->Color(brightness, 0, brightness)); // D, E, F
        pixels->setPixelColor(c, pixels->Color(brightness, 0, 0));          // G, DPr, DPl
        pixels->show();                                                    // Send the updated pixel colors to the hardware.
        break;
    case '6':
        pixels->setPixelColor(a, pixels->Color(brightness, 0, brightness));          // A, B, C
        pixels->setPixelColor(b, pixels->Color(brightness, brightness, brightness)); // D, E, F
        pixels->setPixelColor(c, pixels->Color(brightness, 0, 0));                   // G, DPr, DPl
        pixels->show();                                                             // Send the updated pixel colors to the hardware.
        break;
    case '7':
        pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // A, B, C
        pixels->setPixelColor(b, pixels->Color(0, 0, 0));                            // D, E, F
        pixels->setPixelColor(c, pixels->Color(0, 0, 0));                            // G, DPr, DPl
        pixels->show();                                                             // Send the updated pixel colors to the hardware.
        break;
    case '8':
        pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // A, B, C
        pixels->setPixelColor(b, pixels->Color(brightness, brightness, brightness)); // D, E, F
        pixels->setPixelColor(c, pixels->Color(brightness, 0, 0));                   // G, DPr, DPl
        pixels->show();                                                             // Send the updated pixel colors to the hardware.
        break;
    case '9':
        pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // A, B, C
        pixels->setPixelColor(b, pixels->Color(brightness, 0, brightness));          // D, E, F
        pixels->setPixelColor(c, pixels->Color(brightness, 0, 0));                   // G, DPr, DPl
        pixels->show();                                                             // Send the updated pixel colors to the hardware.
        break;                                                                     ///////
    case 'a':
        pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // B, A, C
        pixels->setPixelColor(b, pixels->Color(brightness, 0, brightness));          // E, D, F
        pixels->setPixelColor(c, pixels->Color(0, 0, 0));                            // DPr, G, DPl
        pixels->show();                                                             // Send the updated pixel colors to the hardware.
        break;
    case 'b':
        pixels->setPixelColor(a, pixels->Color(0, 0, brightness));                   // B, A, C
        pixels->setPixelColor(b, pixels->Color(brightness, brightness, brightness)); // E, D, F
        pixels->setPixelColor(c, pixels->Color(0, 0, 0));                            // DPr, G, DPl
        pixels->show();                                                             // Send the updated pixel colors to the hardware.
        break;
    case 'c':
        pixels->setPixelColor(a, pixels->Color(0, 0, 0));                   // B, A, C
        pixels->setPixelColor(b, pixels->Color(brightness, brightness, 0)); // E, D, F
        pixels->setPixelColor(c, pixels->Color(0, brightness, 0));          // DPr, G, DPl
        pixels->show();                                                    // Send the updated pixel colors to the hardware.
        break;
    case 'd':
        pixels->setPixelColor(a, pixels->Color(brightness, 0, brightness)); // B, A, C
        pixels->setPixelColor(b, pixels->Color(brightness, brightness, 0)); // E, D, F
        pixels->setPixelColor(c, pixels->Color(0, brightness, 0));          // DPr, G, DPl
        pixels->show();                                                    // Send the updated pixel colors to the hardware.
        break;
    case 'e':
        pixels->setPixelColor(a, pixels->Color(0, brightness, 0));                   // B, A, C
        pixels->setPixelColor(b, pixels->Color(brightness, brightness, brightness)); // E, D, F
        pixels->setPixelColor(c, pixels->Color(0, brightness, 0));                   // DPr, G, DPl
        pixels->show();                                                             // Send the updated pixel colors to the hardware.
        break;
    case 'f':
        pixels->setPixelColor(a, pixels->Color(0, brightness, 0));          // B, A, C
        pixels->setPixelColor(b, pixels->Color(brightness, 0, brightness)); // E, D, F
        pixels->setPixelColor(c, pixels->Color(0, 0, 0));                   // DPr, G, DPl
        pixels->show();                                                    // Send the updated pixel colors to the hardware.
        break;
    case 'h':
        pixels->setPixelColor(a, pixels->Color(brightness, 0, brightness)); // B, A, C
        pixels->setPixelColor(b, pixels->Color(brightness, 0, brightness)); // E, D, F
        pixels->setPixelColor(c, pixels->Color(0, 0, 0));                   // DPr, G, DPl
        pixels->show();                                                    // Send the updated pixel colors to the hardware.
        break;
    case 'l':
        pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // B, A, C
        pixels->setPixelColor(b, pixels->Color(brightness, brightness, brightness)); // E, D, F
        pixels->setPixelColor(c, pixels->Color(0, 0, 0));                            // DPr, G, DPl
        pixels->show();                                                             // Send the updated pixel colors to the hardware.
        break;
    case 'o':
        pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // B, A, C
        pixels->setPixelColor(b, pixels->Color(brightness, brightness, brightness)); // E, D, F
        pixels->setPixelColor(c, pixels->Color(0, 0, 0));                            // DPr, G, DPl
        pixels->show();                                                             // Send the updated pixel colors to the hardware.
        break;
    case 'p':
        pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // B, A, C
        pixels->setPixelColor(b, pixels->Color(brightness, brightness, brightness)); // E, D, F
        pixels->setPixelColor(c, pixels->Color(0, 0, 0));                            // DPr, G, DPl
        pixels->show();                                                             // Send the updated pixel colors to the hardware.
        break;
    case 'r':
        pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // B, A, C
        pixels->setPixelColor(b, pixels->Color(brightness, brightness, brightness)); // E, D, F
        pixels->setPixelColor(c, pixels->Color(0, 0, 0));                            // DPr, G, DPl
        pixels->show();                                                             // Send the updated pixel colors to the hardware.
        break;
    }
}

void addressableSegment::printString(String str, int SLocation, int brightness)
{
    char c[32];
    // Serial.println(str);
    str.toCharArray(c, str.length() + 1);
    // Serial.println(c);
    for (int i = SLocation; i < str.length() + SLocation; i++)
    {
        // Serial.print(i);
        // Serial.print(c[i]);
        // Serial.print(",");
        showChar(c[i - SLocation], i, brightness);
    }
    // Serial.println();
    // Serial.println();
}

void addressableSegment::printInt(int nr, int SLocation, int brightness)
{
    // Serial.println(nr);
    String s = String(nr);
    printString(s, SLocation, brightness);
}

void addressableSegment::printInt(unsigned int nr, int SLocation, int brightness)
{
    // Serial.println(nr);
    String s = String(nr);
    printString(s, SLocation, brightness);
}

void addressableSegment::printInt(unsigned long nr, int SLocation, int brightness)
{
    // Serial.println(nr);
    String s = String(nr);
    printString(s, SLocation, brightness);
}

void addressableSegment::SegOff()
{
    for (int i = 0; i < PIXEL_COUNT; i++)
    {
        pixels->setPixelColor(i, pixels->Color(0, 0, 0));
        pixels->show();
    }
}

void addressableSegment::printDouble(double f, int pres, int SLocation, int brightness)
{

    boolean dp[sizeof(f) * 2];
    char CBuffer[sizeof(f) * 2];
    dtostrf(f, 0, pres, CBuffer);

    boolean b = false;
    for (int i = 0; i < sizeof(CBuffer); i++)
    {
        if (CBuffer[i] == '.' && b != true)
        {
            b = true;
            dp[i - 1] = true;
        }
        else
        {
            dp[i - 1] = false;
        }
        if (b)
        {
            CBuffer[i] = CBuffer[i + 1];
        }
    }

    for (int i = 0; i < sizeof(CBuffer); i++)
    {
        if (dp[i])
        {
            showWithDP(CBuffer[i], i + SLocation, true, brightness);
        }
        else
        {
            showWithDP(CBuffer[i], i + SLocation, false, brightness);
        }
    }
}

void addressableSegment::showWithDP(char nr, int SLocation, boolean dp, int brightness)
{
    int a = 0;
    int b = 1;
    int c = 2;
    int l = SLocation;
    while (l > 0)
    {
        a += 3;
        b += 3;
        c += 3;
        l--;
    }
    if (dp)
    {
        switch (nr)
        {
        case '0':
            pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // A, B, C
            pixels->setPixelColor(b, pixels->Color(brightness, brightness, brightness)); // D, E, F
            pixels->setPixelColor(c, pixels->Color(0, brightness, 0));                   // G, DPr, DPl
            pixels->show();                                                             // Send the updated pixel colors to the hardware.
            break;
        case '1':
            pixels->setPixelColor(a, pixels->Color(0, brightness, brightness)); // A, B, C
            pixels->setPixelColor(b, pixels->Color(0, 0, 0));                   // D, E, F
            pixels->setPixelColor(c, pixels->Color(0, brightness, 0));          // G, DPr, DPl
            pixels->show();                                                    // Send the updated pixel colors to the hardware.
            break;
        case '2':
            pixels->setPixelColor(a, pixels->Color(brightness, brightness, 0)); // A, B, C
            pixels->setPixelColor(b, pixels->Color(brightness, brightness, 0)); // D, E, F
            pixels->setPixelColor(c, pixels->Color(brightness, brightness, 0)); // G, DPr, DPl
            pixels->show();                                                    // Send the updated pixel colors to the hardware.
            break;
        case '3':
            pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // A, B, C
            pixels->setPixelColor(b, pixels->Color(brightness, 0, 0));                   // D, E, F
            pixels->setPixelColor(c, pixels->Color(brightness, brightness, 0));          // G, DPr, DPl
            pixels->show();                                                             // Send the updated pixel colors to the hardware.
            break;
        case '4':
            pixels->setPixelColor(a, pixels->Color(0, brightness, brightness)); // A, B, C
            pixels->setPixelColor(b, pixels->Color(0, 0, brightness));          // D, E, F
            pixels->setPixelColor(c, pixels->Color(brightness, brightness, 0)); // G, DPr, DPl
            pixels->show();                                                    // Send the updated pixel colors to the hardware.
            break;
        case '5':
            pixels->setPixelColor(a, pixels->Color(brightness, 0, brightness)); // A, B, C
            pixels->setPixelColor(b, pixels->Color(brightness, 0, brightness)); // D, E, F
            pixels->setPixelColor(c, pixels->Color(brightness, brightness, 0)); // G, DPr, DPl
            pixels->show();                                                    // Send the updated pixel colors to the hardware.
            break;
        case '6':
            pixels->setPixelColor(a, pixels->Color(brightness, 0, brightness));          // A, B, C
            pixels->setPixelColor(b, pixels->Color(brightness, brightness, brightness)); // D, E, F
            pixels->setPixelColor(c, pixels->Color(brightness, brightness, 0));          // G, DPr, DPl
            pixels->show();                                                             // Send the updated pixel colors to the hardware.
            break;
        case '7':
            pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // A, B, C
            pixels->setPixelColor(b, pixels->Color(0, 0, 0));                            // D, E, F
            pixels->setPixelColor(c, pixels->Color(0, brightness, 0));                   // G, DPr, DPl
            pixels->show();                                                             // Send the updated pixel colors to the hardware.
            break;
        case '8':
            pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // A, B, C
            pixels->setPixelColor(b, pixels->Color(brightness, brightness, brightness)); // D, E, F
            pixels->setPixelColor(c, pixels->Color(brightness, brightness, 0));          // G, DPr, DPl
            pixels->show();                                                             // Send the updated pixel colors to the hardware.
            break;
        case '9':
            pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // A, B, C
            pixels->setPixelColor(b, pixels->Color(brightness, 0, brightness));          // D, E, F
            pixels->setPixelColor(c, pixels->Color(brightness, brightness, 0));          // G, DPr, DPl
            pixels->show();                                                             // Send the updated pixel colors to the hardware.
            break;
        }
    }
    else
    {
        switch (nr)
        {
        case '0':
            pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // A, B, C
            pixels->setPixelColor(b, pixels->Color(brightness, brightness, brightness)); // D, E, F
            pixels->setPixelColor(c, pixels->Color(0, 0, 0));                            // G, DPr, DPl
            pixels->show();                                                             // Send the updated pixel colors to the hardware.
            break;
        case '1':
            pixels->setPixelColor(a, pixels->Color(0, brightness, brightness)); // A, B, C
            pixels->setPixelColor(b, pixels->Color(0, 0, 0));                   // D, E, F
            pixels->setPixelColor(c, pixels->Color(0, 0, 0));                   // G, DPr, DPl
            pixels->show();                                                    // Send the updated pixel colors to the hardware.
            break;
        case '2':
            pixels->setPixelColor(a, pixels->Color(brightness, brightness, 0)); // A, B, C
            pixels->setPixelColor(b, pixels->Color(brightness, brightness, 0)); // D, E, F
            pixels->setPixelColor(c, pixels->Color(brightness, 0, 0));          // G, DPr, DPl
            pixels->show();                                                    // Send the updated pixel colors to the hardware.
            break;
        case '3':
            pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // A, B, C
            pixels->setPixelColor(b, pixels->Color(brightness, 0, 0));                   // D, E, F
            pixels->setPixelColor(c, pixels->Color(brightness, 0, 0));                   // G, DPr, DPl
            pixels->show();                                                             // Send the updated pixel colors to the hardware.
            break;
        case '4':
            pixels->setPixelColor(a, pixels->Color(0, brightness, brightness)); // A, B, C
            pixels->setPixelColor(b, pixels->Color(0, 0, brightness));          // D, E, F
            pixels->setPixelColor(c, pixels->Color(brightness, 0, 0));          // G, DPr, DPl
            pixels->show();                                                    // Send the updated pixel colors to the hardware.
            break;
        case '5':
            pixels->setPixelColor(a, pixels->Color(brightness, 0, brightness)); // A, B, C
            pixels->setPixelColor(b, pixels->Color(brightness, 0, brightness)); // D, E, F
            pixels->setPixelColor(c, pixels->Color(brightness, 0, 0));          // G, DPr, DPl
            pixels->show();                                                    // Send the updated pixel colors to the hardware.
            break;
        case '6':
            pixels->setPixelColor(a, pixels->Color(brightness, 0, brightness));          // A, B, C
            pixels->setPixelColor(b, pixels->Color(brightness, brightness, brightness)); // D, E, F
            pixels->setPixelColor(c, pixels->Color(brightness, 0, 0));                   // G, DPr, DPl
            pixels->show();                                                             // Send the updated pixel colors to the hardware.
            break;
        case '7':
            pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // A, B, C
            pixels->setPixelColor(b, pixels->Color(0, 0, 0));                            // D, E, F
            pixels->setPixelColor(c, pixels->Color(0, 0, 0));                            // G, DPr, DPl
            pixels->show();                                                             // Send the updated pixel colors to the hardware.
            break;
        case '8':
            pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // A, B, C
            pixels->setPixelColor(b, pixels->Color(brightness, brightness, brightness)); // D, E, F
            pixels->setPixelColor(c, pixels->Color(brightness, 0, 0));                   // G, DPr, DPl
            pixels->show();                                                             // Send the updated pixel colors to the hardware.
            break;
        case '9':
            pixels->setPixelColor(a, pixels->Color(brightness, brightness, brightness)); // A, B, C
            pixels->setPixelColor(b, pixels->Color(brightness, 0, brightness));          // D, E, F
            pixels->setPixelColor(c, pixels->Color(brightness, 0, 0));                   // G, DPr, DPl
            pixels->show();                                                             // Send the updated pixel colors to the hardware.
            break;
        }
    }
}