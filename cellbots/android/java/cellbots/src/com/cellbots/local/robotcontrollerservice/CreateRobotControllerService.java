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

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class for the robot controller for the iRobot Create.
 * 
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 *
 */
public class CreateRobotControllerService extends AbstractRobotControllerService {
    
    private static final String TAG = "CREATERobotControllerService";
    
    private RobotBtController robotController;
    
    private boolean readingSensors = false;
    
    // [19][N-bytes][packetId1][data][data][packetId2][data][data][checksum]
    private byte[] sensorBytes = new byte[9];
    
    private Thread sensorThread;
    
    private int cumulativeDist = 0;
    
    private int cumulativeAngle = 0;
    
    private double cumulativeX = 0;
    
    private double cumulativeY = 0;
    
    private ReentrantLock lock = new ReentrantLock();
    
    private class MoveWaiter implements Runnable {
        private long waitTime;
        
        public MoveWaiter(long delay) {
            waitTime = delay;
        }
        
        @Override
        public void run() {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
            robotController.write(getIRobotCreateCommand("s"));
        }
    }
    
    private Thread waiterThread;
    
    private Runnable sensorReader = new Runnable() {
        private byte read() throws IOException {
            try {
                if (readingSensors)
                    return robotController.read();
                else
                    throw new IOException();
            } catch (IOException e) {
                throw new IOException();
            }
        }
        
        @Override
        public void run() {
            boolean startByte = false;
            try {
                while (readingSensors) {
                    int val = robotController.read();
                    if (val == 6 && startByte) {
                        // Flush out data for first data set
                        for (int i = 0; i < 7; i++)
                        read();
                        break;
                    } else if (val == 19) {
                        startByte = true;
                    } else {
                        startByte = false;
                    }
                }
                while(readingSensors) {
                    lock.lock();
                    int numRead = 0;
                    while (numRead < sensorBytes.length) {
                        numRead += robotController.read(sensorBytes, numRead,
                                sensorBytes.length - numRead);
                    }
                    if (sensorBytes[0] == 19) {
                        int numPackets = sensorBytes[1];
                        double dist = 0, angle = 0;
                        // Assume that each <packetID + data> = 3 bytes
                        for (int i = 2; i <= numPackets + 1; i += 3) {
                                int packetId = sensorBytes[i];
                                int value = (0x00 | sensorBytes[i + 1]) << 8;
                                value |= sensorBytes[i + 2];
                                if (packetId == 19) {
                                    cumulativeDist += value;
                                    dist = value;
                                } else if (packetId == 20) {
                                    cumulativeAngle = (cumulativeAngle + value) % 360;
                                    angle = value;
                                }
                        }
                        cumulativeX += dist * Math.sin(cumulativeAngle * Math.PI / 180);
                        cumulativeY += dist * Math.cos(cumulativeAngle * Math.PI / 180);
                    }
                    lock.unlock();
                    Thread.sleep(5);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error sending data over Bluetooth.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (intent.getComponent().getClassName().equals(
                CreateRobotControllerService.class.getName())) {
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
                    if (robotController.startConnection()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        sendStartCommand();
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
            readingSensors = false;
            try {
            if (sensorThread != null)
                sensorThread.join(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cumulativeDist = 0;
            cumulativeAngle = 0;
            if (robotController != null)
                robotController.disconnect();
        }

        @Override
        public void sendCommand(String cmd) {
            super.sendCommand(cmd);
            if (robotController != null && mCurrentState == STATE_SUCCESS)
                robotController.write(getIRobotCreateCommand(cmd));
        }

        @Override
        public String getOdometer(boolean reset) {
            if (!robotController.isConnected()) return null;
            if (!readingSensors)
                sendSensorStreamRequest();
            return System.currentTimeMillis() + ":" + cumulativeDist + ":" +
                    cumulativeAngle + ":" + cumulativeX + ":" + cumulativeY;
        }
        
        @Override
        public void resetOdometer() {
            super.resetOdometer();
            cumulativeAngle = 0;
            cumulativeDist = 0;
            cumulativeX = 0;
            cumulativeY = 0;
        }
    };
    
    private void sendStartCommand() {
        byte[] startCmd = new byte[2];
        startCmd[0] = (byte) (1 << 7);    // 128: Start OI
        startCmd[1] = (byte) (0x84);      // 132: full mode
        robotController.write(startCmd);
    }
    
    private void sendSensorStreamRequest() {
        readingSensors = true;
        sensorThread = new Thread(sensorReader);
        sensorThread.start();
        byte[] startCmd = new byte[4];
        startCmd[0] = (byte) 0x94; // 148: Stream opcode
        startCmd[1] = (byte) 0x02; // 2: Number of Packet IDs
        startCmd[2] = (byte) 0x13; // 19: Packet ID for distance
        startCmd[3] = (byte) 0x14; // 20: Packet ID for angle
        robotController.write(startCmd);
    }
    
    @SuppressWarnings("unused")
    private void pauseStream() {
        byte[] startCmd = new byte[2];
        startCmd[0] = (byte) 0x96; // 150: Pause/resume stream opcode
        startCmd[1] = (byte) 0x00; // 0: Pause
        robotController.write(startCmd);
    }
    
    @SuppressWarnings("unused")
    private void resumeStream() {
        byte[] startCmd = new byte[2];
        startCmd[0] = (byte) 0x96; // 150: Pause/resume stream opcode
        startCmd[1] = (byte) 0x01; // 1: Resume
        robotController.write(startCmd);
    }

    private byte[] getIRobotCreateCommand(String cmd) {
        int moveSpeed = 300;
        int turnSpeed = 100;
        String[] tokens = cmd.split(" ");
        if (tokens == null || tokens.length == 0) return null;
        if ((tokens[0].equals("f") || tokens[0].equals("b") ||
             tokens[0].equals("l") || tokens[0].equals("r")) &&
            tokens.length == 3) {
            return getFixedMotionCommand(tokens[0],
                    Math.max(-500, Math.min(500, Integer.parseInt(tokens[1]) * 5)),
                    Integer.parseInt(tokens[2]));
        } else if ((tokens[0].equals("fd") || tokens[0].equals("bd") ||
                    tokens[0].equals("ld") || tokens[0].equals("rd")) &&
                   tokens.length == 3) {
            moveSpeed = turnSpeed = Math.max(-500, Math.min(500, Integer.parseInt(tokens[1]) * 5));
            tokens[0] = tokens[0].charAt(0) + "";
            if (waiterThread != null) {
                waiterThread.interrupt();
            }
            waiterThread = new Thread(new MoveWaiter(Integer.parseInt(tokens[2])));
            waiterThread.start();
        }
        
        int left = 0, right = 0;
        if (tokens[0].equals("s")) {
            tokens = "w 0 0".split(" ");
        }
        if (tokens[0].equals("w")) {
            if (tokens.length != 3) return null;
            try {
                left = Math.max(-500, Math.min(500, Integer.parseInt(tokens[1]) * 5));
                right = Math.max(-500, Math.min(500, Integer.parseInt(tokens[2]) * 5));
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (tokens[0].equals("f")) {
            left = moveSpeed; 
            right = moveSpeed;
        } else if (tokens[0].equals("b")){
            left = -moveSpeed; 
            right = -moveSpeed;
        } else if (tokens[0].equals("l")){
            left = -turnSpeed;
            right = turnSpeed;
        } else if (tokens[0].equals("r")){
            left = turnSpeed;
            right = -turnSpeed;
        }
        byte[] cmdBytes = new byte[5];
        cmdBytes[0] = (byte) 0x91;
        short rightVel = (short) right;
        short leftVel = (short) left;
        cmdBytes[1] = (byte) ((rightVel & 0xFF00) >> 8);
        cmdBytes[2] = (byte) (rightVel & 0x00FF);
        cmdBytes[3] = (byte) ((leftVel & 0xFF00) >> 8);
        cmdBytes[4] = (byte) (leftVel & 0x00FF);
        return cmdBytes;
    }
    
    private byte[] getFixedMotionCommand(String cmd, int speed, int value) {
        byte[] cmdBytes = null;
        if (readingSensors)
            cmdBytes = new byte[20];
        else
            cmdBytes = new byte[16];
        int pos = 0;
        cmdBytes[pos++] = (byte) 0x98; // 152: Opcode for script command
        cmdBytes[pos++] = (byte) 0x11; // 15: Script length
        
        if (readingSensors) {
            // Sensor stream should be paused to execute script commands
            cmdBytes[pos++] = (byte) 0x96; // 150: Pause/resume stream opcode
            cmdBytes[pos++] = (byte) 0x00; // 0: Pause
        }
            
        int ls = 0, rs = 0;
        if (cmd.equals("f")) {
            ls = rs = speed;
        } else if (cmd.equals("b")) {
            ls = rs = -speed;
            value = -value;
        } else if (cmd.equals("l")) {
            ls = -speed;
            rs = speed;
        } else if (cmd.equals("r")) {
            ls = speed;
            rs = -speed;
            value = -value;
        }
        byte[] tmp = getIRobotCreateCommand("w " + ls + " " + rs);
        for (int i = 0; i < tmp.length; i++) {
            cmdBytes[pos++] = tmp[i];
        }
        
        if (cmd.equals("f") || cmd.equals("b"))
            cmdBytes[pos++] = (byte) 0x9C; // 156: opcode for Wait distance
        else if (cmd.equals("r") || cmd.equals("l"))
            cmdBytes[pos++] = (byte) 0x9D; // 157: opcode for Wait angle
        cmdBytes[pos++] = (byte) ((value & 0xFF00) >> 8);
        cmdBytes[pos++] = (byte) (value & 0x00FF);
        tmp = getIRobotCreateCommand("w 0 0");
        for (int i = 0; i < tmp.length; i++)
            cmdBytes[pos++] = tmp[i];

        if (readingSensors) {
            // Resume sensor stream after 
            cmdBytes[pos++] = (byte) 0x96; // 150: Pause/resume stream opcode
            cmdBytes[pos++] = (byte) 0x01; // 0: Resume
        }

        cmdBytes[pos] = (byte) 0x99;    // Loop forever? We somehow need this.
        return cmdBytes;
    }
}
