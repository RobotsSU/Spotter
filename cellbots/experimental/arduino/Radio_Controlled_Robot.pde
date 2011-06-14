// Reads PPM signals from 2 channels of an RC reciever and converts the values to PWM in either direction.
//
// Based on JDW's 2010 LawnBot code which drove motors instead of servos. See his site at www.rediculouslygoodlooking.com
// We do this by using interupts to read the radio signal value in millisecond pulses and then
// use map() to take the incoming radio signal(usually 1000-2000) and turn it into pwm signals (usually 40-145).
// Plus there is a lot of sanity checking to deal with noise and keep things within valid ranges.
//
// This version by Ryan Hickman from Cellbots.com to make R/C car robots that talk to cellphones

#include <Servo.h> 

// Operating modes
boolean DEBUGGING = false; // Whether debugging output over serial is on by defauly (can be flipped with 'h' command)
boolean serial_control_mode = false; // Determines if radio control is overridden by serial commands
#define BUFFERSIZE 20
char inBytes[BUFFERSIZE]; //Buffer for serial in messages
int serialIndex = 0; 
int serialAvail = 0;

// Inbound signals from receiver
int steering_channel = 2;  // r/c channel 1 (steering)
int throttle_channel = 3;  // r/c channel 2 (throttle)

// Outbound steering and motor servos
const int steering_servo_pin = 10;
const int throttle_servo_pin = 11;
Servo steering_servo;
Servo throttle_servo;

unsigned int relay_val;

volatile unsigned long steering_channel_startPulse;  //values for channel 1 signal capture
volatile unsigned steering_channel_val; 
volatile int steering_adjusted;  
volatile int steering_channel_Ready;

volatile unsigned long throttle_channel_startPulse; //values for channel 2 signal capture
volatile unsigned throttle_channel_val; 
volatile int throttle_adjusted;  
volatile int throttle_channel_Ready;

// adjust these values if your R/C normal readings are above or below these for channels 1 and 2.
int steering_low = 1000;
int steering_center = 1500;
int steering_high = 2000;
int throttle_low = 860;
int throttle_center = 1500;
int throttle_high = 2000;

// adjust these values for the range your servos best respond too (no need cranking sterring servo more than it should be)
int steering_servo_low = 40;
int steering_servo_high = 145;
int throttle_servo_low = 40;
int throttle_servo_high = 145;

void setup() {
  
  Serial.begin(9600);
  
  //PPM inputs from RC receiver
  pinMode(steering_channel, INPUT); //Pin 2 as input
  pinMode(throttle_channel, INPUT); //Pin 3 as input
  attachInterrupt(0, steering_begin, RISING);    // catch interrupt 0 (digital pin 2) going HIGH and send to steering()
  attachInterrupt(1, thottle_begin, RISING);    // catch interrupt 1 (digital pin 3) going HIGH and send to thottle()

  //Steering and throttle output pins
  pinMode(steering_servo_pin, OUTPUT);
  pinMode(throttle_servo_pin, OUTPUT);
  steering_servo.attach(steering_servo_pin);
  throttle_servo.attach(throttle_servo_pin);
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
       parseCommand(inBytes); 
       serialIndex = 0;
    }
    else {
      //expecting more of the command to come later.
      serialIndex += serialAvail;
    }
  }  
}

////////// attach servo signal interrupts to catch signals as they go HIGH then again as they go LOW, then calculate the pulse length.
void steering_begin() {           // enter steering_begin when interrupt pin goes HIGH.
  steering_channel_startPulse = micros();     // record microseconds() value as steering_channel_startPulse
  detachInterrupt(0);  // after recording the value, detach the interrupt from steering_begin
  attachInterrupt(0, steering_end, FALLING); // re-attach the interrupt as steering_end, so we can record the value when it goes low
}

void steering_end() {
  steering_channel_val = micros() - steering_channel_startPulse;  // when interrupt pin goes LOW, record the total pulse length by subtracting previous start value from current micros() vlaue.
  detachInterrupt(0);  // detach and get ready to go HIGH again
  attachInterrupt(0, steering_begin, RISING);
}

void thottle_begin() {
  throttle_channel_startPulse = micros();
  detachInterrupt(1);
  attachInterrupt(1, thottle_end, FALLING);
}

void thottle_end() {
  throttle_channel_val = micros() - throttle_channel_startPulse;
  detachInterrupt(1);
  attachInterrupt(1, thottle_begin, RISING); 
}
/////// servo interrupts end

