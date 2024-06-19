#include <Arduino.h>
#include <ArduinoJson.h>
#include <SPI.h>
#include <SD.h>
#include <SD_MMC.h>
#include "soc/soc.h"
#include "soc/sdmmc_reg.h"
#include <ESP32Encoder.h>
#include <Adafruit_NeoPixel.h>
#include <si5351.h>
#include <Wire.h>

#define PLLB_FREQ 87000000000ULL
Si5351 si5351;
/*
9 - SCL
8 - SDA
*/

#define PIN 48      // Which pin the NeoPixels are connected to
#define NUMPIXELS 1 // How many NeoPixels there are in a strip
Adafruit_NeoPixel indicator(NUMPIXELS, PIN, NEO_GRB + NEO_KHZ800);

// Define the chip select pin for the SD card module
#define SD_CS_PIN 34
/*
34 - CS
11 - MISO
12 - SCK
13 - MOSI
*/
#define FILE_NAME "/speedtest.txt"
#define FILE_SIZE 1024 * 1024 // 1 MB
#define BUFFER_SIZE 512       // Buffer size
#define DISABLE_SPEEDTEST true

// init  Encoder object and pins
static IRAM_ATTR void enc_cb(void *arg);
ESP32Encoder encoder(true, enc_cb);
#define encPinA 40
#define encPinB 41
#define encSW 39
bool interrupt_encoder_executed = false;
bool interrupt_encoder_switch_executed = false;

const char *filenameConfig = "/config.json"; // <- SD library uses 8.3 filenames
String OpName = "";
String OpCallSign = "";
String Band_Names[32];
unsigned long long Band_start_stop[32][2];

// ###############################################################################################################################################################################################
void readSpeedTest();
void writeSpeedTest();
IRAM_ATTR void encSW_ISR();
void loadConfiguration(const char *filename);
void writeLineText(String text);
void setupPins();
void testLEDs();
/*
SanDisk Ultra 16GB (10)A1 SDHC
Write speed: 231.99 KB/s
Read speed: 427.74 KB/s
*/
// ###############################################################################################################################################################################################
void setup()
{
  // Initialize serial communication at 115200 bits per second
  Serial.begin(115200);
  setupPins();

  writeLineText("tests");
  testLEDs();

  // error correction = frequency error / wanted frequency
  int32_t freq_correction = 0; // Replace with your calculated ppm error
  if (!si5351.init(SI5351_CRYSTAL_LOAD_8PF, freq_correction, SI5351_XTAL_FREQ))
  {
    Serial.println("Device not found on I2C bus!");
    indicator.setPixelColor(0, indicator.Color(100, 0, 30));
    indicator.show();
  }
  else
  {
    Serial.println("Found Si5351 on I2C bus");
  }

  ESP32Encoder::useInternalWeakPullResistors = puType::up;
  // use pin encPinA and encPinB for the first encoder
  encoder.attachHalfQuad(encPinA, encPinB);

  // Initialize the SD card
  if (!SD.begin(SD_CS_PIN, SPI, 4000000, "/sd", 1))
  {
    Serial.println("Card Mount Failed");
    indicator.setPixelColor(0, indicator.Color(100, 10, 0));
    indicator.show();
  }

  writeLineText("SD card info");
  sdcard_type_t cardType = SD.cardType();
  if (cardType != CARD_NONE)
  {
    Serial.print("SD Card Type: ");
    switch (cardType)
    {
    case CARD_MMC:
      Serial.println("MMC");
      break;
    case CARD_SD:
      Serial.println("SDSC");
      break;
    case CARD_SDHC:
      Serial.println("SDHC");
      break;
    default:
      Serial.println("UNKNOWN");
    }

    uint64_t cardSize = SD.cardSize() / (1024 * 1024); // Get card size in MB
    Serial.print("SD Card Size: ");
    Serial.print(cardSize);
    Serial.println(" MB");

    uint64_t totalSectors = SD.cardSize() / 512;
    Serial.print("Total Sectors: ");
    Serial.println(totalSectors);

    uint32_t sectorsPerCluster = SD.sectorSize();
    Serial.print("Sectors per Cluster: ");
    Serial.println(sectorsPerCluster);

    uint32_t totalClusters = SD.totalBytes();
    Serial.print("Total Clusters: ");
    Serial.println(totalClusters);

    uint32_t freeClusters = SD.usedBytes();
    Serial.print("Free Clusters: ");
    Serial.println(freeClusters);

    uint64_t freeSpace = (uint64_t)freeClusters * sectorsPerCluster * 512 / (1024 * 1024);
    Serial.print("Free Space: ");
    Serial.print(freeSpace);
    Serial.println(" MB");

    // If you want to list the files on the SD card, you can uncomment the following lines
    // Serial.println("Files on the card:");
    // listFiles(SD, "/", 0);
    if (!DISABLE_SPEEDTEST)
    {
      writeSpeedTest();
      readSpeedTest();
    }

    indicator.setPixelColor(0, indicator.Color(0, 100, 0));
    indicator.show();
  }
  else
  {
    Serial.println("No SD card attached");
    indicator.setPixelColor(0, indicator.Color(100, 25, 0));
    indicator.show();
  }

  writeLineText("load config from SD");
  loadConfiguration(filenameConfig);

  writeLineText("SI5351 frequencies");

  // Set CLK0 to output 13.545MHz
  Serial.println("SI5351_CLK0 = 14 MHz - 455kHz = 13.545MHz");
  // si5351.set_ms_source(SI5351_CLK0, SI5351_PLLA);
  si5351.set_freq_manual(1400000000ULL - 455000UL, PLLB_FREQ, SI5351_CLK0);

  // Set CLK2 to output 452.5kHz
  Serial.println("SI5351_CLK2 = 455kHz - 2.5kHz = 452.5kHz");
  si5351.set_ms_source(SI5351_CLK2, SI5351_PLLB);
  si5351.set_freq_manual(452500UL, PLLB_FREQ, SI5351_CLK2);

  // Query a status update and wait a bit to let the Si5351 populate the
  // status flags correctly.
  si5351.update_status();

  writeLineText("Loop start");
}

