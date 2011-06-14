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

__license__ = 'Apache License, Version 2.0'

import ConfigParser
import os
import sys
import time
from threading import Thread

import android
import math
from threadedAndroid import droid
import utils
import xmpp

class RemoteState(object):
  def __init__(self):
    self.running = True
    self.pauseSending = False

class RemoteUplink(object):
  def __init__(self, remoteUplink, state):
    self.remoteUplink = remoteUplink
    self.state = state
    self.previousMsg = ""
    self.lastMsgTime = time.time()
    self.previousToastMsg = ''
    self.lastToastMsgTime = 0

  # Send command out of uplink
  def sendCmd(self, droid, msg, override=False):
    if not self.state.pauseSending or override:
      try:
        # Don't send the same message repeatedly unless 1 second has passed
        if msg != self.previousMsg or (time.time() > self.lastMsgTime + 1000):
          self.remoteUplink.sendCmd(msg)
      except IOError:
        self.specialToast(droid, "Failed to send command to robot")
      self.previousMsg=msg
      self.lastMsgTime = time.time()

  # Send command out of the device over BlueTooth or XMPP
  def specialToast(self, droid, msg):
    try:
      # Don't toast the same message repeatedly unless 5 seconds have passed
      if msg != self.previousToastMsg or \
         (time.time() > self.lastToastMsgTime + 5000):
        droid.makeToast(msg)
    except:
      pass
    self.previousToastMsg=msg
    self.lastToastMsgTime = time.time()

class CellbotRemote(Thread):
  """Cellbot remote control"""

  def __init__(self, config, uplink, sendQuit=False):
    Thread.__init__(self)
    self.config = config
    self.state = RemoteState()
    self.remoteUplink = RemoteUplink(uplink, self.state)
    self.droid = droid
    self.optionsThread = RemoteCommandOptions(self.remoteUplink, self.state,
                                              sendQuit)
    self.optionsThread.daemon = True

  def startOptions(self):
    self.optionsThread.start()

  def run(self):
    self.droid.startSensing()
    time.sleep(1.0) # give the sensors a chance to start up
    while self.state.running:
      try:
        sensor_result = self.droid.readSensors()
        pitch=float(sensor_result.result['pitch'])
        roll=float(sensor_result.result['roll'])
      except TypeError:
        pitch = 0
        roll = 0
        self.remoteUplink.specialToast(self.droid, "Failed to read sensors")

      # Convert the radions returned into degrees
      pitch = pitch * 57.2957795
      roll = roll * 57.2957795

      # Assumes the phone is flat on table for no speed ad no turning
      # Translate the pitch into a speed ranging from -100 (full backward) to
      # 100 (full forward).
      # Also support a gutter (dead spot) in the middle and buzz the phone when
      # user is out of range.
      if pitch > 50:
        speed = 100
        self.droid.vibrate((pitch -50) * 10)
        self.remoteUplink.specialToast(self.droid, "Too far forward")
      elif pitch < -50:
        speed = -100
        self.droid.vibrate(((pitch *-1) -50) * 10)
        self.remoteUplink.specialToast(self.droid, "Too far backward")
      elif pitch in range(-5,5):
        speed = 0
      else:
        # Take the pitch that range from 50 to -50 and multiply it by two and
        # reverse the sign
        speed = pitch * 2

      # Translate the roll into a direction ranging from -100 (full left) to 100
      # (full right).
      # Also support a gutter (dead spot) in the middle and buzz the phone when
      # user is out of range.
      if roll > 50:
        direction = 100
        self.droid.vibrate((roll -50) * 10)
        self.remoteUplink.specialToast(self.droid, "Too far left")
      elif roll < -50:
        direction = -100
        self.droid.vibrate(((roll *-1) -50) * 10)
        self.remoteUplink.specialToast(self.droid, "Too far right")
      elif roll in range(-5,5):
        direction = 0
      else:
        # Take the roll that range from 50 to -50 and multiply it by two and
        # reverse the sign
        direction = roll * 2

      # Reverse turning when going backwards to mimic what happens when steering
      # a non-differential drive system
      # where direction is really a "bias" and not a true turning angle.
      if speed < 0:
        direction = direction * -1

      # Clamp speed and direction between -100 and 100 just in case the above
      # let's something slip
      speed = max(min(speed, 100), -100)
      direction = max(min(direction, 100), -100)

      # Apply acceleration scaling factor since linear use of the accelerometer
      # goes to fast with minor tilts
      # Apply acceleration scaling factor since linear use of the accelerometer
      # goes to fast with minor tilts
      scaledSpeed = math.pow(abs(speed) / 100.00, self.config.speedScaleFactor)
      speed = math.copysign(scaledSpeed, speed) * 100.00
      scaledDirection = math.pow(abs(direction) / 100.00,
                                 self.config.directionScaleFactor)
      direction = math.copysign(scaledDirection, direction) * 100.00

      # Okay, speed and direction are now both in the range of -100:100.
      # Speed=100 means to move forward at full speed.  direction=100 means
      # to turn right as much as possible.

      # Treat direction as the X axis, and speed as the Y axis.
      # If we're driving a differential-drive robot (each wheel moving forward
      # or back), then consider the left wheel as the X axis and the right
      # wheel as Y.
      # If we do that, then we can translate [speed,direction] into [left,right]
      # by rotating by -45 degrees.
      # See the writeup at
      # http://code.google.com/p/cellbots/wiki/TranslatingUserControls

      # This actually rotates by 45 degrees and scales by 1.414, so that full
      # forward = [100,100]
      right = speed - direction
      left = speed + direction

      # But now that we've scaled, asking for full forward + full right turn
      # means the motors need to go to 141.  If we're asking for > 100, scale
      # back without changing the proportion of forward/turning
      if abs(left) > 100 or abs(right) > 100:
        scale = 1.0
        # if left is bigger, use it to get the scaling amount
        if abs(left) > abs(right):
          scale = 100.0 / abs(left)
        else:
          scale = 100.0 / abs(right)

        left = int(scale * left)
        right = int(scale * right)

      command = "ws %d %d" % (left, right)
      self.remoteUplink.sendCmd(self.droid, command)

      time.sleep(0.25)
    sys.exit()

