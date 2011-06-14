int lightPin = 0;  //define a pin for Photo resistor
int ledPin=11;     //define a pin for LED

void setup()
{
    Serial.begin(9600);  //Begin serial communcation
    pinMode( ledPin, OUTPUT );
}

void loop()
{
    Serial.println(analogRead(lightPin)); //Write the value of the photoresistor to the serial monitor.
    analogWrite(ledPin, analogRead(lightPin)/4);  //send the value to the ledPin. Depending on value of resistor 
                                                //you have  to divide the value. for example, 
                                                //with a 10k resistor divide the value by 2, for 100k resistor divide by 4.
   delay(10); //short delay for faster response to light.
}

