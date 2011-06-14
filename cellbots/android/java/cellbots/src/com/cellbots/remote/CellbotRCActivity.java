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

package com.cellbots.remote;

import com.cellbots.LauncherActivity;
import com.cellbots.R;
import com.cellbots.RobotEntry;
import com.cellbots.RobotList;
import com.cellbots.RobotSelectionActivity;
import com.cellbots.communication.AbstractCommChannel.CommMessageListener;
import com.cellbots.communication.CommMessage;
import com.cellbots.communication.CommunicationManager;
import com.cellbots.local.CellDroidActivity;
import com.cellbots.remote.HeadPanControl.PanAction;
import com.cellbots.remote.UiView.UiEventListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

/**
 * A remote control for driving Cellbots. The remote control talks to the
 * cellbot's Android over xmpp using Google Talk accounts. The cellbot Android
 * is able to share a video stream using a known shared location with Remote
 * Eyes. XMPP code based on example from:
 * http://credentiality2.blogspot.com/2010
 * /03/xmpp-asmack-android-google-talk.html
 *
 * @author clchen@google.com (Charles L. Chen)
 * @author keertip@google.com (Keerti Parthasarathy)
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 */
public class CellbotRCActivity extends Activity implements UiEventListener {
    // Making this public so that others can use this tag in their
    // logging.
    public static final String TAG = "Cellbot Remote Control";

    private static final int PREFS_ID = Menu.FIRST;

    public static final int VOICE_RECO_CODE = 42;

    protected RobotList mRobotProfiles;

    private SharedPreferences mPrefs;

    private String mRobotName;

    private String CONTROLLER_ACCOUNT;

    private String CONTROLLER_PASS;

    private String mCellbotGmail;

    private String mCellbotUrl;

    private String mCellbotAgentId;

    private String REMOTE_EYES_IMAGE_URL;

    private int mCommChannel;
    
    private CommunicationManager mCommManager;

    public int state = 0;

    private ImageView remoteEyesImageView;

    private LinearLayout mControlsLayout;

    private UiView mUiView;

    private boolean mTorchOn = false;

    private Context mContext;

    private Activity mActivity;

    private ToastWithoutSpam mSafeToast;
    
    private ProgressDialog progressDialog;

