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

__author__ = 'helenski@google.com (Helen Chou), madsci@google.com (John Hawley)'
__license__ = 'Apache License, Version 2.0'


import datetime
import math
import threading
import time

import differentialDriveBot
from sensorReading import SensorReading

##CONSTANTS
OBSTACLE_THRESHOLD = 60.0

# TODO: currently hardcoded for four sonar readings as on Skeletor
# need to abstract this...



# TODO: currently hardcoded for differential drive bots
# need to refactor this to use turnLeft / turnRight once those are
# implemented (which adds another layer of abstraction for any robot,
# not just Skeletor. i.e. won't need wheelSpeeds, etc. (though future
# devs can make a composition class if they wish)

class obstacleEvasionBot(object):
  """
  Class for a robot that runs obstacle evasion with threading.

  Composition of differential wheel drive class.

  Obstacle avoidance will be toggled on and off depending on whether or not
  sonar readings are picked up from the protocol's sensor stream.
  """
#TODO: Implement last sentence of the class python doc.

##SETUP

  def __init__(self, config, robotProto):
    self.parent = differentialDriveBot.DifferentialDriveBot(config, robotProto)
    self.config = config
    self.robotproto = robotProto

    global obstacle_evasion
    obstacle_evasion = False

    # TODO: clean up globals
    # taken from main class of original cellbots.py
    global speed_lock
    speed_lock = threading.RLock()
    global left_wheel_speed
    global right_wheel_speed
    left_wheel_speed = 0
    right_wheel_speed = 0
    global damping_enabled
    damping_enabled = True
    self.checkEvasionThread()


  def checkEvasionThread(self):
    """If obstacle_evasion has been turned off for some reason (sonar readings
    weren't found, etc.), this method tries to restart the thread.

    Invariant: when obstacle_evasion == True, the EvasionThread is running.
    Invarient: when obstacle_evasion == False, the EvasionThread is not running.
    """
    global obstacle_evasion
    if obstacle_evasion == False:
      evasion_thread = self.EvasionThread(self)
      evasion_thread.start()



##QUERIES



