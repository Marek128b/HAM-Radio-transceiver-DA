#include <Arduino.h>
#include "lut.h"

float NTC_ADC2Temperature(unsigned int adc_value)
{
  return (float) NTC_table[adc_value] / (float)  10;
}

#define NTC_in_pin 6

void setup()
{
  pinMode(NTC_in_pin, INPUT);
  Serial.begin(115200);
}

void loop()
{
  Serial.println("----------------------------------------------------");

  Serial.println("--------------------ADC Value-----------------------");
  Serial.println(analogRead(NTC_in_pin));

  Serial.println("-------------------Temperature-----------------------");
  Serial.printf("%.1f\n\r", NTC_ADC2Temperature(analogRead(NTC_in_pin)));

  Serial.println("----------------------------------------------------\n");
  delay(1000);
}
