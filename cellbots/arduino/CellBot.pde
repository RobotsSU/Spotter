/*
  Servo driven robot commanded by serial input
 
 Looks for a set of ASCII characters in the signal to send
 commands to a set of servos to drive a small robot. LED pin #13
 will remain lit during servo movement and blink for speed changes.
 
 
 The minimum circuit:
 * LED attached from pin 13 to ground (or use built-in LED on most Arduino's)
 * Servos with signal wires connected to pins 3 and 5 (5v power and ground for
 servos can also be wired into Arduino, or power can come from external source)
 * Serial input connected to RX pin 0
 * Serial output connected to TX pin 1
 
 Additional circuits (optional):
 * Forward facing ultrasonic range finder on digital pin 7
 * Downward facing ultrasonic range finder on digital pin 8
 
 Note: If you don't yet have a serial device to connect with, you can use the 
 built in Serial Monitor in the Arduino software when connect via USB for testing.
 Also, be sure to disconect RX & TX pins from other devices when trying to program
 the Arduino over USB.
 
 created 2010
 by Tim Heath, Ryan Hickman, and Glen Arrowsmith
 Visit http://www.cellbots.com for more information
 */

#include <Servo.h> 
#include <EEPROM.h>

#define BUFFERSIZE 20
#define EEPROM_servoCenterLeft 1
#define EEPROM_servoCenterRight 2
#define EEPROM_speedMultiplier 3
#define EEPROM_servosForcedActive 4
#define EEPROM_lastNeckValue 5

#define DEFAULT_servoCenterLeft 90
#define DEFAULT_servoCenterRight 90
#define DEFAULT_speedMultiplier 5
#define DEFAULT_servosForcedActive false
#define DEFAULT_servosForcedActive false
#define DEFAULT_lastNeckValue 255

// ** GENERAL SETTINGS ** - General preference settings
boolean DEBUGGING = false; // Whether debugging output over serial is on by defauly (can be flipped with 'h' command)
const int ledPin = 13; // LED turns on while running servos
char* driveType = "servo"; // Use "servo" for servos powering the wheels; "motor" when bots has a DC motor driver; "dfrobot" for the kit from RobotShop

// ** SERVO SETTINGS ** - Configurable values based on pins used and servo direction
const int servoPinLeft = 3;
const int servoPinRight = 5;
const int servoPinHead = 12; // Servo controlling the angle of the phone
const int servoDirectionLeft = 1; // Use either 1 or -1 for reverse
const int servoDirectionRight = -1; // Use either 1 or -1 for reverse
int servoCenterLeft = DEFAULT_servoCenterLeft; // PWM setting for no movement on left servo
int servoCenterRight = DEFAULT_servoCenterLeft; // PWM setting for no movement on right servo
int servoPowerRange = 30; // PWM range off of center that servos respond best to (set to 30 to work in the 60-120 range off center of 90)
const long maxRunTime = 2000; // Maximum run time for servos without additional command. * Should use a command to set this. *
int speedMultiplier = DEFAULT_speedMultiplier; // Default speed setting. Uses a range from 1-10
int lastNeckValue = DEFAULT_lastNeckValue;

// ** MOTOR DRIVER SETTINGS ** - For use with boards like the Pololu motor driver (also uses left/right servo pin settings above)
int leftMotorPin_1 = 9;
int leftMotorPin_2 = 8;
int rightMotorPin_1 = 10;
int rightMotorPin_2 = 11;
int motor_stby = 12;

// ** DF Robot Motor Pin Settings ** - For the DF Robot from RobotShop.com (the pins aren't adjustable on the board)
int m1SpeedControl = 6; //M1 Speed Control
int m2SpeedControl = 5; //M2 Speed Control
int m1DirectionControl = 8; //M1 Direction Control
int m2DirectionControl = 7; //M2 Direction Control

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
Servo myservoHead;

// No config required for these parameters
boolean servosActive = false; // assume servos are not moving when we begin
boolean servosForcedActive = DEFAULT_servosForcedActive; // will only stop when considered dangerous
unsigned long stopTime=millis(); // used for calculating the run time for servos
char incomingByte; // Holds incoming serial values
char msg[8]; // For passing back serial messages
char inBytes[BUFFERSIZE]; //Buffer for serial in messages
int serialIndex = 0; 
int serialAvail = 0;

