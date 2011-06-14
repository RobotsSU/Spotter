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

__author__ = 'Ryan Hickman <rhickman@gmail.com>'
#Contributers = Glen Arrowsmith glen@cellbots.com
__license__ = 'Apache License, Version 2.0'

import os
import time
import datetime
import socket
import select
import sys
import math
import shlex
import netip
import xmpp
import ConfigParser
import string
import re
import robot
from threading import Thread

# Listen for incoming serial responses. If this thread stops working, try rebooting. 
class serialReader(Thread):
  def __init__ (self):
    Thread.__init__(self)
  def run(self):
    process = robot.getSerialIn()
    while process:
      try:
        botReply = process.readline()
        if botReply:
          if len(botReply.strip()) > 1:
            splitReply = botReply.split(':')
            if len(splitReply) == 2:
              addToWhiteboard(splitReply[0], splitReply[1])
            outputToOperator("Bot says %s " % botReply)
      except:
        outputToOperator("Reader Thread Errored")

#Whiteboard is used to store sensor values and their history
def addToWhiteboard(key, value):
  global whiteboard
  timeAdded = datetime.datetime.today()
  valueAndTime = [value, timeAdded]
  try:
    #get the old values
    values = whiteboard[key]
  except KeyError:
    #Its a new sensor. Create it.
    values = []
  values.insert(0, valueAndTime)
  #stop memory leaks
  while len(values) >= MAX_WHITEBOARD_LENGTH:
    #remove the last one
    del values[len(values)-1]
  #add back to the whiteboard
  whiteboard[key] = values

#For debugging. 
def WhiteboardToString(includeHistory=False):
  global whiteboard
  str = ""
  for key in whiteboard:
    str += key + '\n'
    for history in whiteboard[key]:
      str += '\tvalue: %s' % history[0] + '\n'
      str += '\ttime : %s' % history[1].strftime("%A, %d. %B %Y %I:%M%p") + '\n'
      if includeHistory == False:
        break        
  return str
        
        
# Listen for incoming Bluetooth resonses. If this thread stops working, try rebooting. 
class bluetoothReader(Thread):
  def __init__ (self):
    Thread.__init__(self)
    
  def run(self):
    while True:
      if not robot.bluetoothReady():
        time.sleep(0.05)
        continue
        botReply += robot.bluetoothRead()
        if '\n' in result:
          npos = botReply.find('\n')
          yield botReply[:npos]
          botReply = botReply[npos+1:]
          outputToOperator("Bot says %s " % botReply)

# Initialize Bluetooth outbound if configured for it
def initializeBluetooth():
  robot.toggleBluetoothState(True)
  if bluetoothAddress:
    robot.bluetoothConnect('00001101-0000-1000-8000-00805F9B34FB', bluetoothAddress) #this is a magic UUID for serial BT devices
  robot.makeToast("Initializing Bluetooth connection")
  time.sleep(3)
  
# Command input via open telnet port
def commandByTelnet():
  rs = []
  global svr_sock  # Fixing crash after exit command
  svr_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
  print "Firing up telnet socket..."
  try:
    svr_sock.bind(('', telnetPort))
    svr_sock.listen(3)
    svr_sock.setblocking(0)
    print "Ready to accept telnet. Use %s on port %s\n" % (phoneIP, telnetPort)
  except socket.error, (value,message):
    print "Could not open socket: " + message
    print "You can try using %s on port %s\n" % (phoneIP, telnetPort)

  while 1:
    r,w,_ = select.select([svr_sock] + rs, [], [])

    for cli in r:
      if cli == svr_sock:
        new_cli,addr = svr_sock.accept()
        rs = [new_cli]
      else:  
        input = cli.recv(1024)
        input = input.replace('\r','')
        input = input.replace('\n','')
        if input != '':
          print "Received: '%s'" % input
          cli.sendall("ok\r\n")   # Send OK after every command recieved via telnet
          commandParse(input)

# Command input via XMPP chat
def commandByXMPP():
  global xmppUsername
  global xmppPassword
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
  auth = xmppClient.auth(jid.getNode(), xmppPassword, 'botty')
  if not auth:
    exitCellbot('XMPP Authentication failed!')
    return
  xmppClient.sendInitPresence()
  print "XMPP username for the robot is:\n" + xmppUsername
  print "\r\nRobot is now ready to take commands."
  try:
    while True:
      xmppClient.Process(1)
  except KeyboardInterrupt:
    pass

# Handle XMPP messages coming from commandByXMPP
def XMPP_message_cb(session, message):
  jid = xmpp.protocol.JID(message.getFrom())
  global operator
  operator = jid.getNode() + '@' + jid.getDomain()
  command = message.getBody()
  commandParse(str(command))

