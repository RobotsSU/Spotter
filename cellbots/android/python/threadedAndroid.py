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

"""Thread-safe instance(s) of Adroid objects.

  Most code can just do this:
  from threadedAndroid import droid

  Then call droid.func(..) as you like.  Each call will lock/unlock to be
  thread-safe.  If we find that many threads want to use them simultaneously, we
  could switch to using ThreadlocalAndroid that makes a new one for each thread.

  Anything that needs its own state, like the bluetooth reader/writer, should
  have its OWN LockedAndroid.
"""

import android
import threading

# We'll have one of these as the global object ("droid"), and another
# for bluetooth comm.
class LockedAndroid(object):
  """Works like an adroid object, but calls will block eachother."""

  def __init__(self, debug=False):
    self.my_lock = threading.Lock()
    self.my_droid = android.Android()
    self.debug = debug

  def __getattr__(self, name):
    """Dispatch any droid.FuncX(...) calls to the appropriate obj/method."""

    if self.debug:
      print "Locking/Dispatching to my_droid.%s() on thread %d" % (
          name, threading.currentThread().ident)
    # We use __getattr__ instead of _getattribute__ here because
    # Adroid object has it defined explicitly.
    with self.my_lock:
      return self.my_droid.__getattr__(name)


# Not currently used, but works.
class ThreadlocalAndroid(threading.local):
  """Keep a separate Android object for each thread."""

  def __init__(self, debug=False):
    self.debug = debug
    if self.debug:
      ident = threading.currentThread().ident
      print "MAKING NEW android object for thread %d" % ident
    self.my_droid = android.Android()

  def __getattr__(self, name):
    """Dispatch any droid.FuncX(...) calls to the appropriate obj/method."""

    if self.debug:
      print "Dispatching to my_droid.%s() on thread %d" % (
          name, threading.currentThread().ident)
    # We use __getattr__ instead of _getattribute__ here because
    # Adroid object has it defined explicitly.
    return self.my_droid.__getattr__(name)

# Most code should use this instance:
#droid = ThreadlocalAndroid()
droid = LockedAndroid()
