#include <stdio.h>
#include <SoftwareSerial.h>

const unsigned int voltage = 5000;

class ForceSensor {
private:
  const int analogPin;
  int sum;
  int count;
  int minimum;
  int maximum;
  unsigned long startTime;
  unsigned long endTime;
public:
  ForceSensor(const int& _analogPin) : 
    analogPin(_analogPin) {
    this->reset();
  }
  
  void reset() {
    this->sum = 0;
    this->count = 0;
    this->minimum = voltage;
    this->maximum = 0;
  }
  
  /** Add record to sensor measurrements
  
  record is number in 0..1023. We do scale it, as we do not 
  need scaling, but we need proportions between sensors
  */
  void addRecord(const int& record) {
    if (this->count == 0) {
      this->startTime = millis();
    }
    this->count++;
    this->sum += record;
    this->minimum = min(record, this->minimum);
    this->maximum = max(record, this->maximum);
    this->endTime = millis();
  }
  
  void sendSerialized(Stream& stream) {
    char str[128];
    
    sprintf(
      str, 
      "p=%d,s=%d,c=%d,min=%d,max=%d,t=%u",
      this->analogPin,
      this->sum,
      this->count,
      this->minimum,
      this->maximum,
      this->timeElapsed()
    );
    
    stream.println(str);
  }
  
  unsigned long timeElapsed() {
    return this->endTime - this->startTime;
  }
  
  void readAnalog() {
    int record = analogRead(this->analogPin);
    this->addRecord(record);
  }
};

class ForceSensors {
private:
  ForceSensor sensors[4] = {
    ForceSensor(0),
    ForceSensor(1),
    ForceSensor(2),
    ForceSensor(3)
  };
  int stepCount;
  int sendStep;
  int delayMilliseconds;
public:
  static const int sensorsCount = 4;
  
  ForceSensors() {
    this->reset();
    this->sendStep = 20;
    this->delayMilliseconds = 50;
  }
  
  void reset() {
    this->stepCount = 0;
    for (int i = 0; i < this->sensorsCount; i++) {
      this->sensors[i].reset();
    }
  }
  
  void readAnalog() {
    for (int i = 0; i < this->sensorsCount; i++) {
      this->sensors[i].readAnalog();
    }
  }  
  
  void sendSerialized(Stream& stream) {
    for (int i = 0; i < this->sensorsCount; i++) {
      this->sensors[i].sendSerialized(stream);
    }
  }
  
  void step(Stream& stream) {
    this->readAnalog();
    this->stepCount++;
    if (this->stepCount >= this->sendStep) {
      this->sendSerialized(stream);
      this->reset();
    }
    delay(this->delayMilliseconds);
  }
};

ForceSensors fs;
const int BT_RX_PIN = 2;
const int BT_TX_PIN = 4;
SoftwareSerial bt(BT_RX_PIN, BT_TX_PIN);

void setup() {
  Serial.begin(9600);
}

void loop() {
  fs.step(bt);
}