// ###############################################################################################################################################################################################
void loop()
{
  if (interrupt_encoder_switch_executed)
  {
    Serial.println("encoder switch executed");
    encoder.setCount(0);
    indicator.setPixelColor(0, indicator.ColorHSV((uint16_t)(encoder.getCount() / 2 * 100), 255, 100));
    indicator.show();
    interrupt_encoder_switch_executed = false;
  }
  if (interrupt_encoder_executed)
  {
    Serial.println("Encoder count = " + String((int32_t)encoder.getCount() / 2) + " -> " + String((uint16_t)(encoder.getCount() / 2 * 100)));
    //indicator.setPixelColor(0, indicator.ColorHSV((uint16_t)(encoder.getCount() / 2 * 100), 255, 100));
    //indicator.show();
    
    
    // indicator.clear();
    // indicator.show();
    interrupt_encoder_executed = false;
  }
}

// ###############################################################################################################################################################################################
void setupPins()
{
  indicator.begin();
  pinMode(encSW, INPUT_PULLUP);
  attachInterrupt(encSW, encSW_ISR, FALLING);
}

void testLEDs()
{
  Serial.println("Show red");
  indicator.setPixelColor(0, indicator.Color(255, 0, 0));
  indicator.show();
  delay(200);
  Serial.println("Show green");
  indicator.setPixelColor(0, indicator.Color(0, 255, 0));
  indicator.show();
  delay(200);
  Serial.println("Show blue");
  indicator.setPixelColor(0, indicator.Color(0, 0, 255));
  indicator.show();
  delay(200);
}

void writeSpeedTest()
{
  Serial.println("Starting write speed test...");

  File file = SD.open(FILE_NAME, FILE_WRITE);
  if (!file)
  {
    Serial.println("Failed to open file for writing");
    return;
  }

  uint8_t buffer[BUFFER_SIZE];
  memset(buffer, 'A', BUFFER_SIZE); // Fill buffer with dummy data

  unsigned long startTime = millis();
  for (size_t i = 0; i < FILE_SIZE / BUFFER_SIZE; ++i)
  {
    if (file.write(buffer, BUFFER_SIZE) != BUFFER_SIZE)
    {
      Serial.println("Write failed");
      file.close();
      return;
    }
  }
  file.close();
  unsigned long endTime = millis();

  float writeTime = (endTime - startTime) / 1000.0;
  float writeSpeed = (FILE_SIZE / 1024.0) / writeTime; // Speed in KB/s

  Serial.print("Write time: ");
  Serial.print(writeTime);
  Serial.println(" seconds");

  Serial.print("Write speed: ");
  Serial.print(writeSpeed);
  Serial.println(" KB/s");
}

