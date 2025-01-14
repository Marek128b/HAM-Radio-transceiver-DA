#include <Arduino.h>
#include <BluetoothSerial.h>
#include <ArduinoJson.h>

BluetoothSerial SerialBT;

unsigned long long lastMillis = 0;
#define interval 1000

// Allocate the JSON document
DynamicJsonDocument doc(128);

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
  delay(25);

  if (millis() - lastMillis >= interval)
  {
    doc.clear();
    doc["voltage"] = 11.7;
    doc["call"] = "OE8GKE";
    doc["name"] = "Georg";
    String out;
    deserializeJson(doc, out);
    SerialBT.print(out);

    lastMillis = millis();
  }
}