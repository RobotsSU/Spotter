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

import com.cellbots.LauncherActivity;
import com.cellbots.R;
import com.cellbots.RobotEntry;
import com.cellbots.RobotList;
import com.cellbots.RobotSelectionActivity;
import com.cellbots.httpserver.HttpCommandServerServiceManager;
import com.cellbots.httpserver.HttpCommandServerServiceManager.HttpRequestListener;
import com.cellbots.local.CellDroidManager.CellDroidListener;
import com.cellbots.local.robotcontrollerservice.AbstractRobotControllerService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
 */
public class CellDroidActivity extends Activity implements CellDroidListener {

    private static final String TAG = "Brain.CellDroidActivity";

    private TextView mRobotIdTextView, mTypeTextView, mBtTextView, mGmailTextView, mUrlTextView;

    private SharedPreferences mPrefs;

    private RobotList mRobotProfiles;

    private RobotEntry mEntry;

    private String mRobotName;

    private String mRemoteEyesUrl;

    private Button connectButton, disconnectButton, displayQrButton;

    private CellDroidManager celldroid;

    private ProgressDialog progressDialog;

    private EyesView mEyesView;

    private HttpCommandServerServiceManager localHttpManager = null;

    private Runnable bluetoothFailDialogRunnable = new Runnable() {
        @Override
        public void run() {
            progressDialog.hide();
            AlertDialog.Builder builder = new AlertDialog.Builder(CellDroidActivity.this);
            builder.setTitle("Bluetooth Failure");
            builder.setMessage(getResources().getString(R.string.bt_connection_fail_msg));
            builder.setPositiveButton("Ok", null);
            builder.create().show();
        }
    };

    private Runnable dialogHider = new Runnable() {
        public void run() {
            progressDialog.hide();
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
        }
    };

    private Runnable readyChecker = new Runnable() {
        @Override
        public void run() {
            int state = AbstractRobotControllerService.STATE_NONE;
            while (true) {
                state = celldroid.getState();
                if (state != AbstractRobotControllerService.STATE_NONE
                        && state != AbstractRobotControllerService.STATE_STARTING) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (state == AbstractRobotControllerService.STATE_SUCCESS) {
                runOnUiThread(dialogHider);
            } else if (state == AbstractRobotControllerService.STATE_BLUETOOTH_FAIL) {
                runOnUiThread(bluetoothFailDialogRunnable);
            }
        }
    };

    private HttpRequestListener httpRequestListener = new HttpRequestListener() {

        @Override
        public void onRequest(String req, String[] keys, String[] values, byte[] data) {
            String type = "text/text";
            if (req.equals("image.jpg"))
                type = "image/jpg";
            else if (req.equals("command.php")) {
                String message = new String(data);
                message = CellDroid.stripTimestampIfAny(message);
                if (!processCommand(message) && !celldroid.processCommand(message)) {
                    CellDroidManager.sendDirectCommand(message);
                }
            }
            localHttpManager.setResponseByName(req, data, type);
        }

        @Override
        public void onConnected() {
            localHttpManager.setResponseByName("command.php", "s".getBytes(), "text/text");
        }
    };

    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.local_main);

        mGmailTextView = (TextView) findViewById(R.id.username_text);
        mUrlTextView = (TextView) findViewById(R.id.url_text);
        mRobotIdTextView = (TextView) findViewById(R.id.robotid_text);
        mTypeTextView = (TextView) findViewById(R.id.cellbottype_text);
        mBtTextView = (TextView) findViewById(R.id.bluetooth_text);

