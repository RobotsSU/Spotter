package com.cellbots;

public class Balancer implements OrientationListener
{

  PulseGenerator pulseGenerator;

  ServoTester    servoTester;
  
  int maxLean = 30;
  
  int deadBand = 0;

  // OrientationManager orientationManager;

  int            leftCenter, rightCenter, maxSpeed;

  float          requestedPitch = (float) 90.0;

  long lastTimestamp;
  
  float degPerSecond;
  
  
  
  public Balancer()
  {
    // this.orientationManager = new OrientationManager();
  }

  public void startBalancing(PulseGenerator pulseGenerator, ServoTester servoTester)
  {
    this.pulseGenerator = pulseGenerator;
    this.servoTester = servoTester;
    this.leftCenter = 0;//pulseGenerator.getLeftPulsePercent();
    this.rightCenter = 0;//pulseGenerator.getRightPulsePercent();

    if (Math.abs(leftCenter - 50) > Math.abs(leftCenter - 50))
    {
      maxSpeed = Math.abs(leftCenter - 50) - deadBand;
    }
    else
    {
      maxSpeed = Math.abs(rightCenter - 50) - deadBand;
    }

    if (OrientationManager.isSupported())
    {
      OrientationManager.startListening(this);
    }

  }

  public void stopBalancing(PulseGenerator pulseGenerator, ServoTester servoTester)
  {
    if (OrientationManager.isListening())
    {
      OrientationManager.stopListening();
    }

  }

  public void onOrientationChanged(float azimuth, float pitch, float roll, float timestamp)
  {
    // TODO Auto-generated method stub
    
    Float pitchError = requestedPitch - pitch;
    
    if(pitchError > maxLean)
    {
         //pulseGenerator.setLeftPulsePercent(leftCenter);
         //pulseGenerator.setRightPulsePercent(rightCenter);
         return;
    }
    
    
    
    

  }
}
