#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <BLE2901.h>
#include <SPI.h>
#include <Adafruit_Sensor.h>
#include "Adafruit_BME680.h"
#include <mbedtls/aes.h>
#include <esp_system.h> // For esp_random()
#include <string.h>
#include <stdio.h>

// Constants for BLE and sensor
#define DEVICE_ID 20
#define BME_SCK 18
#define BME_MISO 19
#define BME_MOSI 23
#define BME_CS 5
#define BME_SERVICE_UUID "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define BME_CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

// Constants for encryption
#define BUFFER_SIZE 256
#define IV_SIZE 16

BLEServer *pServer = NULL;
BLECharacteristic *pCharacteristic = NULL;
BLE2901 *descriptor_2901 = NULL;

bool deviceConnected = false;
long StartMillis;
char resultaat[100]; // String to hold sensor data
int timer = 1000;

Adafruit_BME680 bme(BME_CS, BME_MOSI, BME_MISO, BME_SCK);
float temperature, pressure, humidity;

// AES setup
unsigned char key[16] = { '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'A', 'B', 'C', 'D', 'E', 'F' };
unsigned char iv[IV_SIZE];

class MyServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer *pServer) {
    deviceConnected = true;
  };

  void onDisconnect(BLEServer *pServer) {
    deviceConnected = false;
  }
};

void setup() {
  Serial.begin(115200);

  // Initialize the BME680 sensor
  if (!bme.begin()) {
    Serial.println("Error: BME680 not detected!");
    while (1);
  }

  bme.setTemperatureOversampling(BME680_OS_8X);
  bme.setHumidityOversampling(BME680_OS_2X);
  bme.setPressureOversampling(BME680_OS_4X);

  // Initialize BLE
  BLEDevice::init("ESP32");
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  BLEService *pService = pServer->createService(BME_SERVICE_UUID);
  pCharacteristic = pService->createCharacteristic(BME_CHARACTERISTIC_UUID, BLECharacteristic::PROPERTY_NOTIFY);
  pCharacteristic->addDescriptor(new BLE2902());
  descriptor_2901 = new BLE2901();
  descriptor_2901->setDescription("Encrypted Sensor Data");
  pCharacteristic->addDescriptor(descriptor_2901);
  pService->start();

  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(BME_SERVICE_UUID);
  BLEDevice::startAdvertising();

  Serial.println("BLE server started. Waiting for connections...");
}

void loop() {
  if (deviceConnected) {
    if (millis() - StartMillis > timer) {
      readSensorData();
      sendEncryptedData();
      StartMillis = millis();
    }
  }
}

void readSensorData() {
  if (bme.performReading()) {
    temperature = bme.temperature;
    pressure = bme.pressure / 100.0;
    humidity = bme.humidity;
    snprintf(resultaat, sizeof(resultaat), "%d|%.1fC|%.1fhPA|%.1f%%", DEVICE_ID, temperature, pressure, humidity);
  } else {
    snprintf(resultaat, sizeof(resultaat), "Error: Failed to read from BME680!");
  }
}

void sendEncryptedData() {
  unsigned char input[BUFFER_SIZE] = {0};
  unsigned char encrypted[BUFFER_SIZE] = {0};
  size_t padded_len;

  // Initialize AES context
  mbedtls_aes_context aes;
  mbedtls_aes_init(&aes);

  // Copy resultaat to input buffer
  size_t message_len = strlen(resultaat);
  memcpy(input, resultaat, message_len);

  // Add padding
  padded_len = addPadding(input, message_len);

  // Generate IV
  generateRandomIV(iv, IV_SIZE);

  // Encrypt
  mbedtls_aes_setkey_enc(&aes, key, 128);
  if (mbedtls_aes_crypt_cbc(&aes, MBEDTLS_AES_ENCRYPT, padded_len, iv, input, encrypted) != 0) {
    Serial.println("Encryption failed.");
    return;
  }

  // Print original, encrypted, and IV data to Serial
  Serial.print("Original Data: ");
  Serial.println(resultaat);
  Serial.print("Encrypted Data (Hex): ");
  for (size_t i = 0; i < padded_len; i++) {
    Serial.printf("%02X ", encrypted[i]);
  }
  Serial.println();
  Serial.print("IV (Hex): ");
  for (size_t i = 0; i < IV_SIZE; i++) {
    Serial.printf("%02X ", iv[i]);
  }
  Serial.println();

  // Create the final payload (IV + encrypted data)
  unsigned char payload[IV_SIZE + padded_len];
  memcpy(payload, iv, IV_SIZE);
  memcpy(payload + IV_SIZE, encrypted, padded_len);

  // Send encrypted payload via BLE
  pCharacteristic->setValue(payload, IV_SIZE + padded_len);
  pCharacteristic->notify();

  // Free AES context
  mbedtls_aes_free(&aes);
}

size_t addPadding(unsigned char* buffer, size_t original_len) {
  size_t padding_len = 16 - (original_len % 16);
  for (size_t i = 0; i < padding_len; i++) {
    buffer[original_len + i] = padding_len;
  }
  return original_len + padding_len;
}

void generateRandomIV(unsigned char* iv, size_t len) {
  for (size_t i = 0; i < len; i++) {
    iv[i] = esp_random() & 0xFF;
  }
}
