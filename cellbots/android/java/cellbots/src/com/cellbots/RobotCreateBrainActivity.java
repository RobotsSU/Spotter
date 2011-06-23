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
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

import com.cellbots.httpserver.HttpCommandServerServiceManager;

/**
 * Allow a user to edit or create a robot profile for use as a local or "brain"
 * for a cell bot.
 *
 * @author css@google.com (Charles Spirakis)
 */
public class RobotCreateBrainActivity extends RobotBaseCreate {
    private static final String TAG = "CellbotRobotBrainCreate";

    private static final int INTENT_BLUETOOTH_DEVICES = 1;
    
    private Spinner mCommSpinner;

    private EditText mFieldName;

    private Spinner mFieldType;

    private Spinner mFieldBluetooth;

    private EditText mFieldAgentId;

    private EditText mFieldGmail;

    private EditText mFieldPasswd;

    private EditText mFieldUrl;

    private CheckBox mFieldWifiCb;
    
    private CheckBox mFieldIsLocalHttpCb;
    
    private int editIndex = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.robot_share_others);
        final Activity self = this;

        // Setup the activity screen fields.
        mFieldName = (EditText) findViewById(R.id.share_name);
        mFieldType = (Spinner) findViewById(R.id.share_type);
        mFieldBluetooth = (Spinner) findViewById(R.id.share_bluetooth);
        mFieldAgentId = (EditText) findViewById(R.id.share_agentId);
        mFieldGmail = (EditText) findViewById(R.id.share_gmail);
        mFieldPasswd = (EditText) findViewById(R.id.share_passwd);
        mFieldUrl = (EditText) findViewById(R.id.share_url);
        mFieldIsLocalHttpCb = (CheckBox) findViewById(R.id.share_isLocalHttp);
        mFieldWifiCb = (CheckBox) findViewById(R.id.share_wifi_loc);

        mCommSpinner = (Spinner) findViewById(R.id.share_commSelect);
        setupCommSpinner(mCommSpinner);

        setupRobotTypeList(this, mFieldType);
        setupBluetoothPairList(mFieldBluetooth);
        
        if (mName.length() > 0) {
            if (mIsEdit) {
                Log.d(TAG, "adding edit information");

                mFieldName.setText(mName);

                // Find the name that matches and fill in the rest of the
                // information based on the robot profile that already
                // exists.
                editIndex = mRobotProfiles.findIndexByName(mName);
                if (editIndex >= 0) {
                    RobotEntry entry = mRobotProfiles.getEntry(editIndex);
                    fillFormFromEntry(entry);
                } else {
                    Log.e(TAG, "Edit but no profile: " + mName);
                }
            } else {
                // If this is a create, prefill with the fields
                // passed into us.
                Log.d(TAG, "adding create information");

                mFieldName.setText(mName);

                if (mExtras != null) {
                    String initialType = mExtras.getString(RobotSelectionActivity.EXTRA_TYPE);
                    String initialBluetooth = mExtras.getString(RobotSelectionActivity.EXTRA_BT);
                    for (int i = 0; i < mCellbotTypes.length; i++) {
                        if (initialType.equals(mCellbotTypes[i])) {
                            mFieldType.setSelection(i);
                        }
                    }
                    for (int i = 0; i < mCellbotBts.length; i++) {
                        if (initialBluetooth.equals(mCellbotBts[i])) {
                            mFieldBluetooth.setSelection(i);
                        }
                    }
                }
            }
        } else {
            Log.e(TAG, "Bad name passed to edit");
        }

        // Depending on the communication method, swap out the various sections.
        final LinearLayout appEngineSection = (LinearLayout) findViewById(R.id.share_appEngine);
        final LinearLayout xmppSection = (LinearLayout) findViewById(R.id.share_xmpp);
        final LinearLayout httpSection = (LinearLayout) findViewById(R.id.share_http);
        mCommSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long id) {
                switch (pos) {
                    /*case 0:
                        appEngineSection.setVisibility(View.VISIBLE);
                        xmppSection.setVisibility(View.GONE);
                        httpSection.setVisibility(View.GONE);
                        break;*/
                    case 0:
                        appEngineSection.setVisibility(View.GONE);
                        xmppSection.setVisibility(View.VISIBLE);
                        httpSection.setVisibility(View.GONE);
                        break;
                    case 1:
                        appEngineSection.setVisibility(View.GONE);
                        xmppSection.setVisibility(View.GONE);
                        httpSection.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        appEngineSection.setVisibility(View.VISIBLE);
        
        // Now setup the buttons.
        setupCancelButton(self, R.id.share_cancel);
        
        Button saveButton = (Button) findViewById(R.id.share_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Saving brain profile");

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
                String gmail = getStringFromField(mFieldGmail.getText());
                String passwd = getStringFromField(mFieldPasswd.getText());
                String url = getStringFromField(mFieldUrl.getText());
                String agent = getStringFromField(mFieldAgentId.getText());
                boolean useLocalServer = mFieldIsLocalHttpCb.isChecked();
                boolean wifi = mFieldWifiCb.isChecked();

                int which = mCommSpinner.getSelectedItemPosition();
                switch (which) {
                    // TODO(chaitanyag): This was for App engine. Fix it so that
                    // the comm method selection is not dependent on the index.
                    /*case 0:
                        gmail = "";
                        passwd = "";
                        url = "";
                        break;*/
                    case 0:
                        agent = "";
                        url = "";
                        break;
                    case 1:
                        agent = "";
                        gmail = "";
                        passwd = "";
                        break;
                    default:
                        Log.e(TAG, "Unexpected comm spinner value: " + which);
                        // When in doubt, default to agent id.
                        gmail = "";
                        passwd = "";
                        url = "";
                }

                RobotEntry entry = RobotEntry.newLocalRobotEntry(name, gmail, passwd, url,
                        useLocalServer, agent, type, blueInfo[0], wifi, blueInfo[1]);
                
                if (mIsEdit) {
                    if (!mRobotProfiles.replaceEntry(editIndex, entry)) {
                        new AlertDialog.Builder(RobotCreateBrainActivity.this).setTitle("Error")
                                .setMessage(R.string.edit_failed_msg)
                                .setPositiveButton(android.R.string.ok, null)
                                .create().show();
                        return;
                    }
                } else {
                    // Save the new profile in the robot list
                    if (!mRobotProfiles.addEntry(entry)) {
                        new AlertDialog.Builder(RobotCreateBrainActivity.this).setTitle("Error")
                                .setMessage(R.string.add_failed_msg)
                                .setPositiveButton(android.R.string.ok, null)
                                .create().show();
                        return;
                    }
                }
                mRobotProfiles.save();

                // If this was a create, turn this into the brain now.
                if (!mIsEdit) {
                    Intent i = new Intent();
                    i.setClass(self, com.cellbots.local.CellDroidActivity.class);
                    i.putExtra(RobotSelectionActivity.EXTRA_NAME, entry.getName());
                    startActivity(i);
                }

                // Done with this activity. We're either letting others drive
                // the robot or we're heading back to the list of robots.
                self.setResult(RobotSelectionActivity.RESULT_SAVE);
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
        
        mFieldIsLocalHttpCb.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mFieldIsLocalHttpCb.isChecked()) {
                    mFieldUrl.setText("http://" +
                            HttpCommandServerServiceManager.getLocalIpAddress() + ":" +
                            HttpCommandServerServiceManager.PORT + "/");
                } else {
                    mFieldUrl.setText("");
                }
            }
        });
        
        if (!mIsEdit) {
            chooseDevice.setVisibility(View.INVISIBLE);
            ViewGroup.LayoutParams params = chooseDevice.getLayoutParams();
            params.height = 0;
            chooseDevice.setLayoutParams(params);
            mFieldType.setClickable(false);
            mFieldBluetooth.setClickable(false);
        }
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

        mFieldAgentId.setText(entry.getAgentId());
        mFieldGmail.setText(entry.getGmail());
        mFieldPasswd.setText(entry.getPasswd());
        mFieldUrl.setText(entry.getUrl());

        // Pick the comm method based on which fields are filled
        // in. To avoid confusion, clear out the fields that
        // aren't expected to have any values.
        /*if (mFieldAgentId.length() > 0) {
            mCommSpinner.setSelection(0);
            mFieldGmail.setText("");
            mFieldPasswd.setText("");
            mFieldUrl.setText("");
        }*/
        if (mFieldGmail.length() > 0) {
            mCommSpinner.setSelection(0);
            mFieldAgentId.setText("");
            mFieldUrl.setText("");
        } else {
            mCommSpinner.setSelection(1);
            mFieldAgentId.setText("");
            mFieldGmail.setText("");
            mFieldPasswd.setText("");
        }
        mFieldIsLocalHttpCb.setChecked(entry.getUseLocalHttpServer());
        mFieldWifiCb.setChecked(entry.getEnableWiFi());
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