        // By the time this activity is started, the robot entry has already
        // been created and saved away. Thus, only need the robot name to grab
        // be able to get all the robot information.
        Intent startingIntent = getIntent();
        if (startingIntent != null) {
            mRobotName = startingIntent.getStringExtra(RobotSelectionActivity.EXTRA_NAME);
            if ((mRobotName == null) || (mRobotName.length() < 1)) {
                Log.e(TAG, "Cellbot name should be non-null.");
                Toast
                        .makeText(getApplicationContext(),
                                "Error starting up local cellbot control - no name",
                                Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            Log.e(TAG, "Need extras for cellbot name");
            Toast
                    .makeText(getApplicationContext(),
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
            Toast
                    .makeText(getApplicationContext(),
                            "Error starting up local cellbot control - unknown name",
                            Toast.LENGTH_LONG).show();
            finish();
        }

        mEntry = mRobotProfiles.getEntry(index);

        String url = mEntry.getUrl();
        if (!url.equals("") && !url.endsWith("/")) {
            url += "/";
        }
        boolean useLocal = mEntry.getUseLocalHttpServer();
        if (useLocal) {
            url = "http://" + HttpCommandServerServiceManager.LOCAL_IP + ":"
                    + HttpCommandServerServiceManager.PORT + "/";
            mRemoteEyesUrl = url + "image.jpg";
        } else {
            mRemoteEyesUrl = url + "put.php";
        }

        mGmailTextView.setText(mEntry.getGmail());
        mUrlTextView.setText(mEntry.getUrl());
        mRobotIdTextView.setText(mEntry.getName());
        mTypeTextView.setText(mEntry.getType());
        mBtTextView.setText(mEntry.createNameMacPair());

        celldroid = new CellDroidManager(this, this);

        connectButton = (Button) this.findViewById(R.id.connect_button);
        connectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                progressDialog = new ProgressDialog(CellDroidActivity.this);
                progressDialog.setMessage("Connecting...");
                progressDialog.show();
                new Thread(readyChecker).start();

                if (mEntry.getUseLocalHttpServer()) {
                    localHttpManager = new HttpCommandServerServiceManager(
                            CellDroidActivity.this, httpRequestListener);
                }

                // TODO: Make this work with app engine (agent ID)
                celldroid.connect(mEntry.getAgentId(), mEntry.getGmail(), mEntry.getPasswd(),
                        mEntry.getUrl(), mEntry.getUseLocalHttpServer());

                celldroid.setController(
                        mEntry.getType(), mEntry.getBluetoothName(), mEntry.getBluetoothAddr());
            }
        });
        connectButton.setEnabled(true);

        disconnectButton = (Button) this.findViewById(R.id.disconnect_button);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                celldroid.disconnect();
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
            }
        });
        disconnectButton.setEnabled(false);

        displayQrButton = (Button) this.findViewById(R.id.display_qr_button);
        displayQrButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // Send a new robot entry to generate QR code so
                // that only the minimal necessary info is
                // included in the QR code. 
                RobotSelectionActivity.generateQrUri(CellDroidActivity.this,
                        RobotEntry.newRemoteRobotEntry(mEntry.getName(), mEntry.getGmail(),
                                mEntry.getUrl(), mEntry.getAgentId()));
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        if (mEyesView != null)
            mEyesView.hide();
        celldroid.stopCellDroidService();
        if (localHttpManager != null) {
            localHttpManager.disconnect();
        }
    }

    public void setRemoteEyesImage(byte[] data) {
        localHttpManager.setResponseByName("image.jpg", data, "image/jpg");
    }

    private boolean processCommand(String message) {
        // "persona on" shows the EyesView with the persona overlay but without
        // the video preview.
        if (message.equals("persona on")) {
            CellDroidActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mEyesView != null)
                        mEyesView.hide();
                    mEyesView = new EyesView(CellDroidActivity.this, null, false);
                }
            });
            return true;
        } else if (message.equals("video on")) {    // This shows EyesView with
                                                    // both video and personas
            CellDroidActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mEyesView != null)
                        mEyesView.hide();
                    mEyesView = new EyesView(CellDroidActivity.this, mRemoteEyesUrl, false);
                }
            });
            return true;
        } else if (message.equals("torch on")) {
            if (mEyesView != null) {
                mEyesView.setTorchMode(true);
            }
            return true;
        } else if (message.equals("torch off")) {
            if (mEyesView != null) {
                mEyesView.setTorchMode(false);
            }
            return true;
        } else if (message.equals("picture")) {
            if (mEyesView != null) {
                mEyesView.setTakePicture(true);
                //mTts.speak("Taking picture", 0, null);
            }
            return true;
        } else if (message.startsWith("persona")) {
            String[] tokens = message.split(" ");
            String personaExpression = tokens[1].trim();
            int personaId = EyesView.PERSONA_READY;
            if (personaExpression.equalsIgnoreCase("afraid")) {
                personaId = EyesView.PERSONA_AFRAID;
            } else if (personaExpression.equalsIgnoreCase("angry")) {
                personaId = EyesView.PERSONA_ANGRY;
            } else if (personaExpression.equalsIgnoreCase("error")) {
                personaId = EyesView.PERSONA_ERROR;
            } else if (personaExpression.equalsIgnoreCase("happy")) {
                personaId = EyesView.PERSONA_HAPPY;
            } else if (personaExpression.equalsIgnoreCase("idle")) {
                personaId = EyesView.PERSONA_IDLE;
            } else if (personaExpression.equalsIgnoreCase("sad")) {
                personaId = EyesView.PERSONA_SAD;
            } else if (personaExpression.equalsIgnoreCase("surprise")) {
                personaId = EyesView.PERSONA_SURPRISE;
            }
            final int persona = personaId;
            CellDroidActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mEyesView != null) {
                        mEyesView.setPersona(persona);
                    }
                }
            });
            //mTts.speak("Changing persona", 0, null);
            return true;
        }

        return false;
    }

    @Override
    public boolean onMessage(String message) {
        return processCommand(message);
    }
}
