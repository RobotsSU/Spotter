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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import com.cellbots.local.robotcontrollerservice.AbstractRobotControllerService;

import java.util.List;

/**
 * Helps manage connection to the CellDroid service.
 *
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 */
public class CellDroidManager {

    private static final String TAG = "CellDroidManager";

    private static CellDroid mCellDroid = null;

    private Context mContext;

    public static void sendDirectCommand(String cmd) {
        mCellDroid.sendDirectCommand(cmd);
    }

    public static String[] getControllers(Context ct) {
        PackageManager mPackageManager = ct.getPackageManager();
        List<ResolveInfo> resolveInfos = mPackageManager.queryIntentServices(
                new Intent(CellDroid.ACTION_ROBOT_CONTROLLER), PackageManager.GET_META_DATA);
        if (resolveInfos == null || resolveInfos.size() == 0)
            return null;
        String[] controllers = new String[resolveInfos.size()];
        int i = 0;
        for (ResolveInfo resolveInfo : resolveInfos) {
            controllers[i++] = resolveInfo.serviceInfo.loadLabel(mPackageManager).toString();
        }
        return controllers;
    }

    public CellDroidManager(Context ct, CellDroidListener listener) {
        mContext = ct;
        mCellDroid = new CellDroid(mContext, listener);
    }

    /**
     * Connects to the service by the specified username and password.
     *
     * @param username
     * @param password
     */
    public void connect(final String agentId, final String username, final String password,
            final String cmdUrl, final boolean useLocalServer) {
        if (mCellDroid != null) {
            Log.e("DEBUG agentId", agentId);
            mCellDroid.connect(agentId, username, password, cmdUrl, useLocalServer);
        }
    }

    public void disconnect() {
        if (mCellDroid != null)
            mCellDroid.disconnect();
    }

    public int getState() {
        if (mCellDroid != null)
            return mCellDroid.getControllerState();
        return AbstractRobotControllerService.STATE_NONE;
    }
    
    public boolean processCommand(String message) {
        if (mCellDroid != null)
            return mCellDroid.processCommand(message);
        return false;
    }

    /**
     * Stops the CellDroid service. Make sure to call this in your Activity's
     * onDestroy().
     */
    public void stopCellDroidService() {
        disconnect();
    }

    public void setController(String label, String robotBtName, String robotBtAddr) {
        if (mCellDroid != null) {
            mCellDroid.setController(label, robotBtName, robotBtAddr);
        } else {
            Log.w(TAG, "mCellDroid was null in setController");
        }
    }
    
    public interface CellDroidListener {
        public boolean onMessage(String message);
    }
}
