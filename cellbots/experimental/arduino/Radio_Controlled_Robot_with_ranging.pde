// Reads PPM signals from 2 channels of an RC reciever and converts the values to PWM in either direction.
//
// Based on JDW's 2010 LawnBot code which drove motors instead of servos. See his site at www.rediculouslygoodlooking.com
// We do this by using interupts to read the radio signal value in millisecond pulses and then
// use map() to take the incoming radio signal(usually 1000-2000) and turn it into pwm signals (usually 40-145).
// Plus there is a lot of sanity checking to deal with noise and keep things within valid ranges.
//
// This version by Ryan Hickman from Cellbots.com to make R/C car robots that talk to cellphones

#include <Servo.h> 

// Inbound signals from receiver
int steering_channel = 2;  // r/c channel 1 (steering)
int throttle_channel = 3;  // r/c channel 2 (throttle)

// Outbound steering and motor servos
const int steering_servo_pin = 10;
const int throttle_servo_pin = 11;
Servo steering_servo;
Servo throttle_servo;

// adjust these values if your R/C normal readings are above or below these for channels 1 and 2.
int steering_low = 1000;
int steering_center = 1500;
int steering_high = 2000;
int throttle_low = 860;
int throttle_center = 1500;
int throttle_high = 2000;

// adjust these values for the range your servos best respond too (no need cranking sterring servo more than it should be)
int steering_servo_low = 40; //right turn
int steering_servo_center = 90; //center
int steering_servo_high = 145; //left turn
int throttle_servo_low = 65; //backward (temp raised from 40)
int throttle_servo_center = 93; //neutral
int throttle_servo_high = 125; //forward (temp lowered fro 145)

// For ultrasonic range finding
int ultraSoundSignalPins[] = {7,8,9,12}; // Front Left,Front, Front Right, Rear Ultrasound signal pins
unsigned long echo;
int front_left_range;
int front_range;
int front_right_range;
int rear_range;
int loop_count = 0;

// non-configurable values for reading and writing radio signals
unsigned int relay_val;
volatile unsigned long steering_channel_startPulse;  //values for channel 1 signal capture
volatile unsigned steering_channel_val; 
volatile int steering_adjusted;  
volatile int steering_channel_Ready;
volatile unsigned long throttle_channel_startPulse; //values for channel 2 signal capture
volatile unsigned throttle_channel_val; 
volatile int throttle_adjusted;  
volatile int throttle_channel_Ready;
int prev_steering_adjusted = steering_servo_center;
int prev_throttle_adjusted = throttle_servo_center;

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
  
  // Read the range finder values to set initial values
  delay(50);
  front_left_range = ping(0);
  delay(50);
  front_range = ping(1);
  delay(50);
  front_right_range = ping(2);
  delay(50);
  rear_range = ping(3);
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

//Ping function
unsigned long ping(int i)
{
  pinMode(ultraSoundSignalPins[i], OUTPUT); // Switch signalpin to output
  digitalWrite(ultraSoundSignalPins[i], LOW); // Send low pulse
  delayMicroseconds(2); // Wait for 2 microseconds
  digitalWrite(ultraSoundSignalPins[i], HIGH); // Send high pulse
  delayMicroseconds(5); // Wait for 5 microseconds
  digitalWrite(ultraSoundSignalPins[i], LOW); // Holdoff
  pinMode(ultraSoundSignalPins[i], INPUT); // Switch signalpin to input
  digitalWrite(ultraSoundSignalPins[i], HIGH); // Turn on pullup resistor
  echo = pulseIn(ultraSoundSignalPins[i], HIGH, 500000); //Listen for echo but don't wait more than 0.1 seconds
  return (echo / 58.138) * .39; //convert to CM then to inches
}

// Update PWN values on servos
void control_car(){
  steering_servo.write(steering_adjusted);
  throttle_servo.write(throttle_adjusted);
}

/////// MAIN LOOP
void loop() {
  //we do an inner loop to only read one range finder per pass (eliminates putting in a fixed delays between them for faster processing)
  for(int i=0; i < 4; i++){
  
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
  
  // Read only one of the range finder values per pass to avoid collisions
  if (i == 0){front_left_range = ping(0);}; // front left
  if (i == 1){front_range = ping(1);}; // front
  if (i == 2){front_right_range = ping(2);}; // front right
  if (i == 3){rear_range = ping(3);}; // rear
  
  // Determine if steering adjustment needed
  if (front_left_range < 24 and front_right_range > 24){
    steering_adjusted = steering_servo_low; // low values for right turn so low left distance results in tight right steering
  }
  if (front_right_range < 24 and front_left_range > 24){
    steering_adjusted = steering_servo_high; // high values for left turn so high right distance results in tight left steering
  }
  
  // Determine if throttle adjustment needed (only applies if close while attemping to drive it into danger)
  if (front_range < 50 and throttle_adjusted > 95 and prev_throttle_adjusted > 95){
    throttle_adjusted = throttle_servo_low; // low values for braking so low front distance results in higher braking value
  }
  if (rear_range < 50 and throttle_adjusted < 90 and prev_throttle_adjusted < 90){
    throttle_adjusted = throttle_servo_high; // high values for acceleration so low rear distance results in faster acceleration
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

  //Print steering and throttle values to serial monitor in Arduino IDE
  Serial.print(i);
  Serial.print("FL:  ");
  Serial.print(front_left_range);
  Serial.print("  ");
  Serial.print("F:  ");
  Serial.print(front_range);
  Serial.print("  ");
  Serial.print("FR:  ");
  Serial.print(front_right_range);
  Serial.print("  ");
  Serial.print("R:  ");
  Serial.print(rear_range);
  Serial.print("  ");
  
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

  control_car();
  
  // capture the values from this run to compare for the next pass
  prev_steering_adjusted = steering_adjusted;
  prev_throttle_adjusted = throttle_adjusted;
  } // end of inner loop
}

