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

__author__ = 'Marc Alvidrez <cram@google.com>'
__license__ = 'Apache License, Version 2.0'

"""Parse the configuration file, wrapping ConfigParser, and populate
class with the attributes that represent the configuration, asking the
operator to choose between different implemenations if necessary."""

import ConfigParser

import netip
from threadedAndroid import droid
import utils

class Configure(object):
  """A class to encapsulate the configuration of the Android device
  for uplink and downlink. Parse the config and expose a simple, flat
  configuration namespace."""

  kModeRemote = "remote"
  kModeRobot = "robot"

  def __init__(self, configFilePath):
    """Setup the config file for reading and call Configure to
    populate the class attributes.

    Args:
      configFilePath: path to the configuration .ini file
    """
    self._config = ConfigParser.ConfigParser()
    self.configFilePath = configFilePath
    self._config.read(configFilePath)
    self.Configure()

  def getConfigFileValue(self, section, option, title,
                         valueList, saveToFile=True):
    '''Get configurable options from the ini file, prompt user if they
    aren't there, and save if needed.

    Args:
      section: In which section of the .ini will we find the value?
      option: Which option in the section has the value?
      title: Title for multi-selection dialogue.
      valueList: Values to populate a multi-selection dialogue.
      saveToFile: Should we save the selection to the .ini?

    Example:
      inputMethod = getConfigFileValue("control", "inputMethod",
                                       "Select Input Method",
                                       ['commandByXMPP',
                                       'commandByTelnet',
                                       'commandByVoice'], False)
    '''
    # Check if option exists in the file
    if self._config.has_option(section, option):
      values = self._config.get(section, option)
      values = values.split(',')
      # Prompt the user to pick an option if the file specified more
      # than one option
      if len(values) > 1:
        setting = utils.pickFromList(droid, title, values)
      else:
        setting = values[0]
    else:
      setting = ''
    # Deal with blank or missing values by prompting user
    if not setting or not self._config.has_option(section, option):
      # Provide an empty text prompt if no list of values provided
      if not valueList:
        setting = droid.getInput(title).result
      # Let the user pick from a list of values
      else:
        setting = utils.pickFromList(droid, title, valueList)
      if saveToFile:
        self._config.set(section, option, setting)
        with open(self.configFilePath, 'wb') as configfile:
          self._config.write(configfile)
    # Strip whitespace and try turning numbers into floats
    setting = setting.strip()
    try:
      setting = float(setting)
    except ValueError:
      pass
    return setting

  def Configure(self):
    """List of config values to get from file or to prompt user for."""
    self.mode = self.getConfigFileValue("basics", "mode", "Select Mode",
                                        [self.kModeRobot, self.kModeRemote])
    self.inputMethod = self.getConfigFileValue("control", "inputMethod",
                                               "Select Input Method",
                                               ['commandByXMPP',
                                                'commandByTelnet',
                                                'commandByVoice',
                                                'commandBySelf'])
    # TODO: Test that commandBySelf doesn't require mode to be set in the
    # config file.
    if self.mode == self.kModeRobot or self.inputMethod == "commandBySelf":
      self.outputMethod = self.getConfigFileValue("control", "outputMethod",
                                                  "Select Output Method",
                                                  ['outputSerial',
                                                   'outputBluetooth',
                                                   'outputBluetoothICreate'])
      self.microcontroller = self.getConfigFileValue("basics",
                                                     "microcontroller",
                                                     "Microcontroller Type",
                                                     ['arduino', 'serialservo',
                                                      'AVR_Stepper', 'icreate'])
    self.audioOn = self._config.getboolean("basics", "audioOn")
    self.currentSpeed = self._config.getint("basics", "currentSpeed")
    self.cardinalMargin = self._config.getint("basics", "cardinalMargin")
    self.phoneIP = netip.displayNoLo()
    try:
      self.bluetoothAddress = self._config.get("control", "bluetoothAddress")
    except:
      # TODO: Make defaults for this and everything.
      self.bluetoothAddress = None

    if self.inputMethod == "commandByJSON":
      if self.mode == self.kModeRobot:
        self.msgRelayUrl = "/".join([self._config.get("json", "msgRelayUrl"),
                                    "device"])
      elif self.mode == self.kModeRemote:
        self.msgRelayUrl = "/".join([self._config.get("json", "msgRelayUrl"),
                                    "controller"])

    if self.mode == self.kModeRemote or self.inputMethod == "commandBySelf":
      self.speedScaleFactor = self.getConfigFileValue(
          "remote", "speedScaleFactor", "Speed scale factor", '', False)
      self.directionScaleFactor = self.getConfigFileValue(
          "remote", "directionScaleFactor", "Direction scale factor", '', False)

    # Only get these settings if we using XMPP
    if self.inputMethod == "commandByXMPP":
      self.xmppServer = self._config.get("xmpp", "server")
      self.xmppPort = self._config.getint("xmpp", "port")
      self.xmppRobotUsername = self.getConfigFileValue(
          "xmpp", "robotUsername", "Robot chat username", '')
      if self.mode == self.kModeRobot:
        self.xmppRobotPassword = self.getConfigFileValue(
            "xmpp", "robotPassword", "Robot chat password", '', False)
      elif self.mode == self.kModeRemote:
        self.xmppRemoteUsername = self.getConfigFileValue(
            "xmpp", "remoteUsername", "Remote chat username", '')
        self.xmppRemoteUserPassword = self.getConfigFileValue(
            "xmpp", "remoteUserPassword", "Remote chat user password", '',
            False)

    if self.inputMethod == "commandByTelnet":
      self.telnetPort = self._config.getint("telnet", "port")
      if self.mode == self.kModeRemote:
        self.robotHost = self.getConfigFileValue(
            "telnet", "robotHost", "Robot hostname", '')


  def Print(self):
    import pprint
    pprint.pprint(self.__dict__)
