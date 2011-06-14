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

"""Register, parse and dispatch commands from the operator to the
cellbot. Handle all interactions with the uplink."""

__author__ = 'Marc Alvidrez <cram@google.com>, Chris Elliott <chriselliott+cellbots@google.com'
__license__ = 'Apache License, Version 2.0'

import shlex
import time

from threadedAndroid import droid
import utils

class commandTranslator(object):
  def __init__(self, config, uplink, cellbot):
    self.config = config
    self.uplink = uplink
    self.cellbot = cellbot
    # Keep track of whether we're recording audio
    self.audioRecordingOn = 0
    self.commands = self._createCommands()

  def _hasRobotCapability(self, name):
    if hasattr(self.cellbot, name):
      return True
    else:
      self.notifyOperator("Robot implementation does not have the "
                          "function: %s. Unable to execute" % name)
    return False

  # TODO: Some of these commands still need to be converted from using
  # commandOut to calling appropriate methods of the instantiated
  # cellbot. Functions that can be implemented on the phone should be
  # implemented in the instantiated cellbot. Functions that are
  # implemented on the robot should call the appropriate methods of
  # the robot protocol instance available to the cellbot instance.
  #
  # The general approach is to start the code on your phone and invoke
  # the command you are trying to convert. It will either succeed or
  # throw an exception, which should point to what needs to be fixed.
  #
  # Commands that are still unconverted (or only partially converted),
  # should be labeled as such. In addition, the ones that are believed
  # to be working have been converted to use a tuple in the
  # 'elif command in ()' instead of a list '[]'.

  def _createCommands(self):
    commands = {}

    def audioRecording(lexer):
      self.audioRecordingOn = not self.audioRecordingOn
      self.audioFileName = time.strftime(
          "/sdcard/cellbot_%Y-%m-%d_%H-%M-%S.3gp")
      if self.audioRecordingOn:
        self.notifyOperator("Starting audio recording", use_speak=False)
        if self._hasRobotCapability("startAudioRecording"):
          self.cellbot.startAudioRecording(self.audioFileName)
      else:
        self.notifyOperator("Stopping audio recording", use_speak=False)
        if self._hasRobotCapability("stopAudioRecording"):
          self.cellbot.stopAudioRecording()
        self.notifyOperator("Audio file located at '%s'" %
                            self.audioFileName, use_speak=False)
    audioRecordingCmd = (audioRecording, "Record audio.")
    commands["audio"] = audioRecordingCmd
    commands["record"] = audioRecordingCmd

    def moveBackward(lexer):
      self.notifyOperator("Moving backward")
      if self._hasRobotCapability("moveBackward"):
        self.cellbot.moveBackward()
    moveBackwardCmd = (moveBackward, "Move robot backwards.")
    commands["b"] = moveBackwardCmd
    commands["back"] = moveBackwardCmd
    commands["backward"] = moveBackwardCmd
    commands["backwards"] = moveBackwardCmd
    commands["x"] = moveBackwardCmd

    def turnToHeading(lexer):
      # TODO: Needs conversion...
      # convert parsed command into a simple list for backwards
      # compatibility
      degrees = lexer.get_token().lower()
      msg = "Orienting %s degrees." % degrees
      self.notifyOperator(msg)
      if self._hasRobotCapability("turnToHeading"):
        self.cellbot.turnToHeading(int(degrees))
    turnToHeadingCmd = (turnToHeading, "Rotate to desired heading.")
    commands["compass"] = turnToHeadingCmd
    commands["heading"] = turnToHeadingCmd
    commands["p"] = turnToHeadingCmd
    commands["point"] = turnToHeadingCmd
    commands["pointe"] = turnToHeadingCmd
    commands["face"] = turnToHeadingCmd
    commands["facing"] = turnToHeadingCmd

    def getDate(lexer):
      now = time.strftime("Current time is %_I %M %p on %A, %B %_e, %Y")
      self.notifyOperator(now)
    getDateCmd = (getDate, "Output current time.")
    commands["date"] = getDateCmd

    def moveForward(lexer):
      self.notifyOperator("Moving forward")
      if self._hasRobotCapability("moveForward"):
        self.cellbot.moveForward()
    moveForwardCmd = (moveForward, "Move robot forward.")
    commands["f"] = moveForwardCmd
    commands["forward"] = moveForwardCmd
    commands["forwards"] = moveForwardCmd
    commands["scoot"] = moveForwardCmd

    def toggleDamping(lexer):
      if self._hasRobotCapability("toggleDamping"):
        self.notifyOperator("Toggling damping, now enabled: %s"
                            % self.cellbot.damping_enabled)
        self.cellbot.toggleDamping()
    toggleDampingCmd = (toggleDamping,
                     "Toggle damping ratio for obstacle avoidance")
    commands["damping"] = toggleDampingCmd

    def sayHi(lexer):
      self.notifyOperator("Hello.")
    sayHiCmd = (sayHi, "Say hello.")
    commands["hi"] = sayHiCmd
    commands["h"] = sayHiCmd
    commands["hello"] = sayHiCmd

    def toogleInfiniteRotation(lexer):
      self.notifyOperator("Toggle infinite rotation not implemented")
    toggleInfiniteRotationCmd = (toogleInfiniteRotation,
                                 "Toggle infinite rotations")
    commands["i"] = toggleInfiniteRotationCmd
    commands["infinite"] = toggleInfiniteRotationCmd

    def killTheHumans(lexer):
      self.notifyOperator("Kill the humans!")
    killTheHumansCmd = (killTheHumans, "Kill the humans")
    commands["kill"] = killTheHumansCmd
    commands["rm"] = killTheHumansCmd

    def moveLeft(lexer):
      self.notifyOperator("Moving left")
      if self._hasRobotCapability("turnLeft"):
        self.cellbot.turnLeft()
    moveLeftCmd = (moveLeft, "Move robot left.")
    commands["l"] = moveLeftCmd
    commands["left"] = moveLeftCmd
    commands["a"] = moveLeftCmd

    def toggleMute(lexer):
      # audioOn is also used in baseCellBot.speak()
      # TODO: Move the muted state (audioOn) out of the config object.
      self.config.audioOn = not self.config.audioOn
      self.notifyOperator("Audio mute toggled")
    toggleMuteCmd = (toggleMute, "Toggle audio output.")
    commands["m"] = toggleMuteCmd
    commands["mute"] = toggleMuteCmd
    commands["silence"] = toggleMuteCmd

    def takePicture(lexer):
      self.imgFileName = time.strftime(
          "/sdcard/cellbot_%Y-%m-%d_%H-%M-%S.jpg")
      self.notifyOperator("Taking picture", use_speak=False)
      if self._hasRobotCapability("captureImage"):
        self.cellbot.captureImage(self.imgFileName)
        self.notifyOperator("Image file located at '%s'" %
                            self.imgFileName, use_speak=False)
    takePictureCmd = (takePicture, "Take Picture.")
    commands["picture"] = takePictureCmd
    commands["takepicture"] = takePictureCmd

    def quitRoboShenanigans(lexer):
      self.notifyOperator("Exiting program after receiving 'q' command.",
                          use_speak=False)
      self.uplink.Close()
      self.notifyOperator("Bye bye!", use_uplink=False)
      self.cellbot.shutdown()
    quitRoboShenanigansCmd = (quitRoboShenanigans, "Quit python application")
    commands["exit"] = quitRoboShenanigansCmd
    commands["quit"] = quitRoboShenanigansCmd
    commands["q"] = quitRoboShenanigansCmd

    def moveRight(lexer):
      self.notifyOperator("Moving right")
      if self._hasRobotCapability("turnRight"):
        self.cellbot.turnRight()
    moveRightCmd = (moveRight, "Move robot right")
    commands["r"] = moveRightCmd
    commands["right"] = moveRightCmd
    commands["d"] = moveRightCmd

    def stopPlease(lexer):
      self.notifyOperator("Stopping")
      self.cellbot.stop()
    stopPleaseCmd = (stopPlease, "Stop the robot.")
    commands["s"] = stopPleaseCmd
    commands["stop"] = stopPleaseCmd

    def talk(lexer):
      # Reassemble the string we are supposed to speak, convert all
      # words to lowercse.
      speech = ' '.join(lexer)
      if self._hasRobotCapability("speak"):
        self.cellbot.speak(speech.lower(), True)
      self.notifyOperator(speech, use_speak=False)
    talkCmd = (talk, "Speak. ie. \'talk \"hello friend!\"\'")
    commands["t"] = talkCmd
    commands["talk"] = talkCmd
    commands["speak"] = talkCmd
    commands["say"] = talkCmd

    def voiceRecognition(lexer):
      self.notifyOperator("Launching voice recognition", use_speak=False)
      voiceCommands = ""
      try:
        if self._hasRobotCapability("recognizeSpeech"):
          voiceCommands = self.cellbot.recognizeSpeech()
      except:
        voiceCommands = ""
      self.notifyOperator("Voice commands: %s" % voiceCommands,
                          use_speak=False)
      self.Parse(voiceCommands)
    voiceRecognitionCmd = (voiceRecognition,
                           "Enable voice recognition for speaking commands.")
    commands["v"] = voiceRecognitionCmd
    commands["voice"] = voiceRecognitionCmd
    commands["listen"] = voiceRecognitionCmd
    commands["speech"] = voiceRecognitionCmd

    def speakGPS(lexer):
      # TODO: Partially converted. Still needs to be able to get data
      # back from the robot.
      try:
        if self._hasRobotCapability("readLocation"):
          location = self.cellbot.readLocation()
        msg = 'You are in %(locality)s, %(admin_area)s' % location
      except:
        msg = "Failed to find location."
      self.notifyOperator(msg)
    speakGPSCmd = (speakGPS, "Speak GPS Location.")
    commands["x"] = speakGPSCmd
    commands["location"] = speakGPSCmd
    commands["gps"] = speakGPSCmd

    def changeSpeed(lexer):
      # TODO: Partially converted. Still needs verification.
      newSpeed = lexer.get_token()
      if newSpeed in [ str(x) for x in range(10) ]:
        self.notifyOperator("Changing speed to %s" % newSpeed)
        # Can't just use setWheelSpeeds() here because some AVRs apprently
        # can't change speed. Also, changeSpeed requires an integer.
        if self._hasRobotCapability("changeSpeed"):
          self.cellbot.changeSpeed(int(newSpeed))
      else:
        self.notifyOperator("Invalid speed: '%s'" % newSpeed, use_speak=False)
        self.notifyOperator("Invalid speed", use_uplink=False)
    changeSpeedCmd = (changeSpeed, "Change speed. ie. \"speed 4\"")
    commands["speed"] = changeSpeedCmd

    def changeSpeedFaster(lexer):
      # TODO: Partially converted. Still needs verification.
      self.notifyOperator("Speeding up.")
      if self._hasRobotCapability("changeSpeed"):
        self.cellbot.changeSpeed(self.config.currentSpeed + 1)
    changeSpeedFasterCmd = (changeSpeedFaster, "Speed up the robot.")
    commands["faster"] = changeSpeedFasterCmd
    commands["hurry"] = changeSpeedFasterCmd
    commands["fast"] = changeSpeedFasterCmd
    commands["quicker"] = changeSpeedFasterCmd

    def changeSpeedSlower(lexer):
      # TODO: Partially converted. Still needs verification.
      self.notifyOperator("Slowing down.")
      if self._hasRobotCapability("changeSpeed"):
        self.cellbot.changeSpeed(self.config.currentSpeed - 1)
    changeSpeedSlowerCmd = (changeSpeedSlower, "Slow down the robot.")
    commands["slower"] = changeSpeedSlowerCmd
    commands["slow"] = changeSpeedSlowerCmd
    commands["chill"] = changeSpeedSlowerCmd

    def changeVolume(lexer):
      newVolume = lexer.get_token()
      try:
        if self._hasRobotCapability("setVolume"):
          self.cellbot.setVolume(newVolume)
      except ValueError as e:
        self.notifyOperator(str(e), use_speak=False)
    changeVolumeCmd = (changeVolume, "Change volume. ie. \"volume 5\"")
    commands["volume"] = changeVolumeCmd

    def setWheelSpeeds(lexer):
      leftWheelSpeed = lexer.get_token()
      if leftWheelSpeed == '-':
        # -ve number
        leftWheelSpeed += lexer.get_token()
      rightWheelSpeed = lexer.get_token()
      if rightWheelSpeed == '-':
        # -ve number
        rightWheelSpeed += lexer.get_token()
      #TODO: Limits are specific to each robot. Move into robot file.
      if leftWheelSpeed not in [ str(x) for x in range(-100, 100) ]:
        self.notifyOperator(
            "Invalid left wheel speed: '%s'" % leftWheelSpeed, use_speak=False)
        self.notifyOperator(
            "Invalid left wheel speed", use_uplink=False)
      elif rightWheelSpeed not in [ str(x) for x in range(-100, 100) ]:
        self.notifyOperator(
            "Invalid right wheel speed: '%s'" % rightWheelSpeed,
            use_speak=False)
        self.notifyOperator(
            "Invalid right wheel speed", use_uplink=False)
      else:
        self.notifyOperator(
            "Setting wheel speed, left %s, right %s" % (
                leftWheelSpeed, rightWheelSpeed), False)
        if self._hasRobotCapability("setWheelSpeeds"):
          self.cellbot.setWheelSpeeds(leftWheelSpeed, rightWheelSpeed)
    setWheelSpeedsCmd = (setWheelSpeeds, "Set wheel speeds. ie. \"w 5 5\"")
    commands["w"] = setWheelSpeedsCmd
    commands["wheel"] = setWheelSpeedsCmd
    commands["ws"] = setWheelSpeedsCmd

    # Prefixes that we ignore and then process the following word
    def reParse(lexer):
      # Since shlex views something like ',' as a valid token, there
      # may be extra spaces in this reconstituted command. That said,
      # since we're just passing it back to be re-parsed, it will most
      # likely end up in the new lexer object unchanged (quoting will
      # be wrong/lost).
      self.Parse(''.join(lexer))
    reParseCmd = ("Reparse string. For internal use only.")
    commands["move"] = reParseCmd
    commands["go"] = reParseCmd
    commands["turn"] = reParseCmd
    commands["take"] = reParseCmd


    # Pass an arbitrary string down to the robot. Useful only for
    # non-binary protocols.
    def passToRobot(lexer):
      rawCommand = ' '.join(lexer)
      self.notifyOperator("Passing: %s" % rawCommand, use_speak=False)
      if self._hasRobotCapability("writeRawCommand"):
        self.cellbot.writeRawCommand(rawCommand)
    passToRobotCmd = (passToRobot, "Pass raw command directly to robot.")
    commands["send"] = passToRobotCmd
    commands["pass"] = passToRobotCmd

    # Sensors
    def getRange(lexer):
      self.notifyOperator("Checking distance")
      if self._hasRobotCapability("findRange"):
        self.cellbot.findRange()
      # TODO: ReaderThread thread will handle the response.
      self.notifyOperator("Distance unknown")
    getRangeCmd = (getRange, "Get ultrasonic values.")
    commands["range"] = getRangeCmd
    commands["distance"] = getRangeCmd
    commands["dist"] = getRangeCmd
    commands["z"] = getRangeCmd

    # Calibration
    def calibrateServos(lexer):
      x = lexer.get_token()
      y = lexer.get_token()
      if x == '' or y == '':
        self.notifyOperator("usage: calibrate x y", use_speak=False)
      else:
        if self._hasRobotCapability("findRange"):
          self.cellbot.calibrateServo(x, y)
        self.notifyOperator("Calibrating servos to center at %s and %s" %
                            (x, y), use_speak=False)
    calibrateServosCmd = (calibrateServos, "Calibrate Servos")
    commands["c"] = calibrateServosCmd
    commands["configuration"] = calibrateServosCmd
    commands["calibrate"] = calibrateServosCmd

    def resetRobot(lexer):
      if self._hasRobotCapability("reset"):
                       self.cellbot.reset()
      self.notifyOperator("Reset hardware settings to default",
                          use_speak=False)
    resetRobotCmd = (resetRobot, "Reset robot.")
    commands["reset"] = resetRobotCmd

    def pairBluetooth(lexer):
      self.notifyOperator("Asking Bluetooth module to go into pairing",
                          use_speak=False)
      if self._hasRobotCapability("pairBluetooth"):
        self.cellbot.pairBluetooth()
    pairBluetoothCmd = (pairBluetooth, "Repair Bluetooth.")
    commands["pair"] = pairBluetoothCmd
    commands["pairing"] = pairBluetoothCmd

    # TODO: Add support for a '?' command that will list the known
    # commands. This will likely take the form of setting up a command
    # object and having instances register in a central registry.
    def listCommands(lexer):
      self.notifyOperator("Listing Commands")
      msg = ""
      lineCount = 0
      for key in sorted(self.commands.keys()):
        msg += "%s : %s\n" % (str(key),
                              str(self.commands[str(key)][1]))
        lineCount += 1

        # Unable to fit all lines in XMPP Msg, Limit to 50
        # Also unable to send all lines individually on XMPP without breaking.
        if lineCount > 50:
          self.notifyOperator(msg, use_speak=False)
          msg = ""
          lineCount=0

      self.notifyOperator(msg, use_speak=False)
    listCommandsCmd = (listCommands, "List commands.")
    commands["?"] = listCommandsCmd
    commands["help"] = listCommandsCmd
    commands["man"] = listCommandsCmd

    return commands

  def notifyOperator(self, message, use_uplink=True, use_speak=True):
    """Helper to communicate with the operator by uplink and/or
    synthesized voice"""
    if use_uplink:
      utils.outputToOperator(message, self.uplink)
    if use_speak:
      self.cellbot.speak(message)

  def Parse(self, command):
    """Load the incoming command into a shell based lexer and send
    for dispatching."""
    self.Dispatch(shlex.shlex(command))

  def Dispatch(self, lexer):
    # Pull off the command, which is the first token
    command = lexer.get_token()
    try:
      self.commands[command][0](lexer)
    except KeyError:
      self.notifyOperator("Unknown command: '%s'" % command, use_speak=False)
      self.notifyOperator("Unknown command", use_uplink=False)
