import android
import netip
import httplib
import ConfigParser

from SimpleXMLRPCServer import SimpleXMLRPCServer
from SimpleXMLRPCServer import SimpleXMLRPCRequestHandler

droid = android.Android()
config = ConfigParser.ConfigParser()
config.read("/sdcard/ase/scripts/cellbotConfig.ini")

#Used as an indentifier on the app engine
botname = config.get("xmpp", "username") 

#Public ip of the phone. Dont use WIFI unless you forwarded the port.
ip = netip.displayNoLo() 

#Port that is open on your phone. Networks may restrict this. 
botport = 8000

#Location of the app. 
appAddress = "rcptestapp.appspot.com" 


# Restrict to a particular path.
class RequestHandler(SimpleXMLRPCRequestHandler):
  rpc_paths = ('/RPC2',)

# Create server
server = SimpleXMLRPCServer(("", botport),
                            requestHandler=RequestHandler)
server.register_introspection_functions()

# Register a function under a different name
def adder_function(txt):
  droid.speak(str(txt))
  return txt
server.register_function(adder_function, 'send')

#Register the bot online
addr = "/registerbot?botip=%s&botname=%s&botport=%i" %(ip, botname, botport)
print addr
conn = httplib.HTTPConnection(appAddress)
conn.request("GET", addr)
r1 = conn.getresponse()
print r1.read()


# Run the server's main loop
server.serve_forever()
