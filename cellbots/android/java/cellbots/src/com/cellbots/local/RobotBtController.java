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

package com.cellbots.local;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * This class connects to the robot via Bluetooth and sends allows sending
 * commands to it.
 *
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 */
public class RobotBtController {

    private static final String TAG = "BluetoothClient";
    
    private String mDeviceName = "";

    private String mDeviceAddr = "";

    private BluetoothAdapter mBtAdapter;

    private BluetoothDevice mBtDevice = null;

    private BluetoothSocket mBtSocket = null;
    
    private InputStream mInputStream = null;

    /**
     * Creates an instance of RobotBtController which can be used to send
     * commands to the robot via Bluetooth.
     *
     * @param robotName The name of the robot as its GMail account.
     * @param robotBtName The name of the robot's BT device.
     * @param robotBtAddr The MAC address of the robot's BT device.
     */
    public RobotBtController(String robotName, String robotBtName, String robotBtAddr) {
        mDeviceName = robotBtName;
        if (mDeviceName == null)
            return;
        mDeviceAddr = robotBtAddr;
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public synchronized void write(byte[] cmd) {
        if (cmd == null)
            return;
        try {
            mBtSocket.getOutputStream().write(cmd);
        } catch (IOException e) {
            Log.e(TAG, "Error sending data to BT device: " + e.getMessage());
        }
    }

    /**
     * Appends newline to the specified command string and sends it to the robot
     * via Bluetooth.
     *
     * @param cmd
     */
    public synchronized void write(String cmd) {
        if (mBtDevice == null) {
            Log.e(TAG, "Cannot write. Not connected to the robot.");
            return;
        }
        write((cmd + "\n").getBytes());
    }

    public synchronized byte read() {
        try {
            return (byte) mBtSocket.getInputStream().read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    public synchronized int read(byte[] data, int offset, int size) {
        try {
            return mInputStream.read(data, offset, size);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void disconnect() {
        try {
            if (mBtSocket != null) {
                mBtSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        try {
            return (mBtDevice != null && mBtSocket != null && mBtSocket.getInputStream() != null);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Opens a connection with the Bluetooth device specified by |mDeviceName|.
     * The phone should first be paired with the Bluetooth device.
     * If we have the bluetooth mac address, just use that directly. Otherwise
     * search for a device based on the bluetooth name.
     */
    public boolean startConnection() {
        mBtDevice = null;
        if (BluetoothAdapter.checkBluetoothAddress(mDeviceAddr)) {
            mBtDevice = mBtAdapter.getRemoteDevice(mDeviceAddr);
        } else {
            Set<BluetoothDevice> devices = mBtAdapter.getBondedDevices();
            for (BluetoothDevice d : devices) {
                if (mDeviceName.equals(d.getName())) {
                    mBtDevice = d;
                    break;
                }
            }
        }

        // Did we find a device to try?
        if (mBtDevice == null) {
            Log.e(TAG, "Unable to find robot's Bluetooth. Is it paired?");
            return false;
        }

        // According to the docs, always cancel discovery before trying
        // any connections, just to be safe.
        mBtAdapter.cancelDiscovery();

        try {
            mBtSocket = mBtDevice.createRfcommSocketToServiceRecord(
                    UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            mBtSocket.connect();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Unable to connect to robot's Bluetooth." + e.getMessage());
            mBtSocket = null;
        }
        
        // According to stackoverflow.com:
        // http://stackoverflow.com/questions/3397071/android-bluetooth-service-discovery-failed-exception
        // There may be a kernel bug that prevents connecting sometimes.
        // Had code to try the workaround, but recent kernels don't seem to
        // have the problem so code was removed.
                
        mBtDevice = null;
        mBtSocket = null;
        return false;
    }
}
