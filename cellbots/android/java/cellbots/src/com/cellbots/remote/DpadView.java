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

import com.cellbots.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SlidingDrawer;
import android.widget.SlidingDrawer.OnDrawerCloseListener;
import android.widget.SlidingDrawer.OnDrawerOpenListener;

/**
 * UiView for controlling the cellbot with a DPad scheme layout.
 *
 * @author clchen@google.com (Charles L. Chen)
 * @author keertip@google.com (Keerti Parthasarathy)
 */
public class DpadView extends UiView {

    private boolean isDrawer = true;

    private Context mContext;

    private UiEventListener uiEventListener;

    public DpadView(Context context, final UiEventListener eventListener, boolean drawer, int width, int height) {
        super(context, eventListener);

        mContext = context;

        isDrawer = drawer;
        uiEventListener = eventListener;
        
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
            @Override
            public void onDrawerOpened() {
                slidingDrawerButton.setBackgroundResource(R.drawable.icon_tray_top_opened);
                slidingDrawerButton.setContentDescription(
                        mContext.getString(R.string.close_actions_drawer));
            }
        });
        slidingDrawer.setOnDrawerCloseListener(new OnDrawerCloseListener() {
            @Override
            public void onDrawerClosed() {
                slidingDrawerButton.setBackgroundResource(R.drawable.icon_tray_top_closed);
                slidingDrawerButton.setContentDescription(
                        mContext.getString(R.string.open_actions_drawer));
            }
        });

        if (!isDrawer) {
            slidingDrawer.setVisibility(View.GONE);
        }

        // DPAD buttons
        FrameLayout dpadControls = (FrameLayout) inflate.inflate(R.layout.remote_dpad, null);

        LinearLayout linearLayout = (LinearLayout) findViewById(
                R.id.directionalController_container);
        linearLayout.addView(dpadControls);

        ImageButton forward = (ImageButton) findViewById(R.id.forward);
        forward.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    uiEventListener.onActionRequested(UiEventListener.ACTION_FORWARD, "");
                } else if (event.getAction() == MotionEvent.ACTION_UP){
                    uiEventListener.onStopRequested();
                }
                return false;
            }
        });
        
        ImageButton left = (ImageButton) findViewById(R.id.left);
        left.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    uiEventListener.onActionRequested(UiEventListener.ACTION_LEFT, "");
                } else if (event.getAction() == MotionEvent.ACTION_UP){
                    uiEventListener.onStopRequested();
                }
                return false;
            }
        });

        ImageButton right = (ImageButton) findViewById(R.id.right);
        right.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    uiEventListener.onActionRequested(UiEventListener.ACTION_RIGHT, "");
                } else if (event.getAction() == MotionEvent.ACTION_UP){
                    uiEventListener.onStopRequested();
                }
                return false;
            }
        });

        ImageButton backward = (ImageButton) findViewById(R.id.backward);
        backward.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    uiEventListener.onActionRequested(UiEventListener.ACTION_BACKWARD, "");
                } else if (event.getAction() == MotionEvent.ACTION_UP){
                    uiEventListener.onStopRequested();
                }
                return false;
            }
        });

        ImageButton stop = (ImageButton) findViewById(R.id.stop);
        stop.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                uiEventListener.onStopRequested();
            }
        });

        // Action buttons
        ImageButton speak = (ImageButton) findViewById(R.id.speak);
        speak.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                uiEventListener.onActionRequested(UiEventListener.ACTION_SPEAK, "");
            }
        });
        ImageButton takePicture = (ImageButton) findViewById(R.id.takePicture);
        takePicture.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                uiEventListener.onActionRequested(UiEventListener.ACTION_TAKE_PICTURE, "");
            }
        });
        ImageButton getLocation = (ImageButton) findViewById(R.id.getLocation);
        getLocation.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                uiEventListener.onActionRequested(UiEventListener.ACTION_GET_LOCATION, "");
            }
        });
        ImageButton setPersona = (ImageButton) findViewById(R.id.setPersona);
        setPersona.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                uiEventListener.onPopupWindowRequested();
            }
        });

        final ImageButton nextControl = (ImageButton) findViewById(R.id.nextControl);
        // TODO (clchen): Replace this with the right resource once we get
        // it.
        nextControl.setImageResource(R.drawable.mode_switch_voice_right);
        nextControl.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                uiEventListener.onSwitchInterfaceRequested(UiEventListener.INTERFACE_VOICE);
            }
        });
        final ImageButton previousControl = (ImageButton) findViewById(R.id.previousControl);
        previousControl.setImageResource(R.drawable.mode_switch_accelerometer_left);
        previousControl.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                uiEventListener.onSwitchInterfaceRequested(UiEventListener.INTERFACE_ACCELEROMETER);
            }
        });
    }

}
