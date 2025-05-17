#include <Arduino.h>
#include <BluetoothSerial.h>

// V1.1 by Marek OE8GKE
// A Bluetooth to Serial and Serial to BT Adapter

BluetoothSerial SerialBT;
String receivedData = "";

void setup()
{
  Serial.begin(115200);
  SerialBT.begin("FunkY"); // Bluetooth device name
  Serial.println("The device started, now you can pair it with bluetooth!");
}

void loop()
{
  if (Serial.available())
  {
    SerialBT.write(Serial.read());
  }

  while (SerialBT.available())
  {
    char incomingChar = SerialBT.read();
    if (incomingChar == '\n')
    { // End of message
      Serial.println(receivedData);
      receivedData = ""; // Clear buffer
    }
    else
    {
      receivedData += incomingChar;
    }
  }
}