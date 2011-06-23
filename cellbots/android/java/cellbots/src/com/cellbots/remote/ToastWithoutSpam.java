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

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;

/**
 * Send a toast message to the user. Can be called from a background task (that
 * has no UI thread) and is thread safe (so can be called from any thread at any
 * time). Avoids sending the same message to the user within D milliseconds so
 * that threads in a loop don't spam the toast.
 *
 * @author css@google.com (Charles Spirakis)
 */
public class ToastWithoutSpam {
    private static final String TOAST_TAG = "ToastWithoutSpam";

    private Activity mActivity;

    private Context mContext;

    private long mDelayTimeMs;

    private int mDepth;

    private Map<String, Long> mPreviousToast;

    private Semaphore mSem;

    private Random mRand;

    public ToastWithoutSpam(Activity activity, Context context, int depth, int minTimeMs) {
        mActivity = activity;
        mContext = context;
        mDepth = depth;
        mDelayTimeMs = minTimeMs;

        mPreviousToast = new HashMap<String, Long>();
        mRand = new Random();
        mSem = new Semaphore(1);
    }

    /**
     * Cause toast to pop up. Can be called from a background process as this
     * uses runOnUiThread() to make sure the toast can be created.
     *
     * @param text Text to put on the toast
     */
    public boolean sendToast(final String text) {
        Long now = new Long(SystemClock.uptimeMillis());

        try {
            mSem.acquire();
        } catch (InterruptedException e) {
            return false;
        }

        try {
            Long when = mPreviousToast.get(text);

            if (null != when) {
                if ((now - when) < mDelayTimeMs) {
                    // If we've recently sent this message, don't send it again.
                    Log.d(TOAST_TAG, "skipped: " + text + " now: " + now + " when: " + when);
                    return false;
                }
                Log.d(TOAST_TAG, "using: " + text);
            } else {
                if (mPreviousToast.size() > mDepth) {
                    // We could implement LRU, but instead, just do a random
                    // replace as we shouldn't hit the limit that often.
                    String[] s = (String[]) mPreviousToast.keySet().toArray();
                    int pick = (int) (mRand.nextFloat() * mDepth);
                    mPreviousToast.remove(s[pick]);
                    Log.d(TOAST_TAG, "removed: " + s[pick] + "size: " + mPreviousToast.size());
                }
            }
            mPreviousToast.put(text, now);
        } finally {
            mSem.release();
        }

        final Context ctx = mContext;
        Runnable r = new Runnable() {
            public void run() {
                Toast toast = Toast.makeText(ctx, text, Toast.LENGTH_SHORT);
                toast.show();
            }
        };
        mActivity.runOnUiThread(r);

        return true;
    }
}
