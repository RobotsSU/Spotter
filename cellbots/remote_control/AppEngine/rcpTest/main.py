#!/usr/bin/env python
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

__author__ = 'Glen Arrowsmith <glen@cellbots.com>'
__license__ = 'Apache License, Version 2.0'


import xmlrpclib
import cgi
import time
import datetime
import urllib
import sys

from google.appengine.api import users
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from google.appengine.ext import db
from urlparse import urlparse

class BotsDB(db.Model):
    botIP = db.StringProperty(multiline=False)
    botname = db.StringProperty(multiline=False)
    botport = db.StringProperty(multiline=False)
    date = db.DateTimeProperty(auto_now_add=True)

class MainPage(webapp.RequestHandler):
  def get(self):
    self.response.out.write("""
    <html>
      <head>
        <title>Cellbot Remote</title>
        <script src="/scripts/main.js" type="text/javascript"></script>
        <link type="text/css" rel="stylesheet" href="/stylesheets/main.css" />
      </head>
      <body onload="getRobotsOnlineLoop()">
        <h1>Cellbot Remote</h1>
        <h3>Robots Online:</h3>
        <div id="robotsonline"></div>
        <h3>Robot Responses:</h3>
        <div id="robotresponses"></div>
      </body>
    </html>
    """)

class RobotsOnline(webapp.RequestHandler):
  def get(self):
    bots = db.GqlQuery("SELECT * FROM BotsDB ORDER BY date DESC LIMIT 5 ")
    self.response.out.write("<table id='onlinebots'><tr><th>Bot Name</th><th>Bot IP</th><th>Last Contact</th><th></th></tr>")
    for bot in bots:
      datetime.date.today()
      self.response.out.write('<tr><td>%s</td>' % bot.botname)
      self.response.out.write('    <td>%s:%s</td>' % (bot.botIP,bot.botport))
      self.response.out.write('    <td>%s</td>' % (datetime.datetime.today() - bot.date))
      self.response.out.write('<td><input type="text" id="%s"></input>' % bot.botname)
      self.response.out.write('<input type="submit" value="Send" onclick="javascript:sendCommand(document.getElementById(\'%s\').value,\'%s\')">' % (bot.botname,bot.botname))
      self.response.out.write('</td></tr>')
    
    self.response.out.write("</table>")

class RegisterBot(webapp.RequestHandler):
  def get(self):
    parsedUrl = urlparse(self.request.url)
    params = dict([part.split('=') for part in parsedUrl[4].split('&')])
    bots = BotsDB.gql("WHERE botname = :1 ORDER BY date DESC LIMIT 1",
                                 params['botname'])
    try:
      bot = bots[0]
      self.response.out.write('updated:')
    except:
      bot = BotsDB()
      self.response.out.write('registered:')
    bot.botIP = params['botip']
    bot.botname = params['botname']
    bot.botport = params['botport']
    bot.put()
    self.response.out.write('<br>ip:%s:%s<br>name:%s' % (bot.botIP,bot.botport, bot.botname))

class SendMsg(webapp.RequestHandler):
  def get(self):
    try:
      parsedUrl = urlparse(self.request.url)
      params = dict([part.split('=') for part in parsedUrl[4].split('&')])
      botname = urllib.unquote(params['botname'])
      message = urllib.unquote(params['msg'])
      
      #bots = db.GqlQuery("SELECT * FROM BotsDB ORDER BY date DESC LIMIT 1")
      bots = BotsDB.gql("WHERE botname = :1 ORDER BY date DESC LIMIT 1",
                                 botname)

      for bot in bots:
        addr = '%s:%s' % (bot.botIP,bot.botport)
        self.response.out.write('Sending %s : %s<br>' % (addr, message))        
        s = xmlrpclib.ServerProxy('http://' + addr)
        #self.response.out.write(s.system.listMethods())
        self.response.out.write('%s said: %s <br>' % (addr, s.send(message)))
    except:
      self.response.out.write('Unknown error calling RPC')

application = webapp.WSGIApplication(
                                   [('/', MainPage),
                                    ('/registerbot', RegisterBot),
                                    ('/robotsonline', RobotsOnline),
                                    ('/sendmsg', SendMsg)],
                                   debug=True)
def main():
    run_wsgi_app(application)

if __name__ == "__main__":
    main()
