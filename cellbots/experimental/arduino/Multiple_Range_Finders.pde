#include <Servo.h>
Servo servo; // create servo object to control a servo

int ultraSoundSignalPins[] = {7,8,9,12}; // Front Left,Front, Front Right, Rear Ultrasound signal pins
char *pingString[] = {"FL ","F ", "FR ", "R "}; // just something to print to indicate direction

void setup()
{
  servo.attach(10); // attaches a servo to pin 10
  Serial.begin(9600);
}

void loop()
{
  unsigned long ultrasoundValue;
  for(int i=0; i < 4; i++)
  {
    ultrasoundValue = ping(i);
    Serial.print(pingString[i]);
    Serial.print(ultrasoundValue);
    Serial.print("in, ");    
    delay(20);
  }
  Serial.println();
  delay(20);
 }

//Ping function
unsigned long ping(int i)
{
  unsigned long echo;

  pinMode(ultraSoundSignalPins[i], OUTPUT); // Switch signalpin to output
  digitalWrite(ultraSoundSignalPins[i], LOW); // Send low pulse
  delayMicroseconds(2); // Wait for 2 microseconds
  digitalWrite(ultraSoundSignalPins[i], HIGH); // Send high pulse
  delayMicroseconds(5); // Wait for 5 microseconds
  digitalWrite(ultraSoundSignalPins[i], LOW); // Holdoff
  pinMode(ultraSoundSignalPins[i], INPUT); // Switch signalpin to input
  digitalWrite(ultraSoundSignalPins[i], HIGH); // Turn on pullup resistor
  echo = pulseIn(ultraSoundSignalPins[i], HIGH); //Listen for echo
  return (echo / 58.138) * .39; //convert to CM then to inches
}
 

