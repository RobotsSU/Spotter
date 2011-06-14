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


class SensorType(object):
  """
  SensorType base class.
  Each sensor type (e.g. sonar, camera) should create a subclass which implements the way the sensor retrieves data, as well as the way the data should be processed and stored if applicable.
  """

  def __init__(self):
    # Dict which stores each instance of this sensor type in [id=,data=] pairs.
    # id -- string identifier
    # data -- comes in whatever form this SensorType retrieves
    self.sensors = {}

  def addSensorInstance(self, id):
    self.sensors[id] = None

  def getReading(self, id):
    """
    Gets reading from sensor. To be implemented in subclasses.
    """
    pass


class ImageSensorType(SensorType):

  def __init__(self):
    super(ImageSensorType, self).__init__()

  def getReading(self, id):
    """
    Overrides superclass.
    """
    pass


class GpsSensorType(SensorType):

  def __init__(self):
    super(GpsSensorType, self).__init__()

  def getReading(self, id):
    """
    Overrides superclass.
    """
    pass


class SonarSensorType(SensorType):

  def __init__(self):
    super(GpsSensorType, self).__init__()

  def getReading(self, id):
    """
    Overrides superclass.
    """
    pass


class OdometerSensorType(SensorType):

  def __init__(self):
    super(GpsSensorType, self).__init__()

  def getReading(self, id):
    """
    Overrides superclass.
    """
    pass
