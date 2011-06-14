# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#
# See http://www.cellbots.com for more information

__author__ = 'helenski@google.com (Helen Chou), cram@google.com (Marc Alvidrez)'
__license__ = 'Apache License, Version 2.0'


import math
import time

import baseCellBot
from sensorReading import SensorReading
from threadedAndroid import droid

class DifferentialDriveBot(object):
  """
  Class for differential wheel drive robots. Composition of Cellbot base class.
  """

###SETUP

#  def addSensorTypes(self):
#    """Adds the types of sensors this robot has. This base class will
#    contain any sensors the phone possesses.
#    As sensor implementations become more complicated, more processing
#    will be required to set up sensors, which will go into this
#    addSensorTypes() method, but hopefully mostly into init methods in
#    each SensorType class.
#    """
#    self.parent.addSensorTypes()

  def __init__(self, config, robotProto):
    self.parent = baseCellBot.CellBot(config, robotProto)
    self.config = config
    self.robotProto = robotProto

    # wheel speeds of the two different wheels, in tuple (left, right)
    self.wheelSpeeds = SensorReading('wheelSpeeds')


##QUERIES



##ACTIONS

  def setWheelSpeeds(self, left, right):
    """Set wheel speeds.
    Assumes two wheels -- only two wheels are needed to control turning.

    Args:
    left -- speed of left wheel
    right -- speed of right wheel
    """
    self.robotProto.SetWheelSpeeds(left, right)
    self.wheelSpeeds.update((left, right))

  def setMaximumSpeed(self, speed):
    """Sets maximum speed of robot.

    Args:
    speed -- maximum speed
    """
    self.parent.setMaximumSpeed(speed)

  def moveForward(self, distance=None):
    """Moves robot forward.

    Args:
    distance -- distance to travel.
    if distance==None, move forward indefinitely
    """
    self.parent.speed = left = right = self.config.currentSpeed * 10
    self.setWheelSpeeds(left, right)

  def moveBackward(self, distance=None):
    """Moves robot backward.

    Args:
    distance -- distance to travel.
    if distance==None, move backward indefinitely
    """
    self.parent.speed = left = right = self.config.currentSpeed * -10
    self.setWheelSpeeds(left, right)

  def turnLeft(self, angle=90):
    """Turns robot to the left.
    Robot should resume previous speed after completing turn.

    Args:
    angle -- magnitude in degrees
    """
    # self.parent.turnLeft(angle) # angle to be added to implementation
    # divide speed by 5 as turning at max speed is uncontrollable
    left = self.config.currentSpeed * -10 / 2
    right = self.config.currentSpeed * 10 / 2
    self.setWheelSpeeds(left, right)

  def turnRight(self, angle=90):
    """Turns robot to the right.
    Robot should resume previous speed after completing turn.

    Args:
    angle -- magnitude in degrees
    """
    # self.parent.turnRight(angle) # angle to be added implementation
    # divide speed by 4 as turning at max speed is uncontrollable
    left = self.config.currentSpeed * 10 / 2
    right = self.config.currentSpeed * -10 / 2
    self.setWheelSpeeds(left, right)

  def turnToHeading(self, heading):
    """Turns robot to face the given compass heading.
    Robot should resume previous speed after completing turn.

    Args:
    heading -- compass direction in degrees. 0 == North, 90 == East, etc.
    """
    self.parent.turnToHeading(heading)
    self.orientToAzimuth(heading)

  def orientToAzimuth(self, azimuth):
    onTarget = False
    stopTime = time.time() + 60
    while not onTarget and time.time() < stopTime:
      # TODO: Parent (baseCellBot.py) should support reading sensors and 
      # we should call into parent to do this.
      results = droid.readSensors().result
      if results is not None:
        currentHeading = 57.2957795*results['azimuth']
        delta = azimuth - currentHeading
        if math.fabs(delta) > 180:
          if delta < 0:
            adjustment = delta + 360
          else:
            adjustment = delta - 360
        else:
          adjustment = delta
        adjustmentAbs = math.fabs(adjustment)
        if adjustmentAbs < self.config.cardinalMargin:
          self.setWheelSpeeds(0, 0)
          time.sleep(2)
          if adjustmentAbs < self.config.cardinalMargin:
            onTarget = True
        else:
          adjustmentSpeed = min(((adjustmentAbs/180)*40+15),50)
          self.setWheelSpeeds(0, 0)
          if adjustment > self.config.cardinalMargin:
            self.setWheelSpeeds(adjustmentSpeed, -1*adjustmentSpeed)
          elif adjustment < (self.config.cardinalMargin * -1):
            self.setWheelSpeeds(-1*adjustmentSpeed, adjustmentSpeed)
          # Let the robot run for an estimated number of seconds to turn in the intended direction
          # We should have setting for this and/or allow some calibration for turning rate of the bot
          time.sleep(adjustmentAbs/180)
          # Stop for a 1/2 second to take another compass reading
          self.setWheelSpeeds(0, 0)
          time.sleep(0.5)
      else:
        msg = "Could not start sensors."
    # Loop finished because we hit target or ran out of time
    if onTarget:
      msg = "Goal achieved! Facing %d degrees, which is within the %d degree" \
            "margin of %d!" % (currentHeading, self.config.cardinalMargin,
                               azimuth)
      self.speak(msg)
      self.setWheelSpeeds(0, 0)
    else:
      msg = "Ran out of time before hitting proper orientation."
      self.speak(msg)
      self.setWheelSpeeds(0, 0)

  def stop(self):
    """Stops all robot motion."""
    self.setWheelSpeeds(0, 0)

  def readLocation(self):
    """Returns the current location as reported by GPS."""
    return self.parent.readLocation()

  def startAudioRecording(self, fileName):
    """Starts recording audio.

    Args:
      fileName -- file to use to save stored audio
    """
    self.parent.startAudioRecording(fileName)

  def stopAudioRecording(self):
    """Stops recording audio."""
    self.parent.stopAudioRecording()

  def sing(self, song):
    """Outputs audio.

    Args:
    song -- audio stream to output
    """
    self.parent.sing(song)

  def speak(self, speech, override=False):
    """Outputs talk.

    Args:
    speech -- string of characters for android speech
    override -- set to true to override the audioOn state
    """
    self.parent.speak(speech, override)

  def recognizeSpeech(self):
    """Launch the android cloud voice recognition framework and return
    the result."""
    return self.parent.recognizeSpeech()

  def captureImage(self, fileName, camera=None):
    """Capture an image.

    Args:
    fileName -- save the image to this file
    camera -- indicates which camera to use.
    if camera == None, capture n images with all n cameras
    """
    self.parent.captureImage(fileName, camera)

  def setVolume(self, newVolume):
    """Set the media volume (includes the synthesized voice).

    Args:
      newVolume -- a level between 0 and MaxMediaVolume
    """
    self.parent.setVolume(newVolume)

  def shutdown(self, msg='Exiting'):
    """Android specific shutdown."""
    self.stop()
    self.parent.shutdown(msg)


##STATS

  def log(self, foobar):
    """Outputs to log.

    Args:
    foobar -- text to be logged
    """
    self.parent.log(foobar)
