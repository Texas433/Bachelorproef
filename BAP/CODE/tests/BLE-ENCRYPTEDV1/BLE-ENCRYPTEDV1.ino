#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <SPI.h>
#include <Adafruit_Sensor.h>
#include "Adafruit_BME680.h"

#define DEVICE_ID 20
#define BME_SCK 18
#define BME_MISO 19
#define BME_MOSI 23
#define BME_CS 5
#define BME_SERVICE_UUID "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define BME_CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

BLEServer *pServer = NULL;
BLECharacteristic *pCharacteristic = NULL;

bool deviceConnected = false;
long StartMillis = millis();
char resultaat[100]; // String to hold sensor data
int timer = 1000;

Adafruit_BME680 bme(BME_CS, BME_MOSI, BME_MISO, BME_SCK);
float temperature, pressure, humidity;

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

  if (!bme.begin()) {
    Serial.println("Error: BME680 not detected!");
    while (1);
  }

  bme.setTemperatureOversampling(BME680_OS_8X);
  bme.setHumidityOversampling(BME680_OS_2X);
  bme.setPressureOversampling(BME680_OS_4X);

  BLEDevice::init("ESP32");
  BLEDevice::setMTU(256);

  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  BLEService *pService = pServer->createService(BME_SERVICE_UUID);
  pCharacteristic = pService->createCharacteristic(BME_CHARACTERISTIC_UUID, BLECharacteristic::PROPERTY_NOTIFY);
  pCharacteristic->addDescriptor(new BLE2902());
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
      sendDecryptedData();
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

void sendDecryptedData() {
  Serial.print("Sending data: ");
  Serial.println(resultaat);

  pCharacteristic->setValue(resultaat);
  pCharacteristic->notify();
}