##ACTIONS

  def setWheelSpeeds(self, left, right):
    """Set wheel speeds.
    Assumes two wheels -- only two wheels are needed to control turning.

    Args:
    left -- speed of left wheel
    right -- speed of right wheel
    """
    global speed_lock
    speed_lock.acquire()
    global left_wheel_speed
    global right_wheel_speed
    left_wheel_speed = left
    right_wheel_speed = right
    self.outputWheelSpeeds()
    speed_lock.release()

  def outputWheelSpeeds(self):
    """Output the current wheel speeds to the robot, taking into account any
    damping due to obstacle evasion.  Note that damping variables should always
    be in [0, 1], so that 'damping' can never make a wheel turn faster than it
    otherwise would.
    """
    global speed_lock
    speed_lock.acquire()
    global left_wheel_speed
    global right_wheel_speed
    global damping_enabled

    left_wheel_output, right_wheel_output = (0, 0)

    if (not damping_enabled or
        (left_wheel_speed <= 0 and right_wheel_speed >= 0) or
        (left_wheel_speed >= 0 and right_wheel_speed <= 0)):
      # If we are stopped or turning in-place don't apply any damping.
      left_wheel_output, right_wheel_output = (left_wheel_speed,
                                               right_wheel_speed)
    else:
      if left_wheel_speed > 0:
        left_wheel_output = left_wheel_speed * self.left_wheel_fwd_damping
      else:
        left_wheel_output = left_wheel_speed * self.left_wheel_rev_damping
      if right_wheel_speed > 0:
        right_wheel_output = right_wheel_speed * self.right_wheel_fwd_damping
      else:
        right_wheel_output = right_wheel_speed * self.right_wheel_rev_damping

    left_wheel_output = int(left_wheel_output)
    right_wheel_output = int(right_wheel_output)

    print "Thread '%s' setting wheel speeds to (%d, %d)" % (
        threading.currentThread().name, left_wheel_output, right_wheel_output)

    self.robotproto.SetWheelSpeeds(left_wheel_output, right_wheel_output)
    self.parent.wheelSpeeds.update((left_wheel_output, right_wheel_output))

    speed_lock.release()

  def getLatestSensors(self, sensorKey):
    """ Get the latest value from ultrasonic sensors.

    Returns None if no values have been read yet.  Otherwise, returns a dict
    containing one or more of the following keys:
    'front', 'left', 'right', 'rear', 'front-right', 'front-left', 'rear-right',
    'rear-left'

    The value for each key represents the sensor reading for the sensor facing
    that direction.
    """
    global obstacle_evasion
    sensors = self.robotproto.getLatestFromSensorStream(sensorKey)
    if sensors:
      obstacle_evasion = True
      sensors = sensors.split(" ");
      # TODO: This mapping is very platform-specific and should depend on
      # which platform we're paired with, but that information is not
      # currently available.  For now, hardcoded for Rosie.
      try:
        retval = {'front-left': int(sensors[0]),
                  'front': int(sensors[1]),
                  'front-right': int(sensors[2]),
                  'rear': int(sensors[3])}
        print "Sensor values: %s" % retval
        return retval
      except Exception:
        pass
    else:
      obstacle_evasion = False
    return None

  class EvasionThread(threading.Thread):
    """Thread to set damping values based on sonar readings.

    By updating damping values every 100 ms, this thread adjusts the current wheel
    speeds to avoid obstacles.
    """
    def __init__(self, robot_instance):
      threading.Thread.__init__(self)
      self.daemon = True
      self.name = 'EvasionThread'
      self.robot_instance = robot_instance

    def run(self):
      global obstacle_evasion

      # Damping values should always be in [0, 1], wheel speeds are multiplied
      # by the corresponding damping value before being output to the robot.
      while True:
        #self.robotProto.WriteRawCommand('d')
        time.sleep(.1)
        sensors = self.robot_instance.getLatestSensors('us')
        if obstacle_evasion == False:
          break
        else:
          if sensors:
            self.robot_instance.left_wheel_fwd_damping = 1.0
            self.robot_instance.left_wheel_rev_damping = 1.0
            self.robot_instance.right_wheel_fwd_damping = 1.0
            self.robot_instance.right_wheel_rev_damping = 1.0
            # 1.0 = No linear effect, >1.0 decrease damping, <1.0 increase damping.
            # Must be in (0, inf], but reasonable values are in (0, 10] or so.
            linear_factor = 1.5
            # 1.0 = Linearly turn harder when closer to obstacle,
            # >1.0 = Exponentially turn harder when closer to obstacle,
            # <1.0 = Only turn harder when very close to obstacle.
            # Must be in (0, inf], but reasonable values are in (0, 5] or so.
            exponent = 2.0

            if 'front' in sensors and sensors['front'] < OBSTACLE_THRESHOLD:
              self.robot_instance.left_wheel_fwd_damping *= linear_factor * math.pow(
                  sensors['front'] / OBSTACLE_THRESHOLD, exponent * .5)
              self.robot_instance.right_wheel_fwd_damping *= linear_factor * math.pow(
                  sensors['front'] / OBSTACLE_THRESHOLD, exponent * .5)

            if ('front-right' in sensors
                and sensors['front-right'] < OBSTACLE_THRESHOLD):
              self.robot_instance.left_wheel_fwd_damping *= linear_factor * math.pow(
                  sensors['front-right'] / OBSTACLE_THRESHOLD, exponent)
              self.robot_instance.right_wheel_fwd_damping /= linear_factor * math.pow(
                  sensors['front-right'] / OBSTACLE_THRESHOLD, exponent)

            if ('front-left' in sensors
                and sensors['front-left'] < OBSTACLE_THRESHOLD):
              self.robot_instance.right_wheel_fwd_damping *= linear_factor * math.pow(
                  sensors['front-left'] / OBSTACLE_THRESHOLD, exponent)
              self.robot_instance.left_wheel_fwd_damping /= linear_factor * math.pow(
                  sensors['front-left'] / OBSTACLE_THRESHOLD, exponent)

            # TODO: Note that rear evasion has not really been tested.
            if ('rear' in sensors
                and sensors['rear'] < OBSTACLE_THRESHOLD):
              self.robot_instance.left_wheel_rev_damping *= linear_factor * math.pow(
                  sensors['rear'] / OBSTACLE_THRESHOLD, exponent * .5)
              self.robot_instance.right_wheel_rev_damping *= linear_factor * math.pow(
                  sensors['rear'] / OBSTACLE_THRESHOLD, exponent * .5)

            if 'rear-left' in sensors:
              self.robot_instance.right_wheel_rev_damping *= linear_factor * math.pow(
                  sensors['rear-left'] / OBSTACLE_THRESHOLD, exponent)
              self.robot_instance.left_wheel_rev_damping /= linear_factor * math.pow(
                  sensors['rear-left'] / OBSTACLE_THRESHOLD, exponent)
            if 'rear-right' in sensors:
              self.robot_instance.left_wheel_rev_damping *= linear_factor * math.pow(
                  sensors['rear-right'] / OBSTACLE_THRESHOLD, exponent)
              self.robot_instance.right_wheel_rev_damping /= linear_factor * math.pow(
                  sensors['rear-right'] / OBSTACLE_THRESHOLD, exponent)

              # If we don't have diagonal sensors, double the front/rear sensor
              # damping so we're a little more consistent.
            if 'front-right' not in sensors and 'front-left' not in sensors:
              self.robot_instance.left_wheel_fwd_damping = math.pow(self.robot_instance.left_wheel_fwd_damping, 2)
              self.robot_instance.right_wheel_fwd_damping = math.pow(self.robot_instance.right_wheel_fwd_damping, 2)
            if 'rear-right' not in sensors and 'rear-left' not in sensors:
              self.robot_instance.left_wheel_rev_damping = math.pow(self.robot_instance.left_wheel_rev_damping, 2)
              self.robot_instance.right_wheel_rev_damping = math.pow(self.robot_instance.right_wheel_rev_damping, 2)

              # Special cases to stop before we hit something, since the sonars
              # won't actually give us a value of 0.
            if 'front' in sensors and sensors['front'] < OBSTACLE_THRESHOLD:
              self.robot_instance.left_wheel_fwd_damping = 0
              self.robot_instance.right_wheel_fwd_damping = 0

            if 'rear' in sensors and sensors['rear'] < OBSTACLE_THRESHOLD:
              self.robot_instance.left_wheel_rev_damping = 0
              self.robot_instance.right_wheel_rev_damping = 0

              # Clamp values to [0, 1], note that none of the math above should
              # result in a value <0.
            if self.robot_instance.left_wheel_fwd_damping > 1:
              self.robot_instance.left_wheel_fwd_damping = 1
            if self.robot_instance.right_wheel_fwd_damping > 1:
              self.robot_instance.right_wheel_fwd_damping = 1

            # We have to re-output the wheel speeds to the robot, using the
            # updated damping values.
            self.robot_instance.outputWheelSpeeds()
          else:
            time.sleep(10)


  def toggleDamping(self):
    global damping_enabled
    damping_enabled = not damping_enabled

  def toggleObstacleEvasion(self):
    global obstacle_evasion
    obstacle_evasion = not obstacle_evasion

