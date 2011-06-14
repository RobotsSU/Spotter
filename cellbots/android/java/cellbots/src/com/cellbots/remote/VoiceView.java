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

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SlidingDrawer;
import android.widget.SlidingDrawer.OnDrawerCloseListener;
import android.widget.SlidingDrawer.OnDrawerOpenListener;
import android.widget.TextView;
import com.cellbots.R;
import com.googlecode.eyesfree.voicecontrol.GrammarRecognizer;
import com.googlecode.eyesfree.voicecontrol.GrammarRecognizer.GrammarMap;
import com.googlecode.eyesfree.voicecontrol.GrammarRecognizer.GrammarResult;

import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * Voice control view for cloud robotics command & control.
 *
 * @author alanv@google.com (Alan Viverette)
 */
public class VoiceView extends UiView {
    private static final String TAG = "VoiceView";

    private static final String CMD_FORWARD = "FWD";

    private static final String CMD_BACKWARD = "BACK";

    private static final String CMD_LEFT = "LEFT";

    private static final String CMD_RIGHT = "RIGHT";

    private static final String CMD_PICTURE = "PICTURE";

    private static final String CMD_LOCATION = "LOCATION";

    private static final String CMD_SPEAK = "SPEAK";

    private static final String CMD_STOP = "STOP";

    private static final int LEFT_CONTROL = UiEventListener.INTERFACE_DPAD;

    private static final int RIGHT_CONTROL = UiEventListener.INTERFACE_JOYSTICK;

    private static final int LEFT_CONTROL_RES = R.drawable.mode_switch_rocker_left;

    private static final int RIGHT_CONTROL_RES = R.drawable.mode_switch_joystick_right;

    /**
     * Confidence threshold for recognition results. Value tested is
     * confidence(result) / sum of all confidences returned.
     */
    private static final double CONF_THRESH = 0.5;

    private final TextView mResult;

    private final View mCancel;

    private final UiEventListener mListener;

    private final SoundPool mPool;

    private final GrammarRecognizer mRecognizer;

    private final Thread mLoader;

    private final Handler mHandler;

    private int mClickId;

    private boolean mWasListening;

    /**
     * Constructs a voice controller view using the specified event listener
     *
     * @param context
     * @param uiEventListener
     */
    public VoiceView(final Context context, UiEventListener uiEventListener, 
            boolean isDrawer, int width, int height) {
        super(context, uiEventListener);

        mWasListening = false;
        mListener = uiEventListener;
        mHandler = new Handler();
        
        LayoutParams params = new LayoutParams(width, height);
        setLayoutParams(params);

        LayoutInflater inflate = LayoutInflater.from(context);

        FrameLayout frameLayout = (FrameLayout) inflate.inflate(
                R.layout.remote_drawer_with_directional_control_container, null);
        addView(frameLayout);

        // Sliding drawer arrow flip for open vs closed
        final Button slidingDrawerButton = (Button) findViewById(R.id.slideHandleButton);
        SlidingDrawer slidingDrawer = (SlidingDrawer) findViewById(R.id.SlidingDrawer);
        slidingDrawer.setOnDrawerOpenListener(new OnDrawerOpenListener() {
            public void onDrawerOpened() {
                slidingDrawerButton.setBackgroundResource(R.drawable.icon_tray_top_opened);
                slidingDrawerButton.setContentDescription(
                        context.getString(R.string.close_actions_drawer));
            }
        });
        slidingDrawer.setOnDrawerCloseListener(new OnDrawerCloseListener() {
            @Override
            public void onDrawerClosed() {
                slidingDrawerButton.setBackgroundResource(R.drawable.icon_tray_top_closed);
                slidingDrawerButton.setContentDescription(
                        context.getString(R.string.open_actions_drawer));
            }
        });

        if (!isDrawer) {
            slidingDrawer.setVisibility(View.GONE);
        }

        // Add the voice view to the parent container
        View voiceView = inflate.inflate(R.layout.remote_voice, null);
        LinearLayout linearLayout = (LinearLayout) findViewById(
                R.id.directionalController_container);
        linearLayout.addView(voiceView);

        mResult = (TextView) findViewById(R.id.voice_result);
        mResult.setText(context.getString(R.string.voice_loading));

        mCancel = findViewById(R.id.cancel);
        mCancel.setVisibility(View.INVISIBLE);

        // These objects won't be ready until mLoader finishes
        mPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        mRecognizer = new GrammarRecognizer(context);

        // TODO(alanv): Load in a different thread
        mLoader = new Thread() {
            @Override
            public void run() {
                loadGrammarAsync();
            }
        };
        mLoader.start();

        findViewById(R.id.help).setOnClickListener(clickListener);
        findViewById(R.id.voice_mic).setOnClickListener(clickListener);
        findViewById(R.id.cancel).setOnClickListener(clickListener);

        // TODO(clchen): Maybe put these in the constructor or let the parent
        // handle it so we don't have to change three views just to add one
        // control view.

        ImageButton previousControl = (ImageButton) findViewById(R.id.previousControl);
        previousControl.setImageResource(LEFT_CONTROL_RES);
        previousControl.setOnClickListener(clickListener);

        ImageButton nextControl = (ImageButton) findViewById(R.id.nextControl);
        nextControl.setImageResource(RIGHT_CONTROL_RES);
        nextControl.setOnClickListener(clickListener);
    }

