#include <Arduino.h>
#include <BluetoothSerial.h>
#include <ArduinoJson.h>

BluetoothSerial SerialBT;

unsigned long long lastMillis = 0;
#define interval 1000

// Allocate the JSON document
JsonDocument doc;

void setup()
{
  Serial.begin(115200);
  SerialBT.begin("FunkY BT"); // Name des ESP32
}

void loop()
{
  if (Serial.available())
  {
    SerialBT.write(Serial.read());
  }
  if (SerialBT.available())
  {
    Serial.write(SerialBT.read());
  }

  if (millis() - lastMillis >= interval)
  {
    doc.clear();
    doc["voltage"] = round((3*(3.7+((float)random(4)/(float)10))) * 1000.0) / 1000.0;;
    doc["frequency"] = 14.122100;
    doc["call"] = "OE8GKE";
    doc["name"] = "Georg";
    String out;
    serializeJson(doc, out);
    //Serial.println(out);
    SerialBT.println(out);

    lastMillis = millis();
  }
}