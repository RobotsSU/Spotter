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

package com.cellbots.local;

import com.cellbots.R;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.NoHttpResponseException;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.os.Environment;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

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

/**
 * This class is a modified version of EyesActivity. This is implemented to
 * avoid starting a new Activity for remote eyes.
 *
 * @author clchen@google.com (Charles L. Chen)
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 */

public class EyesView implements Callback {
    public static final int PERSONA_AFRAID = R.drawable.persona_tuby_afraid;
    public static final int PERSONA_ANGRY = R.drawable.persona_tuby_angry;
    public static final int PERSONA_ERROR = R.drawable.persona_tuby_error;
    public static final int PERSONA_HAPPY = R.drawable.persona_tuby_happy;
    public static final int PERSONA_IDLE = R.drawable.persona_tuby_idle;
    public static final int PERSONA_READY = R.drawable.persona_tuby_ready;
    public static final int PERSONA_SAD = R.drawable.persona_tuby_sad;
    public static final int PERSONA_SURPRISE = R.drawable.persona_tuby_surprise;
    
    public static final String TAG = "EyesActivity"; 
    
    public static final String EYES_COMMAND = "android.intent.action.EYES_COMMAND";
    
    private CellDroidActivity mParent;

    private SurfaceHolder mHolder;

    private String putUrl = "";
    
    private boolean isLocalUrl = false;

    private String server = "";

    private int port = 80;

    private FrameLayout mFrame;
    
    private ImageView mImageView;

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

    public EyesView(CellDroidActivity ct, String url, boolean torch) {
        Log.e("remote eyes", "started " + url);
        mParent = ct;
        putUrl = url;
        
        PowerManager pm = (PowerManager) ct.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE
                        | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Cellbot Eyes");
        mWakeLock.acquire();

        out = new ByteArrayOutputStream();

        if (putUrl != null) {
            isLocalUrl = putUrl.contains("127.0.0.1") || putUrl.contains("localhost");
            server = putUrl.replace("http://", "");
            server = server.substring(0, server.indexOf("/"));
            mTorchMode = torch;
            resetConnection();
            mHttpState = new HttpState();
        }

        ct.setContentView(R.layout.eyes_main);
        mPreview = (SurfaceView) ct.findViewById(R.id.eyes_preview);
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

        ct.registerReceiver(mReceiver, new IntentFilter(EyesView.EYES_COMMAND));

        mFrame = (FrameLayout) ct.findViewById(R.id.eyes_frame);
        mImageView = new ImageView(ct);
        mImageView.setScaleType(ScaleType.FIT_CENTER);
        mImageView.setBackgroundColor(Color.BLACK);
        
        setPersona(PERSONA_READY);
        
        mFrame.addView(mImageView);
    }
    
    public void setPersona(int personaId){
        mImageView.setImageResource(personaId);
    }

    public void hide() {
        try {
            if (mReceiver != null) {
                mParent.unregisterReceiver(mReceiver);
                mReceiver = null;
            }
        } catch (IllegalArgumentException e) {
            // Silently ignore this exception.
        }
        if (mWakeLock != null) {
            mWakeLock.release();
        }
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    private void resetConnection() {
        if (isLocalUrl) return;
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
        // If putUrl is null, it means that only personas overlay was requested.
        if (putUrl == null) {
            return;
        }
        if (isLocalUrl) {
            mParent.setRemoteEyesImage(new byte[0]);
            return;
        }
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
        // If putUrl is null, it means that only personas overlay was requested.
        if (putUrl == null) {
            mCamera.startPreview();
            return;
        }
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
                        takePicture(imageData, false);
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
            if (isLocalUrl) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mParent.setRemoteEyesImage(out.toByteArray());
            } else {
                PutMethod put = new PutMethod(putUrl);
                put.setRequestBody(new ByteArrayInputStream(out.toByteArray()));
                int result = put.execute(mHttpState, mConnection);
            }
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

    public void setTorchMode(boolean on) {
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

    public void setTakePicture(boolean shouldTake) {
        if (putUrl == null) {
            // Take a proper picture if only personas overlay is active, instead
            // of saving a preview frame as a picture when video preview is on.
            takeProperPicture();
        } else {
            needToTakePicture = shouldTake;
        }
    }

    boolean needToTakePicture = false;

    public void takeProperPicture() {
        if (mCamera == null) {
            return;
        }
        Parameters params = mCamera.getParameters();
        params.setFlashMode(Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(params);
        mCamera.takePicture(null, null, new PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                takePicture(data, true);
            }
        });
    }
    
    private void takePicture(byte[] imageData, boolean isJpg) {
        if (!isJpg) {
            YuvImage yuvImage = new YuvImage(
                    imageData, previewFormat, previewWidth, previewHeight, null);
            yuvImage.compressToJpeg(r, 100, out);
        }
        File dir = new File(Environment.getExternalStorageDirectory() + "/cellbots/pictures");
        dir.mkdirs();
        FileOutputStream outStream;
        try {
            String picName = dir.toString() + "/" + System.currentTimeMillis() + ".jpg";
            outStream = new FileOutputStream(picName);
            outStream.write(!isJpg ? out.toByteArray() : imageData);
            outStream.flush();
            outStream.close();
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
}
