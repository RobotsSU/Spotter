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

__author__ = 'Ryan Hickman <rhickman@gmail.com>', 'Glen Arrowsmith <glen@cellbots.com>', 'Marc Alvidrez <cram@google.com>', 'Nathan Parker <nparker@google.com>'
__license__ = 'Apache License, Version 2.0'

import glob
import os.path
import sys
import cellbotRemote
import configuration
import commandTranslator
import downlinks
import uplinks
import utils
from threadedAndroid import droid # Sets the thread-safe "droid" instance

# TODO: Set imports based on configuration

# AVR CellBot Imports
import avrCellBot
import avrRobotProtocol

# ICreate Imports
import icreatecellbot
import icreaterobotprotocol

CONFIG_DIR = os.path.join("/", "sdcard", "sl4a", "scripts", "cellbot",
                          "configs")

# TODO: Switch this to a true factory instead of hard coding a single
# cellbot.
def cellbotFactory(config, robotProto):
  """Consult the configuration to determine which cellbot to
  instantiate, and return it"""
  if config.microcontroller == "arduino":
    cellbot = avrCellBot.AVRCellBot(config, robotProto)
  elif config.microcontroller == "icreate":
    cellbot = icreatecellbot.ICreateBot(config, robotProto)
  else:
    utils.outputToOperator("ERROR: No cellbot for microcontroller %s" %
                           config.microcontroller)
    sys.exit()
  return cellbot

# TODO: Switch this to a true factory instead of hard coding a single
# robot protocol.
def robotProtoFactory(config, downlink):
  """Consult the configuration to determine which robot protocol to
  instantiate, and return it"""
  if config.microcontroller == "arduino":
    robotProto = avrRobotProtocol.AVRAsciiRobotProtocol(downlink)
  elif config.microcontroller == "icreate":
    robotProto = icreaterobotprotocol.ICreateRobotProtocol(downlink)
  else:
    utils.outputToOperator("ERROR: No robot protocol for microcontroller %s" %
                           config.microcontroller)
    sys.exit()
  return robotProto

def selectConfigFile(configDir):
  iniFiles = glob.glob(os.path.join(configDir, "*.ini"))
  if len(iniFiles) < 1:
    utils.outputToOperator("ERROR: No config files found in %s!\n" % configDir)
    sys.exit()
  if len(iniFiles) == 1:
    return iniFiles[0]
  else:
    configFile = utils.pickFromList(droid, "Choose a config file to use",
        [os.path.basename(file) for file in iniFiles])
    configFile = os.path.join(configDir, configFile)
    utils.outputToOperator("Using config file %s.\n" % configFile)
    return configFile

def createUplink(config):
  return uplinks.uplinkFactory(config)

def startRobot(config, uplink):
  utils.outputToOperator("Send the letter 'q' or say 'quit' to exit the " +
                         "program.\n")

  # Call factories to instantiate instances for each layer based on
  # the config, and wire them together.
  downlink = downlinks.downlinkFactory(config)
  robotProto = robotProtoFactory(config, downlink)
  cellbot = cellbotFactory(config, robotProto)
  dispatch = commandTranslator.commandTranslator(config, uplink, cellbot)

  # Register an operator command callback with the uplink and start it
  uplink.SetCommandParser(dispatch.Parse)
  uplink.start()

def startRemote(config, uplink):
  def remote_print(msg):
    print(msg)
  # Self remote is special, it's uplink is shared on the same machine and it'll
  # use the command parser from the robot.
  if (config.inputMethod != "commandBySelf"):
    uplink.SetCommandParser(remote_print)
  remote = cellbotRemote.CellbotRemote(config, uplink, True)
  remote.startOptions()
  remote.start()

def startSelfRemote(config, uplink):
  startRobot(config, uplink)
  startRemote(config, uplink)

# Raise the sails and fire the cannons
def main():
  config = configuration.Configure(selectConfigFile(CONFIG_DIR))
  uplink = createUplink(config)
  if (config.inputMethod == "commandBySelf"):
    startSelfRemote(config, uplink)
  elif config.mode == config.kModeRobot:
    startRobot(config, uplink)
  elif config.mode == config.kModeRemote:
    startRemote(config, uplink)

if __name__ == '__main__':
    main()