void setup() {
  pinMode(servoPinLeft, OUTPUT);
  pinMode(servoPinRight, OUTPUT);
  pinMode(servoPinHead, OUTPUT);
  pinMode(leftMotorPin_1,OUTPUT);
  pinMode(leftMotorPin_2,OUTPUT);
  pinMode(rightMotorPin_1,OUTPUT);
  pinMode(rightMotorPin_2,OUTPUT);
  if (driveType == "dfrobot"){
    pinMode(m1SpeedControl,OUTPUT);
    pinMode(m2SpeedControl,OUTPUT);
    pinMode(m1DirectionControl,OUTPUT);
    pinMode(m2DirectionControl,OUTPUT);
  }
  pinMode(ledPin, OUTPUT);
  digitalWrite(servoPinLeft,0);
  digitalWrite(servoPinRight,0);
  digitalWrite(servoPinHead,0);
  digitalWrite(motor_stby,HIGH);
  Serial.begin(9600);
  servoCenterLeft = readSetting(EEPROM_servoCenterLeft, servoCenterLeft);
  servoCenterRight = readSetting(EEPROM_servoCenterRight, servoCenterRight);
  speedMultiplier = readSetting(EEPROM_speedMultiplier, speedMultiplier);  
  servosForcedActive = readSetting(EEPROM_servosForcedActive, servosForcedActive);  
  lastNeckValue = readSetting(EEPROM_lastNeckValue, lastNeckValue);
  if (lastNeckValue != DEFAULT_lastNeckValue) {
    myservoHead.attach(servoPinHead);
    myservoHead.write(lastNeckValue);
  }
} 

//Safely reads EEPROM
int readSetting(int memoryLocation, int defaultValue) {
  int value = EEPROM.read(memoryLocation);
  if (value == 255) { 
    EEPROM.write(memoryLocation, defaultValue);    
  }
  return value;  
}

//Sets the EEPROM settings to the default values
void setEepromsToDefault() {
  servosForcedActive = DEFAULT_servosForcedActive;
  speedMultiplier = DEFAULT_speedMultiplier;
  servoCenterRight = DEFAULT_servoCenterRight;
  servoCenterLeft = DEFAULT_servoCenterLeft;
  lastNeckValue = DEFAULT_lastNeckValue;
  EEPROM.write(EEPROM_servosForcedActive, DEFAULT_servosForcedActive);
  EEPROM.write(EEPROM_speedMultiplier, DEFAULT_speedMultiplier);
  EEPROM.write(EEPROM_servoCenterRight, DEFAULT_servoCenterRight);
  EEPROM.write(EEPROM_servoCenterLeft, DEFAULT_servoCenterLeft);
  EEPROM.write(EEPROM_lastNeckValue, DEFAULT_lastNeckValue);
  if (DEBUGGING) {
      Serial.println("All EEPROM values set to defaults.");
  }
}

// Convert directional text commands ("forward"/"backward") into calculated servo speed
int directionValue(char* directionCommand, int servoDirection) {
  if (directionCommand == "forward") {
    return (10 * speedMultiplier * servoDirection);
  }
  else if (directionCommand == "backward") {
    return (-10 * speedMultiplier * servoDirection);
  }
  else {
    if (DEBUGGING) { Serial.println("Houston, we have a problem!"); }
    return 0; // Attemp to set value to center - this shouldn't be needed
  }
}

// Translate text commands into PWM values for the bot to move (left servo command, right servo command)
unsigned long moveBot(char* commandLeft, char* commandRight) {
  int valueLeft = directionValue(commandLeft, servoDirectionLeft) + servoCenterLeft;
  int valueRight = directionValue(commandRight, servoDirectionRight) + servoCenterRight;
  driveWheels(valueLeft, valueRight);
}


