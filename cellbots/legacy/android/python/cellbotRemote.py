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

import android
import os
import time
import xmpp
import ConfigParser
import math
import robot
import sys
from threading import Thread

# Establish an XMPP connection
def commandByXMPP():
  global xmppClient
  jid = xmpp.protocol.JID(xmppUsername)
  xmppClient = xmpp.Client(jid.getDomain(), debug=[])
  xmppClient.connect(server=(xmppServer, xmppPort))
  try:
    xmppClient.RegisterHandler('message', XMPP_message_cb)
  except:
    exitCellbot('XMPP error. You sure the phone has an internet connection?')
  if not xmppClient:
    exitCellbot('XMPP Connection failed!')
    return
  auth = xmppClient.auth(jid.getNode(), xmppPassword, 'cellbot')
  if not auth:
    exitCellbot('XMPP Authentication failed!')
    return
  xmppClient.sendInitPresence()
  print "XMPP username for the robot is:\n" + xmppUsername
  runRemoteControl()


# Handle XMPP messages coming from commandByXMPP
def XMPP_message_cb(session, message):
  jid = xmpp.protocol.JID(message.getFrom())
  global operator
  operator = jid.getNode() + '@' + jid.getDomain()
  command = message.getBody()
  print str(command)
      
# Listen for incoming Bluetooth resonses. If this thread stops working, try rebooting. 
class bluetoothReader(Thread):
  def __init__ (self):
    Thread.__init__(self)
 
  def run(self):
    while True:
      if not droid.bluetoothReady():
        time.sleep(0.05)
        continue
        result += droid.bluetoothRead()
        if '\n' in result:
          npos = result.find('\n')
          yield result[:npos]
          result = result[npos+1:]
          print result

# Give the user an option to try other actions while still using the remote as an accelerometer
class remoteCommandOptions(Thread):
  def __init__ (self):
    Thread.__init__(self)
 
  def run(self):
    command = ''
    msg = ''
    global running
    global pauseSending
    while command != "Exit":
      try:
        command = robot.pickFromList("Pick an action (set down phone to pause)",
                                     ['Say Hello', 'Point Using Compass', 'Take Picture','Speak Location','Voice Command','Exit'])
        # Pause sending commands so that robot only does what user selected here
        pauseSending = True
        if command == "Take Picture":
          commandOut("picture", True)
          droid.speak("Asking robot to take a picture")
          droid.makeToast("Please wait, this may take a few seconds")
          time.sleep(5)
          msg = "Picture should be taken by now"
        elif command == "Speak Location":
          msg = "Speaking location"
          commandOut("x", True)
        elif command == "Voice Command":
          try:
            voiceCommand = droid.recognizeSpeech().result
            commandOut(voiceCommand, True)
            msg = "Told the robot to " + voiceCommand
            droid.makeToast(msg)
            time.sleep(2)
          except:
            msg = "Could not understand"
        elif command == "Point Using Compass":
          direction = robot.pickFromList("Pick a direction", ['North','East','West','South'])
          commandOut("p " + direction, True)
          msg = "Asking robot to point " + direction
          droid.speak(msg)
          time.sleep(10)
          msg = "Robot should be facing " + direction
        elif command == "Say Hello":
          msg = "Asking robot to say hello"
          commandOut("hi", True)
        elif command == "Exit":
          msg = "Bye bye. Come again."
      except KeyError:
        msg = "Sorry, please try that again"
      droid.speak(msg)
      time.sleep(1)
      # This resumes sending of normal accelerometer stream of commands
      pauseSending = False
    commandOut("w 0 0", True)
    # This will exit the main loop as well. Remove this if you only want to exit the pop-up menu.
    running = False

# Initialize Bluetooth outbound if configured for it
def initializeBluetooth():
  droid.toggleBluetoothState(True)
  droid.bluetoothConnect("00001101-0000-1000-8000-00805F9B34FB", bluetoothAddress) #this is a magic UUID for serial BT devices
  droid.makeToast("Initializing Bluetooth connection")
  time.sleep(4)

# Send command out of the device over BlueTooth or XMPP
def commandOut(msg, override=False):
  if outputMethod == "outputBluetooth":
    droid.bluetoothWrite(msg + '\r\n')
  elif not pauseSending or override:
    global previousMsg
    global lastMsgTime
    try:
      # Don't send the same message repeatedly unless 1 second has passed
      if msg != previousMsg or (time.time() > lastMsgTime + 1000):
        xmppClient.send(xmpp.Message(xmppRobotUsername, msg))
    except IOError:
      specialToast("Failed to send command to robot")
    previousMsg=msg
    lastMsgTime = time.time()

# Send command out of the device over BlueTooth or XMPP
def specialToast(msg):
  global previousToastMsg
  global lastToastMsgTime
  try:
    # Don't toast the same message repeatedly unless 5 seconds have passed
    if msg != previousToastMsg or (time.time() > lastToastMsgTime + 5000):
      droid.makeToast(msg)
  except:
    pass
  previousToastMsg=msg
  lastToastMsgTime = time.time()
    