// Update PWN values on servos
void control_car(){
  steering_servo.write(steering_adjusted);
  throttle_servo.write(throttle_adjusted);
}

// Cleans and parses the command
void parseCommand(char* com) {
  if (com[0] == '\0') { return; } //bit of error checking
  int start = 0;
  //get start of command
  while (com[start] != '<'){
    start++; 
    if (com[start] == '\0') {
      //its not there. Must be old version
      start = -1;
      break;
    }
  }
  start++;
  //Shift to beginning
  int i = 0;
  while (com[i + start - 1] != '\0') {
    com[i] = com[start + i];
    i++; 
  } 
  performCommand(com);
}

void performCommand(char* com) {  
  if (strcmp(com, "h") == 0) { // Help mode - debugging toggle
    // Print out some basic instructions when first turning on debugging
    if (not DEBUGGING) {
      Serial.println("Debugging is now on.");
    }
    DEBUGGING = !DEBUGGING;
  }
  else if (strcmp(com, "s") == 0) { // Serial drive toggle
    serial_control_mode = !serial_control_mode;
    if (serial_control_mode) {
      Serial.println("Serial control mode is now on.");
    }
    else{
      Serial.println("Serial control mode is now off.");
    }
  } else if (com[0] == 'r') { // Handle "wheel" command and translate into PWM values ex: "w -100 100" [range is from -100 to 100]
    int valueLeft=90, valueRight=90;
    sscanf (com,"r %d %d",&steering_channel_val, &throttle_channel_val); // Parse the input into multiple values
    steering_adjusted = map(steering_channel_val, steering_low, steering_high, steering_servo_low, steering_servo_high); 
    throttle_adjusted = map(throttle_channel_val, throttle_low, throttle_high, throttle_servo_low, throttle_servo_high); 
  } else { 
    if (DEBUGGING) {
      Serial.print("Unknown command: ");
      Serial.println(com);
    }
  }
}

/////// MAIN LOOP
void loop() {

  if ( not serial_control_mode){
    ///// channel 1 good signal check
    if (steering_channel_val < 600 || steering_channel_val > 2400) {  // only set the steering_channel_Ready flag if the value is a valid Servo pulse between 600-2400 microseconds.
      steering_channel_Ready = false; 
      steering_channel_val = steering_center;
     }
    else {
      steering_channel_Ready = true; // if not, don't pass the value to be processed
     }
  
    ///// channel 2 good signal check
    if (throttle_channel_val < 600 || throttle_channel_val > 2400) {
      throttle_channel_Ready = false; 
      throttle_channel_val = throttle_center;
     }
    else {
      throttle_channel_Ready = true; 
     }
  
    ////////// Steering	         
    if (steering_channel_Ready) {
      
      steering_channel_Ready = false;  
      steering_adjusted = map(steering_channel_val, steering_low, steering_high, steering_servo_low, steering_servo_high); 
      constrain(steering_adjusted, steering_servo_low, steering_servo_high);
    }
      
    ///////////// Throttle
   if (throttle_channel_Ready) {
      
      throttle_channel_Ready = false;
      throttle_adjusted = map(throttle_channel_val, throttle_low, throttle_high, throttle_servo_low, throttle_servo_high); 
      constrain(throttle_adjusted, throttle_servo_low, throttle_servo_high);
    }
  }
  
  // Ensure the sero values are still within range
  if (steering_adjusted < steering_servo_low) {
    steering_adjusted = steering_servo_low;
  }     
  if (steering_adjusted > steering_servo_high) {
    steering_adjusted = steering_servo_high;
  }
  if (throttle_adjusted < throttle_servo_low) {
    throttle_adjusted = throttle_servo_low;
  }     
  if (throttle_adjusted > throttle_servo_high) {
    throttle_adjusted = throttle_servo_high;
  }
  
  if (DEBUGGING) {
  //Print steering and throttle values to serial monitor in Arduino IDE
  Serial.print("steer:  ");
  Serial.print(steering_adjusted);
  Serial.print("  ");
  Serial.print("ch1:  ");
  Serial.print(steering_channel_val);
  Serial.print("  ");
  Serial.print("throttle:  ");
  Serial.print(throttle_adjusted);
  Serial.print("  ");
  Serial.print("ch2:  ");
  Serial.print(throttle_channel_val);
  Serial.println();
  }

  control_car();
  readSerialInput();
}

