# Copyright (C) 2010 www.cellbots.com
#
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

"""Helper functions for interacting with the phone."""

__license__ = 'Apache License, Version 2.0'

import time

def outputToOperator(msg, uplink=None):
  """Display information on screen and/or reply to the human operator"""
  if uplink:
    uplink.Write(msg)
  print msg

def pickFromList(droid, title, options):
    droid.dialogCreateAlert(title)
    droid.dialogSetItems(options)
    droid.dialogShow()
    time.sleep(0.25)
    response = droid.dialogGetResponse().result['item']
    return options[response]

def log(droid, msg):
  return droid.log(msg)