# Command input via speech recognition
def commandByVoice(mode='continuous'):
  try:
    voiceCommands = str(robot.recognizeSpeech().result)
  except:
    voiceCommands = ""
  outputToOperator("Voice commands: %s" % voiceCommands)
  commandParse(voiceCommands)
  if mode == 'continuous':
    commandByVoice()

# Speak using TTS or make toasts
def speak(msg,override=False):
  global previousMsg
  if (audioOn and msg != previousMsg) or override:
    robot.speak(msg)
  elif msg != previousMsg:
    robot.makeToast(msg)
  outputToOperator(msg)
  previousMsg=msg

# Handle changing the speed setting  on the robot
def changeSpeed(newSpeed):
  newSpeed = int(newSpeed)  # For correct math operation must be newSpeed integer not string
  global currentSpeed
  if newSpeed in [0,1,2,3,4,5,6,7,8,9]:
    msg = "Changing speed to %s" % newSpeed
    if microcontroller != "AVR_Stepper": 
      commandOut(chr(newSpeed))
    currentSpeed = newSpeed
  else:
    msg = "Speed %s is out of range [0-9]" % newSpeed
  speak(msg)

   
# Point towards a specific compass heading
def orientToAzimuth(azimuth):
  onTarget = False
  stopTime = time.time() + 60
  while not onTarget and time.time() < stopTime:
    results = robot.readSensors().result
    if results is not None:
      currentHeading = results['azimuth']
      msg = "Azimuth: %d Heading: %d" % (azimuth,currentHeading)
      delta = azimuth - currentHeading
      if math.fabs(delta) > 180:
        if delta < 0:
          adjustment = delta + 360
        else:
          adjustment = delta - 360
      else:
        adjustment = delta
      adjustmentAbs = math.fabs(adjustment)
      if adjustmentAbs < cardinalMargin:
        onTarget = True
      else:
        if adjustment > cardinalMargin:
          outputToOperator("Moving %d right." % adjustmentAbs)
          commandOut('w 15 -15')
        if adjustment < (cardinalMargin * -1):
          outputToOperator("Moving %d left." % adjustmentAbs)
          commandOut('w -15 15')
        # Let the robot run for an estimated number of seconds to turn in the intended direction
        # We should have setting for this and/or allow some calibration for turning rate of the bot
        time.sleep(adjustmentAbs/180)
        commandOut('w 0 0')
        # Stop for a 1/2 second to take another compass reading
        time.sleep(0.5)
    else:
      msg = "Could not start sensors."
  # Loop finished because we hit target or ran out of time
  if onTarget:
    msg = "Goal achieved! Facing %d degrees, which is within the %d degree margin of %d!" % (currentHeading, cardinalMargin, azimuth)
    speak(msg)
    commandOut('w 0 0')
  else:
    msg = "Ran out of time before hitting proper orientation."
    speak(msg)
    commandOut('w 0 0')   

# Extra processing when using a serial servo controller
def serialServoDrive(leftPWM, rightPWM):
  #Turn a range of -100 to 100 into a range of 1 to 127
  leftPWM = (int(leftPWM) + 100) * 0.635
  rightPWM = ((int(rightPWM) * -1) + 100) * 0.635
  #Make sure nothing fell out of range
  leftPWM = max(min(int(leftPWM), 127), 1)
  rightPWM = max(min(int(rightPWM), 127), 1)
  #Turn the numbers into hex for the servo controller plus some expected commands in front
  serialCommand = '\x80\x01\x02\x01' + chr(leftPWM) + '\x80\x01\x02\x02' + chr(rightPWM)
  commandOut(serialCommand)
  
# Extra processing when using a serial AVR Stepper Driver by MiliKiller
def AVR_Stepper_Controll(leftPWM, rightPWM):
  #Make sure nothing fell out of range
  leftPWM = max(min(int(leftPWM), 100), (-100))
  rightPWM = max(min(int(rightPWM), 100), (-100))
  #Send Command to AVR Stepper Driver Board
  serialCommand = "w " + str(leftPWM) + " " + str(rightPWM)
  commandOut(serialCommand)

# Send command out of the device via Bluetooth or serial
def commandOut(msg):
  if outputMethod == "outputBluetooth":
    if microcontroller == "AVR_Stepper":
      robot.bluetoothWrite(msg.split(" ")[1] + "," + msg.split(" ")[2] + '\r\n')
    else:
      robot.bluetoothWrite(msg + '\n')
  elif microcontroller == "serialservo":
    robot.writeSerialOut(r"echo -e '%s' > /dev/ttyMSM2" % msg)
  elif microcontroller == "AVR_Stepper":
    robot.writeSerialOut(r"echo -e '%s' > /dev/ttyMSM2" % (msg.split(" ")[1] + "," + msg.split(" ")[2] + '\r\n'))
  else:
    robot.writeSerialOut("echo '<%s>' > /dev/ttyMSM2" % msg)

