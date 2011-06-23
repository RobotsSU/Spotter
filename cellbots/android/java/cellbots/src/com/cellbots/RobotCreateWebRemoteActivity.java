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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * Allow a user to edit or create a robot profile for use as a remote (over the
 * web) access.
 *
 * @author css@google.com (Charles Spirakis)
 */
public class RobotCreateWebRemoteActivity extends RobotBaseCreate {

    private static final String TAG = "CellbotRobotRemoteCreate";

    private Spinner mCommSpinner;

    private EditText mFieldName;

    private EditText mFieldAgentId;

    private EditText mFieldGmail;

    private EditText mFieldUrl;
    
    private int editIndex;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.robot_web_remote);
        final Activity self = this;

        // Setup the activity screen fields.
        mFieldName = (EditText) findViewById(R.id.remote_name);
        mFieldAgentId = (EditText) findViewById(R.id.remote_agentId);
        mFieldGmail = (EditText) findViewById(R.id.remote_gmail);
        mFieldUrl = (EditText) findViewById(R.id.remote_url);

        mCommSpinner = (Spinner) findViewById(R.id.remote_commSelect);
        setupCommSpinner(mCommSpinner);

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
            }
        } else {
            // No name means treat as a create.
            Log.d(TAG, "empty name passed in");
            mFieldName.setText("");
        }

        // Depending on the communication method, swap out the various sections.
        final LinearLayout appEngineSection = (LinearLayout) findViewById(R.id.remote_appEngine);
        final LinearLayout xmppSection = (LinearLayout) findViewById(R.id.remote_xmpp);
        final LinearLayout httpSection = (LinearLayout) findViewById(R.id.remote_http);
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

        // Finally, setup the buttons
        setupCancelButton(self, R.id.remote_cancel);
        setupQrScanButton(self, R.id.remote_scan_qrcode);

        Button saveButton = (Button) findViewById(R.id.remote_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Saving brain profile");

                String name = getStringFromField(mFieldName.getText());
                if (name.length() <= 0) {
                    // No name, so tell them and let them try again.
                    Toast
                            .makeText(getApplicationContext(), "Robot needs a name",
                                    Toast.LENGTH_LONG).show();
                    return;
                }

                String gmail = getStringFromField(mFieldGmail.getText());
                String url = getStringFromField(mFieldUrl.getText());
                String agent = getStringFromField(mFieldAgentId.getText());

                int which = mCommSpinner.getSelectedItemPosition();
                switch (which) {
                    case 0:
                        url = "";
                        agent = "";
                        break;
                    case 1:
                        gmail = "";
                        agent = "";
                        break;
                    default:
                        Log.e(TAG, "Unexpected comm spinner value: " + which);
                        agent = "";
                        gmail = "";
                }

                RobotEntry entry = RobotEntry.newRemoteRobotEntry(name, gmail, url, agent);

                if (mIsEdit) {
                    if (!mRobotProfiles.replaceEntry(editIndex, entry)) {
                        new AlertDialog.Builder(RobotCreateWebRemoteActivity.this).setTitle("Error")
                                .setMessage(R.string.edit_failed_msg)
                                .setPositiveButton(android.R.string.ok, null)
                                .create().show();
                        return;
                    }
                } else {
                    // Save the new profile in the robot list
                    if (!mRobotProfiles.addEntry(entry)) {
                        new AlertDialog.Builder(RobotCreateWebRemoteActivity.this).setTitle("Error")
                                .setMessage(R.string.add_failed_msg)
                                .setPositiveButton(android.R.string.ok, null)
                                .create().show();
                        return;
                    }
                }
                mRobotProfiles.save();

                // If this was a create, then we want to let others
                // drive the robot now.
                if (!mIsEdit) {
                    Intent i = new Intent();
                    i.setClass(self, com.cellbots.remote.CellbotRCActivity.class);
                    i.putExtra(RobotSelectionActivity.EXTRA_NAME, entry.getName());
                    startActivity(i);
                }

                // Done with this activity. We're either letting others drive
                // the robot or we're heading back to the list of robots.
                self.setResult(RobotSelectionActivity.RESULT_SAVE);
                self.finish();
            }
        });
    }

    private void fillFormFromEntry(RobotEntry entry) {
        mFieldAgentId.setText(entry.getAgentId());
        mFieldGmail.setText(entry.getGmail());
        mFieldUrl.setText(entry.getUrl());

        // Pick the comm method based on which fields are filled
        // in. To avoid confusion, clear out the fields that
        // aren't expected to have any values.
        /*if (mFieldAgentId.length() > 0) {
            mCommSpinner.setSelection(0);
            mFieldGmail.setText("");
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
        }
    }

    protected void robotScanResult(RobotEntry scannedEntry) {
        fillFormFromEntry(scannedEntry);
    }
}
