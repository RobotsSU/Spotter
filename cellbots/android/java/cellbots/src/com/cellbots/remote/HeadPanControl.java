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

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * This touch controller for panning/tilting the head of the robot attaches
 * itself to the specified View, and sends callbacks to the specified listener
 * when a pan/tilt gesture is performed by the user.
 *
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 * @author keertip@google.com (Keerti Parthasarathy)
 */
public class HeadPanControl {

    public enum PanAction {
        PAN_UP, PAN_DOWN, PAN_RIGHT, PAN_LEFT, PAN_CENTER, PAN_NONE
    }

    private View mView;

    private long mTimeDown;

    private float mDownX, mDownY;

    private boolean mTouching = false;

    private boolean mDoPanning = false;

    private PanAction mPanAction;

    private HeadPanListener mPanListener;

    private Runnable messageSender = new Runnable() {
        @Override
        public void run() {
            while (mTouching) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (mDoPanning && mPanListener != null) {
                    mPanListener.onPan(mPanAction);
                }
            }
            mPanAction = PanAction.PAN_NONE;
            mPanListener.onPan(mPanAction);
        }
    };

    private OnTouchListener touchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mDownX = event.getX();
                mDownY = event.getY();
                mTimeDown = System.currentTimeMillis();
                mTouching = true;
                new Thread(messageSender).start();
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if (Math.abs(x - mDownX) > Math.abs(y - mDownY) && Math.abs(x - mDownX) > 20) {
                    if (x > mDownX) {
                        mPanAction = PanAction.PAN_RIGHT;
                    } else {
                        mPanAction = PanAction.PAN_LEFT;
                    }
                    mDoPanning = true;
                } else if (Math.abs(y - mDownY) > Math.abs(x - mDownX)
                        && Math.abs(y - mDownY) > 20) {
                    if (y > mDownY) {
                        mPanAction = PanAction.PAN_DOWN;
                    } else {
                        mPanAction = PanAction.PAN_UP;
                    }
                    mDoPanning = true;
                } else if (Math.abs(x - mDownX) < 10 && Math.abs(y - mDownY) < 10
                        && System.currentTimeMillis() - mTimeDown > 1000) {
                    mPanAction = PanAction.PAN_CENTER;
                    mDoPanning = true;
                } else {
                    mPanAction = PanAction.PAN_NONE;
                    mDoPanning = false;
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                mPanAction = PanAction.PAN_NONE;
                mTouching = false;
                mDoPanning = false;
            }
            return true;
        }
    };

    public HeadPanControl(View view, HeadPanListener listener, boolean panControl) {
        mView = view;
        mPanListener = listener;
        if (panControl) {
            mView.setOnTouchListener(touchListener);
        }
    }

    public interface HeadPanListener {
        public void onPan(PanAction action);
    }
}
