#include <Servo.h> 
#include <stdlib.h>
#define BUFFERSIZE 20

// ** GENERAL SETTINGS ** - General preference settings
boolean DEBUGGING = false; // Whether debugging output over serial is on by defauly (can be flipped with 'h' command)
const int ledPin = 13; // LED turns on while running servos

// ** SERVO SETTINGS ** - Configurable values based on pins used and servo direction
const int servoPinLeft = 3;
const int servoPinRight = 5;
const int servoDirectionLeft = 1; // Use either 1 or -1 for reverse
const int servoDirectionRight = -1; // Use either 1 or -1 for reverse
int servoCenterLeft = 90; // PWM setting for no movement on left servo
int servoCenterRight = 90; // PWM setting for no movement on right servo
int servoPowerRange = 30; // PWM range off of center that servos respond best to (set to 30 to work in the 60-120 range off center of 90)
const long maxRunTime = 2000; // Maximum run time for servos without additional command. * Should use a command to set this. *
int speedMultiplier = 5; // Default speed setting. Uses a range from 1-10

// ** RANGE FINDING *** - The following settings are for ultrasonic range finders. OK to lave as-is if you don't have them on your robot
long dist, microseconds, cm, inches; // Used by the range finder for calculating distances
const int rangePinForward = 7; // Digital pin for the forward facing range finder (for object distance in front of bot)
const int rangeToObjectMargin = 0; // Range in cm to forward object (bot will stop when distance closer than this - set to 0 if no sensor)
const int rangePinForwardGround = 8; // Digital pin for downward facing range finder on the front (for edge of table detection)
const int rangeToGroundMargin = 0; // Range in cm to the table (bot will stop when distance is greater than this  set to 0 if no sensor)
const int rangeSampleCount = 3; // Number of range readings to take and average for a more stable value

// Create servo objects to control the servos
Servo myservoLeft;
Servo myservoRight;

// No config required for these parameters
boolean servosActive = false; // assume servos are not moving when we begin
boolean servosForcedActive = false; // will only stop when considered dangerous
unsigned long stopTime=millis(); // used for calculating the run time for servos
char incomingByte; // Holds incoming serial values
char msg[8]; // For passing back serial messages
char inBytes[BUFFERSIZE]; //Buffer for serial in messages
int serialIndex = 0; 
int serialAvail = 0;

int out_A_pwm = 3; //pwm of motor A
int out_A_in_1 = 9;
int out_A_in_2 = 8;
int out_B_pwm = 5; //pwm of motor B
int out_B_in_1 = 10;
int out_B_in_2 = 11;
int out_stby = 12;
int PWMvalue = 200;


void setup(){

  pinMode(out_A_pwm,OUTPUT);
  pinMode(out_A_in_1,OUTPUT);
  pinMode(out_A_in_2,OUTPUT);
  pinMode(out_B_pwm,OUTPUT);
  pinMode(out_B_in_1,OUTPUT);
  pinMode(out_B_in_2,OUTPUT);
  pinMode(out_stby,OUTPUT);
  Serial.begin(9600);
  digitalWrite(out_stby,HIGH);
    
}

// Reads serial input if available and parses command when full command has been sent. 
void readSerialInput() {
  serialAvail = Serial.available();
  //Read what is available
  for (int i = 0; i < serialAvail; i++) {
    //Store into buffer.
    inBytes[i + serialIndex] = Serial.read();
    //Check for command end. 
    
    if (inBytes[i + serialIndex] == '\n' || inBytes[i + serialIndex] == ';' || inBytes[i + serialIndex] == '>') { //Use ; when using Serial Monitor
       inBytes[i + serialIndex] = '\0'; //end of string char
       driveMotors(inBytes); 
       serialIndex = 0;
    }
    else {
      //expecting more of the command to come later.
      serialIndex += serialAvail;
    }
  }  
}

void driveMotors(char* inBytes){
  int valueLeft=0, valueRight=0, PWMLeft, PWMRight;
  sscanf (inBytes,"w %d %d", &valueLeft, &valueRight); // Parse the input into multiple values
  PWMLeft = map(abs(valueLeft), 0, 100, 0, 255); // Maps to the wider range that the motor responds to
  PWMRight = map(abs(valueRight), 0, 100, 0, 255);
  // Set left motor pins to turn in the desired direction
  if (valueLeft < 0){
    digitalWrite(out_A_in_1,LOW);
    digitalWrite(out_A_in_2,HIGH);
  }
  else {
    digitalWrite(out_A_in_1,HIGH);
    digitalWrite(out_A_in_2,LOW);
  }
  // Set right motor pins to turn in the desired direction
  if (valueRight < 0){
    digitalWrite(out_B_in_1,LOW);
    digitalWrite(out_B_in_2,HIGH);
  }
  else {
    digitalWrite(out_B_in_1,HIGH);
    digitalWrite(out_B_in_2,LOW);
  }
  analogWrite(out_A_pwm,PWMLeft);
  analogWrite(out_B_pwm,PWMRight);
} 

void loop(){
  readSerialInput();
}


