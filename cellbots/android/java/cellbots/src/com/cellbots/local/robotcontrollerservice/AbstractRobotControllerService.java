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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.cellbots.local.IRobotControllerService;

/**
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 *
 */
public class AbstractRobotControllerService extends Service {

    public static final int STATE_BLUETOOTH_FAIL = 0;
    
    public static final int STATE_ERROR = 1;

    public static final int STATE_SUCCESS = 2;

    public static final int STATE_STARTING = 3;
    
    public static final int STATE_NONE = 4;
    
    protected int mCurrentState = STATE_NONE;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public class Stub extends IRobotControllerService.Stub {

        /* (non-Javadoc)
         * @see com.cellbots.local.IRobotControllerService#connect(java.lang.String, java.lang.String, java.lang.String)
         */
        @Override
        public void connect(String username, String robotBtName, String robotBtAddr) {
            mCurrentState = STATE_STARTING;
        }

        /* (non-Javadoc)
         * @see com.cellbots.local.IRobotControllerService#disconnect()
         */
        @Override
        public void disconnect() {
        }

        /* (non-Javadoc)
         * @see com.cellbots.local.IRobotControllerService#getOdometer(boolean)
         */
        @Override
        public String getOdometer(boolean reset) {
            return null;
        }

        /* (non-Javadoc)
         * @see com.cellbots.local.IRobotControllerService#getPose()
         */
        @Override
        public String getPose() {
            return null;
        }

        /* (non-Javadoc)
         * @see com.cellbots.local.IRobotControllerService#getSensorData(int)
         */
        @Override
        public byte[] getSensorData(int sensorType) {
            return null;
        }

        /* (non-Javadoc)
         * @see com.cellbots.local.IRobotControllerService#getState()
         */
        @Override
        public int getState() {
            return mCurrentState;
        }

        /* (non-Javadoc)
         * @see com.cellbots.local.IRobotControllerService#getStateJson()
         */
        @Override
        public String getStateJson() {
            return null;
        }

        /* (non-Javadoc)
         * @see com.cellbots.local.IRobotControllerService#resetOdometer()
         */
        @Override
        public void resetOdometer() {
        }

        /* (non-Javadoc)
         * @see com.cellbots.local.IRobotControllerService#sendCommand(java.lang.String)
         */
        @Override
        public void sendCommand(String cmd) {
        }
        
    }
}