// Drive servo or DC motors to move the robot using values in range -100 to 100 for left and right
unsigned long driveWheels(int valueLeft, int valueRight) {
  // Detach both servo pins which will stop whine and de-energize the motors so they don't kill the compass readings
  if (valueLeft == 0 and valueRight == 0){
    myservoLeft.detach();
    myservoRight.detach();
  }
  // Drive the wheels based on "servo" driveType
  if (driveType == "servo"){
    valueLeft = valueLeft * servoDirectionLeft; // Flip positive to negative if needed based on servo direction value setting
    valueRight = valueRight * servoDirectionRight;
    // Map "w" values to the narrow range that the servos respond to
    valueLeft = map(valueLeft, -100, 100, (servoCenterLeft - servoPowerRange), (servoCenterLeft + servoPowerRange));
    valueRight = map(valueRight, -100, 100, (servoCenterRight - servoPowerRange), (servoCenterRight + servoPowerRange));
    digitalWrite(ledPin, HIGH);   // set the LED on
    // Restart the servo PWM and send them commands
    myservoLeft.attach(servoPinLeft);
    myservoRight.attach(servoPinRight);
    myservoLeft.write(valueLeft);
    myservoRight.write(valueRight);
    // Spit out some diagnosis info over serial
    if (DEBUGGING) {
      Serial.print("Moving left servo ");
      Serial.print(valueLeft, DEC);
      Serial.print(" and right servo ");
      Serial.println(valueRight, DEC);
    }
  }
  // Drive the wheels based on "motor" driveType
  else if (driveType == "motor"){
    // Set left motor pins to turn in the desired direction
    if (valueLeft < 0){
      digitalWrite(leftMotorPin_1,LOW);
      digitalWrite(leftMotorPin_2,HIGH);
    }
    else {
      digitalWrite(leftMotorPin_1,HIGH);
      digitalWrite(leftMotorPin_2,LOW);
    }
    // Set right motor pins to turn in the desired direction
    if (valueRight < 0){
      digitalWrite(rightMotorPin_1,LOW);
      digitalWrite(rightMotorPin_2,HIGH);
    }
    else {
      digitalWrite(rightMotorPin_1,HIGH);
      digitalWrite(rightMotorPin_2,LOW);
    }
    // Maps "w" values to the wider range that the motor responds to
    valueLeft = map(abs(valueLeft), 0, 100, 0, 255);
    valueRight = map(abs(valueRight), 0, 100, 0, 255);
    analogWrite(servoPinLeft,valueLeft);
    analogWrite(servoPinRight,valueRight);
  }// Drive the wheels based on "dfrobot" driveType
  else if (driveType == "dfrobot"){
    // Set left motor pins to turn in the desired direction
    if (valueLeft < 0){
      digitalWrite(m1DirectionControl,HIGH);
    }
    else {
      digitalWrite(m1DirectionControl,LOW);
    }
    // Set right motor pins to turn in the desired direction
    if (valueRight < 0){
      digitalWrite(m2DirectionControl,HIGH);
    }
    else {
      digitalWrite(m2DirectionControl,LOW);
    }
    // Maps "w" values to the wider range that the motor responds to
    valueLeft = map(abs(valueLeft), 0, 100, 0, 255);
    valueRight = map(abs(valueRight), 0, 100, 0, 255);
    analogWrite(m1SpeedControl,valueLeft);
    analogWrite(m2SpeedControl,valueRight);
  }

  stopTime=millis() + maxRunTime; // Set time to stop running based on allowable running time
  return stopTime;
}

// Stop the bot
void stopBot() {
  driveWheels(0,0);
  digitalWrite(ledPin, LOW);  // Turn the LED off
  if (DEBUGGING) { Serial.println("Stopping both wheels"); }
  serialReply("i", "st"); // Tell the phone that the robot stopped
}

// Read and process the values from an ultrasonic range finder (you can leave this code in even if you don't have one)
long getDistanceSensor(int ultrasonicPin) {
  // Take multiple readings and average them
  microseconds = 0;
  for(int sample = 1 ; sample <= rangeSampleCount; sample ++) {
	// The Parallax PING))) is triggered by a HIGH pulse of 2 or more microseconds.
	// Give a short LOW pulse beforehand to ensure a clean HIGH pulse:
	// The Maxsonar does not seem to need this part but it does not hurt either
	pinMode(ultrasonicPin, OUTPUT);
	digitalWrite(ultrasonicPin, LOW);
	delayMicroseconds(2);
	digitalWrite(ultrasonicPin, HIGH);
	delayMicroseconds(5);
	digitalWrite(ultrasonicPin, LOW);

	// The same pin is used to read the signal from the ultrasonic detector: a HIGH
	// pulse whose duration is the time (in microseconds) from the sending
	// of the ping to the reception of its echo off of an object.
	pinMode(ultrasonicPin, INPUT);
    microseconds += pulseIn(ultrasonicPin, HIGH);
    delayMicroseconds(5); // Very short pause between readings
  }
  microseconds = microseconds / rangeSampleCount;
  // Convert the averaged sensor reading to centimeters and return it
  cm = microsecondsToCentimeters(microseconds);
  inches = microsecondsToInches(microseconds);
  if (DEBUGGING) {
    Serial.print("Micro: "); Serial.print(microseconds); 
    Serial.print(" Inches: "); Serial.print(inches);
    Serial.print(" cm: "); Serial.println(cm);
  }
  return cm;
}

long microsecondsToCentimeters(long microseconds) {
  // The speed of sound is 340 m/s or 29 microseconds per centimeter.
  // The ping travels out and back, so to find the distance of the
  // object we take half of the distance travelled.
  return microseconds / 29 / 2;
}