# Display information on screen and/or reply to the human operator
def outputToOperator(msg):
  print msg
  try:
    if (inputMethod == 'commandByXMPP'):
      xmppClient.send(xmpp.Message(operator, msg))
  except:
    pass
  
# Shut down sensing and other open items and quit
def exitCellbot(msg='Exiting'):
  if inputMethod == "commandByTelnet":
    svr_sock.close()
  robot.stopSensing()
  time.sleep(1)   # Fixing crash before exit on G1
  robot.stopLocating()
  #Stop the servos if using a serial controller
  if microcontroller == "serialservo":
    serialServoDrive(0, 0)
    time.sleep(1)
  sys.exit(msg)

# Parse the first character of incoming commands to determine what action to take
def commandParse(input):
  # Split the incoing string into a command and up to two values
  try:
    commandList = shlex.split(input)
  except:
    commandList = []
    outputToOperator("Could not parse command")
  try:
    command = commandList[0].lower()
  except IndexError:
    command = ""
  try:
    commandValue = commandList[1]
  except IndexError:
    commandValue = ""
  try:
    commandValue2 = commandList[2]
  except IndexError:
    commandValue2 = ""

  # Process known commands
  if command in ["a", "audio", "record"]:
    global audioRecordingOn
    audioRecordingOn = not audioRecordingOn
    fileName=time.strftime("/sdcard/cellbot_%Y-%m-%d_%H-%M-%S.3gp")
    if audioRecordingOn:
      outputToOperator("Starting audio recording")
      robot.makeToast("Starting audio recording")
      robot.startAudioRecording(fileName)
    else:
      robot.stopAudioRecording()
      robot.makeToast("Stopping audio recording")
      outputToOperator("Stopped audio recording. Audio file located at '%s'" % fileName)
  elif command  in ["b", "back", "backward", "backwards"]:
    speak("Moving backward")
    cmd = "w %s %s" % (currentSpeed * -10, currentSpeed * -10)
    commandOut(cmd)
  elif command in ["compass", "heading"]:
    orientToAzimuth(int(commandValue[:3]))
  elif command in ["d", "date"]:
    speak(time.strftime("Current time is %_I %M %p on %A, %B %_e, %Y"))
  elif command in ["f", "forward", "forwards", "scoot"]:
    speak("Moving forward")
    cmd = "w %s %s" % (currentSpeed * 10, currentSpeed * 10)
    commandOut(cmd)
  elif command in ["h", "hi", "hello"]:
    speak("Hello. Let's play.")
  elif command in ["l", "left"]:
    speak("Moving left")
    cmd = "w %s %s" % (currentSpeed * -10, currentSpeed * 10)
    commandOut(cmd)
  elif command in ["m", "mute", "silence"]:
    global audioOn
    audioOn = not audioOn
    speak("Audio mute toggled")
  elif command in ["p", "point", "pointe", "face", "facing"]:
    msg = "Orienting %s" % cardinals[commandValue[:1].lower()][0]
    speak(msg)
    try:
      orientToAzimuth(int(cardinals[commandValue[:1].lower()][1]))
    except:
      outputToOperator("Could not orient towards " + commandValue)
  elif command in ["q", "quit", "exit"]:
    speak("Bye bye!")
    exitCellbot("Exiting program after receiving 'q' command.")
  elif command in ["r", "right"]:
    speak("Moving right")
    cmd = "w %s %s" % (currentSpeed * 10, currentSpeed * -10)
    commandOut(cmd)
  elif command in ["s", "stop"]:
    commandOut('w 0 0')
    outputToOperator("Stopping")
  elif command in ["t", "talk", "speak", "say"]:
    msg = robot.replaceInsensitive(input, command, '').strip()
    speak(msg,True)
  elif command in ["v", "voice", "listen", "speech"]:
    robot.makeToast("Launching voice recognition")
    outputToOperator("Launching voice recognition")
    commandByVoice("onceOnly")
  elif command in ["x", "location", "gps"]:
    try:
      robot.startLocating()
      location = robot.readLocation().result
      addresses = robot.geocode(location['latitude'], location['longitude']).result
      firstAddr = addresses[0]
      msg = 'You are in %(locality)s, %(admin_area)s' % firstAddr
    except:
      msg = "Failed to find location."
    speak(msg)
  elif command == "speed":
    if commandValue in ["0","1","2","3","4","5","6","7","8","9"]:
      changeSpeed(commandValue)
    else:
      outputToOperator("Invalid speed setting: '%s'" % command)
  elif command in ["faster", "hurry", "fast", "quicker"]:
    changeSpeed(currentSpeed + 1)
  elif command in ["slower", "slow", "chill"]:
    changeSpeed(currentSpeed - 1)
  # Prefixes that we ignore and then process the following word
  elif command in ["move", "go", "turn", "take"]:
    commandParse(commandValue)
  elif command in ["send", "pass"]:
    commandOut(commandValue)
    print "Passing %s" % commandValue
  elif command in ["range", "distance", "dist", "z"]:
    commandOut("fr")
    outputToOperator("Checking distance")    
    #ReaderThread thread will handle the response.
  elif command in ["c", "config", "calibrate"]:
    commandOut("c" + commandValue + " " + commandValue2)
    msg = "Calibrating servos to center at %s and %s" % (commandValue, commandValue2)
    outputToOperator(msg)
  elif command in ["w", "wheel"]:
    if microcontroller == "arduino":
      commandOut("w" + commandValue + " " + commandValue2)
    elif microcontroller == "serialservo":
      serialServoDrive(commandValue, commandValue2)
    elif microcontroller == "AVR_Stepper":
      AVR_Stepper_Controll(commandValue, commandValue2)
    else:
      msg = "Unknown microcontroller type: " + microcontroller
      outputToOperator(msg)
    addToWhiteboard("w", commandValue + " " + commandValue2)
  elif command in ["i", "infinite"]:
    commandOut("i")
    outputToOperator("Toggled infinite rotation mode on robot")
  elif command in ["picture", "takepicture"]:
    fileName=time.strftime("/sdcard/cellbot_%Y-%m-%d_%H-%M-%S.jpg")
    robot.cameraTakePicture(fileName)
    outputToOperator("Took picture. Image file located at '%s'" % fileName)
    addToWhiteboard("Picture", fileName)
  elif command in ["whiteboard", "whiteboardfull"]:
    if command == "whiteboardfull":
      outputToOperator(WhiteboardToString(True))
    else:
      outputToOperator(WhiteboardToString())
  elif command in ["reset"]:
    commandOut("reset")
    outputToOperator("Reset hardwares settings to default")
  elif command in ["pair", "pairing"]:
    commandOut("p")
    outputToOperator("Asking Bluetooth module to go into pairing")
  else:
    outputToOperator("Unknown command: '%s'" % command)

