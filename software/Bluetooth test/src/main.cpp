#include <Arduino.h>
#include <BluetoothSerial.h>
#include <ArduinoJson.h>

BluetoothSerial SerialBT;

// Allocate the JSON document
JsonDocument doc;
JsonDocument docSend;

// Pseudo Values
float frequency = 14.150; // in MHz
String name = "Georg";
String call = "OE8GKE";

//---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
void handleBT();
void handleOther();
//---------------------------------------------------------------------------------------------------------------------------------------------------------------------------

void setup()
{
  Serial.begin(115200);
  Serial.println("Hello World");

  Serial.print("setup() running on core ");
  Serial.println(xPortGetCoreID());

  SerialBT.begin("FunkY BT"); // Name des ESP32
}

void loop()
{
  handleBT();
  handleOther();
}

void handleOther()
{
  if (Serial.available())
  {
    SerialBT.write(Serial.read());
  }
}

void handleBT()
{
  // Serial.print("Task1 running on core ");
  // Serial.println(xPortGetCoreID());

  if (SerialBT.available())
  {
    // Serial.write(SerialBT.read());
    doc.clear();
    String s_in = SerialBT.readString();
    Serial.println(s_in);
    deserializeJson(doc, s_in);

    if (doc["op"]) // set value code
    {
      frequency = doc["frequency"];
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
      if (doc["frequency"].as<String>() != "null")
      {
        docSend["frequency"] = frequency;
      }
      if (doc["voltage"].as<String>() != "null")
      {
        docSend["voltage"] = round((3 * (3.7 + ((float)random(4) / (float)10))) * 1000.0) / 1000.0;
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
        docSend["temperature"] = (float)random(800) / (float)10 + 20;
      }

      String out;
      serializeJson(docSend, out);

      Serial.println(out);
      SerialBT.println(out);
    }
  }
}