'''
netip.py

this program will go through the output of the netcfg command
and list the ip address for any interface that has the UP state
there are two functions, the firstlists all interfaces, the second will exclude the lo interface

The format for the output of netcfg is:
interface state ip_address net_mask Hexstring

off hand I have not investigated the hexstring enough to identify it's purpose, so if you need it at some time, feel free to pull the appropriate function
'''
import os, sys, android, string, time

def colint():
    status = []
    a = os.popen('netcfg','r')
    while 1:
      line = a.readlines()
      if line: 
        status.append(line)
      else:
        return status
        break
    return status

def upips(work):
    status = []
    a = 0
    while a < len(work[0]):
      if "UP" in work[0][a]:
        status.append(work[0][a])
      a += 1
    return status

def nolo(work):
# given a string (with embeded '\n') break as needed, return
# list of strings w/o 'lo' in them)
  status = []
  a = 0
  while a < len(work):
    if "lo" in work[a]:
      a += 1
    else:
      status.append(work[a])
      a += 1
  if len(status) == 1:
    rets = status[0]
    statusl = rets.split()
    status = statusl[2]
  return status

def displayUp():
  nets = colint()
  nets = upips(nets)
  return nets

def displayNoLo():
  nets = colint()
  nets = upips(nets)
  nets = nolo(nets)
  return nets

if __name__ == '__main__':
  droid = android.Android()
  droid.makeToast(displayUp())
  time.sleep(5)
  droid.makeToast(displayNoLo())
  time.sleep(5)
  droid.exit()

