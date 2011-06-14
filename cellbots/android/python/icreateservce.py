#!/usr/bin/python

import logging
import threading
import time


class Service(object):

  """A Service runs in a separate thread and can be started and stopped."""

  def __init__(self):
    self.name = self.__class__.__name__
    self._join = False
    self._thread = None

  def Loop(self):
    """Should be overridden by subclass to define a single loop iteration."""
    raise NotImplementedError

  def _Loop(self):
    """Loop until asked to stop."""
    while not self._join:
      try:
        self.Loop()
      except:
        logging.info('Exception in service %s.' % self.name)
        raise

  def Start(self):
    """Start up the service."""
    if self._thread is not None:
      logging.info('Restarting service %s.' % self.name)
      self.Stop()
    else:
      logging.info('Starting service %s.' % self.name)
    self._thread = threading.Thread(target=self._Loop)
    self._thread.setDaemon(True)
    self._thread.start()

  def Stop(self):
    """Stop the service."""
    self._join = True
    if self._thread is not None:
      self._thread.join()
      self._thread = None
    self._join = False
