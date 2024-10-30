#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <BLE2901.h>

BLEServer *pServer = NULL;
BLECharacteristic *pCharacteristic = NULL;
BLE2901 *descriptor_2901 = NULL;
BLE2901 *descriptor_2902 = NULL; // Added declaration for LDR descriptor

bool deviceConnected = false;
bool oldDeviceConnected = false;
int BUTTON_PIN = 13;
int LDR_PIN = 12;
long StartMillis;

int ButtonValue, LDRValue;

uint8_t dataPacket[4];

#define BUTTON_SERVICE_UUID "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define BUTTON_CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"  // char > property notify

#define LDR_SERVICE_UUID "cfa0bdb3-c363-405d-a6b7-f22d80bb894d"
#define LDR_CHARACTERISTIC_UUID "381ba698-d265-4a18-b551-97302808e067"  // char > property notify

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

  pinMode(BUTTON_PIN, INPUT);
  pinMode(LDR_PIN, INPUT);
  BLEDevice::init("ESP32");

  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Create button service
  BLEService *pButtonService = pServer->createService(BUTTON_SERVICE_UUID);
  pCharacteristic = pButtonService->createCharacteristic(BUTTON_CHARACTERISTIC_UUID, BLECharacteristic::PROPERTY_NOTIFY);
  pCharacteristic->addDescriptor(new BLE2902());
  descriptor_2901 = new BLE2901();
  descriptor_2901->setDescription("BUTTON");
  pCharacteristic->addDescriptor(descriptor_2901);
  pButtonService->start();  // Start button service

  // // Create LDR service
  // BLEService *pLDRService = pServer->createService(LDR_SERVICE_UUID);
  // pCharacteristic = pLDRService->createCharacteristic(LDR_CHARACTERISTIC_UUID, BLECharacteristic::PROPERTY_NOTIFY);
  // pCharacteristic->addDescriptor(new BLE2902());
  // descriptor_2902 = new BLE2901();
  // descriptor_2902->setDescription("PhotoResistor");
  // pCharacteristic->addDescriptor(descriptor_2902);
  // pLDRService->start();  // Start LDR service

  // Start advertising
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(BUTTON_SERVICE_UUID);
  // pAdvertising->addServiceUUID(LDR_SERVICE_UUID);  // Add LDR service to advertising
  BLEDevice::startAdvertising();
}

void loop() {
  ButtonValue = analogRead(BUTTON_PIN);
  if (deviceConnected) {
    toByte();
    pCharacteristic->setValue(dataPacket, 4);
    if (millis() - StartMillis > 1000) {
      pCharacteristic->notify();
      StartMillis = millis();
    }
  }
}

void toByte() {
  dataPacket[0] = (ButtonValue >> 8) & 0xFF; // High byte
  dataPacket[1] = ButtonValue & 0xFF;        // Low byte
  dataPacket[2] = (LDRValue >> 8) & 0xFF; // Placeholder for additional data
  dataPacket[3] = LDRValue & 0xFF; // Placeholder for additional data
}
