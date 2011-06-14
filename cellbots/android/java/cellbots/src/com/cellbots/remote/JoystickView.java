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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SlidingDrawer;
import android.widget.SlidingDrawer.OnDrawerCloseListener;
import android.widget.SlidingDrawer.OnDrawerOpenListener;

/**
 * This View allows joystick-like touch interaction to obtain direction and
 * speed which can be used to drive the robot.
 *
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 * @author keertip@google.com (Keerti Parthasarathy)
 */
public class JoystickView extends UiView {

    private class JoystickImageView extends ImageView {
        public JoystickImageView(Context context) {
            super(context);
            setImageDrawable(context.getResources().getDrawable(R.drawable.joystick_background));
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            // don't draw the joystick until we have determined the center
            // of the drawing circle.
            if ((mCenterX > 0) && (mCenterY > 0)) {
                float radius = animJoystickRadius > 0 || stopped ? animJoystickRadius
                        : mJoystickRadius;
                canvas.drawLine(mCenterX, mCenterY, mCenterX
                        + (int) (radius * Math.sin(mDirection * Math.PI / 180)), mCenterY
                        - (int) (radius * Math.cos(mDirection * Math.PI / 180)), mPaint);

                canvas.drawBitmap(joystickBallBitmap, mCenterX - joystickBallBitmap.getWidth() / 2
                        + (int) (radius * Math.sin(mDirection * Math.PI / 180)), mCenterY
                        - joystickBallBitmap.getHeight() / 2
                        - (int) (radius * Math.cos(mDirection * Math.PI / 180)), mPaint);
            }
        }
    }

    private static final String TAG = "CellbotJoystick";

    private int mWidth;

    private int mHeight;

    private int mCenterX = -1;

    private int mCenterY = -1;

    private float mPrevX, mPrevY;

    private float mDirection = 0; // Default direction is forward


    private float mSpeed = 20; // % speed


    private float mJoystickRadius;

    private Paint mPaint;

    private UiEventListener mJoystickListener;

    private float animJoystickRadius;

    private Handler refreshHandler = null;

    private boolean stopped;

    private boolean isDrawer = true;

    private Runnable doStopAnim = new Runnable() {
        @Override
        public void run() {
            invalidate();
            animJoystickRadius = Math.max(animJoystickRadius - 20, 0);
            if (animJoystickRadius > 0) {
                refreshHandler.removeCallbacks(doStopAnim);
                refreshHandler.postDelayed(this, 50);
            }
        }
    };

    private JoystickImageView joystickImage;

    private Bitmap joystickBallBitmap;

    private Vibrator mVibe;

