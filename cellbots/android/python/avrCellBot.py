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

__author__ = 'cram@google.com (Marc Alvidrez)'
__license__ = 'Apache License, Version 2.0'

"""An implementation of the Cellbot robot API for an AVR CellBot."""

import math

import differentialDriveBot
from threadedAndroid import droid

class AVRCellBot(object):
  """AVR CellBot."""


##SETUP

  def addSensorTypes(self):
    """Adds the types of sensors this robot has. This base class will
    contain any sensors the phone possesses.
    As sensor implementations become more complicated, more processing
    will be required to set up sensors, which will go into this
    addSensorTypes() method, but hopefully mostly into init methods in
    each SensorType class.
    """
    self.parent.addSensorTypes()

  def __init__(self, config, robotProto):
    self.parent = differentialDriveBot.DifferentialDriveBot(config, robotProto)
    self.config = config
    self.robotProto = robotProto


##QUERIES

  def findRange(self):
    # TODO: Figure out how to get a value back, and where to put it.
    self.robotProto.FindRange()


##ACTIONS

  def setWheelSpeeds(self, left, right):
    """Set wheel speeds.
    Assumes two wheels -- only two wheels are needed to control turning.

    Args:
    left -- speed of left wheel
    right -- speed of right wheel
    """
    self.parent.setWheelSpeeds(left, right)

  def setMaximumSpeed(self, speed):
    """Sets maximum speed of robot.

    Args:
    speed -- maximum speed
    """
    self.parent.setMaximumSpeed(speed)

  def changeSpeed(self, newSpeed):
    """Adjusts the speed of the robot without altering direction.

    Args:
      newSpeed -- an integer in range(10)
    """
    # AVR microcontroller specific logic from the original changeSpeed()
    if self.config.microcontroller != "AVR_Stepper":
      self.parent.speed = left = right = self.config.currentSpeed = newSpeed
      self.setWheelSpeeds(left, right)
    else:
      # throw an exception or do nothing?
      pass

  def moveForward(self, distance=None):
    """Moves robot forward.

    Args:
    distance -- distance to travel.
    if distance==None, move forward indefinitely
    """
    self.parent.moveForward(distance)

  def moveBackward(self, distance=None):
    """Moves robot backward.

    Args:
    distance -- distance to travel.
    if distance==None, move backward indefinitely
    """
    self.parent.moveBackward(distance)

  def turnLeft(self, angle=None):
    """Turns robot to the left.
    Robot should resume previous speed after completing turn.

    Args:
    angle -- magnitude in degrees
    """
    self.parent.turnLeft(angle)

  def turnRight(self, angle=None):
    """Turns robot to the right.
    Robot should resume previous speed after completing turn.

    Args:
    angle -- magnitude in degrees
    """
    self.parent.turnRight(angle)

  def turnToHeading(self, heading):
    """Turns robot to face the given compass heading.
    Robot should resume previous speed after completing turn.

    Args:
    heading -- compass direction in degrees. 0 == North, 90 == East, etc.
    """
    self.parent.turnToHeading(heading)

  def stop(self):
    """Stops all robot motion."""
    self.parent.stop()

  def readLocation(self):
    """Returns the current location as reported by GPS."""
    return self.parent.readLocation()

  def startAudioRecording(self, fileName):
    """Starts recording audio."""
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
    self.parent.speak(speech, override)

  def recognizeSpeech(self):
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

  def reset(self):
    """Reset hardware settings to default"""
    self.robotProto.Reset()

  def pairBluetooth(self):
    """Put the Bluetooth device on the robot in pairing mode (not the
    Android)"""
    self.robotProto.PairBluetooth()

  def calibrateServo(self, x, y):
    """Calibrating servos to center at coordinates x and y"""
    self.robotProto.CalibrateServo(x, y)

  def writeRawCommand(self, rawCommand):
    """Pass through an arbitrary string command to the robot.

    Args:
      command -- uninterpreted string of characters
    """
    self.robotProto.WriteRawCommand(rawCommand)

  def shutdown(self, msg="Exiting"):
    """Stop the servos if using a serial controller."""
    self.parent.shutdown(msg)

##STATS

  def log(self, foobar):
    """Outputs to log.

    Args:
    foobar -- text to be logged
    """
    self.parent.log(foobar)

# TODO: this should go into the protocol eventually

  # Extra processing when using a serial AVR Stepper Driver by MiliKiller
  def AVR_Stepper_Controll(self, leftPWM, rightPWM):
    #Make sure nothing fell out of range
    leftPWM = max(min(int(leftPWM), 100), (-100))
    rightPWM = max(min(int(rightPWM), 100), (-100))
    #Send Command to AVR Stepper Driver Board
    serialCommand = "w " + str(leftPWM) + " " + str(rightPWM)
    commandOut(serialCommand)