# Give the user an option to try other actions while still using the remote as
# an accelerometer
class RemoteCommandOptions(Thread):
  kCardinals = {
    'North': '0', 'East': '90', 'West': '270', 'South': '180'
    }

  def __init__ (self, remoteUplink, remoteState, sendQuit=False):
    """ Initialize  remote command options thread.

    This handles the remote control menu, displays menu, get user input, send
    commands.

    Args:
      remoteUplink: RemoteUplink object.
      remoteState: RemoteState object, shared with CellbotRemote object.
      sendQuit: If true, send quit command on exit.
    """
    Thread.__init__(self)
    self.remoteUplink = remoteUplink
    self.state = remoteState
    self.droid = droid
    self.unlocked_droid = android.Android()
    self.sendQuit = sendQuit

  def run(self):
    command = ''
    msg = ''
    while command != "Exit":
      try:
        command = utils.pickFromList(self.unlocked_droid,
            "Pick an action (set down phone to pause)",
            ['Say Hello', 'Point Using Compass', 'Take Picture',
             'Speak Location', 'Voice Command','Exit'])
      except KeyError as e:
        msg = "Sorry, please try that again. %s" % str(e)
        self.droid.makeToast(msg)
      else:
        # Pause sending commands so that robot only does what user selected here
        self.state.pauseSending = True
        if command == "Take Picture":
          self.remoteUplink.sendCmd(self.droid, "picture", True)
          self.droid.ttsSpeak("Asking robot to take a picture")
          self.droid.makeToast("Please wait, this may take a few seconds")
          time.sleep(5)
          msg = "Picture should be taken by now"
        elif command == "Speak Location":
          msg = "Speaking location"
          self.remoteUplink.sendCmd(self.droid, "x", True)
        elif command == "Voice Command":
          try:
            voiceCommand = droid.recognizeSpeech().result
            self.remoteUplink.sendCmd(self.droid, voiceCommand, True)
            msg = "Told the robot to " + voiceCommand
            self.droid.makeToast(msg)
            time.sleep(2)
          except:
            msg = "Could not understand"
        elif command == "Point Using Compass":
          msg = "This feature is currently not available on the robot."
          self.droid.makeToast(msg)
          # try:
          #   direction = utils.pickFromList(self.unlocked_droid,
          #       "Pick a direction", sorted([c for c in self.kCardinals]))
          # except KeyError as e:
          #   msg = "Sorry, please try that again. %s" % str(e)
          #   self.droid.makeToast(msg)
          # else:
          #   self.droid.ttsSpeak("Selected direction %s." % direction)
          #   cmd = "p " + self.kCardinals[direction]
          #  self.remoteUplink.sendCmd(self.droid, cmd, True)
          #  msg = "Asking robot to point " + direction
          #  self.droid.ttsSpeak(msg)
          #  time.sleep(2)
          #  msg = "Robot should be facing " + direction
        elif command == "Say Hello":
          msg = "Asking robot to say hello"
          self.remoteUplink.sendCmd(self.droid, "hi", True)
        elif command == "Exit":
          msg = "Bye bye. Come again."
          if self.sendQuit:
            self.remoteUplink.sendCmd(self.droid, "q", True)
      self.droid.ttsSpeak(msg)
      time.sleep(1)
      # This resumes sending of normal accelerometer stream of commands
      self.state.pauseSending = False
    self.remoteUplink.sendCmd(self.droid, "ws 0 0", True)
    # This will exit the main loop as well. Remove this if you only want to exit
    # the pop-up menu.
    self.state.running = False
