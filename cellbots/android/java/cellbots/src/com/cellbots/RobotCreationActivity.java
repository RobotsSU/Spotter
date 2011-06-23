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
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * Allow a user to edit or create a robot profile. The profile contains
 * information like the name, method of connection, etc.
 *
 * @see RobotEntry.java
 *@author css@google.com (Charles Spirakis)
 */
public class RobotCreationActivity extends RobotBaseCreate {
    private static final String TAG = "CellbotRobotCreation";

    private static final int INTENT_BLUETOOTH_DEVICES = 1;

    private EditText mFieldName;

    private Spinner mFieldType;

    private Spinner mFieldBluetooth;
    
    private LinearLayout mTopChoiceLayout;
    
    private LinearLayout mCreateProfileLayout;
    
    private int editIndex;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.robot_creation);
        final Activity self = this;

        mFieldName = (EditText) findViewById(R.id.create_name);
        mFieldType = (Spinner) findViewById(R.id.create_type);
        mFieldBluetooth = (Spinner) findViewById(R.id.create_bluetooth);
        mTopChoiceLayout = (LinearLayout) findViewById(R.id.top_choices_view);
        mCreateProfileLayout = (LinearLayout) findViewById(R.id.add_cellbot_profile_view);

        setupRobotTypeList(this, mFieldType);
        setupBluetoothPairList(mFieldBluetooth);

        if (mIsEdit) {
            if (mName.length() < 1) {
                Log.e(TAG, "Bad name passed to creation");
                // treat as a create
            } else {
                // If this is an edit, pre-fill in fields.
                Log.d(TAG, "adding edit information");

                // Always fill in the name if we have one.
                mFieldName.setText(mName);

                // Find the name that matches
                editIndex = mRobotProfiles.findIndexByName(mName);
                if (editIndex >= 0) {
                    RobotEntry entry = mRobotProfiles.getEntry(editIndex);
                    fillFormFromEntry(entry);
                }
            }
        }

        // Finally, setup the buttons
        setupCancelButton(self, R.id.create_cancel);
        setupQrScanButton(self, R.id.create_scan_qrcode);

        Button saveButton = (Button) findViewById(R.id.create_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Saving");

                String name = getStringFromField(mFieldName.getText());
                if (name.length() <= 0) {
                    // No name, so tell them and let them try again.
                    Toast.makeText(getApplicationContext(),
                            "Robot needs a name",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                String type = getStringFromField(mFieldType.getSelectedItem());
                String bluetoothPair = getStringFromField(mFieldBluetooth.getSelectedItem());
                
                String[] blueInfo = RobotEntry.splitNameMacPair(bluetoothPair); 

                RobotEntry entry = RobotEntry.newDirectRobotEntry(
                        name, type, blueInfo[0], blueInfo[1]);

                if (mIsEdit) {
                  if (!mRobotProfiles.replaceEntry(editIndex, entry)) {
                      new AlertDialog.Builder(RobotCreationActivity.this).setTitle("Error")
                              .setMessage(R.string.edit_failed_msg)
                              .setPositiveButton(android.R.string.ok, null)
                              .create().show();
                      return;
                  }
                } else {
                    // Save the new profile in the robot list
                    if (!mRobotProfiles.addEntry(entry)) {
                        new AlertDialog.Builder(RobotCreationActivity.this).setTitle("Error")
                                .setMessage(R.string.add_failed_msg)
                                .setPositiveButton(android.R.string.ok, null)
                                .create().show();
                        return;
                    }
                }
                mRobotProfiles.save();

                // If this was a create, then we want to start
                // driving the robot now.
                if (!mIsEdit) {
                    Intent i = new Intent();
                    i.setClass(self, com.cellbots.directcontrol.CellDroidActivity.class);
                    i.putExtra(RobotSelectionActivity.EXTRA_NAME, entry.getName());
                    startActivity(i);
                }

                // Done with this activity. We're either driving a robot at this
                // point or we're heading back to the list of robots.
                self.setResult(RobotSelectionActivity.RESULT_SAVE);
                self.finish();
            }
        });

        Button skipToCloudButton = (Button) findViewById(R.id.create_goto_cloud);
        skipToCloudButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Cloud Remote");
                // if we are heading off to web remote land, we are done here.
                self.setResult(RobotSelectionActivity.RESULT_START_WEB_CREATE);
                self.finish();
            }
        });

        Button chooseDevice = (Button) findViewById(R.id.check_devices);
        chooseDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Device");
                Intent i = new Intent();
                i.setClass(self, com.cellbots.FindBluetoothDevicesActivity.class);
                startActivityForResult(i, INTENT_BLUETOOTH_DEVICES);
            }
        });
        
        Button createCellbotProfile = (Button) findViewById(R.id.create_cellbot);
        createCellbotProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              showCreateProfileLayout();
            }
        });
        
        if (mIsEdit) {
          showCreateProfileLayout();
        }
    }

    private void showCreateProfileLayout() {
      mCreateProfileLayout.setVisibility(View.VISIBLE);
      mCreateProfileLayout.setLayoutParams(new LinearLayout.LayoutParams(
              LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
      mTopChoiceLayout.setVisibility(View.INVISIBLE);
      mTopChoiceLayout.setLayoutParams(new LinearLayout.LayoutParams(
              LayoutParams.FILL_PARENT, 0));  
    }
    
    private void fillFormFromEntry(RobotEntry entry) {
        for (int i = 0; i < mCellbotTypes.length; i++) {
            if (entry.getType().equals(mCellbotTypes[i])) {
                mFieldType.setSelection(i);
            }
        }
        for (int i = 0; i < mCellbotBts.length; i++) {
            if (entry.createNameMacPair().equals(mCellbotBts[i])) {
                mFieldBluetooth.setSelection(i);
            }
        }
    }

    protected void robotScanResult(RobotEntry scannedEntry) {
        fillFormFromEntry(scannedEntry);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == INTENT_BLUETOOTH_DEVICES) {
            if (resultCode == FindBluetoothDevicesActivity.RESULT_OK) {
                // User may have changed the pairings, so rescan the available
                // bluetooth devices.
                setupBluetoothPairList(mFieldBluetooth);
                Bundle extras = data.getExtras();
                if (extras != null) {
                    String btName = extras.getString(
                            FindBluetoothDevicesActivity.EXTRA_BLUETOOTH_NAME);
                    String btMac = extras.getString(
                        FindBluetoothDevicesActivity.EXTRA_BLUETOOTH_MAC);
                    if ((btName != null) && (btName.length() > 0)) {
                        String pairName = RobotEntry.generateNameMacPair(btName, btMac);
                        for (int i = 0; i < mCellbotBts.length; i++) {
                            if (pairName.equals(mCellbotBts[i])) {
                                mFieldBluetooth.setSelection(i);
                            }
                        }
                    }
                }
            }
        } else {
            Log.e(TAG, "unexpected activity requestCode: " + requestCode);
        }
    }
}
