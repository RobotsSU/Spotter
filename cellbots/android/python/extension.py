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

__author__ = 'Helen Chou <helenski@google.com>'
__license__ = 'Apache License, Version 2.0'

"""Extension class for robot interactions.

Assumes capabilities of Android phone + wheel speed readings, and
beyond.  Needs to be separated into different classes....
"""

import math

class ExtensionBot(CellBot):

  def __init__(self):
    super(ExtensionBot, self).__init__()

# current tilt angle (in radians)
#	default 0 == phone face points along a plane parallel to the floor
#	increases as the phone faces upward
    self.tilt = 0
# current pan angle (in radians)
#	default 0 == phone face points along a plane parallel to the one that intersects the robot heading and the floor
#	increases as the phone faces to the left
    self.pan = 0


### QUERIES

# Get sonar readings.
  def getSonar(self):
    pass


### ACTIONS

# Angle phone.
  def phoneTilt(self, angle=math.pi/4):
    pass
# Angle phone.
  def phonePan(self, angle=math.pi/4):
    pass