##############################################

  def moveForward(self, distance=None):
    """Moves robot forward.

    Args:
    distance -- distance to travel.
    if distance==None, move forward indefinitely
    """
    self.checkEvasionThread()

    left = right = self.config.currentSpeed * 10
    global obstacle_evasion
    if obstacle_evasion:
      self.setWheelSpeeds(left, right)
    else:
      self.parent.moveForward()

  def moveBackward(self, distance=None):
    """Moves robot backward.

    Args:
    distance -- distance to travel.
    if distance==None, move backward indefinitely
    """
    self.checkEvasionThread()

    left = right = self.config.currentSpeed * 10
    global obstacle_evasion
    if obstacle_evasion:
      self.setWheelSpeeds(left, right)
    else:
      self.parent.moveBackward()

  def turnLeft(self, angle=90):
    """Turns robot to the left.
    Robot should resume previous speed after completing turn.

    Args:
    angle -- magnitude in degrees
    """
    # self.parent.turnLeft(angle) # angle to be added to implementation
    self.parent.turnLeft()

  def turnRight(self, angle=90):
    """Turns robot to the right.
    Robot should resume previous speed after completing turn.

    Args:
    angle -- magnitude in degrees
    """
    # self.parent.turnRight(angle) # angle to be added implementation
    self.parent.turnRight()

  def turnToHeading(self, heading):
    """Turns robot to face the given compass heading.
    Robot should resume previous speed after completing turn.

    Args:
    heading -- compass direction in degrees. 0 == North, 90 == East, etc.
    """
    self.parent.turnToHeading(heading)

  def orientToAzimuth(self, azimuth):
    onTarget = False
    stopTime = time.time() + 60
    while not onTarget and time.time() < stopTime:
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
      msg = "Goal achieved! Facing %d degrees, which is within the %d degree margin of %d!" % (currentHeading, self.config.cardinalMargin, azimuth)
      self.speak(msg)
      self.setWheelSpeeds(0, 0)
    else:
      msg = "Ran out of time before hitting proper orientation."
      self.speak(msg)
      self.setWheelSpeeds(0, 0)

  def stop(self):
    """Stops all robot motion."""
    global obstacle_evasion
    obstacle_evasion = False
    self.parent.stop()

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
    self.stop()
    self.parent.shutdown(msg)

##STATS

  def log(self, foobar):
    """Outputs to log.

    Args:
    foobar -- text to be logged
    """
    self.parent.log(foobar)

