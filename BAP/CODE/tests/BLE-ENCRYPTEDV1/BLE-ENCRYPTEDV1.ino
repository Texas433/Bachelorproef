
#include <BLEDevice.h>

#include <BLEServer.h>

#include <BLEUtils.h>

#include <BLE2902.h>

#include <BLE2901.h>

#include <SPI.h>

#include <Adafruit_Sensor.h>

#include "Adafruit_BME680.h"



BLEServer *pServer = NULL;

BLECharacteristic *pCharacteristic = NULL;

BLE2901 *descriptor_2901 = NULL;



bool deviceConnected = false;



long StartMillis;

char resultaat[100]; // String to hold sensor data

int timer = 1000;

#define DEVICE_ID 20

#define BME_SCK 18

#define BME_MISO 19

#define BME_MOSI 23

#define BME_CS 5

Adafruit_BME680 bme(BME_CS, BME_MOSI, BME_MISO, BME_SCK);



float temperature, pressure, humidity;



#define BME_SERVICE_UUID "4fafc201-1fb5-459e-8fcc-c5c9c331914b"

#define BME_CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"



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



  // Create BLE service and characteristic

  BLEService *pService = pServer->createService(BME_SERVICE_UUID);

  pCharacteristic = pService->createCharacteristic(BME_CHARACTERISTIC_UUID, BLECharacteristic::PROPERTY_NOTIFY);

  pCharacteristic->addDescriptor(new BLE2902());

  descriptor_2901 = new BLE2901();

  descriptor_2901->setDescription("BME680 Sensor Data");

  pCharacteristic->addDescriptor(descriptor_2901);

  pService->start();



  // Start advertising

  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();

  pAdvertising->addServiceUUID(BME_SERVICE_UUID);

  BLEDevice::startAdvertising();



  Serial.println("BLE server started. Waiting for connections...");

}



void loop() {

  if (deviceConnected) {

    if (millis() - StartMillis > timer) {

      readSensorData();

      delay(10);

      sendSensorData();



      StartMillis = millis();

    }

  }

}



void readSensorData() {

  if (bme.performReading()) {

    temperature = bme.temperature;               // Read temperature

    pressure = bme.pressure / 100.0;             // Convert pressure to hPa

    humidity = bme.humidity;                     // Read humidity

  } else {

    Serial.println("Error: Failed to read from BME680!");

    temperature = 0;

    pressure = 0;

    humidity = 0;

  }

}



void sendSensorData() {

  snprintf(resultaat, sizeof(resultaat), "%d|%.1fC|%.1fhPA|%.1f%%", 

          DEVICE_ID, temperature, pressure, humidity);

  pCharacteristic->setValue((uint8_t*)resultaat, strlen(resultaat));

  Serial.println(resultaat);



  // Notify the client

  pCharacteristic->notify();

}
