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

package com.cellbots.eyes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.http.HttpConnection;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.SurfaceHolder.Callback;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.cellbots.R;

/**
 * Remote eyes turns your Android device into a wireless web cam. With Froyo's
 * camera enhancements (ability to manage the preview memory buffer + access to
 * Skia for quickly converting from YUV to JPG), it is possible to capture and
 * upload preview frames fast enough for it to look like video. The behavior is
 * that a new frame will not be captured and processed until the previous frame
 * has been processed and uploaded. This means that if the connection slows
 * down, the video may have brief stutters, but it won't fall behind. See
 * put.php for a simple, working example of what is running server side. Once
 * you have put.php uploaded to your server, be sure to remember to update
 * "putUrl", "server", and "port" in the Remote Eyes code to match your server
 * instead of "myexampleserver.com". See remote_eyes.html for the client HTML
 * page that can be used to see what Remote Eyes is seeing.
 *
 * @author clchen@google.com (Charles L. Chen)
 */

public class EyesActivity extends Activity implements Callback {
    
    public static final String TAG = "EyesActivity"; 
    
    public static final String EYES_COMMAND = "android.intent.action.EYES_COMMAND";

    private SurfaceHolder mHolder;

    private String putUrl = "";

    private String server = "";

    private int port = 80;

    private FrameLayout mFrame;

    private WebView mWebView;

    private SurfaceView mPreview;

    private Camera mCamera;

    private boolean mTorchMode;

    private HttpConnection mConnection;

    private HttpState mHttpState;

    private Rect r;

    private int previewHeight = 0;

    private int previewWidth = 0;

    private int previewFormat = 0;

    private boolean isUploading = false;

    private byte[] mCallbackBuffer;

    private ByteArrayOutputStream out;

    private BroadcastReceiver mReceiver;

    private WakeLock mWakeLock;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.e("remote eyes", "started");
        super.onCreate(savedInstanceState);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE
                        | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Cellbot Eyes");
        mWakeLock.acquire();

        mTorchMode = false;

        out = new ByteArrayOutputStream();

        if ((getIntent() != null) && (getIntent().getData() != null)) {
            putUrl = getIntent().getData().toString();
            server = putUrl.replace("http://", "");
            server = server.substring(0, server.indexOf("/"));
            Bundle extras = getIntent().getExtras();
            if ((extras != null) && (extras.getBoolean("TORCH", false))) {
                mTorchMode = true;
            }
        } else {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            putUrl = prefs.getString("REMOTE_EYES_PUT_URL", "");
            Log.e("prefs", putUrl);
            if (putUrl.length() < 1) {
                Intent i = new Intent();
                i.setClass(this, PrefsActivity.class);
                startActivity(i);
                finish();
                return;
            } else {
                server = putUrl.replace("http://", "");
                server = server.substring(0, server.indexOf("/"));
            }
        }

        resetConnection();
        mHttpState = new HttpState();

