/*
  Blink
 
 Turns on an LED on for one second, then off for one second, repeatedly.
 
 The circuit:
 * LED connected from digital pin 13 to ground.
 
 * Note: On most Arduino boards, there is already an LED on the board
 connected to pin 13, so you don't need any extra components for this example.
 
 
 Created 1 June 2005
 By David Cuartielles
 
 http://arduino.cc/en/Tutorial/Blink
 
 based on an orginal by H. Barragan for the Wiring i/o board
 
 */

int ledPin =  13;    // LED connected to digital pin 13
int serialReadPin = 12;
char incomingByte;

// The setup() method runs once, when the sketch starts

void setup()   {                
  // initialize the digital pin as an output:
  pinMode(ledPin, OUTPUT);    
 pinMode(serialReadPin, OUTPUT);  
  Serial.begin(9600);
  Serial.println("Ready to listen to commands!");
  incomingByte = '0';
  digitalWrite(ledPin, HIGH);
}

// the loop() method runs over and over again,
// as long as the Arduino has power

void loop()                     
{
  if (Serial.available() > 0) {
      // Read the oldest byte in the serial buffer
      digitalWrite(serialReadPin, HIGH);
      delay(1000);
      digitalWrite(serialReadPin, LOW);
      incomingByte = Serial.read();
      Serial.println("read occurred: ");
      Serial.println(incomingByte);
  }
  
  switch(incomingByte) {
     case 'd':
       digitalWrite(ledPin, HIGH);   // set the LED on
       break;
     case 'e':
       digitalWrite(ledPin, LOW);   // set the LED off
       break;
  }

  //Serial.println(incomingByte);
  //digitalWrite(ledPin, HIGH);   // set the LED on
  //delay(1000);                  // wait for a second
 
  
  
  //digitalWrite(ledPin, LOW);    // set the LED off
  //delay(1000);                  // wait for a second
}
