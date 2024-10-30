long StartMillis;
float tempC;

int LMT = 12;

void setup() {
  Serial.begin(115200);
  pinMode(LMT, INPUT);

}

void loop() {
if (millis()- StartMillis >=1000){
  tempC = analogRead(LMT);
  Serial.println(tempC);
  StartMillis = millis();
}
  // LMTinput();

}

// void LMTinput() {
//   if (millis() - StartMillis >= 1000) {  // om de seconde wordt er een nieuwe waarde gelezen enz

//     tempC = ((2103 - (analogRead(LMT) * 3.3 / 1024.0) * 1000)) / (10.9);
//    Serial.println(tempC);
//   StartMillis = millis();
//   }
// }