long microsecondsToInches(long microseconds) {
  // According to Parallax's datasheet for the PING))), there are
  // 73.746 microseconds per inch (i.e. sound travels at 1130 feet per
  // second).  This gives the distance travelled by the ping, outbound
  // and return, so we divide by 2 to get the distance of the obstacle.
  // See: http://www.parallax.com/dl/docs/prod/acc/28015-PING-v1.3.pdf
  // Same is true for the MaxSonar by MaxBotix
  return microseconds / 74 / 2;
}

// Replies out over serial and handles pausing and flushing the data to deal with Android serial comms
void serialReply(char* sensorname, char* tmpmsg) {
  Serial.print(sensorname);
  Serial.print(":");
  Serial.println(tmpmsg); // Send the message back out the serial line
  //Wait for the serial debugger to shut up
  delay(200); //this is a magic number
  Serial.flush(); //clears all incoming data
}

// Checks range finders to see if it is safe to continue moving (* need to add way to know which direction we're moving *)
boolean safeToProceed(){
  boolean safe = false; // Assume it isn't safe to proceed
  // Check the distance to the nearest object in front of the bot and stop if too close
  if (rangeToObjectMargin != 0){  // Don't bother sending if margin set to zero because it hangs when no sensor present
    dist = getDistanceSensor(rangePinForward);
    if (dist > rangeToObjectMargin) {
      safe = true;
    }
    else if (DEBUGGING) {Serial.print("Object too close in front - ");}
  }
  // Check the distance to the ground in front of the bot to make sure the table is still there
  if (rangeToGroundMargin != 0){  // Don't bother sending if margin set to zero because it hangs when no sensor present
    dist = getDistanceSensor(rangePinForwardGround);
    if (dist > rangeToGroundMargin) {
      safe = true;
    }
    else if (DEBUGGING) {Serial.print("End of surface reached - ");}
  }
  if (rangeToGroundMargin == 0 && rangeToObjectMargin == 0) {return true;}
  return safe;
}

// Check if enough time has elapsed to stop the bot and if it is safe to proceed
void checkIfStopBot() {
  if (not servosForcedActive and servosActive and (stopTime < millis() or not safeToProceed())) {
    stopBot();
    servosActive = false;
  } else if (not safeToProceed()) {
    stopBot();
    servosActive = false;
  }
}

// Send command to attached Bluetooth device to initiate pairing
void pairBluetooth() {
  Serial.print("\r\n+INQ=1\r\n"); // This is for Seeedstudio master/slave unit (change as needed for your model)
}

// Reads serial input if available and parses command when full command has been sent. 
void readSerialInput() {
  while(Serial.available() && serialIndex < BUFFERSIZE) {
    //Store into buffer.
    inBytes[serialIndex] = Serial.read();

    //Check for command end.    
    if (inBytes[serialIndex] == '\n' || inBytes[serialIndex] == ';' || inBytes[serialIndex] == '>') { //Use ; when using Serial Monitor
       inBytes[serialIndex] = '\0'; //end of string char
       parseCommand(inBytes); 
       serialIndex = 0;
    }
    else{
      serialIndex++;
    }
  }
  
  if(serialIndex >= BUFFERSIZE){
    //buffer overflow, reset the buffer and do nothing
    //TODO: perhaps some sort of feedback to the user?
    for(int j=0; j < BUFFERSIZE; j++){
      inBytes[j] = 0;
      serialIndex = 0;
    }
  }
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
  performCommand(com);
}

