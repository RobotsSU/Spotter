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

import com.cellbots.communication.AbstractCommChannel.CommMessageListener;
import com.cellbots.communication.CommMessage;
import com.cellbots.communication.CommunicationManager;
import com.cellbots.local.AndroidCommandProcessor.ResponseListener;
import com.cellbots.local.CellDroidManager.CellDroidListener;
import com.cellbots.local.robotcontrollerservice.AbstractRobotControllerService;

import java.util.List;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * This class manages the connection to the robot and to the remote-controller.
 *
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 */
public class CellDroid {

    public static final String ACTION_ROBOT_CONTROLLER =
            "com.cellbots.controller.ROBOT_CONTROLLER_SERVICE";

    private static final String TAG = "CellDroid";
    
    private Context mContext;
    
    private CellDroidListener mCelldroidListener;
    
    private CommunicationManager mCommManager;

    private boolean connected = false;

    private AndroidCommandProcessor commandProcessor;

    private PackageManager mPackageManager;

    private IRobotControllerService mControllerService = null;

    private ServiceConnection mServiceConnection = null;

    public static String stripTimestampIfAny(String message) {
        // TODO (chaitanyag): Change this after we come up with a better
        // protocol. The first (space separated) token in the command
        // string could be a timestamp. This is useful if the same commands
        // are sent back to back. For example, the controller sends
        // consecutive "hu" (head up) commands to tilt the head up in small
        // increments.
        if (message.indexOf(' ') >= 0) {
            try {
                Long.parseLong(message.substring(0, message.indexOf(' ')));
                message = message.substring(message.indexOf(' ') + 1);
            } catch (NumberFormatException e) {
            }
        }
        return message;
    }


    private CommMessageListener mCommMessageListener = new CommMessageListener() {
        @Override
        public void onConnected(String channelName, int channel) {
            mCommManager.listenForMessages(channelName, 100, false);
        }
        
        @Override
        public void onConnectError(String channelName, int channel) {
        }

        @Override
        public void onDisconnected(String channelName, int channel) {
        }

        @Override
        public void onMessage(CommMessage msg) {
            String message = CellDroid.stripTimestampIfAny(msg.getMessage());
            if (mCelldroidListener == null || !mCelldroidListener.onMessage(message)) {
                if (!commandProcessor.processCommand(message)) {
                    sendCommandToController(message);
                }
            }
            if (msg.getCommChannel() == CommunicationManager.CHANNEL_XMPP) {
                mCommManager.sendMessage("XMPP", "ACK", "text/text");
            }
        }
    };
    
    private void sendCommandToController(String cmd) {
        if (mControllerService != null) {
            try {
                mControllerService.sendCommand(cmd);
            } catch (RemoteException e) {
                Log.e(TAG, "Error sending command to robot controller service: " + e.getMessage());
            }
        }
    }

    public CellDroid(Context ct, CellDroidListener listener) {
        mContext = ct;
        mCelldroidListener = listener;
        mPackageManager = ct.getPackageManager();
        mCommManager = new CommunicationManager();
        commandProcessor = new AndroidCommandProcessor(ct, new ResponseListener() {
            @Override
            public void onResponseRequested(String response) {
                mCommManager.sendMessage("HTTP_COMMAND", response, "text/text");
            }
        });
    }

    public boolean isConnected() {
        return connected;
    }

    private void startController(
            ServiceInfo serviceInfo, final String robotName,
            final String robotBtName, final String robotBtAddr) {
        if (mControllerService == null) {
            mServiceConnection = new ServiceConnection() {
                public void onServiceConnected(ComponentName name, IBinder service) {
                    mControllerService = IRobotControllerService.Stub.asInterface(service);
                    try {
                        mControllerService.connect(robotName, robotBtName, robotBtAddr);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                public void onServiceDisconnected(ComponentName name) {
                    mControllerService = null;
                    Log.d(TAG, "Robot controller service disconnected.");
                }
            };
            Intent serviceIntent = new Intent();
            serviceIntent.setComponent(
                    new ComponentName(serviceInfo.packageName, serviceInfo.name));
            mContext.bindService(serviceIntent, mServiceConnection, Service.BIND_AUTO_CREATE);
        }
    }

    public void connect(String agentId, String username, String password, String baseUrl,
            boolean useLocalServer) {
        if (username != null && password != null && !username.equals("")) {
            mCommManager.addXmppCommChannel("XMPP", username, password, null, mCommMessageListener);
        }
        if (baseUrl != null && !baseUrl.equals("") && !useLocalServer) {
            mCommManager.addHttpChannel("HTTP_COMMAND", baseUrl + "command.txt",
                    baseUrl + "response.php", mCommMessageListener);
        }
        if (agentId != null && !agentId.equals("")) {
            mCommManager.addAppEngineCommChannel("APP_ENGINE",
                    "http://botczar.appspot.com/" + agentId + "/device", mCommMessageListener);
        }
        mCommManager.connectAll();
    }

    public void disconnect() {
        mCommManager.disconnectAll();
        disconnectController();
        if (commandProcessor != null)
            commandProcessor.shutdown();
    }

    public void setController(String name, String robotBtName, String robotBtAddr) {
        if (name == null || robotBtName == null) {
            return;
        }
        List<ResolveInfo> resolveInfos = mPackageManager.queryIntentServices(
                new Intent(ACTION_ROBOT_CONTROLLER), PackageManager.GET_META_DATA);
        for (ResolveInfo resolveInfo : resolveInfos) {
            ServiceInfo info = resolveInfo.serviceInfo;
            String label = info.loadLabel(mPackageManager).toString();
            if (name.equals(label)) {
                disconnectController();
                startController(info, name, robotBtName, robotBtAddr);
                return;
            }
        }
    }

    public void disconnectController() {
        if (mControllerService != null) {
            try {
                mControllerService.disconnect();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            if (mServiceConnection != null)
                mContext.unbindService(mServiceConnection);
            mServiceConnection = null;
            mControllerService = null;
        }
    }

    public int getControllerState() {
        if (mControllerService == null)
            return AbstractRobotControllerService.STATE_NONE;
        try {
            return mControllerService.getState();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return AbstractRobotControllerService.STATE_ERROR;
    }

    public void sendDirectCommand(String cmd) {
        sendCommandToController(cmd);
    }
    
    public boolean processCommand(String message) {
        return commandProcessor.processCommand(message);
    }
}
