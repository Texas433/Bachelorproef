#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <BLE2901.h>


BLEServer *pServer = NULL;
BLECharacteristic *pCharacteristic = NULL;
BLE2901 *descriptor_2901 = NULL;

bool deviceConnected = false;
bool oldDeviceConnected = false;
int BUTTON_PIN = 12;
long StartMillis;

#define BUTTON_SERVICE_UUID "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define BUTTON_CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"  // char > property notify

class MyServerCallbacks : public BLEServerCallbacks {  //callback => klasse met ftes wanneer er een verandering in connectie plaatsvindt ( / !)
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
  BLEDevice::init("ESP32");  // die :: zorgt ervoor dat de init vanuit de bledevice class wordt opgeroepen en geen globale init


  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  BLEService *pService = pServer->createService(BUTTON_SERVICE_UUID);


  pCharacteristic = pService->createCharacteristic(BUTTON_CHARACTERISTIC_UUID, BLECharacteristic::PROPERTY_NOTIFY);
  //BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE | | BLECharacteristic::PROPERTY_INDICATE

  pCharacteristic->addDescriptor(new BLE2902()); //Client characteristic descriptor
  descriptor_2901 = new BLE2901(); // user characteristic descriptor 
  descriptor_2901->setDescription("PhotoResistor");
  pCharacteristic->addDescriptor(descriptor_2901);

  pService->start();

  // Start advertising
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(BUTTON_SERVICE_UUID);
  BLEDevice::startAdvertising();

}

void loop() {
  int buttonValue = analogRead(BUTTON_PIN);
  if (deviceConnected) {
    char ValStr[3];
    dtostrf(buttonValue,2,0,ValStr);
    pCharacteristic->setValue(ValStr);
    if (millis() - StartMillis > 1000) {
      pCharacteristic->notify();
      StartMillis = millis();
    }
  }

  // // Disconnect logic
  // if (!deviceConnected && oldDeviceConnected) {
  //   delay(500);
  //   pServer->startAdvertising();
  //   Serial.println("Start advertising");
  //   oldDeviceConnected = deviceConnected;
  // }

  // // Connect logic
  // if (deviceConnected && !oldDeviceConnected) {
  //   oldDeviceConnected = deviceConnected;
  // }
}