# The main thread that continuously takes accelerometer data to drive the robot
def runRemoteControl():
  droid.startSensing()
  time.sleep(1.0) # give the sensors a chance to start up
  while running:
    try:
      sensor_result = droid.readSensors()
      pitch=float(sensor_result.result['pitch'])
      roll=float(sensor_result.result['roll'])
    except TypeError:
      pitch = 0
      roll = 0
      specialToast("Failed to read sensors")

    #Convert the radions returned into degrees
    pitch = pitch * 57.2957795
    roll = roll * 57.2957795

    # Assumes the phone is flat on table for no speed ad no turning
    # Translate the pitch into a speed ranging from -100 (full backward) to 100 (full forward).
    # Also support a gutter (dead spot) in the middle and buzz the phone when user is out of range.
    if pitch > 50:
      speed = 100
      droid.vibrate((pitch -50) * 10)
      specialToast("Too far forward")
    elif pitch < -50:
      speed = -100
      droid.vibrate(((roll *-1) -50) * 10)
      specialToast("Too far backward")
    elif pitch in range(-5,5):
      speed = 0
    else:
      # Take the roll that range from 50 to -50 and multiply it by two and reverse the sign
      speed = pitch * 2

    # Translate the roll into a direction ranging from -100 (full left) to 100 (full right).
    # Also support a gutter (dead spot) in the middle and buzz the phone when user is out of range.
    if roll > 50:
      direction = 100
      droid.vibrate((roll -50) * 10)
      specialToast("Too far left")
    elif roll < -50:
      direction = -100
      droid.vibrate(((roll *-1) -50) * 10)
      specialToast("Too far right")
    elif roll in range(-5,5):
      direction = 0
    else:
      # Take the roll that range from 50 to -50 and multiply it by two and reverse the sign
      direction = roll * 2

    # Reverse turning when going backwards to mimic what happens when steering a non-differential drive system
    # where direction is really a "bias" and not a true turning angle.
    if speed < 0:
      direction = direction * -1

    # Clamp speed and direction between -100 and 100 just in case the above let's something slip
    speed = max(min(speed, 100), -100)
    direction = max(min(direction, 100), -100)

    # Apply acceleration scaling factor since linear use of the accelerometer goes to fast with minor tilts
    scaledSpeed = math.pow(abs(speed) / 100.00, speedScaleFactor)
    speed = math.copysign(scaledSpeed, speed) * 100.00
    scaledDirection = math.pow(abs(direction) / 100.00, directionScaleFactor)
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
    # See the writeup at http://code.google.com/p/cellbots/wiki/TranslatingUserControls

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

    #print pitch, roll, int(speed), int(direction)

    command = "w %d %d" % (left, right)
    #print command
    commandOut(command)

    time.sleep(0.10)
  sys.exit()

# Get configurable options from the ini file, prompt user if they aren't there, and save if needed
def getConfigFileValue(config, section, option, title, valueList, saveToFile):
  # Check if option exists in the file
  if config.has_option(section, option):
    values = config.get(section, option)
    values = values.split(',')
    # Prompt the user to pick an option if the file specified more than one option
    if len(values) > 1:
      setting = robot.pickFromList(title, values)
    else:
      setting = values[0]
  else:
    setting = ''
  # Deal with blank or missing values by prompting user
  if not setting or not config.has_option(section, option):
    # Provide an empty text prompt if no list of values provided
    if not valueList:
      setting = robot.getInput(title).result
    # Let the user pick from a list of values
    else:
      setting = robot.pickFromList(title, valueList)
    if saveToFile:
      config.set(section, option, setting)
      with open(configFilePath, 'wb') as configfile:
        config.write(configfile)
  # Strip whitespace and try turning numbers into floats
  setting = setting.strip()
  try:
    setting = float(setting)
  except ValueError:
    pass
  return setting

# Setup the config file for reading and be sure we have a phone type set
config = ConfigParser.ConfigParser()
configFilePath = "/sdcard/sl4a/scripts/cellbotRemoteConfig.ini"
config.read(configFilePath)
if config.has_option("basics", "phoneType"):
  phoneType = config.get("basics", "phoneType")
else:
  phoneType = "android"
  config.set("basics", "phoneType", phoneType)
robot = robot.Robot(phoneType)

#Non-configurable settings
droid = android.Android()
previousMsg = ""
lastMsgTime = time.time()
running = True
pauseSending = False

# List of config values to get from file or prompt user for
outputMethod = getConfigFileValue(config, "control", "outputMethod", "Select Output Method", ['outputXMPP', 'outputBluetooth'], True)
speedScaleFactor = getConfigFileValue(config, "control", "speedScaleFactor", "Speed scale factor", '', False)
directionScaleFactor = getConfigFileValue(config, "control", "directionScaleFactor", "Direction scale factor", '', False)
bluetoothAddress = config.get("control", "bluetoothAddress")

# Only get these settings if we using XMPP
if outputMethod == "outputXMPP":
  xmppUsername = getConfigFileValue(config, "xmpp", "username", "Remote control username", '', True)
  xmppPassword = getConfigFileValue(config, "xmpp", "password", "Chat password", '', False)
  xmppRobotUsername = getConfigFileValue(config, "xmpp", "robotUsername", "Robot username", '', True)
  xmppServer = config.get("xmpp", "server")
  xmppPort = config.getint("xmpp", "port")

# The main loop that fires up a telnet socket and processes inputs
def main():
  print "Lay the phone flat to pause the program.\n"
  # When controlling a robt directly over Bluetooth we can also listen for a response in a new thread.
  # We assume there is no phone on the robot to take additional commands though.
  if outputMethod == "outputBluetooth":
    initializeBluetooth()
    readerThread = bluetoothReader()
    #TEMP REM THIS OUT UNTIL ASE FIXES BLUETOOTH HANDLING readerThread.start()
  # When sending XMPP to the bot we assume a phone is on the robot to take additional commands.
  elif outputMethod == "outputXMPP":
    global optionsThread
    optionsThread = remoteCommandOptions()
    optionsThread.daemon = True
    optionsThread.start()
    commandByXMPP()
  else:
    droid.makeToast("Unsupported output method")
    time.sleep(1)
    sys.exit()
  droid.makeToast("Move the phone to control the robot")
  runRemoteControl()

if __name__ == '__main__':
    main()
