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

__author__ = 'madsci@google.com (John Hawley)'

import datetime
import downlinks
import threading
import time
import traceback

MAX_SENSORSTREAM_LENGTH = 50

# NOTE: AVRAsciiRobotProtocol and AVRBinaryRobotProtocol should have identical
# public APIs.

class AVRAsciiRobotProtocol(object):

  def __init__(self, downlink):
    """Create a RobotProtocol to talk to an AVR-controlled robot using
    the ASCII protocol.

    Args:
      downlink: An instance of downlinks.Downlink to use for
      communication with the robot.
    """
    self.downlink = downlink
    self.sensorStream = {}

  def SetWheelSpeeds(self, left, right):
    self.downlink.WriteCommand("w %s %s" % (left, right))

  def Reset(self):
    """Reset hardware settings to default"""
    self.downlink.WriteCommand("reset")

  def PairBluetooth(self):
    """Put the Bluetooth device on the robot in pairing mode (not the
    Android)"""
    self.downlink.WriteCommand("p")

  def CalibrateServo(self, x, y):
    """Calibrating servos to center at coordinates x and y"""
    self.downlink.WriteCommand(" ".join(["c", x, y]))

  def WriteRawCommand(self, rawCommand):
    """Pass through a raw command."""
    self.downlink.WriteCommand(rawCommand)

  def GetSensorData(self):
    """Requests data through the downlink.

    Returns: (key, val) tuple, or None on error or timeout."""
    self.downlink.WriteCommand("d")
    return self.downlink.ReadReply(2)

  def FindRange(self):
    """Check range sensor to find the distance to closest object"""
    # TODO: Figure out how to return a result.
    self.downlink.WriteCommand("fr")

  def StartSensorStream(self):
    """Starts the SensorStream thread."""
    global alive
    alive = True
    sensorThread = self.SensorThread(self)
    sensorThread.start()
    return sensorThread

  def StopSensorStream(self):
    """Stops the SensorStream thread."""
    global alive
    alive = False

  class SensorThread(threading.Thread):
    """SensorThread class.

    run() function should call the processing function for the sensor type appropriate to incoming reading"""
    def __init__(self, proto_instance):
      threading.Thread.__init__(self)
      self.name = "SensorThread"
      self.proto_instance = proto_instance

    def run(self):
      global alive
      while alive:
        try:
          time.sleep(1)
          pass
          # TODO: The following requires further testing before implementation
          #data = self.proto_instance.GetSensorData()
          #if data:
          #  self.proto_instance.addToSensorStream(data[0], data[1])
        except:
          print("Reader Thread errored:")
          traceback.print_exc()

  # SensorStream is used to store sensor values and their history
  def addToSensorStream(self, key, value):
    # Get the old values, or create a new list if this is a new key.
    values = self.sensorStream.get(key, [])
    values.insert(0, (value, datetime.datetime.today()))
    # Limit the number of values we can have for any given key.
    del values[MAX_SENSORSTREAM_LENGTH:]
    # Add back to the self.sensorStream
    self.sensorStream[key] = values

  # Get the latest value from the self.sensorStream for the given key, or None if the
  # key is not in the self.sensorStream.
  def getLatestFromSensorStream(self, key):
    return self.sensorStream.get(key, [(None,)])[0][0]

  # For debugging.
  def SensorStreamToString(self, includeHistory=False):
    str = ""
    for key in self.sensorStream:
      str += key + '\n'
      for history in self.sensorStream[key]:
        str += '\tvalue: %s' % history[0] + '\n'
        str += '\ttime : %s' % history[1].strftime("%A, %d. %B %Y %I:%M%p") + '\n'
        if includeHistory == False:
          break
    return str



class AVRBinaryRobotProtocol(object):

  BINARY_COMMANDS = {'SET_MOTOR_SPEED': 0x2}

  def __init__(self, downlink):
    """Create a RobotProtocol to talk to an AVR-controlled robot using the
    binary protocol.

    Args:
      downlink: An instance of downlinks.Downlink to use for communication with
      the robot.
    """
    self.downlink = downlink

  # TODO: Make a self._Write() that writes and then waits for ACK

  def SetWheelSpeeds(self, left_speed, right_speed):
    self.downlink.WriteCommand(
        BINARY_COMMANDS['SET_MOTOR_SPEED'], 1, left_speed, 0)
    self.downlink.WriteCommand(
        BINARY_COMMANDS['SET_MOTOR_SPEED'], 2, right_speed, 0)

  def Reset(self):
    pass

  def GetSensorData(self):
    pass
