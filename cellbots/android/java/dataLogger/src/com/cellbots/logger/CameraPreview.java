/*
 * Copyright (C) 2011 Google Inc.
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

package com.cellbots.logger;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * View that handles the picture taking functionality.
 *
 * @author clchen@google.com (Charles L. Chen)
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    public static final String TAG = "CELLBOTS LOGGER";

    private String timeString;

    private SurfaceHolder holder;

    private Camera mCamera;

    private File directory;

    private long mDelay = 0;

    private boolean mTakingPictures = false;

    private int mPictureCount = 0;

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPictureCount = 0;
        timeString = ((LoggerActivity) context).timeString;
        directory = new File(
                Environment.getExternalStorageDirectory() + "/cellbots_logger/" + timeString
                        + "/pictures/");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.setDisplayOrientation(90);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mCamera.startPreview();
    }

    public void takePictures(long delay) {
        if (!mTakingPictures) {
            mDelay = delay;
            mTakingPictures = true;
            runPictureTakingLoop();
        }
    }

    public void stop() {
        mTakingPictures = false;
    }

    public int getPictureCount() {
        return mPictureCount;
    }

    private void runPictureTakingLoop() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!mTakingPictures) {
                        return;
                    }
                    Thread.sleep(mDelay);
                    if (!mTakingPictures) {
                        return;
                    }
                    mCamera.takePicture(null, null, new PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            try {
                                FileOutputStream outStream = new FileOutputStream(
                                        directory.getAbsoluteFile().toString() + "/"
                                                + System.currentTimeMillis() + ".jpg");
                                outStream.write(data);
                                outStream.close();
                                mPictureCount++;
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                mCamera.startPreview();
                                runPictureTakingLoop();
                            }
                            Log.d(TAG, "onPictureTaken - jpeg");
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        stop();
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

}
