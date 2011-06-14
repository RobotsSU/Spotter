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

__author__ = 'Helen Chou <helenski@google.com>'
__license__ = 'Apache License, Version 2.0'

import time

class SensorReading(object):
  """
  Sensor readings.
  """

  def __init__(self, type):
    self.type = type
    self.timestamp = time.time()
    self.data = None

  def update(self, data):
    self.timestamp = time.time()
    self.data = data