    private CommMessageListener mCommMessageListener = new CommMessageListener() {
        @Override
        public void onConnected(String channelName, int channel) {
            CellbotRCActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.hide();
                }
            });
            if (channelName.equals("HTTP_IMAGE")) {
                mCommManager.listenForMessages(channelName, -1, true);
            }
            else if (channelName.equals("HTTP")) {
                sendCommand("video on");
            } else if(channelName.equals("XMPP")) {
                sendCommand("persona on");
            }
            
        }
        
        @Override
        public void onConnectError(final String channelName, int channel) {
            if (channelName.equals("HTTP_IMAGE"))
                return;
            CellbotRCActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  progressDialog.hide();
                  new AlertDialog.Builder(CellbotRCActivity.this).setTitle("Error")
                          .setMessage("Error connecting to the robot.")
                          .setPositiveButton(android.R.string.cancel,
                          new DialogInterface.OnClickListener() {
                              @Override
                              public void onClick(DialogInterface dialog, int which) {
                                  finish();
                              }
                          }).create().show();
                }
            });
        }

        @Override
        public void onDisconnected(String channelName, int channel) {
        }

        @Override
        public void onMessage(CommMessage msg) {
            if (msg.getChannelName().equals("HTTP_IMAGE")) {
                // we currently get only image data on this channel
                final Bitmap bmp = BitmapFactory.decodeStream(msg.getMessageInputStream());
                CellbotRCActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateRemoteEyesView(bmp);
                    }
                });
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mCommManager.listenForMessages("HTTP_IMAGE", -1, true);
            }
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
        mActivity = this;
        mSafeToast = new ToastWithoutSpam(mActivity, mContext, 10, 500);

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

        RobotEntry entry = mRobotProfiles.getEntry(index);

        /**
         *How the robot wants to communicate via the web is stored in the robot
         * entry. We use a hierarchical determination based on which field have
         * values and which don't. This hierarchy should match the one for
         * remote creation.
         *
         * @see RobotCreateWebRemoteActivity
         */
        mCellbotGmail = entry.getGmail();
        mCellbotUrl = entry.getUrl();
        mCellbotAgentId = entry.getAgentId();

        if (mCellbotAgentId.length() > 0) {
            mCommChannel = CommunicationManager.CHANNEL_GAE;
        } else if (mCellbotGmail.length() > 0) {
            mCommChannel = CommunicationManager.CHANNEL_XMPP;
        } else if (mCellbotUrl.length() > 0) {
            mCommChannel = CommunicationManager.CHANNEL_HTTP;
        } else {
            mCommChannel = CommunicationManager.CHANNEL_NONE;
            Toast.makeText(getApplicationContext(),
                    "Cellbot needs a communication method. Please edit: " + mRobotName,
                    Toast.LENGTH_LONG).show();
            finish();
        }
        
        mCommManager = new CommunicationManager();

        if (mCommChannel == CommunicationManager.CHANNEL_HTTP) {
            mCellbotUrl = mCellbotUrl.endsWith("/") ? mCellbotUrl : mCellbotUrl + "/";
            REMOTE_EYES_IMAGE_URL = mCellbotUrl + "image.jpg";
            mCommManager.addHttpChannel("HTTP", null, mCellbotUrl + "command.php",
                    mCommMessageListener);
            mCommManager.addHttpChannel("HTTP_IMAGE", REMOTE_EYES_IMAGE_URL, null,
                    mCommMessageListener);
        }


        if (mCommChannel == CommunicationManager.CHANNEL_XMPP) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            CONTROLLER_ACCOUNT = prefs.getString("CONTROLLER_ACCOUNT", "");
            CONTROLLER_PASS = prefs.getString("CONTROLLER_PASS", "");
            
            if (CONTROLLER_ACCOUNT.equals("") || CONTROLLER_PASS.equals("")) {
                new AlertDialog.Builder(this).setTitle("Error")
                        .setMessage("Please set GTalk username and password through"
                                + " Settings on this phone, in order to communicate with the "
                                    + "remote Cellbot.")
                        .setPositiveButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .create().show();
                return;
            } else {
                mCommManager.addXmppCommChannel("XMPP", CONTROLLER_ACCOUNT, CONTROLLER_PASS,
                        mCellbotGmail, mCommMessageListener);
            }
        }

        setContentView(R.layout.remote_main);

        remoteEyesImageView = (ImageView) findViewById(R.id.remoteEyesView);
        remoteEyesImageView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (!mTorchOn) {
                    sendCommand("torch on");
                    mTorchOn = true;
                } else {
                    sendCommand("torch off");
                    mTorchOn = false;
                }
            }
        });

        new HeadPanControl(remoteEyesImageView, panListener, true);

        // Only startup the appengine stuff if we are talking app engine.
        /*if (mCommChannel == CommunicationManager.CHANNEL_GAE) {
          mSendAppEngineCommandTask = new SendAppEngineCommandTask();
        }*/

        mControlsLayout = (LinearLayout) findViewById(R.id.controls_layout);
        Display disp = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        mUiView = new DpadView(this, this, true, disp.getWidth(), disp.getHeight());
        mControlsLayout.addView(mUiView);
        
        progressDialog = new ProgressDialog(CellbotRCActivity.this);
        progressDialog.setMessage("Connecting...");
        progressDialog.show();

        mCommManager.connectAll();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCommManager.disconnectAll();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    private boolean sendCommand(String command) {
        String channelName = "";
        if (mCommChannel == CommunicationManager.CHANNEL_XMPP) {
            channelName = "XMPP";
        } else if (mCommChannel == CommunicationManager.CHANNEL_HTTP) {
            channelName = "HTTP";
        } /*else if (mCommChannel == CommunicationManager.CHANNEL_GAE) {
            channelName = "APPENGINE";
        }*/ else {
            Log.e("CellbotRC", "Communication channel error sending command: " + command);
            mSafeToast.sendToast("Unable to send command");
            return false;
        }
        return mCommManager.sendMessage(channelName, command, "text/text");
    }

    private void updateRemoteEyesView(final Bitmap bmp) {
        runOnUiThread(new Runnable() {
            public void run() {
                Bitmap oldBmp = (Bitmap) remoteEyesImageView.getTag();
                remoteEyesImageView.setImageBitmap(bmp);
                remoteEyesImageView.setTag(bmp);
                if (oldBmp != null) {
                    oldBmp.recycle();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, PREFS_ID, 0, R.string.settings).setIcon(android.R.drawable.ic_menu_preferences);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case PREFS_ID:
                Intent intent = new Intent(this, com.cellbots.PrefsActivity.class);
                startActivity(intent);
                break;
            default:
                Log.e(TAG, "bad menu item number: " + item.getItemId());
        }

        return super.onOptionsItemSelected(item);
    }

    public void switchToAccelerometerControl() {
        mControlsLayout.removeAllViews();
        Display disp = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
        .getDefaultDisplay();
        mUiView = new AccelerometerView(this, this, true, disp.getWidth(), disp.getHeight());
        mControlsLayout.addView(mUiView);
    }

    public void switchToDpadControl() {
        mControlsLayout.removeAllViews();
        Display disp = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        mUiView = new DpadView(this, this, true, disp.getWidth(), disp.getHeight());
        mControlsLayout.addView(mUiView);
    }

    public void switchToVoiceControl() {
        mControlsLayout.removeAllViews();
        Display disp = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
        .getDefaultDisplay();
        mUiView = new VoiceView(this, this, true, disp.getWidth(), disp.getHeight());
        mControlsLayout.addView(mUiView);
    }

    public void switchToJoystickControl() {
        mControlsLayout.removeAllViews();
        Display disp = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
        .getDefaultDisplay();
        mUiView = new JoystickView(this, this, true, disp.getWidth(), disp.getHeight());
        mControlsLayout.addView(mUiView);
        // This looks weird, but if we don't do this, the joystick view will
        // not draw its initial state properly.
        this.openOptionsMenu();
        this.closeOptionsMenu();
    }

    private void showSpeechAlert(String input) {
        Builder speechAlertBuilder = new Builder(this);
        final EditText speechInput = new EditText(this);
        speechInput.setText(input);
        speechAlertBuilder.setView(speechInput);
        speechAlertBuilder.setPositiveButton("Speak!", new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                sendCommand("speak: " + speechInput.getText().toString());
            }
        });
        speechAlertBuilder.setNegativeButton("Cancel", new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
            }
        });
        speechAlertBuilder.show();
    }

    @Override
    public void onWheelVelocitySetRequested(float direction, float speed) {
        if (direction >= 0 && direction <= 90) {
            sendCommand("w " + (int) speed + " " + (int) (speed - direction / 90 * speed));
        } else if (direction > 90) {
            sendCommand(
                    "w -" + (int) speed + " -" + (int) (speed - (180 - direction) / 90 * speed));
        } else if (direction < 0 && direction >= -90) {
            sendCommand(
                    "w " + (int) (speed - Math.abs(direction) / 90 * speed) + " " + (int) speed);
        } else if (direction < -90) {
            sendCommand("w -" + (int) (speed - (180 - Math.abs(direction)) / 90 * speed) + " -"
                    + (int) speed);
        }
    }

    @Override
    public void onStopRequested() {
        sendCommand("s");
    }

    @Override
    public void onActionRequested(int action, String values) {
        switch (action) {
            case UiEventListener.ACTION_FORWARD:
                sendCommand("f");
                break;
            case UiEventListener.ACTION_BACKWARD:
                sendCommand("b");
                break;
            case UiEventListener.ACTION_LEFT:
                sendCommand("l");
                break;
            case UiEventListener.ACTION_RIGHT:
                sendCommand("r");
                break;
            case UiEventListener.ACTION_SPEAK:
                showSpeechAlert(values);
                break;
            case UiEventListener.ACTION_STRING:
                sendCommand(values);
                break;
            case UiEventListener.ACTION_TAKE_PICTURE:
                sendCommand("picture");
                CellbotRCActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(CellbotRCActivity.this, R.string.picture_taken_msg,
                                Toast.LENGTH_LONG).show();
                    }
                });
                break;
            case UiEventListener.ACTION_GET_LOCATION:
                sendCommand("location");
                // TODO (chaitanyag): Remove this after we add support for
                // reporting location.
                CellbotRCActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(CellbotRCActivity.this, R.string.no_geolocation_msg,
                                Toast.LENGTH_LONG).show();
                    }
                });
                break;
            case UiEventListener.ACTION_CHANGE_PERSONA:
                sendCommand("persona " + values);
                break;
        }

    }

    private HeadPanControl.HeadPanListener panListener = new HeadPanControl.HeadPanListener() {
        @Override
        public void onPan(PanAction action) {
            if (action == PanAction.PAN_CENTER)
                sendCommand("hc");
            else if (action == PanAction.PAN_LEFT)
                sendCommand("hl");
            else if (action == PanAction.PAN_RIGHT)
                sendCommand("hr");
            else if (action == PanAction.PAN_UP)
                sendCommand("hu");
            else if (action == PanAction.PAN_DOWN)
                sendCommand("hd");
        }
    };

    /*
     * (non-Javadoc)
     * @see
     * com.cellbots.remote.UiView.UiEventListener#onSwitchInterfaceRequested
     * (int)
     */
    @Override
    public void onSwitchInterfaceRequested(int interfaceId) {
        switch (interfaceId) {
            case UiEventListener.INTERFACE_ACCELEROMETER:
                switchToAccelerometerControl();
                break;
            case UiEventListener.INTERFACE_DPAD:
                switchToDpadControl();
                break;
            case UiEventListener.INTERFACE_JOYSTICK:
                switchToJoystickControl();
                break;
            case UiEventListener.INTERFACE_VOICE:
                switchToVoiceControl();
                break;
        }

    }

    @Override
    public void onPopupWindowRequested() {
        showPopupMenu();

    }

    PopupWindow pw;

    private void showPopupMenu() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View layout = inflater.inflate(
                R.layout.personas_popup, (ViewGroup) findViewById(R.id.personas_popup_menu));
        pw = new PopupWindow(layout, 130, 190, true);
        // set actions to buttons we have in our popup
        Button button1 = (Button) layout.findViewById(R.id.popup_happy);
        button1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View vv) {
                pw.dismiss();
                onActionRequested(UiEventListener.ACTION_CHANGE_PERSONA, "happy");
            }
        });
        Button button2 = (Button) layout.findViewById(R.id.popup_sad);
        button2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View vv) {
                pw.dismiss();
                onActionRequested(UiEventListener.ACTION_CHANGE_PERSONA, "sad");
            }
        });
        Button button3 = (Button) layout.findViewById(R.id.popup_suprised);
        button3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View vv) {
                pw.dismiss();
                onActionRequested(UiEventListener.ACTION_CHANGE_PERSONA, "suprise");
            }
        });
        Button button4 = (Button) layout.findViewById(R.id.popup_afraid);
        button4.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View vv) {
                pw.dismiss();
                onActionRequested(UiEventListener.ACTION_CHANGE_PERSONA, "afraid");
            }
        });
        Button button5 = (Button) layout.findViewById(R.id.popup_angry);
        button5.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View vv) {
                pw.dismiss();
                onActionRequested(UiEventListener.ACTION_CHANGE_PERSONA, "angry");

            }
        });
        Button button6 = (Button) layout.findViewById(R.id.popup_ready);
        button6.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View vv) {
                pw.dismiss();
                onActionRequested(UiEventListener.ACTION_CHANGE_PERSONA, "ready");

            }
        });
        Button button7 = (Button) layout.findViewById(R.id.popup_error);
        button7.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View vv) {
                pw.dismiss();
                onActionRequested(UiEventListener.ACTION_CHANGE_PERSONA, "error");

            }
        });
        Button button8 = (Button) layout.findViewById(R.id.popup_idle);
        button8.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View vv) {
                pw.dismiss();
                onActionRequested(UiEventListener.ACTION_CHANGE_PERSONA, "idle");

            }
        });

        pw.showAtLocation(layout, Gravity.BOTTOM, 0, 0);

    }
}
