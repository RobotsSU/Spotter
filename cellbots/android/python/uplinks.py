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

"""The uplink connects an Android to the cloud.

When running in Robot Brain mode, the uplink is the conduit for
receiving commands and sending output to the operator. When running in
Remote Control mode, the uplink is the conduit for sending commands to
an Android device running in Robot Brain mode and controlling the
actual robot.
"""

__author__ = 'Marc Alvidrez <cram@google.com>'
__license__ = 'Apache License, Version 2.0'

import logging
import select
import simplejson
import socket
import sys
import threading
import time
import xmpp
import urllib
import urllib2

from threadedAndroid import droid

class UplinkError(Exception):
  """Exception for uplink errors"""
  pass

def uplinkFactory(config):
  """Consult the configuration to determine which uplink to
  instantiate, and return it

  Args:
    config -- global configuration object

  Returns:
    uplink -- configured uplink instance required to talk to the cloud
  """
  if config.inputMethod == "commandByJSON":
    print "Initiating input by JSON"
    droid.makeToast("Initiating input by JSON")
    uplink = commandByJSON(config.msgRelayUrl)
  elif config.inputMethod == "commandByXMPP":
    print "Initiating input by XMPP"
    droid.makeToast("Initiating input by XMPP")
    if config.mode == config.kModeRobot:
      uplink = commandByXMPP(
          config.xmppServer, config.xmppPort, config.xmppRobotUsername,
          config.xmppRobotPassword, None)
    elif config.mode == config.kModeRemote:
      uplink = commandByXMPP(
          config.xmppServer, config.xmppPort, config.xmppRemoteUsername,
          config.xmppRemoteUserPassword, config.xmppRobotUsername)
  elif config.inputMethod == "commandByTelnet":
    print "Initiating input by telnet"
    droid.makeToast("Initiating input by telnet")
    if config.mode == config.kModeRemote:
      phoneIP = config.robotHost
    else:
      phoneIP = config.phoneIP
    uplink = commandByTelnet(phoneIP, config.telnetPort,
                             config.mode == config.kModeRobot)
  elif config.inputMethod == "commandByVoice":
    print "Initiating input by voice"
    droid.makeToast("Initiating input by voice")
    uplink = commandByVoice()
  elif (config.inputMethod =="commandBySelf"):
    print "Initiating input by self remote"
    droid.makeToast("Initiating input by self remote")
    uplink = commandBySelf()
  else:
    raise UplinkError("Uplink type unrecognized: '%s'" % config.inputMethod)
  return uplink

class Uplink(threading.Thread):
  """Abstract base class that defines the interface for communicating
  with the cloud."""

  def __init__(self):
    threading.Thread.__init__(self)

  def SetCommandParser(self, callback):
    self.callback = callback

  def Write(self, msg):
    pass

  def Close(self):
    raise NotImplementedError

  def run(self):
    raise NotImplementedError

  def Print(self):
    import pprint
    pprint.pprint(self.__dict__)


class commandByJSON(Uplink):
  """Concrete implementation of uplink using the botzczar relay with
  JSON.
  """

  def __init__(self, msgRelayUrl):
    if hasattr(Uplink, '__init__'):
      Uplink.__init__(self)
    self.msgRelayUrl = msgRelayUrl

  def Close(self):
    """Closes the commandByJSON Uplink.

    Since there is no state to clean up there is nothing to be done.
    """
    pass

  # Examples:
  #   cmd = '{ "status_update" : {} }'
  #   cmd = '{ "put_cmd" : [ "w 10 10" ] }'
  # Returns: handle.
  #   Return value of urllib2.urlopen()
  def sendData(self, cmd):
    data = urllib.urlencode([('msg', cmd)])
    return urllib2.urlopen(self.msgRelayUrl, data, 60)

  def sendCmd(self, cmd):
    self.sendData ('{ "put_cmd" : [ "%s" ] }' % cmd)

  def run(self):
    print "\nmsgRelayUrl: %s" % self.msgRelayUrl
    print "\r\nRobot is now ready to take commands."
    logging.info('about to post status and get latest commands.')
    try:
      while True:
        # For now no status is returned. In the future we will want to have a
        # StatusObject that can be consulted for status changes since some
        # time.
        handle = self.sendData('{ "status_update" : {} }')
        json = handle.read()
        cmds_dict = simplejson.loads(json)
        if cmds_dict['commands']:
          handle, cmd_dict = cmds_dict['commands'].items()[0]
          # Not sure why this has to be explicitly set as a string, but it
          # doesn't work without it. Strange JSON encoding?
          self.callback(str(cmd_dict['cmd']))
          time.sleep(0.3)

    except KeyboardInterrupt:
      pass


