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

__author__ = 'helenski@google.com (Helen Chou), madsci@google.com (John Hawley)'
__license__ = 'Apache License, Version 2.0'

"""This is the Cellbot base API.

The Cellbot base contains the basic functionality of an Android phone,
as well as the skeleton methods for the robot which every child robot class
should implement according to its own protocol.

Child robot classes should be created using composition rather than inheritance;
the robot class should extend object, Cellbot should be instantiated as a member
in the robot class, and each of Cellbot's methods should be called
within the corresponding implementing method.
"""

import collections
import math
import sys
import time

import avrCellBot
from sensorReading import SensorReading
import sensorType
from threadedAndroid import droid
import utils

class CellBot(object):
  """
  Base class for robot interactions.
  """

##SETUP

#  def addSensorTypes(self):
#    """Adds the types of sensors this robot has. This base class will
#    contain any sensors the phone possesses.
#    As sensor implementations become more complicated, more processing
#    will be required to set up sensors, which will go into this
#    addSensorTypes() method, but hopefully mostly into init methods in
#    each SensorType class.
#    """
#    self.sensorTypes['camera'] = sensorType.ImageSensorType()
#    self.sensorTypes['gps'] = sensorType.GpsSensorType()
#    self.startSensorStream(self.sensorTypes)

  def __init__(self, config, robotProto):
    # reference to the global configuration object
    self.config = config
    self.robotProto = robotProto
    # current heading on robot in degrees
    self.heading = SensorReading('heading')
    # speed
    self.speed = SensorReading('speed')
    # last queried location by tuple (latitude, longitude)
    self.location = SensorReading('location')
    # available sensor types
#    self.sensorTypes = {}
    # adds SensorType objects.
#    self.addSensorTypes()
    # previous spoken messages
    self.previousMsg = ''
    droid.startSensing()
    droid.startLocating()
    self.robotProto.StartSensorStream()


##QUERIES



##ACTIONS

  def setMaximumSpeed(self, speed):
    """Sets maximum speed of robot.

    Args:
    speed -- maximum speed
    """
    self.speed.update(speed)
    pass

  def moveForward(self, distance=None):
    """Moves robot forward.

    Args:
    distance -- distance to travel.
    if distance==None, move forward indefinitely
    """
    pass

  def moveBackward(self, distance=None):
    """Moves robot backward.

    Args:
    distance -- distance to travel.
    if distance==None, move backward indefinitely
    """
    pass

  def turnLeft(self, angle=90):
    """Turns robot to the left.
    Robot should resume previous speed after completing turn.

    Args:
    angle -- magnitude in degrees
    """
    self.heading.update((self.heading - angle) % 360)

  def turnRight(self, angle=90):
    """Turns robot to the right.
    Robot should resume previous speed after completing turn.

    Args:
    angle -- magnitude in degrees
    """
    self.heading.update((self.heading + angle) % 360)

  def turnToHeading(self, heading):
    """Turns robot to face the given compass heading.
    Robot should resume previous speed after completing turn.

    Args:
    heading -- compass direction in degrees. 0 == North, 90 == East, etc.
    """
    self.heading.update(heading)

  def stop(self):
    """Stops all robot motion."""
    pass

  def readLocation(self):
    """Returns the common form of the current location as reported by
    GPS.

    Returns:
      dict: { 'locality': foo, 'admin_area': bar }
    """
    # TODO: This never seems to work. Test outside.
    location = droid.readLocation().result
    addresses = droid.geocode(location['latitude'],
                              location['longitude']).result
    return addresses[0]

  def startAudioRecording(self, fileName):
    """Starts recording audio.

    Args:
      fileName -- file to use to save stored audio
    """
    droid.makeToast("Starting audio recording")
    droid.recorderStartMicrophone(fileName)

  def stopAudioRecording(self):
    """Stops recording audio."""
    droid.makeToast("Stopping audio recording")
    droid.recorderStop()

  def sing(self, song):
    """Outputs audio.

    Args:
    song -- audio stream to output
    """
    pass

  def speak(self, speech, override=False):
    """Outputs talk.

    Args:
    speech -- string of characters for android speech
    override -- set to true to override the audioOn state
    """
    # TODO: Move the muted state (audioOn) out of the config object.
    if (self.config.audioOn and speech != self.previousMsg) or override:
      droid.ttsSpeak(speech)
    elif speech != self.previousMsg:
      droid.makeToast(speech)
    self.previousMsg = speech

  def recognizeSpeech(self):
    """Launch the android cloud voice recognition framework and return
    the result."""
    droid.makeToast("Launching voice recognition")
    return str(droid.recognizeSpeech().result)

  def captureImage(self, fileName, camera=None):
    """Capture an image.

    Args:
    fileName -- save the image to this file
    camera -- indicates which camera to use.
    if camera == None, capture n images with all n cameras
    """
    # TODO: Add support for more than one camera
    droid.cameraCapturePicture(fileName)

  def setVolume(self, newVolume):
    """Set the media volume (includes the synthesized voice).

    Args:
      newVolume -- a level between 0 and MaxMediaVolume
    """
    try:
      newVolume = int(newVolume)
    except ValueError:
      # We pass a nicer error message with raise ValueError below
      pass
    maxVolume = droid.getMaxMediaVolume().result
    print "Current volume: %d" % droid.getMediaVolume().result
    print "Max volume:     %d" % droid.getMaxMediaVolume().result
    if newVolume in range(maxVolume + 1):
      droid.setMediaVolume(int(newVolume))
      print "New volume:     %d" % droid.getMediaVolume().result
    else:
      raise ValueError("New volume must be an integer between 0 and %d" %
                       maxVolume)

  def shutdown(self, msg='Exiting'):
    """Android specific shutdown."""
    self.robotProto.StopSensorStream()
    droid.stopSensing()
    time.sleep(1)   # Fixing crash before exit on G1
    droid.stopLocating()
    sys.exit(msg)


##STATS

  def log(self, foobar):
    """Outputs to log.

    Args:
    foobar -- text to be logged
    """
    pass
