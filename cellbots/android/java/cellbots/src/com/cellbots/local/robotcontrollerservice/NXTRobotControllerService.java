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

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Class for the robot controller for Lego Mindstorms NXT.
 * 
 * @author knagpal@google.com (Kewaljit Nagpal)
 * @author keertip@google.com (Keerti Parthasarathy)
 *
 */
public class NXTRobotControllerService extends AbstractRobotControllerService {
    
    private static final String TAG = "NXTRobotControllerService";
    
    private RobotBtController robotController;
    
    // Constants for setOutputState parameters.
    private static final int COMMAND_TYPE_DIRECT_WITH_RESPONSE = 0x00;
    private static final int COMMAND_TYPE_SYSTEM_WITH_RESPONSE = 0x01;
    private static final int COMMAND_TYPE_RESPONSE = 0x02;
    private static final int COMMAND_TYPE_DIRECT_NO_RESPONSE = 0x80;
    private static final int COMMAND_TYPE_SYSTEM_NO_RESPONSE = 0x81;
    private static final int OUTPUT_PORT_A = 0x00;
    private static final int OUTPUT_PORT_B = 0x01;
    private static final int OUTPUT_PORT_C = 0x02;
    private static final int MODE_MOTORON = 0x01;
    private static final int MODE_BRAKE = 0x02;
    private static final int MODE_REGULATED = 0x04;
    private static final int REGULATION_MODE_IDLE = 0x00;
    private static final int REGULATION_MODE_MOTOR_SPEED = 0x01;
    private static final int REGULATION_MODE_MOTOR_SYNC = 0x02;
    private static final int MOTOR_RUN_STATE_IDLE = 0x00;
    private static final int MOTOR_RUN_STATE_RAMPUP = 0x10;
    private static final int MOTOR_RUN_STATE_RUNNING = 0x20;
    private static final int MOTOR_RUN_STATE_RAMPDOWN = 0x40;

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
                NXTRobotControllerService.class.getName())) {
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
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.i("Connect","This is a test\n");
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
            }).start();
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
            if (robotController != null ) {
                robotController.write(getNXTCreateCommand(cmd));
            }
        }
    };
    
    private void sendStartCommand() {
        byte[] startCmd = new byte[2];
        startCmd[0] = (byte) (1 << 7);    // 128: Start OI
        startCmd[1] = (byte) (0x84);    // 132: full mode
        robotController.write(startCmd);
    }

    private byte[] getNXTCreateCommand(String cmd) {
        String[] tokens = cmd.split(" ");
        byte[] cmdBytes = new byte[28];
        
        if (tokens == null || tokens.length == 0) return null;
        if (tokens[0].equals("s")) {
            cmdBytes[0] = (byte) 0x0c; // Command Length LSB
            cmdBytes[1] = (byte) 0x00; // Command Length MSB
            cmdBytes[2] = (byte) COMMAND_TYPE_DIRECT_NO_RESPONSE;
            cmdBytes[3] = (byte) 0x04; // SETOUTPUTSTATE command
            cmdBytes[4] = (byte) OUTPUT_PORT_B;
            cmdBytes[5] = (byte) 0x00; // Power setting between 100 and -100
            cmdBytes[6] = (byte) MODE_BRAKE;
            cmdBytes[7] = (byte) REGULATION_MODE_IDLE;
            cmdBytes[8] = (byte) 0x00; // Turn ratio between 100 and -100
            cmdBytes[9] = (byte) MOTOR_RUN_STATE_IDLE;
            cmdBytes[10] = (byte) 0x00; // TachoLimit
            cmdBytes[11] = (byte) 0x00; // TachoLimit
            cmdBytes[12] = (byte) 0x00; // TachoLimit
            cmdBytes[13] = (byte) 0x00; // TachoLimit
            cmdBytes[14] = (byte) 0x0c; // Command Length LSB
            cmdBytes[15] = (byte) 0x00; // Command Length MSB
            cmdBytes[16] = (byte) COMMAND_TYPE_DIRECT_NO_RESPONSE;
            cmdBytes[17] = (byte) 0x04; // SETOUTPUTSTATE command
            cmdBytes[18] = (byte) OUTPUT_PORT_C;
            cmdBytes[19] = (byte) 0x00; // Power setting between 100 and -100
            cmdBytes[20] = (byte) MODE_BRAKE;
            cmdBytes[21] = (byte) REGULATION_MODE_IDLE;
            cmdBytes[22] = (byte) 0x00; // Turn ratio between 100 and -100
            cmdBytes[23] = (byte) MOTOR_RUN_STATE_IDLE;
            cmdBytes[24] = (byte) 0x00; // TachoLimit
            cmdBytes[25] = (byte) 0x00; // TachoLimit
            cmdBytes[26] = (byte) 0x00; // TachoLimit
            cmdBytes[27] = (byte) 0x00; // TachoLimit
            return cmdBytes;
        }
        if (tokens[0].equals("f") || tokens[0].equals("b") ||
            (tokens[0].equals("l")) || tokens[0].equals("r")){
            int left = 100, right = 100;
            if (tokens[0].equals("r")){
                right = 40;
            }
            if (tokens[0].equals("l")){
                left = 40;
            }
            if (tokens[0].equals("b")){
                right = -100;
                left = -100;
            }
            cmdBytes[0] = (byte) 0x0c; // Command Length LSB
            cmdBytes[1] = (byte) 0x00; // Command Length MSB
            cmdBytes[2] = (byte) COMMAND_TYPE_DIRECT_NO_RESPONSE;
            cmdBytes[3] = (byte) 0x04; // SETOUTPUTSTATE command
            cmdBytes[4] = (byte) OUTPUT_PORT_B;
            cmdBytes[5] = (byte) left; // Power setting between 100 and -100
            cmdBytes[6] = (byte) MODE_MOTORON;
            cmdBytes[7] = (byte) REGULATION_MODE_MOTOR_SPEED;
            cmdBytes[8] = (byte) 0x00; // Turn ratio between 100 and -100
            cmdBytes[9] = (byte) MOTOR_RUN_STATE_RUNNING;
            cmdBytes[10] = (byte) 0x00; // TachoLimit
            cmdBytes[11] = (byte) 0x00; // TachoLimit
            cmdBytes[12] = (byte) 0x00; // TachoLimit
            cmdBytes[13] = (byte) 0x00; // TachoLimit
            cmdBytes[14] = (byte) 0x0c; // Command Length LSB
            cmdBytes[15] = (byte) 0x00; // Command Length MSB
            cmdBytes[16] = (byte) COMMAND_TYPE_DIRECT_NO_RESPONSE;
            cmdBytes[17] = (byte) 0x04; // SETOUTPUTSTATE command
            cmdBytes[18] = (byte) OUTPUT_PORT_C;
            cmdBytes[19] = (byte) right; // Power setting between 100 and -100
            cmdBytes[20] = (byte) MODE_MOTORON;
            cmdBytes[21] = (byte) REGULATION_MODE_MOTOR_SPEED;
            cmdBytes[22] = (byte) 0x00; // Turn ratio between 100 and -100
            cmdBytes[23] = (byte) MOTOR_RUN_STATE_RUNNING;
            cmdBytes[24] = (byte) 0x00; // TachoLimit
            cmdBytes[25] = (byte) 0x00; // TachoLimit
            cmdBytes[26] = (byte) 0x00; // TachoLimit
            cmdBytes[27] = (byte) 0x00; // TachoLimit
            return cmdBytes;
           
        }
        
        if (tokens[0].equals("w")) {
            if (tokens.length != 3) return null;
            int left = 0, right = 0;
            try {
                left = Math.max(-100, Math.min(100, Integer.parseInt(tokens[1]) * 5));
                right = Math.max(-100, Math.min(100, Integer.parseInt(tokens[2]) * 5));
            } catch (NumberFormatException e) {
                return null;
            } 
            cmdBytes[0] = (byte) 0x0c; // Command Length LSB
            cmdBytes[1] = (byte) 0x00; // Command Length MSB
            cmdBytes[2] = (byte) COMMAND_TYPE_DIRECT_NO_RESPONSE;
            cmdBytes[3] = (byte) 0x04; // SETOUTPUTSTATE command
            cmdBytes[4] = (byte) OUTPUT_PORT_B;
            cmdBytes[5] = (byte) left; // Power setting between 100 and -100
            cmdBytes[6] = (byte) MODE_MOTORON;
            cmdBytes[7] = (byte) REGULATION_MODE_MOTOR_SPEED;
            cmdBytes[8] = (byte) 0x00; // Turn ratio between 100 and -100
            cmdBytes[9] = (byte) MOTOR_RUN_STATE_RUNNING;
            cmdBytes[10] = (byte) 0x00; // TachoLimit
            cmdBytes[11] = (byte) 0x00; // TachoLimit
            cmdBytes[12] = (byte) 0x00; // TachoLimit
            cmdBytes[13] = (byte) 0x00; // TachoLimit
            cmdBytes[14] = (byte) 0x0c; // Command Length LSB
            cmdBytes[15] = (byte) 0x00; // Command Length MSB
            cmdBytes[16] = (byte) COMMAND_TYPE_DIRECT_NO_RESPONSE;
            cmdBytes[17] = (byte) 0x04; // SETOUTPUTSTATE command
            cmdBytes[18] = (byte) OUTPUT_PORT_C;
            cmdBytes[19] = (byte) right; // Power setting between 100 and -100
            cmdBytes[20] = (byte) MODE_MOTORON;
            cmdBytes[21] = (byte) REGULATION_MODE_MOTOR_SPEED;
            cmdBytes[22] = (byte) 0x00; // Turn ratio between 100 and -100
            cmdBytes[23] = (byte) MOTOR_RUN_STATE_RUNNING;
            cmdBytes[24] = (byte) 0x00; // TachoLimit
            cmdBytes[25] = (byte) 0x00; // TachoLimit
            cmdBytes[26] = (byte) 0x00; // TachoLimit
            cmdBytes[27] = (byte) 0x00; // TachoLimit
            return cmdBytes;
        }
        if (tokens[0].equals("hl") || tokens[0].equals("hu")) {
            int hls = 0;
            if (tokens.length == 2) { 
                hls = Math.max(0, Math.min(100, Integer.parseInt(tokens[1])));
            } else { 
                hls = 50;
            }
            cmdBytes[0] = (byte) 0x0c; // Command Length LSB
            cmdBytes[1] = (byte) 0x00; // Command Length MSB
            cmdBytes[2] = (byte) COMMAND_TYPE_DIRECT_NO_RESPONSE;
            cmdBytes[3] = (byte) 0x04; // SETOUTPUTSTATE command
            cmdBytes[4] = (byte) OUTPUT_PORT_A;
            cmdBytes[5] = (byte) hls; // Power setting between 100 and -100
            cmdBytes[6] = (byte) MODE_MOTORON;
            cmdBytes[7] = (byte) REGULATION_MODE_MOTOR_SPEED;
            cmdBytes[8] = (byte) 0x00; // Turn ratio between 100 and -100
            cmdBytes[9] = (byte) MOTOR_RUN_STATE_RUNNING;
            cmdBytes[10] = (byte) 0x00; // TachoLimit
            cmdBytes[11] = (byte) 0x00; // TachoLimit
            cmdBytes[12] = (byte) 0x00; // TachoLimit
            cmdBytes[13] = (byte) 0x00; // TachoLimit
            return cmdBytes;
        }
        if (tokens[0].equals("hr") || tokens[0].equals("hd")) {
            int hrs = 0;
            if (tokens.length == 2) { 
                hrs = Math.max(0, Math.min(100, Integer.parseInt(tokens[1])));
            } else { 
                hrs = 50;
            }
            cmdBytes[0] = (byte) 0x0c; // Command Length LSB
            cmdBytes[1] = (byte) 0x00; // Command Length MSB
            cmdBytes[2] = (byte) COMMAND_TYPE_DIRECT_NO_RESPONSE;
            cmdBytes[3] = (byte) 0x04; // SETOUTPUTSTATE command
            cmdBytes[4] = (byte) OUTPUT_PORT_A;
            cmdBytes[5] = (byte) -hrs; // Power setting between 100 and -100
            cmdBytes[6] = (byte) MODE_MOTORON;
            cmdBytes[7] = (byte) REGULATION_MODE_MOTOR_SPEED;
            cmdBytes[8] = (byte) 0x00; // Turn ratio between 100 and -100
            cmdBytes[9] = (byte) MOTOR_RUN_STATE_RUNNING;
            cmdBytes[10] = (byte) 0x00; // TachoLimit
            cmdBytes[11] = (byte) 0x00; // TachoLimit
            cmdBytes[12] = (byte) 0x00; // TachoLimit
            cmdBytes[13] = (byte) 0x00; // TachoLimit
            return cmdBytes;
        }
        if (tokens[0].equals("hs")) {
            cmdBytes[0] = (byte) 0x0c; // Command Length LSB
            cmdBytes[1] = (byte) 0x00; // Command Length MSB
            cmdBytes[2] = (byte) COMMAND_TYPE_DIRECT_NO_RESPONSE;
            cmdBytes[3] = (byte) 0x04; // SETOUTPUTSTATE command
            cmdBytes[4] = (byte) OUTPUT_PORT_A;
            cmdBytes[5] = (byte) 0x00; // Power setting between 100 and -100
            cmdBytes[6] = (byte) MODE_BRAKE;
            cmdBytes[7] = (byte) REGULATION_MODE_IDLE;
            cmdBytes[8] = (byte) 0x00; // Turn ratio between 100 and -100
            cmdBytes[9] = (byte) MOTOR_RUN_STATE_IDLE;
            cmdBytes[10] = (byte) 0x00; // TachoLimit
            cmdBytes[11] = (byte) 0x00; // TachoLimit
            cmdBytes[12] = (byte) 0x00; // TachoLimit
            cmdBytes[13] = (byte) 0x00; // TachoLimit
            return cmdBytes;
        }
        return null;
    }
}
