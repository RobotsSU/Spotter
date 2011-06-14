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

package com.cellbots.directcontrol;

import com.cellbots.LauncherActivity;
import com.cellbots.R;
import com.cellbots.RobotEntry;
import com.cellbots.RobotList;
import com.cellbots.RobotSelectionActivity;
import com.cellbots.local.CellDroidManager;
import com.cellbots.local.robotcontrollerservice.AbstractRobotControllerService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This Activity binds to the CellDroid service with a specified username and
 * password.
 *
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 * @author keertip@google.com (Keerti Parthasarathy)
 */
public class CellDroidActivity extends Activity {
    
    private static final String TAG = "DirectCellDroidActivity";

    protected RobotList mRobotProfiles;

    private SharedPreferences mPrefs;

    private TextView mNameTextView;

    private TextView mTypeTextView;

    private TextView mBtTextView;

    private String mRobotName;
    
    private String mRobotType;

    private String mRobotBtName;
    
    private String mRobotBtAddr;

    private Button connectButton, disconnectButton, petButton;

    private CellDroidManager celldroid;

    private ProgressDialog progressDialog;

    private Runnable directControlStarter = new Runnable() {
        public void run() {
            progressDialog.hide();
            Intent i = new Intent();
            i.setClass(CellDroidActivity.this, CellbotDirectControlActivity.class);
            startActivity(i);
            connectButton.setEnabled(false);
            petButton.setEnabled(false);
            disconnectButton.setEnabled(true);
        }
    };
    
    private Runnable bluetoothFailDialogRunnable = new Runnable() {
        @Override
        public void run() {
            progressDialog.hide();
            new AlertDialog.Builder(CellDroidActivity.this).setTitle("Bluetooth Failure")
                    .setMessage(R.string.bt_connection_fail_msg)
                    .setPositiveButton(android.R.string.ok, null).create().show();
        }
    };

    private Runnable readyChecker = new Runnable() {
        @Override
        public void run() {
            int state = AbstractRobotControllerService.STATE_NONE;
            while (true) {
                state = celldroid.getState();
                if (state != AbstractRobotControllerService.STATE_NONE &&
                    state != AbstractRobotControllerService.STATE_STARTING) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (state == AbstractRobotControllerService.STATE_SUCCESS) {
                runOnUiThread(directControlStarter);
            } else if (state == AbstractRobotControllerService.STATE_BLUETOOTH_FAIL) {
                runOnUiThread(bluetoothFailDialogRunnable);
            }
        }
    };

    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.directcontrol_main);

        mNameTextView = (TextView) findViewById(R.id.cellbotname_text);
        mTypeTextView = (TextView) findViewById(R.id.cellbottype_text);
        mBtTextView = (TextView) findViewById(R.id.bluetooth_text);

        Intent startingIntent = getIntent();
        
        if (startingIntent != null) {
            mRobotName = startingIntent.getStringExtra(RobotSelectionActivity.EXTRA_NAME);
            if ((mRobotName == null) || (mRobotName.length() < 1)) {
                Log.e(TAG, "Cellbot name should be non-null.");
                Toast.makeText(getApplicationContext(),
                        "Error starting up local cellbot control - no name",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            Log.e(TAG, "Need extras for cellbot name");
            Toast.makeText(getApplicationContext(),
                    "Error starting up local cellbot control - no extras",
                    Toast.LENGTH_LONG).show();
            finish();
        }

        mPrefs = getSharedPreferences(LauncherActivity.PREFERENCES_NAME, MODE_PRIVATE);
        mRobotProfiles = new RobotList(mPrefs);
        mRobotProfiles.load();
        
        int index = mRobotProfiles.findIndexByName(mRobotName);
        if (index < 0) {
            Log.e(TAG, "Cellbot name not found.");
            Toast.makeText(getApplicationContext(),
                    "Error starting up local cellbot control - unknown name",
                    Toast.LENGTH_LONG).show();
            finish();
        }

        RobotEntry entry = mRobotProfiles.getEntry(index);

        mRobotType = entry.getType();
        mRobotBtName = entry.getBluetoothName();
        mRobotBtAddr = entry.getBluetoothAddr();

        String btNamePair = entry.createNameMacPair();
        
        if (mRobotType == null) {
            Log.e(TAG, "Bad cellbot type for: " + mRobotName);
            Toast.makeText(getApplicationContext(),
                    "Error starting up local cellbot control - bad type",
                    Toast.LENGTH_LONG).show();
            finish();
        }
        if (btNamePair == null) {
            Log.e(TAG, "Bad cellbot bluetooth for: " + mRobotName);
            Toast.makeText(getApplicationContext(),
                    "Error starting up local cellbot control - bad bluetooth",
                    Toast.LENGTH_LONG).show();
            finish();
        }

        mNameTextView.setText(mRobotName);
        mTypeTextView.setText(mRobotType);
        mBtTextView.setText(btNamePair);

        celldroid = new CellDroidManager(this, null);
        final Context self = this;
        connectButton = (Button) this.findViewById(R.id.connect_button);
        connectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                progressDialog = new ProgressDialog(CellDroidActivity.this);
                progressDialog.setMessage("Connecting...");
                progressDialog.show();
                new Thread(readyChecker).start();
                celldroid.setController(mRobotType, mRobotBtName, mRobotBtAddr);
            }
        });
        connectButton.setEnabled(true);

        disconnectButton = (Button) this.findViewById(R.id.disconnect_button);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                celldroid.disconnect();
                connectButton.setEnabled(true);
                petButton.setEnabled(true);
                disconnectButton.setEnabled(false);
            }
        });
        disconnectButton.setEnabled(false);

        petButton = (Button) this.findViewById(R.id.pet_button);
        petButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                progressDialog = new ProgressDialog(CellDroidActivity.this);
                progressDialog.setMessage("Connecting...");
                progressDialog.show();
                new Thread(readyChecker).start();

                celldroid.setController(mRobotType, mRobotBtName, mRobotBtAddr);
                Intent i = new Intent();
                i.setClass(self, CellbotPetActivity.class);
                startActivity(i);
                connectButton.setEnabled(false);
                petButton.setEnabled(false);
                disconnectButton.setEnabled(true);
            }
        });
        petButton.setEnabled(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        celldroid.stopCellDroidService();
    }
}
