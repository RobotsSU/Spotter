# Copyright (C) 2010 www.cellbots.com
#
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
# Wrapper for phone operations. 
# Extend this with your phones equilivant functions. 
#
__author__ = 'Glen Arrowsmith <glen.arrowsmith@gmail.com>'

import os
#import sys
import android
import time

class Robot(object):

  def __init__(self, phoneType):
    self.phoneType = phoneType
    if phoneType == "android":
      self.droid = android.Android()
    else:
      raise Exception('Unsupported', 'Phone type is unsupported')

  def dialogCreateAlert(self, prompt):
    if self.phoneType == "android":
      return self.droid.dialogCreateAlert(prompt)
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')
    
  def dialogSetItems(self, items):
    if self.phoneType == "android":
      return self.droid.dialogSetItems(items)
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')

  def dialogShow(self):
    if self.phoneType == "android":
      return self.droid.dialogShow()
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')
    
  def dialogGetResponse(self, dialog):
    if self.phoneType == "android":
      return self.droid.dialogGetResponse(dialog)
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')
    
  def startSensing(self):
    if self.phoneType == "android":
      return self.droid.startSensing()
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')
  
  def startLocating(self):
    if self.phoneType == "android":
      return self.droid.startLocating()
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')
    
  def stopSensing(self):
    if self.phoneType == "android":
      return self.droid.stopSensing()
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')
    
  def stopLocating(self):
    if self.phoneType == "android":
      return self.droid.stopLocating()
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')
  
  def makeToast(self, text):
    if self.phoneType == "android":
      return self.droid.makeToast("Initiating input method...")
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')
    
  def getSerialIn(self):
    if self.phoneType == "android":
      return os.popen('cat /dev/ttyMSM2')
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')
    
  def writeSerialOut(self, text):
    if self.phoneType == "android":
      return os.system(text)
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')
    
  def bluetoothReady(self):
    if self.phoneType == "android":
      return self.droid.bluetoothReady()
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')
    
  def bluetoothRead(self):
    if self.phoneType == "android":
      return self.droid.bluetoothRead()
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')
    
  def bluetoothWrite(self, msg):
    if self.phoneType == "android":
      return self.droid.bluetoothWrite(msg)
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')
    
  def toggleBluetoothState(self, state):
    if self.phoneType == "android":
      return self.droid.toggleBluetoothState(state)
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')
    
  def bluetoothConnect(self, uuid, address):
    if self.phoneType == "android":
      if address:
        return self.droid.bluetoothConnect(uuid, address)
      else:
        return self.droid.bluetoothConnect(uuid)
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')
    
  def getInput(self, text):
    if self.phoneType == "android":
      return self.droid.getInput(text)
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')

  def recognizeSpeech(self):
    if self.phoneType == "android":
      return self.droid.recognizeSpeech()
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')
    
  def speak(self, msg):
    if self.phoneType == "android":
      return self.droid.ttsSpeak(msg)
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')
    
  def readSensors(self):
    if self.phoneType == "android":
      return self.droid.readSensors()
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')
    
  def startAudioRecording(self, fileName):
    if self.phoneType == "android":
      return self.droid.startAudioRecording(fileName)
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')
    
  def stopAudioRecording(self):
    if self.phoneType == "android":
      return self.droid.stopAudioRecording()
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')
  
  def readLocation(self):
    if self.phoneType == "android":
      return self.droid.readLocation()
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')
    
  def geocode(self, x, y):
    if self.phoneType == "android":
      return self.droid.geocode(x, y)
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')
    
  def cameraTakePicture(self, fileName):
    if self.phoneType == "android":
      return self.droid.cameraTakePicture(fileName)
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')

  def pickFromList(self, title, options):
    if self.phoneType == "android":
      self.droid.dialogCreateAlert(title)
      self.droid.dialogSetItems(options)
      self.droid.dialogShow()
      time.sleep(0.25)
      response = self.droid.dialogGetResponse().result['item']
      return options[response]
    else:
      raise Exception('Unsupported', 'Function unsupported on this phone')

  def replaceInsensitive(self, string, target, replacement):
    no_case = string.lower()
    index = no_case.find(target.lower())
    if index >= 0:
        result = string[:index] + replacement + string[index + len(target):]
        return result
    else: # no results so return the original string
        return string
