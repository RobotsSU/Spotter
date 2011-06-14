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

package com.cellbots;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * Determine whether Bluetooth is enabled or not. If it is not enabled,
 * ask the user whether they want it enabled. This activity returns a
 * result indicating success or user intent (intention cancel vs.
 * intention cloud only).
 * 
 * @author css@google.com (Charles Spirakis)
 *
 */
public class CheckBluetoothEnabledActivity extends Activity {
    private static final String TAG = "CheckBluetoothEnableActivity";

    // Validated bluetooth is up.
    public static final int RESULT_SUCCESS = Activity.RESULT_OK;

    // User canceled. Don't know intent beyond wanted to cancel.
    public static final int RESULT_CANCEL = Activity.RESULT_CANCELED;

    // Wanted bluetooth, but there was a failure somewhere
    public static final int RESULT_FAILURE = 2;

    // Expressly said didn't want bluetooth and only wanted
    // to control robots via the cloud/web.
    public static final int RESULT_WEB_ONLY = 3;

    private static final String PREF_HELP_BLUETOOTH_PAIRING = "BLUE_HELP";

    private SharedPreferences mGlobalPrefs;
    
    private BluetoothAdapter mBtAdapter;
    
    private boolean mShowWebControl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.check_bluetooth_enable);

        mGlobalPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        // If the user doesn't want the bluetooth checker to run, then
        // just leave.
        if (!mGlobalPrefs.getBoolean(PREF_HELP_BLUETOOTH_PAIRING, true)) {
            setResult(CheckBluetoothEnabledActivity.RESULT_CANCEL);
            finish();
        }

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBtAdapter == null) {
            Log.e(TAG, "No bluetooth default adapter");
            setResult(CheckBluetoothEnabledActivity.RESULT_FAILURE);
            finish();
        }

        if (mBtAdapter.isEnabled()) {
            setResult(CheckBluetoothEnabledActivity.RESULT_SUCCESS);
            finish();
        }
        
        Button yes = (Button) findViewById(R.id.blue_yes);
        yes.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mBtAdapter.enable();
                setResult(CheckBluetoothEnabledActivity.RESULT_SUCCESS);
                finish();
            }
        });
        
        Button cancel = (Button) findViewById(R.id.blue_cancel);
        cancel.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                setResult(CheckBluetoothEnabledActivity.RESULT_CANCEL);
                finish();
            }
        });

        // When in doubt, show the "web only" button
        mShowWebControl = true;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mShowWebControl = extras.getBoolean(RobotSelectionActivity.EXTRA_SHOW_WEBONLY_BUTTON,
                    true);
        }

        Button webOnly = (Button) findViewById(R.id.blue_cloud);
        if (mShowWebControl) {
            webOnly.setVisibility(View.VISIBLE);
            webOnly.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    setResult(CheckBluetoothEnabledActivity.RESULT_WEB_ONLY);
                    finish();
                }
            });
        } else {
            webOnly.setVisibility(View.INVISIBLE);
        }
    }
    
    /**
     * Check whether Bluetooth is enabled or not. Can be called from
     * anyone, anywhere.
     * 
     * @return True, bluetooth is enabled. False otherwise.
     */
    public static final boolean bluetoothIsEnabled() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter == null) {
            Log.e(TAG, "bluetoothIsEnabled: No bluetooth default adapter");
            return false;
        }

        return btAdapter.isEnabled();
    }
}
