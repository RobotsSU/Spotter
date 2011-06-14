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

"""Downlink class and implementations for talking to robots.

  Current implementations:
    BluetoothDownlinkASCII (used by AVRAsciiRobotProtocol)
    BluetoothDownlinkBinary (used by AVRBinaryRobotProtocol)
  Planned implementations:
    SerialDownlinkASCII, for direct-wired phones
"""

__author__ = 'nparker@google.com (Nathan Parker)'

import struct
import threading
import time
import base64

import threadedAndroid

class DownlinkError(Exception):
  """Exception for downlink errors"""
  pass

def downlinkFactory(config):
  """Consult the configuration to determine which downlink to
  instantiate, and return it

  Args:
    config -- global configuration object

  Returns:
    downlink -- configured downlink instance required to talk to the
      robot
  """
  if (config.outputMethod == "outputBluetoothASCII" or
      config.outputMethod == "outputBluetooth"): # Backwards compat.
    downlink = BluetoothDownlinkASCII(bluetoothAddress=config.bluetoothAddress)
  elif config.outputMethod == "outputBluetoothBinary":
    downlink = BluetoothDownlinkBinary(
        bluetoothAddress=config.bluetoothAddress)
  elif config.outputMethod == "outputBluetoothICreate":
    downlink = BluetoothDownlinkICreate(
        bluetoothAddress=config.bluetoothAddress)
  else:
    raise DownlinkError("Unknown downlink: '%s'" % config.outputMethod)

  # TODO: make this a downlink obj
  # print "Outputting elsewhere"
  # serialReader.lifeline = re.compile(r"(\d) received")
  # readerThread = serialReader()
  # readerThread.start()

  return downlink

class Downlink(object):
  """Base class that defines the interface for communication between
     the RobotProtocol classes and the robot (arduino, etc).

  The structure is as follows:
    WriteCommand() should send a command immediately.
    ReadReply() returns some structured msg based on the protocol, within
      timeout seconds.

  """

  def start(self):
    pass

  def WriteCommand(self, *msg):
    raise NotImplementedError

  def ReadReply(self, timeout):
    raise NotImplementedError

  def FlushInput(self):
    raise NotImplementedError


# For debugging "ASCII," that isn't always.
def _StrToHex(str):
  return "".join(["%x" % ord(x) for x in str])


class BluetoothDownlink(Downlink):
  """Parent class for BluetoothDownlink{ASCII,Binary}.

  Just does initialization.  This uses its own Android object since we
  need to maintain state of the connection.  """

  def __init__(self, bluetoothAddress=None):
    """
    Args:
      bluetoothAddress: HEX string, or None.
    """

    self.droid = threadedAndroid.LockedAndroid()

    # Initialize Bluetooth outbound if configured for it
    self.droid.toggleBluetoothState(True)
    if bluetoothAddress:
      self.droid.bluetoothConnect('00001101-0000-1000-8000-00805F9B34FB',
                                  bluetoothAddress)
      # this is a magic UUID for serial BT devices
    else:
      self.droid.bluetoothConnect('00001101-0000-1000-8000-00805F9B34FB')
      # this is a magic UUID for serial BT devices
    print "Initializing Bluetooth connection"
    self.droid.makeToast("Initializing Bluetooth connection")
    time.sleep(3) # TODO: remove magic number

  # Child classes will implement WriteCommand() and ReadReply()

  def _WaitForDataReady(self, deadline_time):
    """ Wait for a deadline_time (epoch secs, float) for bluetoothReadReady.

    Returns True if ready, False on timeout.
    """

    while time.time() < deadline_time:
      try:
        dataReady = self.droid.bluetoothReadReady()
        #print(dataReady)
        if dataReady.result:
          return True
        time.sleep(0.02) # TODO: find a better way than polling.
        continue
      except:
        print "BluetoothDownlink: reading failed"
        return False
    print "BluetoothDownlink: timeout on reading"
    return False

  def FlushInput(self):
    self.droid.bluetoothSkipPendingInput()



