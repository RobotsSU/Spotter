# Copyright(c) 2010 Patrick Engelman
# This code is public domain.
import socket
import btsocket
import appuifw
import e32
import sys
import os
import threading
import time
from time import sleep
address = ("192.168.1.134",17171)
basesock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
#basesock.connect(("65.96.132.180",17171))
basesock.connect(address)

class SendUpdate(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)
    def run(self):
        while 1:
            print "Sending telemtry update"
            basesock.sendto("TELEMETRY UPDATE", address)
            time.sleep(1000000)


class BTSSC:
    def connect(self):
        self.sock=btsocket.socket(btsocket.AF_BT,btsocket.SOCK_STREAM)
	btsocket.set_default_access_point(btsocket.access_point(btsocket.select_access_point()))
        addr,services=btsocket.bt_discover()
        print "Discovered: %s, %s"%(addr,services)
        if len(services)>0:
            import appuifw
            choices=services.keys()
            choices.sort()
            choice=appuifw.popup_menu([unicode(services[x])+": "+x
                                       for x in choices],u'Choose port:')
            port=services[choices[choice]]
        else:
            port=1
        address=(addr,port)
        print "Connecting to "+str(address)+"...",
        self.sock.connect(address)
        #except socket.error, err:
        #    if err[0]==54: # "connection refused"
        #        if _s60_UI:
        #            appuifw.note(u'Connection refused.','error')
        #        else:
        #            print "Connection refused."
        #        return None
        #    raise
        print "OK." 
    def write(self, message):
        self.sock.send(message+'\n')

#    def readline(self):
#        line=[]
#        while 1:
#            ch=self.sock.recv(1)
#            if(ch=='\n'):
#                break
#            line.append(ch)
#        return ''.join(line)
    def close(self):
        self.sock.close()

# Socket stuff
# Base station address

# UDP client code 
# basesock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
# basesock.bind(('0.0.0.0',17171))

#Begin sending a telemetry update to the console every second. 
backgroundtelemetry = SendUpdate()
backgroundtelemetry.start()

bt=BTSSC()
bt.connect()
#basesock.send("a");
i=50

# Main loop
while True:

        
    servocmds = basesock.recv(1024)
    #print "Command should be \xff\x01\x50\n"
    #print 'Received', repr(servocmds)
    #print "receiving data %s" % (servocmds)
    #bt.write(servocmds)
    
    bt.write(servocmds)
    
bt.close()
basesock.close()