class commandByXMPP(Uplink):
  """Concrete implemenation of uplink using XMPP for communications."""

  def __init__(self, xmppServer, xmppPort, xmppUsername, xmppPassword,
               xmppOtherUser):
    if hasattr(Uplink, '__init__'):
      Uplink.__init__(self)
    self.xmppServer = xmppServer
    self.xmppPort = xmppPort
    self.xmppUsername = xmppUsername
    self.xmppPassword = xmppPassword
    self.xmppOtherUser = xmppOtherUser
    self.XMPPLogin()

  def XMPPLogin(self):
    jid = xmpp.protocol.JID(self.xmppUsername)
    self.xmppClient = xmpp.Client(jid.getDomain(), debug=[])
    self.xmppClient.connect(server=(self.xmppServer,
                                    self.xmppPort))

    def XMPP_message_cb(session, message):
      """Handle XMPP messages coming from commandByXMPP"""
      jid = xmpp.protocol.JID(message.getFrom())
      self.operator = jid.getNode() + '@' + jid.getDomain()
      command = message.getBody()
      self.callback(str(command))

    try:
      self.xmppClient.RegisterHandler('message', XMPP_message_cb)
    except:
      # This exception has a sucky error message
      raise UplinkError('XMPP error. You sure the phone has an ' +
                        'internet connection?')
    if not self.xmppClient:
      raise UplinkError('XMPP Connection failed!')

    auth = self.xmppClient.auth(jid.getNode(), self.xmppPassword,
                                'botty')
    if not auth:
      raise UplinkError('XMPP Authentication failed!')
    self.xmppClient.sendInitPresence()
    print "XMPP username for the robot is: %s" % self.xmppUsername

  def WriteBase(self, to, msg):
    try:
      self.xmppClient.send(xmpp.Message(to, msg))
    except:
      pass

  def Write(self, msg):
    self.WriteBase(self.operator, msg)

  def Close(self):
    """From http://xmpppy.sourceforge.net/basic.html: 'We're done! The
    session must now be closed but since we have not registered
    disconnect handler we will just leave it to python and TCP/IP
    layer. All jabber servers that I know handle such disconnects
    correctly.'"""
    pass

  def sendCmd(self, cmd):
    self.WriteBase(self.xmppOtherUser, cmd)

  def run(self):
    print "\r\nRobot is now ready to take commands."
    try:
      while True:
        # import pprint
        # pprint.pprint(threading.enumerate())
        self.xmppClient.Process(1)
    except KeyboardInterrupt:
      pass


class commandByTelnet(Uplink):
  """Concrete implemenation of uplink using telnet for communications."""

  def __init__(self, phoneIP, telnetPort, listen=True):
    if hasattr(Uplink, '__init__'):
      Uplink.__init__(self)
    # phoneIP always refers to the robot phone's IP.
    # If this phone is the robot phone, the phoneIP is it's own IP.
    self.phoneIP = phoneIP
    self.telnetPort = telnetPort
    if listen:
      self.Listen()
    else:
      self.Connect()

  def setupSocket (self, telnetPort):
    self.svr_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    self.svr_sock.bind(('', telnetPort))

  def Connect(self):
    self.setupSocket(0)
    print "Connecting to robot: %s:%d..." % (self.phoneIP, self.telnetPort)
    self.svr_sock.connect((self.phoneIP, self.telnetPort))

  def Listen(self):
    """Command input via open telnet port"""
    self.rs = []
    print "Firing up telnet socket..."
    try:
      self.setupSocket(self.telnetPort)
      if self.telnetPort == 0:
        host, self.telnetPort = self.svr_sock.getsockname()
      self.svr_sock.listen(3)
      self.svr_sock.setblocking(0)
      print "Ready to accept telnet. Use %s on port %s\n" % (self.phoneIP,
                                                             self.telnetPort)
    except socket.error, (value,message):
      print "Could not open socket: " + message
      print "You can try using %s on port %s\n" % (self.phoneIP,
                                                   self.telnetPort)

  def Close(self):
    print "Shutting down telnet"
    self.svr_sock.close()

  def sendCmd(self, msg):
    self.svr_sock.sendall(msg)

  def run(self):
    print "\r\nRobot is now ready to take commands."
    while True:
      r,w,_ = select.select([self.svr_sock] + self.rs, [], [])

      for cli in r:
        if cli == self.svr_sock:
          new_cli,addr = self.svr_sock.accept()
          self.rs = [new_cli]
        else:
          input = cli.recv(1024)
          input = input.replace('\r','')
          input = input.replace('\n','')
          if input != '':
            print "Received: '%s'" % input
            # Send OK after every command recieved via telnet
            cli.sendall("ok\r\n")
            self.callback(input)

class commandBySelf(Uplink):
  """Concrete implementation of uplink using self remote."""
  def __init__(self):
    if hasattr(Uplink, '__init__'):
      Uplink.__init__(self)
    self.robot = commandByTelnet('', 0)
    phoneIP, port = self.robot.svr_sock.getsockname()
    self.remote = commandByTelnet(phoneIP, port, False)

  def remote_print(msg):
    print(msg)

  def SetCommandParser(self, callback):
    self.robot.SetCommandParser(callback)
    self.remote.SetCommandParser(self.remote_print)

  def Close(self):
    self.remote.Close()
    self.robot.Close()

  def sendCmd(self, msg):
    self.remote.sendCmd(msg)

  def run(self):
    self.robot.run()

class commandByVoice(Uplink):
  """Concrete implemenation of uplink using voice control."""

  def __init__(self):
    if hasattr(Uplink, '__init__'):
      Uplink.__init__(self)

  def Close(self):
    pass

  def run(self):
    print "\r\nRobot is now ready to take commands."
    while True:
      try:
        voiceCommands = str(droid.recognizeSpeech().result)
      except:
        voiceCommands = ""
      print "Voice commands: %s" % voiceCommands
      self.callback(voiceCommands)
