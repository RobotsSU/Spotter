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

import com.cellbots.PrefsActivity;
import com.cellbots.R;
import com.cellbots.local.CellDroidManager;
import com.cellbots.remote.AccelerometerView;
import com.cellbots.remote.DpadView;
import com.cellbots.remote.HeadPanControl;
import com.cellbots.remote.HeadPanControl.PanAction;
import com.cellbots.remote.JoystickView;
import com.cellbots.remote.UiView;
import com.cellbots.remote.UiView.UiEventListener;
import com.cellbots.communication.UserTask;
import com.cellbots.remote.VoiceView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * A remote control for directly driving the Cellbots.
 *
 * @author clchen@google.com (Charles L. Chen)
 * @author keertip@google.com (Keerti Parthasarathy)
 */
public class CellbotDirectControlActivity extends Activity implements UiEventListener {
    // Making this public so that others can use this tag in their
    // logging.
    public static final String TAG = "Cellbot Direct Control";

    private static final int PREFS_ID = Menu.FIRST;

    private String REMOTE_EYES_IMAGE_URL = "http://personabots.appspot.com/personas/face/happy.png";

    private URL url;

    public static final int VOICE_RECO_CODE = 42;

    public int state = 0;

    private ImageView personasView;

    private LinearLayout mControlsLayout;

    private UiView mUiView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.directcontrol_ui_main);
        personasView = (ImageView) findViewById(R.id.personasView);
        personasView.setPadding(0, 30, 0, 0);
        personasView.setScaleType(ScaleType.FIT_START);
        mControlsLayout = (LinearLayout) findViewById(R.id.dc_controls_layout);
        Display disp = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        mUiView = new DpadView(this, this, false, disp.getWidth(), disp.getHeight());
        mControlsLayout.addView(mUiView);
        new HeadPanControl(personasView, panListener, true);
        new UpdateImageTask().execute();

    }

    private void updateRemoteEyesView(final Bitmap bmp) {
        runOnUiThread(new Runnable() {
            public void run() {
                Bitmap oldBmp = (Bitmap) personasView.getTag();
                personasView.setImageBitmap(bmp);
                personasView.setTag(bmp);
                if (oldBmp != null) {
                    oldBmp.recycle();
                }
            }
        });
    }

    private class UpdateImageTask extends UserTask<Void, Void, Bitmap> {
        @Override
        @SuppressWarnings("unchecked")
        public Bitmap doInBackground(Void... params) {

            try {
                if (url == null) {
                    url = new URL(REMOTE_EYES_IMAGE_URL);
                }
                URLConnection cn = url.openConnection();
                cn.connect();
                InputStream stream = cn.getInputStream();
                Bitmap bmp = BitmapFactory.decodeStream(stream);
                stream.close();
                return bmp;
            } catch (Exception e) {
                // do nothing
            }
            return null;
        }

        @Override
        public void onPostExecute(Bitmap bmp) {
            if (bmp != null) {
                updateRemoteEyesView(bmp);
            }
        }
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
                Intent intent = new Intent(this, PrefsActivity.class);
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
        mUiView = new AccelerometerView(this, this, false, disp.getWidth(), disp.getHeight());
        mControlsLayout.addView(mUiView);
    }

    public void switchToDpadControl() {
        mControlsLayout.removeAllViews();
        Display disp = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        mUiView = new DpadView(this, this, false, disp.getWidth(), disp.getHeight());
        mControlsLayout.addView(mUiView);
    }

    public void switchToVoiceControl() {
        mControlsLayout.removeAllViews();
        Display disp = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        mUiView = new VoiceView(this, this, false, disp.getWidth(), disp.getHeight());
        mControlsLayout.addView(mUiView);
    }

    public void switchToJoystickControl() {
        mControlsLayout.removeAllViews();
        Display disp = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        mUiView = new JoystickView(this, this, false, disp.getWidth(), disp.getHeight());
        mControlsLayout.addView(mUiView);
        // This looks weird, but if we don't do this, the joystick view will
        // not draw its initial state properly.
        this.openOptionsMenu();
        this.closeOptionsMenu();
    }

    @Override
    public void onWheelVelocitySetRequested(float direction, float speed) {
        if (direction >= 0 && direction <= 90) {
            CellDroidManager.sendDirectCommand(
                    "w " + (int) speed + " " + (int) (speed - direction / 90 * speed));
        } else if (direction > 90) {
            CellDroidManager.sendDirectCommand(
                    "w -" + (int) speed + " -" + (int) (speed - (180 - direction) / 90 * speed));
        } else if (direction < 0 && direction >= -90) {
            CellDroidManager.sendDirectCommand(
                    "w " + (int) (speed - Math.abs(direction) / 90 * speed) + " " + (int) speed);
        } else if (direction < -90) {
            CellDroidManager.sendDirectCommand(
                    "w -" + (int) (speed - (180 - Math.abs(direction)) / 90 * speed) + " -"
                            + (int) speed);
        }
    }

    @Override
    public void onStopRequested() {
        CellDroidManager.sendDirectCommand("s");
    }

    @Override
    public void onActionRequested(int action, String values) {

        switch (action) {
            case UiEventListener.ACTION_FORWARD:
                CellDroidManager.sendDirectCommand("f");
                break;
            case UiEventListener.ACTION_BACKWARD:
                CellDroidManager.sendDirectCommand("b");
                break;
            case UiEventListener.ACTION_LEFT:
                CellDroidManager.sendDirectCommand("l");
                break;
            case UiEventListener.ACTION_RIGHT:
                CellDroidManager.sendDirectCommand("r");
                break;
        }

    }

    private HeadPanControl.HeadPanListener panListener = new HeadPanControl.HeadPanListener() {
        @Override
        public void onPan(PanAction action) {
            if (action == PanAction.PAN_CENTER) {
                CellDroidManager.sendDirectCommand("hc");
            } else if (action == PanAction.PAN_LEFT) {
                CellDroidManager.sendDirectCommand("hl");
            } else if (action == PanAction.PAN_RIGHT) {
                CellDroidManager.sendDirectCommand("hr");
            } else if (action == PanAction.PAN_UP) {
                CellDroidManager.sendDirectCommand("hu");
            } else if (action == PanAction.PAN_DOWN) {
                CellDroidManager.sendDirectCommand("hd");
            } else if (action == PanAction.PAN_NONE) {
                CellDroidManager.sendDirectCommand("hs");
            }
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

    }

}