void performCommand(char* com) {  
  if (strcmp(com, "f") == 0) { // Forward
    stopTime = driveWheels(speedMultiplier * 10, speedMultiplier * 10);
    servosActive = true;
  } else if (strcmp(com, "r") == 0) { // Right
    stopTime = driveWheels(speedMultiplier * 10, speedMultiplier * -10);
    servosActive = true;  
  } else if (strcmp(com, "l") == 0) { // Left
    stopTime = driveWheels(speedMultiplier * -10, speedMultiplier * 10);
    servosActive = true;  
  } else if (strcmp(com, "b") == 0) { // Backward
    stopTime = driveWheels(speedMultiplier * -10, speedMultiplier * -10);
    servosActive = true;
  } else if (strcmp(com, "s") == 0) { // Stop
    stopBot();
    servosActive = false;
  } else if (strcmp(com, "fr") == 0 || strcmp(com, "fz") == 0 || strcmp(com, "x") == 0) { // Read and print forward facing distance sensor
    dist = getDistanceSensor(rangePinForward);
    itoa(dist, msg, 10); // Turn the dist int into a char
    serialReply("x", msg); // Send the distance out the serial line
  } else if (strcmp(com, "z") == 0) { // Read and print ground facing distance sensor
    dist = getDistanceSensor(rangePinForwardGround);
    itoa(dist, msg, 10); // Turn the dist int into a char
    serialReply("z", msg); // Send the distance out the serial line
  } else if (strcmp(com, "h") == 0) { // Help mode - debugging toggle
    // Print out some basic instructions when first turning on debugging
    if (not DEBUGGING) {
      Serial.println("Ready to listen to commands! Try ome of these:");
      Serial.println("F (forward), B (backward), L (left), R (right), S (stop), D (demo).");
      Serial.println("Also use numbers 1-9 to adjust speed (0=slow, 9=fast).");
    }
    DEBUGGING = !DEBUGGING;
  } else if (strcmp(com, "1") == 0 || strcmp(com, "2") == 0 || strcmp(com, "3") == 0 || strcmp(com, "4") == 0 || strcmp(com, "5") == 0 || strcmp(com, "6") == 0 || strcmp(com, "7") == 0 || strcmp(com, "8") == 0 || strcmp(com, "9") == 0 || strcmp(com, "0") == 0) {
    //I know the preceeding condition is dodgy but it will change soon 
    if (DEBUGGING) { Serial.print("Changing speed to "); }
    int i = com[0];
    speedMultiplier = i - 48; // Set the speed multiplier to a range 1-10 from ASCII inputs 0-9
    EEPROM.write(EEPROM_speedMultiplier, speedMultiplier); 
    if (DEBUGGING) { Serial.println(speedMultiplier); }
    // Blink the LED to confirm the new speed setting
    for(int speedBlink = 1 ; speedBlink <= speedMultiplier; speedBlink ++) { 
      digitalWrite(ledPin, HIGH);   // set the LED on           
      delay(100);
      digitalWrite(ledPin, LOW);   // set the LED off
      delay(100);
    }  
  } else if (com[0] == 'c') { // Calibrate center PWM settings for both servos ex: "c 90 90"
    int valueLeft=90, valueRight=90;
    sscanf (com,"c %d %d",&valueLeft, &valueRight); // Parse the input into multiple values
    servoCenterLeft = valueLeft;
    servoCenterRight = valueRight;
    stopTime = driveWheels(0,0); // Drive the servos with 0 value which should result in no movement when calibrated correctly
    servosActive = true;
    EEPROM.write(EEPROM_servoCenterLeft, servoCenterLeft); 
    EEPROM.write(EEPROM_servoCenterRight, servoCenterRight); 
    if (DEBUGGING) {
      Serial.print("Calibrated servo centers to ");
      Serial.print(servoCenterLeft);
      Serial.print(" and ");
      Serial.println(servoCenterRight);
    }
  } else if (strcmp(com, "i") == 0) { // Toggle servo to infinite active mode so it doesn't time out automatically
    servosForcedActive = !servosForcedActive; // Stop only when dangerous
    EEPROM.write(EEPROM_servosForcedActive, servosForcedActive);
    if (DEBUGGING) {
      Serial.print("Infinite rotation toggled to ");
      if (servosForcedActive){Serial.println("on");}
      else {Serial.println("off");}
    }
  } else if (com[0] == 'w') { // Handle "wheel" command and translate into PWM values ex: "w -100 100" [range is from -100 to 100]
    int valueLeft=90, valueRight=90;
    sscanf (com,"w %d %d",&valueLeft, &valueRight); // Parse the input into multiple values
    stopTime = driveWheels(valueLeft, valueRight);
    servosActive = true;
  } else if (strcmp(com, "reset") == 0) { // Resets the eeprom settings
    setEepromsToDefault();
  } else if (com[0] == 'n') { // Move head up
    sscanf (com,"n %d",&lastNeckValue); // Parse the input into multiple values
    myservoHead.attach(servoPinHead);
    myservoHead.write(lastNeckValue);
    EEPROM.write(EEPROM_lastNeckValue, lastNeckValue); 
    if (DEBUGGING) {
      Serial.print("Neck moved to ");
      Serial.println(lastNeckValue);
    }
  } else if (com[0] == 'p') { // Initiates Bluetooth pairing so another device can connect
    pairBluetooth();
  } else { 
    serialReply("e", com);// Echo unknown command back
    if (DEBUGGING) {
      Serial.print("Unknown command: ");
      Serial.println(com);
    }
  }
}

// Main loop running at all times
void loop() 
{
  readSerialInput();
  checkIfStopBot();
}