    private void loadGrammarAsync() {
        Context context = getContext();

        // Load sound resources
        mClickId = mPool.load(context, R.raw.click, 1);

        // Load available voice commands from String array resources
        String[] forward = context.getResources().getStringArray(R.array.voice_cmd_forward);
        String[] backward = context.getResources().getStringArray(R.array.voice_cmd_backward);
        String[] left = context.getResources().getStringArray(R.array.voice_cmd_left);
        String[] right = context.getResources().getStringArray(R.array.voice_cmd_right);
        String[] picture = context.getResources().getStringArray(R.array.voice_cmd_picture);
        String[] location = context.getResources().getStringArray(R.array.voice_cmd_location);
        String[] speak = context.getResources().getStringArray(R.array.voice_cmd_speak);
        String[] stop = context.getResources().getStringArray(R.array.voice_cmd_stop);

        // Add the words to the Commands slot in the grammar
        GrammarMap grammarMap = new GrammarMap();
        grammarMap.addWords("Commands", forward, null, null, CMD_FORWARD);
        grammarMap.addWords("Commands", backward, null, null, CMD_BACKWARD);
        grammarMap.addWords("Commands", left, null, null, CMD_LEFT);
        grammarMap.addWords("Commands", right, null, null, CMD_RIGHT);
        grammarMap.addWords("Commands", picture, null, null, CMD_PICTURE);
        grammarMap.addWords("Commands", location, null, null, CMD_LOCATION);
        grammarMap.addWords("Commands", speak, null, null, CMD_SPEAK);
        grammarMap.addWords("Commands", stop, null, null, CMD_STOP);

        // Prepare the recognizer and load the grammar
        mRecognizer.setListener(recoListener);
        mRecognizer.loadGrammar(R.raw.generic);
        mRecognizer.loadSlots(grammarMap);

        mHandler.post(new Runnable() {
            public void run() {
                onGrammarLoaded();
            }
        });
    }

    private void onGrammarLoaded() {
        Context context = getContext();

        mResult.setText(context.getString(R.string.voice_loaded));
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        if (!gainFocus && mWasListening) {
            mRecognizer.recognize();
        } else {
            mWasListening = mRecognizer.isListening();

            mRecognizer.stop();
        }
    }

    /**
     * Shuts down the recognizer. Similar to onDestroy().
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        try {
            mLoader.join();
            mPool.release();
            mRecognizer.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private final void onCommandError(String reason) {
        Context context = getContext();

        mCancel.setVisibility(View.INVISIBLE);
        mResult.setText(context.getString(R.string.voice_failed, reason));
    }

    /**
     * Receives command recognition results, parses them, and sends movement
     * commands to the robot
     *
     * @param results
     */
    private final void onCommandRecognized(TreeSet<GrammarResult> results) {
        Context context = getContext();

        if (results == null) {
            mResult.setText(context.getString(R.string.voice_not_recognized));
            return;
        }

        int totalConfidence = 0;

        for (GrammarResult entry : results) {
            totalConfidence += entry.getConfidence();
        }

        GrammarResult bestResult = results.iterator().next();

        // For debugging voice recognition confidence levels
//        String toast = context.getString(R.string.voice_debug, bestResult.getLiteral(),
//                bestResult.getConfidence(), totalConfidence);
//        Toast.makeText(getContext(), toast, 3000).show();

        // If the best result's confidence is less than 0.5 * the sum of all
        // confidences, we'll ignore it.
        if (bestResult.getConfidence() / (double) totalConfidence < CONF_THRESH) {
            mResult.setText(context.getString(R.string.voice_not_recognized));
            return;
        }

        GrammarResult primary = null;

        for (GrammarResult result : results) {
            if (primary == null) {
                primary = result;
            }

            String meaning = result.getMeaning();
            int conf = result.getConfidence();

            Log.e(TAG, conf + ":\n" + meaning);
        }

        String literal = primary.getLiteral();
        String meaning = primary.getMeaning();
        StringTokenizer toke = new StringTokenizer(meaning, "|");

        mResult.setText(context.getString(R.string.voice_recognized, literal, meaning));

        parseCommand(toke);
    }

