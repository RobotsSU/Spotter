# Licensed under the Apache License, Version 2.0 (the "License"); you
# may not use this file except in compliance with the License. You may
# obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied. See the License for the specific language governing
# permissions and limitations under the License.
#
# See http://www.cellbots.com for more information

__author__ = 'Chris Elliott <chriselliott+cellbots@google.com>'

import os
import time
import logging
import math
import base64
import downlinks
import threading
import differentialDriveBot

class ICreateBot(object):

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

##ACTIONS

  def setWheelSpeeds(self, left, right):
    """Set wheel speeds.
    Assumes two wheels -- only two wheels are needed to control turning.

    Args:
    left -- speed of left wheel
    right -- speed of right wheel
    """
    self.parent.setWheelSpeeds(left, right)

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


  def shutdown(self, msg="Exiting"):
    """Stop the servos if using a serial controller."""
    self.parent.shutdown(msg)

  def log(self, foobar):
    """Outputs to log.

    Args:
    foobar -- text to be logged
    """
    self.parent.log(foobar)

# TODO: Require setMaximumSpeed(self, speed): Function
# TODO: Require changeSpeed(self, speed): Function
# TODO: Require turnToHeading(self, heading): Function
# TODO: Require writeRawCommand(self, rawCommand): Function


"""
  def moveForward(self, distance=None):
    self.speak("Moving forward.")
    if distance:
      self.sensor_monitor.get_distance()  # Reset distance
      total_distance = 0
      self.setWheelSpeeds(self.max_power/4, self.max_power/4)
      while(total_distance <= distance):
        if(self.sensor_monitor.is_bumper_hit()):
          break
        total_distance += self.sensor_monitor.get_distance()
        time.sleep(0.25)
      self.stop()
    else:
      self.setWheelSpeeds(self.max_power, self.max_power)

  def moveBackward(self, distance=None):
    self.speak("Moving backward.")
    if distance:
      self.sensor_monitor.get_distance()  # Reset distance
      total_distance = 0
      self.setWheelSpeeds(-1*self.max_power/4, -1*self.max_power/4)
      while(total_distance <= distance):
        if(self.sensor_monitor.is_bumper_hit()):
          break
        total_distance += math.fabs(self.sensor_monitor.get_distance())
        time.sleep(0.25)
      self.stop()
    else:
      self.setWheelSpeeds(-1*self.max_power, -1*self.max_power)

  def turnLeft(self, angle=None):
    self.speak("Turning left.")
    if angle:
      absAngle = math.fabs(angle)/3 # For some reason angle traveled is 3 fold
      self.sensor_monitor.get_angle()  # Reset angle
      total_angle = 0
      self.setWheelSpeeds(-1*self.max_power/10, self.max_power/10)
      while(total_angle <= absAngle):
        if(self.sensor_monitor.is_bumper_hit()):
          break
        total_angle += math.fabs(self.sensor_monitor.get_angle())
        time.sleep(0.25)
      self.stop()
    else:
      self.setWheelSpeeds(-1*self.max_power/5, self.max_power/5)

  def turnRight(self, angle=None):
    self.speak("Turning right.")
    if angle:
      absAngle = math.fabs(angle)/3 # For some reason angle traveled is 3 fold
      self.sensor_monitor.get_angle()  # Reset angle
      total_angle = 0
      self.setWheelSpeeds(self.max_power/10, -1*self.max_power/10)
      while(total_angle <= absAngle):
        if(self.sensor_monitor.is_bumper_hit()):
          break
        total_angle += math.fabs(self.sensor_monitor.get_angle())
        time.sleep(0.25)
      self.stop()
    else:
      self.setWheelSpeeds(self.max_power/5, -1*self.max_power/5)

  def stop(self):
    self.speak("Stopping.")
    self.setWheelSpeeds(0, 0)

  def setWheelSpeeds(self, left, right):
    try:
      self.robot.SetWheelVelocity(left, right)
      print 'Setting wheel velocity to %r:%r' % (left, right)
    except pyrobot.StateError, e:
      print 'Trying to set wheel velocity in wrong state.'

  def getSensors(self):
    sensor_data = self.sensor_monitor.get_sensors().get_sensor_data()
    d = {}
    for key in sensor_data:
      d[key] = sensor_data[key]
    return d
"""