    private OnTouchListener joystickTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (mJoystickListener != null) {
                    handleStop();
                }
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                boolean moved = false;
                float distFromCenter = distance(mCenterX, mCenterY, event.getX(), event.getY());
                if (distFromCenter > mJoystickRadius) {
                    mPrevX = mCenterX;
                    mPrevY = mCenterY;
                    mVibe.vibrate(20);
                } else if (distance(event.getX(), event.getY(), mPrevX, mPrevY) > 10) {
                    moved = true;
                    mDirection = direction(mCenterX, mCenterY, event.getX(), event.getY());
                    mSpeed = distFromCenter / mJoystickRadius * 200 / 3;
                    animJoystickRadius = distFromCenter;
                    stopped = false;
                    if (mJoystickListener != null) {
                        mJoystickListener.onWheelVelocitySetRequested(mDirection, mSpeed);
                        Log.e("joystick", mDirection + " : " + mSpeed);
                        invalidate();
                    }
                    mPrevX = event.getX();
                    mPrevY = event.getY();
                }
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mVibe.vibrate(20);
            }
            return true;
        }
    };

    public JoystickView(final Context ct, final UiEventListener joystickListener,
            boolean drawer, int width, int height) {
        super(ct, joystickListener);
        isDrawer = drawer;
        mVibe = (Vibrator) ct.getSystemService(Context.VIBRATOR_SERVICE);

        joystickImage = new JoystickImageView(ct);
        joystickBallBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.joystick_ball);
        
        LayoutParams params = new LayoutParams(width, height);
        setLayoutParams(params);
        
        LayoutInflater inflate = LayoutInflater.from(ct);

        FrameLayout frameLayout = (FrameLayout) inflate.inflate(
                R.layout.remote_drawer_with_directional_control_container, null);

        addView(frameLayout);

        final Button slidingDrawerButton = (Button) findViewById(R.id.slideHandleButton);
        SlidingDrawer slidingDrawer = (SlidingDrawer) findViewById(R.id.SlidingDrawer);
        slidingDrawer.setOnDrawerOpenListener(new OnDrawerOpenListener() {
            @Override
            public void onDrawerOpened() {
                slidingDrawerButton.setBackgroundResource(R.drawable.icon_tray_top_opened);
                slidingDrawerButton.setContentDescription(
                        ct.getString(R.string.close_actions_drawer));
            }
        });
        slidingDrawer.setOnDrawerCloseListener(new OnDrawerCloseListener() {
            @Override
            public void onDrawerClosed() {
                slidingDrawerButton.setBackgroundResource(R.drawable.icon_tray_top_closed);
                slidingDrawerButton.setContentDescription(
                        ct.getString(R.string.open_actions_drawer));
            }
        });

        if (!isDrawer) {
            slidingDrawer.setVisibility(View.GONE);
        }

        LinearLayout linearLayout = (LinearLayout) findViewById(
                R.id.directionalController_container);
        linearLayout.addView(joystickImage);
        joystickImage.setOnTouchListener(joystickTouchListener);

        final ImageButton nextControl = (ImageButton) findViewById(R.id.nextControl);
        nextControl.setImageResource(R.drawable.mode_switch_accelerometer_right);
        nextControl.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                joystickListener.onSwitchInterfaceRequested(
                        UiEventListener.INTERFACE_ACCELEROMETER);
            }
        });
        final ImageButton previousControl = (ImageButton) findViewById(R.id.previousControl);
        previousControl.setImageResource(R.drawable.mode_switch_voice_left);
        previousControl.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                joystickListener.onSwitchInterfaceRequested(UiEventListener.INTERFACE_VOICE);
            }
        });

        ImageButton speak = (ImageButton) findViewById(R.id.speak);
        speak.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                joystickListener.onActionRequested(UiEventListener.ACTION_SPEAK, "");
            }
        });
        ImageButton takePicture = (ImageButton) findViewById(R.id.takePicture);
        takePicture.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                joystickListener.onActionRequested(UiEventListener.ACTION_TAKE_PICTURE, "");
            }
        });
        ImageButton setPersona = (ImageButton) findViewById(R.id.setPersona);
        setPersona.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                joystickListener.onPopupWindowRequested();
            }
        });

        mJoystickListener = joystickListener;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(5);
        refreshHandler = new Handler();
    }

    @Override
    public void drawableStateChanged() {
        mWidth = joystickImage.getWidth();
        mHeight = joystickImage.getHeight();
        mCenterX = (mWidth / 2);
        mCenterY = (mHeight / 2);
        mJoystickRadius = Math.min(mWidth / 2, mHeight / 2) + 10;
        handleStop();
    }

    /**
     * Handles the operations to be performed when the user intends to stop the
     * robot.
     */
    private void handleStop() {
        if (!stopped) {
            mVibe.vibrate(20);
            stopped = true;
            initStopAnimation();
        }
        if (mJoystickListener != null) {
            mJoystickListener.onStopRequested();
        }
    }

    /**
     * Initiates animation when stop gesture if performed on the joystick.
     */
    private void initStopAnimation() {
        refreshHandler.removeCallbacks(doStopAnim);
        refreshHandler.postDelayed(doStopAnim, 0);
    }

    private float distance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
    }

    private float direction(float fromX, float fromY, float toX, float toY) {
        float dir = (float) (90 + (Math.atan2(toY - fromY, toX - fromX)) * 180 / Math.PI);
        dir = dir > 180 ? (-90 + dir - 270) : dir;
        return dir;
    }
}