    /**
     * Parses out commands from the tokenized speech recognition result and
     * sends it to the event listener.
     *
     * @param toke
     */
    private void parseCommand(StringTokenizer toke) {
        if (!toke.hasMoreTokens()) {
            return;
        }

        String command = toke.nextToken();

        mPool.play(mClickId, 1.0f, 1.0f, 1, 0, 1.0f);

        if (CMD_FORWARD.equals(command)) {
            mListener.onActionRequested(UiEventListener.ACTION_FORWARD, null);
        } else if (CMD_BACKWARD.equals(command)) {
            mListener.onActionRequested(UiEventListener.ACTION_BACKWARD, null);
        } else if (CMD_LEFT.equals(command)) {
            mListener.onActionRequested(UiEventListener.ACTION_LEFT, null);
        } else if (CMD_RIGHT.equals(command)) {
            mListener.onActionRequested(UiEventListener.ACTION_RIGHT, null);
        } else if (CMD_PICTURE.equals(command)) {
            mListener.onActionRequested(UiEventListener.ACTION_TAKE_PICTURE, null);
        } else if (CMD_LOCATION.equals(command)) {
            mListener.onActionRequested(UiEventListener.ACTION_GET_LOCATION, null);
        } else if (CMD_SPEAK.equals(command)) {
            mListener.onActionRequested(UiEventListener.ACTION_SPEAK, "");
            return;
        } else if (CMD_STOP.equals(command)) {
            mListener.onStopRequested();
        }

        startListening();
    }

    /**
     * Displays an alert dialog with a list of available commands pulled from a
     * string resource. These are maintained separately from the list of
     * commands pushed into the recognition grammar.
     */
    private void displayHelp() {
        new AlertDialog.Builder(getContext()).setTitle(R.string.voice_help_title).setMessage(
                R.string.voice_help_message).setCancelable(true).setNegativeButton(
                android.R.string.ok, null).show();
    }

    /**
     * Starts voice recognition and displays the cancel button.
     */
    public void startListening() {
        mRecognizer.recognize();
        mCancel.setVisibility(View.VISIBLE);
    }

    /**
     * Stops recognition, hides the cancel button, and shows the "not listening"
     * text dialog.
     */
    public void stopListening() {
        mRecognizer.stop();
        mCancel.setVisibility(View.INVISIBLE);
    }

    private final View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.voice_mic:
                    startListening();
                    mResult.setText(R.string.voice_listening);
                    break;
                case R.id.help:
                    displayHelp();
                    break;
                case R.id.cancel:
                    stopListening();
                    mListener.onStopRequested();
                    mResult.setText(R.string.voice_not_listening);
                    break;
                case R.id.previousControl:
                    mListener.onSwitchInterfaceRequested(LEFT_CONTROL);
                    break;
                case R.id.nextControl:
                    mListener.onSwitchInterfaceRequested(RIGHT_CONTROL);
                    break;
            }
        }
    };

    /**
     * Handles status, recognition, and failure callbacks from Voice Control
     */
    private final GrammarRecognizer.GrammarListener recoListener =
            new GrammarRecognizer.GrammarListener() {
                @Override
                public void onRecognitionSuccess(final TreeSet<GrammarResult> results) {
                    mHandler.post(new Runnable() {
                        public void run() {
                            mCancel.setVisibility(View.INVISIBLE);
                            
                            onCommandRecognized(results);
                        }
                    });
                }

                @Override
                public void onRecognitionFailure() {
                    mHandler.post(new Runnable() {
                        public void run() {
                            mCancel.setVisibility(View.INVISIBLE);
                            
                            onCommandRecognized(null);
                        }
                    });
                }

                @Override
                public void onRecognitionError(final String reason) {
                    mHandler.post(new Runnable() {
                        public void run() {
                            mCancel.setVisibility(View.INVISIBLE);
                            
                            onCommandError(reason);
                        }
                    });
                }
            };
}
