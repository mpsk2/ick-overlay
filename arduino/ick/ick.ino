#include <stdio.h>

const unsigned int voltage = 5000;

class ForceSensor {
private:
  const int number;
  int sum;
  int count;
  int minimum;
  int maximum;
  unsigned long startTime;
  unsigned long endTime;
public:
  ForceSensor(const int& _number) : 
    number(_number) {
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
      "n=%d,s=%d,c=%d,min=%d,max=%d,t=%u",
      this->number,
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
};

ForceSensor fs(1);

void setup() {
  Serial.begin(9600);
}

void loop() {
  delay(500);
  fs.addRecord(15);
  fs.sendSerialized(Serial);
}