class BluetoothDownlinkASCII(BluetoothDownlink):
  """Implementation of a BluetoothDownlink that writes/reads ascii.
  """

  def __init__(self, bluetoothAddress=None):
    super(BluetoothDownlinkASCII,self).__init__(
        bluetoothAddress=bluetoothAddress)
    self.buf = "" # Keep partially read messages

  def WriteCommand(self, msg):
    self.droid.bluetoothWrite(msg + '\n')

  def ReadReply(self, timeout):
    """Read one line, keeping any partial line that's come in after that.

    Returns: (key, val) tuple, or None on error or timeout.
    """

    start_time = time.time()
    deadline = start_time + timeout

    # Read chunks and add them to buf.
    # Once we've gotten a whole line, split it into key/val by colons.
    # TODO: Can we just use bluetoothReadline()?
    while self._WaitForDataReady(deadline):
      try:
        buf_partial = self.droid.bluetoothRead().result
      except:
        print "BTDownlinkASCII: reading failed"
        return None
      if buf_partial:
        if not len(buf_partial): continue
        #print "BTDownlinkASCII: Read Hex Chars: %s " % _StrToHex(buf_partial)
        print "BTDownlinkASCII: Read Chars: %s " % buf_partial
        self.buf += buf_partial
        if not '\n' in self.buf: continue
        # We know we have at least one line.  Parse 1 line
        npos = self.buf.find('\n')
        msg_line = self.buf[:npos]
        self.buf = self.buf[npos+1:]
        print "Bot says: %s " % _StrToHex(msg_line)
        if len(msg_line.strip()) < 2:
          print "BTDownlinkASCII: Trouble parsing k/v pair -- too short"
          return None
        word_list = msg_line.split(':')
        if len(word_list) != 2:
          print "BTDownlinkASCII: Trouble parsing k/v pair"
          return None
        # Success
        return (word_list[0], word_list[1])
    # Timeout
    return None


class BluetoothDownlinkBinary(BluetoothDownlink):
  """Implementation of a BluetoothDownlink, writes/reads the binary protocol.

  The protocol spec is here:
  https://docs.google.com/a/google.com/Doc?docid=0AeQa1c1ypp_UZGc1Y3A0dmdfMGNjcDl2ZmZj&hl=en
  """

  def __init__(self, bluetoothAddress=None):
    super(BluetoothDownlinkBinary,self).__init__(
        bluetoothAddress=bluetoothAddress)

  def WriteCommand(self, command, *args):
    # TODO: Make the 'tag' value auto-increment, and then make a reader
    # thread (in AVRBinaryProtocol) that associates reads w/ writes based on
    # that tag.
    bin = struct.pack(">HBB%dh" % len(args), command, 1, len(args), *args)
    self.droid.bluetoothWriteBinary(bin.encode("base64"))

  def ReadReply(self, timeout):
    """Read one message.
       Not _guaranteed_ to return within timeout, but normally should.
       Returns: tuple (command, (args,...)) or None.
    """

    if not self._WaitForDataReady(deadline):
      return None

    header = self.droid.bluetoothReadBinary(4).result
    header = header.decode("base64")
    command, tag, count = struct.unpack(">HBB", header)
    if count > 0:
      if not self._WaitForDataReady(deadline):
        return None
      # TODO: Should verify the length is what we expect
      data = self.droid.bluetoothReadBinary(
          count * 2).result.decode("base64")
      args = struct.unpack(">%dh" % count, data)
    else:
      args = []
    return (command, tuple(args))

# Listen for incoming serial responses. If this thread stops working,
# try rebooting.
class serialReader(threading.Thread):
  def __init__ (self):
    threading.Thread.__init__(self)
  def run(self):
    process = droid.getSerialIn()
    while process:
      try:
        botReply = process.readline()
        if botReply:
          backoff = 2
          if len(botReply.strip()) > 1:
            splitReply = botReply.split(':')
            if len(splitReply) == 2:
              addToWhiteboard(splitReply[0], splitReply[1])
            outputToOperator("Bot says %s " % botReply)
      except:
        print "Reader Thread Errored"
        time.sleep(2)


class BluetoothDownlinkICreate(BluetoothDownlink):
  """Implementation of a BluetoothDownlink that writes/reads to iCreate."""

  def __init__(self, bluetoothAddress=None):
    super(BluetoothDownlinkICreate,self).__init__(
        bluetoothAddress=bluetoothAddress)

  def WriteCommand(self, msg):
    encoded_data = base64.encodestring(msg)
    self.droid.bluetoothWriteBinary(encoded_data)

  def ReadReplyWithBuffer(self, timeout, bufferSize):
    encoded_data = self.droid.bluetoothReadBinary(bufferSize)
    return base64.decodestring(encoded_data.result)
