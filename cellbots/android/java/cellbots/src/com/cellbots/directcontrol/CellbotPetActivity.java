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

import com.cellbots.R;
import com.cellbots.local.CellDroidManager;
import com.cellbots.remote.AccelerometerView;
import com.cellbots.remote.DpadView;
import com.cellbots.remote.HeadPanControl;
import com.cellbots.remote.HeadPanControl.PanAction;
import com.cellbots.remote.JoystickView;
import com.cellbots.remote.UiView;
import com.cellbots.remote.UiView.UiEventListener;
import com.cellbots.remote.VoiceView;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * Turns your Cellbot into a pet robot that can obey basic commands and track
 * faces. TODO (clchen, css, bendavies): Work out where this should go in the
 * workflow.
 *
 * @author clchen@google.com (Charles L. Chen)
 */
public class CellbotPetActivity extends Activity implements UiEventListener, Callback {
    public static final String TAG = "Cellbot Pet";

    public static final int VOICE_RECO_CODE = 42;

    public static final String PERSONA_IDLE =
            "http://personabots.appspot.com/personas/tuby/idle.png";

    public static final String PERSONA_READY =
            "http://personabots.appspot.com/personas/tuby/ready.png";

    public int state = 0;

    private LinearLayout mControlsLayout;

    private UiView mUiView;

    private int previewHeight = 0;

    private int previewWidth = 0;

    private int previewFormat = 0;

    private byte[] mCallbackBuffer;

    private ByteArrayOutputStream out;

    private WakeLock mWakeLock;

    private SurfaceHolder mHolder;

    private FrameLayout mFrame;

    private WebView mWebView;

    private ImageView mImageView;

    private SurfaceView mPreview;

    private Camera mCamera;

    private Rect r;

    private TextToSpeech mTts;

    private VoiceView mVoiceView;

    private Face[] faces = new Face[1];

    private FaceDetector fd;

    private byte[] imageBytes;

    private Bitmap previewImage;

    private boolean userWasVisible = false;

    private boolean voiceControlsActive = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTts = new TextToSpeech(this, new OnInitListener() {
            @Override
            public void onInit(int arg0) {
                mTts.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {
                    @Override
                    public void onUtteranceCompleted(String arg0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (voiceControlsActive) {
                                    mVoiceView.startListening();
                                }
                            }
                        });
                    }
                });
                HashMap<String, String> hashMap = new HashMap<String, String>();
                hashMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "foo");
                mTts.speak("Shall we play a game?", 0, hashMap);
            }
        });
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE
                | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Cellbot Eyes");
        mWakeLock.acquire();
        out = new ByteArrayOutputStream();

        setContentView(R.layout.eyes_main);
        mPreview = (SurfaceView) findViewById(R.id.eyes_preview);
        mHolder = mPreview.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mFrame = (FrameLayout) findViewById(R.id.eyes_frame);

        Display disp = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        mVoiceView = new VoiceView(this, this, false, disp.getWidth(), disp.getHeight());
        mVoiceView.setClickable(false);
        mFrame.addView(mVoiceView);

        mImageView = new ImageView(this);
        int width = getWindowManager().getDefaultDisplay().getWidth();
        int height = getWindowManager().getDefaultDisplay().getHeight();
        LayoutParams params = new LayoutParams(width, height);
        mImageView.setLayoutParams(params);
        mImageView.setScaleType(ScaleType.FIT_CENTER);
        mImageView.setImageResource(R.drawable.persona_tuby_ready);
        mImageView.setBackgroundColor(Color.BLACK);

        mImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (voiceControlsActive) {
                    mVoiceView.stopListening();
                    onStopRequested();
                    mImageView.setImageResource(R.drawable.persona_tuby_idle);
                    voiceControlsActive = false;
                } else {
                    mVoiceView.startListening();
                    mImageView.setImageResource(R.drawable.persona_tuby_ready);
                    voiceControlsActive = true;
                }
            }
        });

        mFrame.addView(mImageView);

        voiceControlsActive = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mWakeLock != null) {
            mWakeLock.release();
        }
        if (mTts != null) {
            mTts.shutdown();
        }
        if (mVoiceView != null) {
            mVoiceView.stopListening();
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        mHolder.setFixedSize(w, h);
        // Start the preview
        Parameters params = mCamera.getParameters();
        previewHeight = params.getPreviewSize().height;
        previewWidth = params.getPreviewSize().width;
        previewFormat = params.getPreviewFormat();

        // Crop the edges of the picture to reduce the image size
        r = new Rect(80, 30, previewWidth - 80, previewHeight - 30);

        mCallbackBuffer = new byte[460800];

        mCamera.setParameters(params);
        mCamera.setPreviewCallbackWithBuffer(new PreviewCallback() {
            public void onPreviewFrame(byte[] imageData, Camera arg1) {
                trackface(imageData);
                if (mCamera != null) {
                    mCamera.addCallbackBuffer(mCallbackBuffer);
                }
            }
        });
        mCamera.addCallbackBuffer(mCallbackBuffer);
        mCamera.startPreview();
    }

    private void trackface(byte[] imageData) {
        YuvImage yuvImage = new YuvImage(
                imageData, previewFormat, previewWidth, previewHeight, null);
        yuvImage.compressToJpeg(r, 50, out); // Tweak the quality here - 50 for
                                             // detection
        imageBytes = out.toByteArray();
        previewImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        if (fd == null) {
            fd = new FaceDetector(previewImage.getWidth(), previewImage.getHeight(), 1);
        }
        int faceCount = fd.findFaces(previewImage, faces);
        if (faceCount > 0) {
            if (!userWasVisible) {
                userWasVisible = true;
                speak("Peek a boo, I see you!");
            }
            PointF p = new PointF();
            faces[0].getMidPoint(p);
            if (p.x < 150) {
                CellDroidManager.sendDirectCommand("w -5 5");
            } else if (p.x > 350) {
                CellDroidManager.sendDirectCommand("w 5 -5");
            } else {
                CellDroidManager.sendDirectCommand("s");
            }
        } else {
            if (userWasVisible) {
                userWasVisible = false;
                speak("Hey, where did you go?");
                CellDroidManager.sendDirectCommand("s");
            }
        }
        out.reset();
        previewImage.recycle();
    }

    private void speak(String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVoiceView.stopListening();
            }
        });
        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "foo");
        mTts.speak(text, 0, hashMap);
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

    /*
     * (non-Javadoc)
     * @see com.cellbots.remote.UiView.UiEventListener#onPopupWindowRequested()
     */
    @Override
    public void onPopupWindowRequested() {

    }

}
