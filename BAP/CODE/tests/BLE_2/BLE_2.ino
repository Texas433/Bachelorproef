#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_BME680.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

// Instantieer BME680 sensor
Adafruit_BME680 bme;

// BLE variabelen
BLECharacteristic *pCharacteristic = NULL;
bool deviceConnected = false;
unsigned long lastSendTime = 0;  // Voor het bijhouden van de tijd

class MyServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    deviceConnected = true;
  }

  void onDisconnect(BLEServer* pServer) {
    deviceConnected = false;
  }
};

void setup() {
  Serial.begin(115200);

  // Initialiseer de BME680 sensor
  if (!bme.begin()) {
    Serial.println("Kan de BME680 sensor niet vinden!");
    while (1);
  }
  // Stel de sensor instellingen in
  bme.setTemperatureOversampling(BME680_OS_8X);
  bme.setHumidityOversampling(BME680_OS_2X);
  bme.setPressureOversampling(BME680_OS_4X);
  bme.setIIRFilterSize(BME680_FILTER_SIZE_3);

  // Initialiseer BLE
  BLEDevice::init("ESP32 BME680");
  BLEServer *pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  BLEService *pService = pServer->createService("12345678-1234-1234-1234-123456789abc");
  pCharacteristic = pService->createCharacteristic( "87654321-4321-4321-4321-abc123456789", BLECharacteristic::PROPERTY_NOTIFY);
  pCharacteristic->addDescriptor(new BLE2902());

  pService->start();
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(pService->getUUID());
  BLEDevice::startAdvertising();
}

void sendSensorData() {
  // Meet de waarden
  if (!bme.performReading()) {
    Serial.println("Sensor reading mislukt!");
    return;
  }

  // Verkrijg temperatuur, vochtigheid en druk
  float temperature = bme.temperature;
  float humidity = bme.humidity;
  float pressure = bme.pressure / 100.0;  // Omzetten naar hPa

  // Maak een geformatteerde string (CSV-formaat)
  char dataPacket[50];
  snprintf(dataPacket, sizeof(dataPacket), "%.2f,%.2f,%.2f", temperature, humidity, pressure);

  // Stuur de string via BLE
  pCharacteristic->setValue(dataPacket);
  pCharacteristic->notify();

  // Debug output om te zien wat er wordt verstuurd
  Serial.print("Verzonden data: ");
  Serial.println(dataPacket);
}

void loop() {
  // Verzend data elke seconde
  if (deviceConnected && (millis() - lastSendTime > 1000)) {
    sendSensorData();
    lastSendTime = millis();  // Update de laatste verzendtijd
  }
}
