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
  Serial.println("Hello World");
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
    // Serial.write(SerialBT.read());
    doc.clear();
    String s_in = SerialBT.readString();
    Serial.println(s_in);
    deserializeJson(doc, s_in);

    serializeJsonPretty(doc, Serial);
  }

  if (millis() - lastMillis >= interval)
  {
    doc.clear();
    doc["op"] = false;
    doc["frequency"] = 14.122100;
    doc["voltage"] = round((3 * (3.7 + ((float)random(4) / (float)10))) * 1000.0) / 1000.0;
    doc["name"] = "Georg";
    doc["call"] = "OE8GKE";
    doc["temperature"] = (float)random(800) / (float)10 + 20;
    String out;
    serializeJson(doc, out);

    Serial.println(out);
    SerialBT.println(out);

    lastMillis = millis();
  }
}