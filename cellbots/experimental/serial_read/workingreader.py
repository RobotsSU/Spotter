import os
import time
import socket
import select
import sys
import android
import math
import re
from threading import Thread

droid = android.Android()

class serialReader(Thread):
   def __init__ (self):
      Thread.__init__(self)
   def run(self):
     process = os.popen('cat /dev/ttyMSM2')
     while process:
       line = process.readline()
       if line:
         print line


serialReader.lifeline = re.compile(r"(\d) received")
report = ("No response","Partial Response","Alive")

# Send command out of the device (currently serial but other protocals could be added)
def commandOut(msg):
  os.system("echo '%s\n' > /dev/ttyMSM2" % msg)
  print msg


# The main loop that fires up a telnet socket and processes inputs
def main():
  current = serialReader()
  current.start()

  HOST = ''                # Symbolic name meaning all available interfaces
  PORT = 9002              # Arbitrary non-privileged port
  s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
  s.bind((HOST, PORT))
  s.listen(1)
  conn, addr = s.accept()
  print 'Connected by', addr
  conn.send('You are connected')
  while 1:
    data = conn.recv(1024)
    if not data: break
    conn.send(data)
    commandOut(data)
  conn.close()
  current.join()



if __name__ == '__main__':
    main()