        setContentView(R.layout.eyes_main);
        mPreview = (SurfaceView) findViewById(R.id.eyes_preview);
        mHolder = mPreview.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mPreview.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                setTorchMode(!mTorchMode);
            }
        });

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean useTorch = intent.getBooleanExtra("TORCH", false);
                boolean shouldTakePicture = intent.getBooleanExtra("PICTURE", false);
                setTorchMode(useTorch);
                setTakePicture(shouldTakePicture);
            }
        };

        this.registerReceiver(mReceiver, new IntentFilter(EyesActivity.EYES_COMMAND));

        mFrame = (FrameLayout) findViewById(R.id.eyes_frame);
        mWebView = new WebView(this);
        mWebView.getSettings().setJavaScriptEnabled(true);
        // Use this if you want to load content locally
        // mWebView.loadUrl("content://com.cellbot.localpersonas/default/index.html");
        mWebView.loadUrl("http://personabots.appspot.com/expressions/tuby");

        mFrame.addView(mWebView);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mReceiver != null) {
            this.unregisterReceiver(mReceiver);
        }
        if (mWakeLock != null) {
            mWakeLock.release();
        }
    }

    private void resetConnection() {
        //Log.e("server", server);
        try {
            String ip = new URL(putUrl).getHost();
            int port = new URL(putUrl).getPort();
            mConnection = new HttpConnection(ip, port == -1 ? 80 : port);
            mConnection.open();
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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

        try {
            PutMethod put = new PutMethod(putUrl);
            put.setRequestBody(new ByteArrayInputStream(new byte[0]));
            put.execute(mHttpState, mConnection);
        } catch (NoHttpResponseException e) {
            // Silently ignore this.
        } catch (IOException e) {
            e.printStackTrace();
            resetConnection();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        mHolder.setFixedSize(w, h);
        // Start the preview
        Parameters params = mCamera.getParameters();
        previewHeight = params.getPreviewSize().height;
        previewWidth = params.getPreviewSize().width;
        previewFormat = params.getPreviewFormat();

        // Crop the edges of the picture to reduce the image size
        r = new Rect(80, 20, previewWidth - 80, previewHeight - 20);

        mCallbackBuffer = new byte[460800];

        mCamera.setParameters(params);
        mCamera.setPreviewCallbackWithBuffer(new PreviewCallback() {
            public void onPreviewFrame(byte[] imageData, Camera arg1) {
                final byte[] data = imageData;
                if (!isUploading) {
                    if (needToTakePicture) {
                        takePicture(imageData);
                    } else {
                        isUploading = true;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                uploadImage(data);
                            }
                        }).start();
                        // appEngineUploadImage(imageData);
                    }
                }
            }
        });
        mCamera.addCallbackBuffer(mCallbackBuffer);
        mCamera.startPreview();
        setTorchMode(mTorchMode);
    }

    private void uploadImage(byte[] imageData) {
        try {
            YuvImage yuvImage = new YuvImage(
                    imageData, previewFormat, previewWidth, previewHeight, null);
            yuvImage.compressToJpeg(r, 20, out); // Tweak the quality here - 20
            // seems pretty decent for quality + size.
            if (putUrl.contains("127.0.0.1") || putUrl.contains("localhost")) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                resetConnection();
            }
            PutMethod put = new PutMethod(putUrl);
            put.setRequestBody(new ByteArrayInputStream(out.toByteArray()));
            int result = put.execute(mHttpState, mConnection);
            //Log.e("result", result + "");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "UnsupportedEncodingException: Error uploading image: " + e.getMessage());
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException: Error uploading image: " + e.getMessage());
            resetConnection();
        } catch (ClientProtocolException e) {
            Log.e(TAG, "ClientProtocolException: Error uploading image: " + e.getMessage());
            resetConnection();
        } catch (UnknownHostException e) {
            Log.e(TAG, "UnknownHostException: Error uploading image: " + e.getMessage());
            resetConnection();
        } catch (NoHttpResponseException e) {
            // Silently ignore this.
        } catch (IOException e) {
            Log.e(TAG, "IOException: Error uploading image: " + e.getMessage());
            resetConnection();
        }
        finally {
            out.reset();
            if (mCamera != null) {
                mCamera.addCallbackBuffer(mCallbackBuffer);
            }
            isUploading = false;
        }

    }

    HttpClient httpclient = new DefaultHttpClient();

    String postUrl = "http://botczar.appspot.com/abc123/image";

    private void appEngineUploadImage(byte[] imageData) {
        Log.e("app engine remote eyes", "called");
        try {
            YuvImage yuvImage = new YuvImage(
                    imageData, previewFormat, previewWidth, previewHeight, null);
            yuvImage.compressToJpeg(r, 20, out); // Tweak the quality here - 20
            // seems pretty decent for
            // quality + size.
            Log.e("app engine remote eyes", "upload starting");
            HttpPost httpPost = new HttpPost(postUrl);
            Log.e("app engine perf", "0");
            MultipartEntity entity = new MultipartEntity();
            Log.e("app engine perf", "1");
            entity.addPart("img",
                    new InputStreamBody(new ByteArrayInputStream(out.toByteArray()), "video.jpg"));
            Log.e("app engine perf", "2");
            httpPost.setEntity(entity);
            Log.e("app engine perf", "3");
            HttpResponse response = httpclient.execute(httpPost);
            Log.e("app engine remote eyes", "result: " + response.getStatusLine());
            Log.e("app engine remote eyes", "upload complete");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            resetAppEngineConnection();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            resetAppEngineConnection();
        } catch (IOException e) {
            e.printStackTrace();
            resetAppEngineConnection();
        } finally {
            out.reset();
            if (mCamera != null) {
                mCamera.addCallbackBuffer(mCallbackBuffer);
            }
            isUploading = false;
            Log.e("app engine remote eyes", "finished");
        }
    }

    private void resetAppEngineConnection() {
        mConnection = new HttpConnection("http://botczar.appspot.com/", port);
        try {
            mConnection.open();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void setTorchMode(boolean on) {
        if (mCamera != null) {
            Parameters params = mCamera.getParameters();
            if (on) {
                params.setFlashMode(Parameters.FLASH_MODE_TORCH);
            } else {
                params.setFlashMode(Parameters.FLASH_MODE_AUTO);
            }
            mTorchMode = on;
            mCamera.setParameters(params);
        }
    }

    private void setTakePicture(boolean shouldTake) {
        needToTakePicture = shouldTake;
    }

    boolean needToTakePicture = false;

    private void takePicture(byte[] imageData) {
        YuvImage yuvImage = new YuvImage(
                imageData, previewFormat, previewWidth, previewHeight, null);
        yuvImage.compressToJpeg(r, 100, out);
        File dir = new File(Environment.getExternalStorageDirectory() + "/cellbot/pictures");
        dir.mkdirs();
        FileOutputStream outStream;
        try {
            String picName = dir.toString() + "/" + System.currentTimeMillis() + ".jpg";
            outStream = new FileOutputStream(picName);
            outStream.write(out.toByteArray());
            outStream.flush();
            outStream.close();
            Log.e("Picture saved:", picName);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            needToTakePicture = false;
            out.reset();
            if (mCamera != null) {
                mCamera.addCallbackBuffer(mCallbackBuffer);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, R.string.settings, 0, R.string.settings).setIcon(
                android.R.drawable.ic_menu_preferences);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.string.settings:
                intent = new Intent(this, PrefsActivity.class);
                startActivity(intent);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
