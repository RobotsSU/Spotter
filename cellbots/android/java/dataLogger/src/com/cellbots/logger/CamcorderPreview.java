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
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * View that handles the video recording functionality. Parts of this code were
 * taken from memofy'stutorialat:http://memofy.com/memofy/show/2008c618f15fc61801ca038cbfe138/how-to-use-mediarecorder-in-android
 *
 * @author clchen@google.com (Charles L. Chen)
 */
public class CamcorderPreview extends SurfaceView implements SurfaceHolder.Callback,
        MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {

    public final static String TAG = "CELLBOTS LOGGER";

    private MediaRecorder recorder;

    private SurfaceHolder holder;

    private FileOutputStream fos;

    private static final int FRAME_RATE = 15;

    private static final int CIF_WIDTH = 720;

    private static final int CIF_HEIGHT = 480;

    private boolean initialized;

    private String timeString;

    public CamcorderPreview(Context context, AttributeSet attrs) {
        super(context, attrs);

        timeString = ((LoggerActivity) context).timeString;

        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        initializeRecording();
    }

    public void initializeRecording() {
        if (!initialized) {
            recorder = new MediaRecorder();
            recorder.setOnErrorListener(this);
            recorder.setOnInfoListener(this);
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

            String path = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/cellbots_logger/" + timeString + "/video-" + timeString + ".mp4";

            Log.i(TAG, "Video file to use: " + path);

            final File file = new File(path);
            File directory = file.getParentFile();
            if (!directory.exists() && !directory.mkdirs()) {
                try {
                    throw new IOException(
                            "Path to file could not be created. " + directory.getAbsolutePath());
                } catch (IOException e) {
                    Log.e(TAG, "Directory could not be created. " + e.toString());
                }
            }

            if (file.exists()) {
                file.delete();
            }

            if (recorder != null) {
                try {
                    file.createNewFile();
                    fos = new FileOutputStream(path);
                    recorder.setOutputFile(fos.getFD());
                    recorder.setVideoSize(CIF_WIDTH, CIF_HEIGHT);
                    recorder.setVideoFrameRate(FRAME_RATE);
                    recorder.setPreviewDisplay(holder.getSurface());
                    recorder.prepare();
                } catch (IllegalStateException e) {
                    Log.e("ls", e.toString(), e);
                } catch (IOException e) {
                    Log.e("ls", e.toString(), e);
                }
            }
            initialized = true;
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void onError(MediaRecorder mediaRecorder, int what, int extra) {
        Log.e(TAG, "Error received in media recorder: " + what + ", " + extra);
    }

    public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
        Log.e(TAG, "Info received from media recorder: " + what + ", " + extra);
    }

    public void releaseRecorder() throws IOException {

        if (recorder != null) {
            recorder.reset();
            recorder.release();
        }

        if (fos != null) {
            fos.close();
        }

        initialized = false;
    }

    public void startRecording() {
        recorder.start();
    }

    public void stopRecording() {
        recorder.stop();
    }

}
