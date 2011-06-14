#!/usr/bin/python2.4
#
# Copyright 2009 Google Inc. All Rights Reserved.

"""One-line documentation for sensors module.

A detailed description of sensors.
"""

__author__ = 'damonkohler@google.com (Damon Kohler)'


import service
import pyrobot

import copy
import logging
import threading
import time
import traceback


class PyrobotSensors(object):

  def __init__(self, robot):
    self._sensors = pyrobot.CreateSensors(robot)

  def refresh_sensor_data(self):
    self._sensors.GetAll()

  def get_sensor_data(self):
    return self._sensors;


class MockSensors(object):

  def __init__(self):
    self._data = {'charging-state': 0,
                  'charge': 27,
                  'capacity': 28}
    self._lock = threading.Lock()

  def refresh_sensor_data(self):
    self._data['charging-state'] = (self._data['charging-state'] + 1) % 5
    self._data['charge'] = (self._data['charge'] + 1) % 50

  def get_sensor_data(self):
    self._lock.acquire()
    result = copy.deepcopy(self._data)
    self._lock.release()
    return result


class SensorMonitor(service.Service):

  def __init__(self, sensors):
    super(SensorMonitor, self).__init__()
    self._sensors = sensors
    self._data = {}
    self._sensor_listener_cb = None
    self._distance_since = 0
    self._angle_since = 0
    self._hit_left_bumper = False
    self._hit_right_bumper = False

  def get_sensors(self):
    return self._sensors

  def get_distance(self):
    distance = self._distance_since
    self._distance_since = 0
    return distance

  def get_angle(self):
    angle = self._angle_since
    self._angle_since = 0
    return angle

  def is_bumper_hit(self):
    return (self._hit_left_bumper or self._hit_right_bumper)

  def Loop(self):
    old_data = self._data
    try:
      self._sensors.refresh_sensor_data()
      new_data = self._sensors.get_sensor_data()
    except pyrobot.PyRobotError, e:
      logging.warn('Could not get sensor data.')
      traceback.print_exc()
    except pyrobot.StateError, e:
      logging.debug('Trying to read in uncontrollable state.')
    else:
      if self._sensor_listener_cb != None:
        self._sensor_listener_cb(old_data, new_data)
      self._data = new_data
      self._distance_since += new_data['distance']
      self._angle_since += new_data['angle']
      self._hit_left_bumper = new_data['bump-left']
      self._hit_right_bumper = new_data['bump-right']
    time.sleep(0.5)

  def set_sensor_listener_cb(self, callback):
    self._sensor_listener_cb = callback
