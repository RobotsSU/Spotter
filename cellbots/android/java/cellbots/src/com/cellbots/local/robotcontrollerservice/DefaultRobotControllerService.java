/*
 * Copyright (C) 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.cellbots.local.robotcontrollerservice;

import com.cellbots.local.RobotBtController;

import org.jivesoftware.smack.sasl.SASLMechanism.Success;

import android.content.Intent;
import android.os.IBinder;

/**
 * Class for the robot controller for all robots that conform with the motor
 * command spec, and thus respond to the same commands that CellDroid receives
 * from the remote controller. Thus class just forwards the commands received
 * from the remote controller, to the robot over Bluetooth.
 *
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 */
public class DefaultRobotControllerService extends AbstractRobotControllerService {

    private static final String TAG = "DefaultRobotControllerService";

    private RobotBtController robotController;

    /** Called when the activity is first created. */
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /*
     * (non-Javadoc)
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent intent) {
        if (intent.getComponent().getClassName().equals(
                DefaultRobotControllerService.class.getName())) {
            return mBinder;
        }
        return null;
    }

    /**
     * Stub for exposing the service's interface.
     */
    private final AbstractRobotControllerService.Stub mBinder =
            new AbstractRobotControllerService.Stub() {
        @Override
        public void connect(String username, String robotBtName, String robotBtAddr) {
            super.connect(username, robotBtName, robotBtAddr);
            robotController = new RobotBtController(username, robotBtName, robotBtAddr);
            if (robotController.startConnection()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mCurrentState = STATE_SUCCESS;
            } else {
                mCurrentState = STATE_BLUETOOTH_FAIL;
            }
        }

        @Override
        public void disconnect() {
            super.disconnect();
            if (robotController != null)
                robotController.disconnect();
        }

        @Override
        public void sendCommand(String cmd) {
            super.sendCommand(cmd);
            if (robotController != null && mCurrentState == STATE_SUCCESS)
                robotController.write(cmd);
        }
    };
}