#Non-configurable settings
cardinals = {}
cardinals['n']=('North','0')
cardinals['e']=('East','90')
cardinals['w']=('West','270')
cardinals['s']=('South','180')
previousMsg = ""
audioRecordingOn = False
phoneIP = netip.displayNoLo()

# Defines the dict of sensor values and their history
whiteboard = {}
MAX_WHITEBOARD_LENGTH = 30

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
configFilePath = "/sdcard/sl4a/scripts/cellbotConfig.ini"
config.read(configFilePath)
if config.has_option("basics", "phoneType"):
  phoneType = config.get("basics", "phoneType")
else:
  phoneType = "android"
  config.set("basics", "phoneType", phoneType)
robot = robot.Robot(phoneType)

# List of config values to get from file or prompt user for
inputMethod = getConfigFileValue(config, "control", "inputMethod", "Select Input Method", ['commandByXMPP', 'commandByTelnet', 'commandByVoice'], True)
outputMethod = getConfigFileValue(config, "control", "outputMethod", "Select Output Method", ['outputSerial', 'outputBluetooth'], True)
microcontroller = getConfigFileValue(config, "basics", "microcontroller", "Microcontroller Type", ['arduino', 'serialservo','AVR_Stepper'], True)
audioOn = config.getboolean("basics", "audioOn")
currentSpeed = config.getint("basics", "currentSpeed")
cardinalMargin = config.getint("basics", "cardinalMargin")
telnetPort = config.getint("control", "port")
bluetoothAddress = config.get("control", "bluetoothAddress")

# Only get these settings if we using XMPP
if inputMethod == "commandByXMPP":
  xmppUsername = getConfigFileValue(config, "xmpp", "username", "Chat username", '', True)
  xmppPassword = getConfigFileValue(config, "xmpp", "password", "Chat password", '', False)
  xmppServer = config.get("xmpp", "server")
  xmppPort = config.getint("xmpp", "port")

# Raise the sails and fire the cannons
def main():
  outputToOperator("Send the letter 'q' or say 'quit' to exit the program.\n")
  robot.startSensing()
  robot.startLocating()
  if outputMethod == "outputBluetooth":
    initializeBluetooth()
    readerThread = bluetoothReader()
    #TEMP REM THIS OUT UNTIL ASE FIXES BLUETOOTH HANDLING readerThread.start()
  else:
    serialReader.lifeline = re.compile(r"(\d) received")
    readerThread = serialReader()
    readerThread.start()
  global currentSpeed
  if microcontroller == "arduino":
    commandOut(str(currentSpeed))
  robot.makeToast("Initiating input method...\n")
  globals()[inputMethod]()

if __name__ == '__main__':
    main()