void readSpeedTest()
{
  Serial.println("Starting read speed test...");

  File file = SD.open(FILE_NAME, FILE_READ);
  if (!file)
  {
    Serial.println("Failed to open file for reading");
    return;
  }

  uint8_t buffer[BUFFER_SIZE];

  unsigned long startTime = millis();
  while (file.available())
  {
    if (file.read(buffer, BUFFER_SIZE) != BUFFER_SIZE)
    {
      Serial.println("Read failed");
      file.close();
      return;
    }
  }
  file.close();
  unsigned long endTime = millis();

  float readTime = (endTime - startTime) / 1000.0;
  float readSpeed = (FILE_SIZE / 1024.0) / readTime; // Speed in KB/s

  Serial.print("Read time: ");
  Serial.print(readTime);
  Serial.println(" seconds");

  Serial.print("Read speed: ");
  Serial.print(readSpeed);
  Serial.println(" KB/s");

  // Optionally, delete the test file
  SD.remove(FILE_NAME);
}

static IRAM_ATTR void enc_cb(void *arg)
{
  interrupt_encoder_executed = true;
}

// variables to keep track of the timing of recent interrupts
unsigned long encoder_switch_time = 0;
unsigned long last_encoder_switch_time = 0;
IRAM_ATTR void encSW_ISR()
{
  encoder_switch_time = millis();
  if (encoder_switch_time - last_encoder_switch_time > 250)
  {
    interrupt_encoder_switch_executed = true;
    last_encoder_switch_time = encoder_switch_time;
  }
}

void writeLineText(String text)
{
  Serial.println();

  // Define the word you want to center
  const char *word = text.c_str();
  // Calculate the length of the word
  int wordLength = strlen(word);

  // Define the total line length
  const int lineLength = 128;
  // Calculate the total space taken by the word with single spaces on both sides
  int totalWordSpace = wordLength + 2;

  // Calculate the left padding (half of the remaining space)
  int leftPadding = (lineLength - totalWordSpace) / 2;
  // Calculate the right padding
  int rightPadding = lineLength - totalWordSpace - leftPadding;

  // Print the left padding
  for (int i = 0; i < leftPadding; i++)
  {
    Serial.print('#');
  }

  // Print the space, word, and space
  Serial.print(" ");
  Serial.print(word);
  Serial.print(" ");

  // Print the right padding
  for (int i = 0; i < rightPadding; i++)
  {
    Serial.print('#');
  }

  // Print a newline at the end
  Serial.println('\n');
}

// Loads the configuration from a file
void loadConfiguration(const char *filename)
{
  // Open file for reading
  File file = SD.open(filename);

  // Allocate a temporary JsonDocument
  JsonDocument doc;

  // Deserialize the JSON document
  DeserializationError error = deserializeJson(doc, file);
  if (error)
    Serial.println(F("Failed to read file, using default configuration"));

  Serial.println("JSON read: ");
  serializeJsonPretty(doc, Serial);
  Serial.println("\n\n");

  // Copy values from the JsonDocument to the Config
  OpName = doc["name"].as<String>();
  OpCallSign = doc["call"].as<String>();

  Serial.print("Name: ");
  Serial.println(OpName);
  Serial.print("Call: ");
  Serial.println(OpCallSign);

  // Get all frequencies
  JsonObject frequencies = doc["frequencies"];
  for (JsonPair kv : frequencies)
  {
    static int countfreq = 0;
    const char *band = kv.key().c_str();
    JsonArray values = kv.value().as<JsonArray>();

    Band_Names[countfreq] = (String)band;
    Band_start_stop[countfreq][0] = values[0].as<long>();
    Band_start_stop[countfreq][1] = values[0].as<long>();

    Serial.print("Frequencies for ");
    Serial.print(band);
    Serial.println(":");

    Serial.println(values[0].as<String>());
    Serial.println(values[1].as<String>());

    countfreq++;
  }
  // Close the file (Curiously, File's destructor doesn't close the file)
  file.close();
  Serial.println();
  
  Serial.println(Band_Names[0]);
  Serial.println(Band_start_stop[0][0]);
  Serial.println(Band_start_stop[0][1]);
}
