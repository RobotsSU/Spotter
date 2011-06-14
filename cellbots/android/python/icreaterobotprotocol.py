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

__author__ = 'chriselliott+cellbots@google.com (Chris Elliott)'

import downlinks
import threading
import icreatepyrobot
#import icreatesensors  # TODO: Add Sensors.
import math
import logging
import time
from threadedAndroid import droid


class SerialConnection(object):
  """Custom serial connection required to utilized pyrobot iCreate code."""

  def __init__(self, downlink):
    self.downlink = downlink

  def open(self):
    pass

  def flushInput(self):
    self.downlink.FlushInput()

  def write(self, data):
    self.downlink.WriteCommand(data)

  def read(self, buffer_size):
    return self.downlink.ReadReplyWithBuffer(timeout=None,
                                             bufferSize=buffer_size)


class ICreateRobotProtocol(object):

  def __init__(self, downlink):
    """Create a RobotProtocl ot talk to an iRobot bot using iRobot protocol.

    Args:
      downlink: An instance of downlinks.Downlink to use for
      communication with the robot.
    """
    self.downlink = downlink
    serial = SerialConnection(self.downlink)
    self.iCreateRobot = icreatepyrobot.Create(serial)
    self.iCreateRobot.safe = False
    self.Reset()
    # TODO: Verify sensors work correctly
    #self.sensors = icreatesensors.PyrobotSensors(self.iCreateRobot)
    #self.sensor_monitor = icreatesensors.SensorMonitor(self.sensors)
    #self.sensor_monitor.Start()

  def SetWheelSpeeds(self, left, right):
    try:
      # Multiplied by 5, as max speed is 5 times higher than default max
      left = left * 5
      right = right * 5
      self.iCreateRobot.SetWheelVelocity(left, right)
    except icreatepyrobot.StateError, e:
      print 'Trying to set wheel velocity in wrong state.'

  def Reset(self):
    """Reset robot state."""
    self.iCreateRobot.SoftReset()
    self.iCreateRobot.Control()

  def StartSensorStream(self):
    """Initiate sensors"""
    #TODO: Implement ICreate Sensors
    pass

  def StopSensorStream(self):
    """Stop sensor stream."""
    #TODO: Implement ICreate Sensors
    pass
