/*
 * Celljoust, a cellbot remote control program
 * 
 * Copyright (C) 2010 All Things Geek LLC, portions may be copyright others as noted below
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
 * 
 * Contributors:  darrell@allthingsgeek.com (Darrell Taylor), --author of CellJoust
 *                eric@allthingsgeek.com (Eric Hokanson) --author of ServoBot
 *                clchen@google.com (Charles L. Chen)  --author of RemoteEye
 *                chaitanyag@google.com (Chaitanya Gharpure) --author Celldroid
 *                
 *                
 * Portions of this code are based on RemoteEye 
 * RemoteEye is Apache Licensed and Copyright 2010 Google Inc.                 
 * 
 * Portions of this code are based on Celldroid 
 * Celldroid is Apache Licensed and Copyright 2010 Google Inc.                 
 * 
 */


package com.allthingsgeek.celljoust;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import com.allthingsgeek.celljoust.R;
import com.cellbots.CellbotProtos.AudioVideoFrame;
import com.cellbots.CellbotProtos.PhoneState;
import com.cellbots.sensors.CompassManager;
import com.cellbots.sensors.LightSensorManager;
import com.cellbots.sensors.OrientationManager;
import com.cellbots.sensors.SensorListenerImpl;
import com.google.protobuf.ByteString;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;

public class MainActivity extends Activity implements Callback
{
  public static final String    PREFS_NAME           = "ServoBotPrefsFile";

  private static final String   TAG                  = "CellJoust";

  PulseGenerator                noise;

  Movement                      mover;

  private SurfaceHolder         mHolder;

  public static String          putUrl               = "";

  private SurfaceView           mPreview;

  private Camera                mCamera;

  private boolean               mTorchMode;

  // private HttpState mHttpState;

  private Rect                  r;

  private int                   previewHeight        = 0;

  private int                   previewWidth         = 0;

  private int                   previewFormat        = 0;
  
  //how much to crop the edges
  private int                   previewShrink        = 0;
  

  private int                   jpegCompressionLevel = 20;

  private byte[]                mCallbackBuffer;

  byte[]                        buff;

  private ByteArrayOutputStream out;

  private ConversionWorker      convWorker;

  public static SensorManager   sensorManager;

  RobotStateHandler             state;

  WifiManager wifiManager;
  
  SensorListenerImpl            sensorListener;

  public boolean sendVideoFrames = true;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ServoOn");
    // wl.acquire();
    // wl.release();

    wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
    WifiInfo wifiInfo = wifiManager.getConnectionInfo();

    noise = PulseGenerator.getInstance();
    mover = Movement.getInstance();

    loadPrefs();

    mTorchMode = false;

    out = new ByteArrayOutputStream();

    setContentView(R.layout.main);

    if (sensorManager == null)
    {
      sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
    }

    startListening();

    mPreview = (SurfaceView) findViewById(R.id.preview);
    mHolder = mPreview.getHolder();
    mHolder.addCallback(this);
    mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    noise.pause();

  }

  private void loadPrefs()
  {
    // Restore preferences
    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    putUrl = settings.getString("REMOTE_EYES_PUT_URL", "http://example.com:8080/cellserv");
    noise.setOffsetPulsePercent(settings.getInt("servo1Percent", 50), 0);
    noise.setOffsetPulsePercent(settings.getInt("servo2Percent", 50), 1);
    noise.setOffsetPulsePercent(settings.getInt("servo3Percent", 50), 2);
    noise.setOffsetPulsePercent(settings.getInt("servo4Percent", 50), 3);
    mover.setOffset(settings.getInt("wheelOffset", 0));
    RobotStateHandler.ROBOT_ID = settings.getString("ROBOT_ID", RobotStateHandler.ROBOT_ID);

  }

  @Override
  public void onResume()
  {
    loadPrefs();
    super.onResume();
  }

  private Handler handler = new Handler()
                          {

                            @Override
                            public void handleMessage(Message msg)
                            {
                              // utterTaunt((String) msg.obj);
                              if (msg.obj instanceof PhoneState)
                              {
                                // this.state = msg.obj;
                              }
                            }

                          };


  private synchronized void startListening()
  {
    Log.d(TAG, "startListening called");

    convWorker = new ConversionWorker();

    if (state == null)
    {
      state = new RobotStateHandler(handler);
      state.start();
      while(state.handler == null)
      {
        try
        {
          Thread.sleep(10);
        }
        catch (InterruptedException e)
        {
        }
      }
      
    }

    if (sensorListener == null)
    {
      sensorListener = new SensorListenerImpl(state.handler,wifiManager);
    }

    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    RobotStateHandler.ROBOT_ID = settings.getString("ROBOT_ID", RobotStateHandler.ROBOT_ID);

    // Toast.makeText(CONTEXT, "Current IP:" + state.getLocalIpAddress(),
    // Toast.LENGTH_LONG);
    // ProgressDialog.show(me, msg,
    // "Searching for a Bluetooth serial port...");

    ProgressDialog btDialog = null;

    String connectivity_context = Context.WIFI_SERVICE;
    WifiManager wifi = (WifiManager) getSystemService(connectivity_context);

    this.registerReceiver(sensorListener.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

    this.registerReceiver(sensorListener.mWifiInfoReceiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));

    if (OrientationManager.isSupported())
    {
      OrientationManager.startListening(sensorListener);
    }

    if (LightSensorManager.isSupported())
    {
      LightSensorManager.startListening(sensorListener);
    }

    if (CompassManager.isSupported())
    {
      CompassManager.startListening(sensorListener);
    }

  }

  private synchronized void stopListening()
  {

    Log.d(TAG, "stopListening called");

    convWorker.kill();

    try
    {
      this.unregisterReceiver(sensorListener.mBatInfoReceiver);
    }
    catch (Exception e)
    {
    }

    try
    {
      this.unregisterReceiver(sensorListener.mWifiInfoReceiver);
    }
    catch (Exception e)
    {
    }
    
    OrientationManager.stopListening();
    LightSensorManager.stopListening();
    CompassManager.stopListening();

    // if (state.isAlive())
    // {
    // state.stopListening();
    // }

  }

  protected void onDestroy()
  {
    //noise.stop();
    super.onDestroy();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    // Handle item selection
    switch (item.getItemId())
    {
      case R.id.setup:
        Intent i = new Intent(this, SetupActivity.class);
        startActivity(i);
        break;
      case R.id.quit:
        finish();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
    return true;
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event)
  {
    return mover.processKeyDownEvent(keyCode) != null;
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event)
  {
    return mover.processKeyUpEvent(keyCode);
  }

  public void surfaceCreated(SurfaceHolder holder)
  {
    mCamera = Camera.open();
    try
    {
      mCamera.setPreviewDisplay(holder);
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }

  public void surfaceDestroyed(SurfaceHolder holder)
  {
    mCamera.stopPreview();
    mCamera.release();
    mCamera = null;
    mCamera = null;
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int w, int h)
  {
    mHolder.setFixedSize(w, h);
    // Start the preview
    Parameters params = mCamera.getParameters();
    previewHeight = params.getPreviewSize().height;
    previewWidth = params.getPreviewSize().width;
    previewFormat = params.getPreviewFormat();

    // Crop the edges of the picture to reduce the image size
    r = new Rect(previewShrink, previewShrink, previewWidth - previewShrink, previewHeight - previewShrink);

    mCallbackBuffer = new byte[497664];

    mCamera.setParameters(params);
    mCamera.setPreviewCallbackWithBuffer(new PreviewCallback()
    {
      public void onPreviewFrame(byte[] imageData, Camera arg1)
      {
        convWorker.nextFrame(imageData);
      }
    });
    mCamera.addCallbackBuffer(mCallbackBuffer);
    mCamera.startPreview();
    setTorchMode(mTorchMode);
  }

  private void setTorchMode(boolean on)
  {
    if (mCamera != null)
    {
      Parameters params = mCamera.getParameters();
      if (on)
      {
        params.setFlashMode(Parameters.FLASH_MODE_TORCH);
      }
      else
      {
        params.setFlashMode(Parameters.FLASH_MODE_AUTO);
      }
      mTorchMode = on;
      mCamera.setParameters(params);
    }
  }

  class ConversionWorker extends Thread
  {

    // private HttpConnection mConnection;

    HttpClient httpclient;

    boolean    alive;

    volatile HttpPost   post;

    volatile boolean    sending = false;

    public ConversionWorker()
    {
      // setDaemon(true);

      // this client should automatically reuse its connection
      
      httpclient = new DefaultHttpClient();
      alive = true;

      start();
    }

    public void kill()
    {
      alive = false;
      this.notify();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public synchronized void run()
    {
      try
      {
        wait();// wait for initial frame
      }
      catch (InterruptedException e)
      {
      }
      while (alive && sendVideoFrames)
      {

        try
        {
          httpclient = new DefaultHttpClient();
          sending = true;
          YuvImage yuvImage = new YuvImage(mCallbackBuffer, previewFormat, previewWidth, previewHeight, null);
          yuvImage.compressToJpeg(r, jpegCompressionLevel, out); // Tweak the
                                                                 // quality here

          // state.setVideoFrame(ByteString.copyFrom(out.toByteArray()));

          AudioVideoFrame.Builder avFrame = AudioVideoFrame.newBuilder();

          avFrame.setData(ByteString.copyFrom(out.toByteArray()));
          avFrame.setBotID(RobotStateHandler.ROBOT_ID);
          avFrame.setCompressionLevel(jpegCompressionLevel);
          
          //FIXME:  need to be able to change url
          post = new HttpPost("http://"+putUrl + "/video");

          post.setEntity(new ByteArrayEntity(avFrame.build().toByteArray()));

          // Log.i(TAG, "sending video");
          
          httpclient.execute(post);
          
          sending = false;
          // Log.i(TAG, "sent video");

          // InputStream resStream = resp.getEntity().getContent();

          // ControllerState cs = ControllerState.parseFrom(resStream);

          // mover.processControllerStateEvent(cs);
        }
        catch (UnsupportedEncodingException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();

        }
        catch (IllegalStateException e)
        {
          e.printStackTrace();
        }
        catch (com.google.protobuf.InvalidProtocolBufferException e)
        {
          // e.printStackTrace();
          // resetConnection();
        }
        catch (IOException e)
        {
          e.printStackTrace();
        }
        finally
        {
          out.reset();
          if (mCamera != null)
          {
            mCamera.addCallbackBuffer(mCallbackBuffer);
          }
          sending = false;
          // isUploading = false;
        }

        try
        {
          wait();// wait for next frame
        }
        catch (InterruptedException e)
        {
        }
      }
    }

    synchronized boolean nextFrame(byte[] frame)
    {
      if (this.getState() == Thread.State.WAITING && !sending)
      {
        // ok, we are ready for a new frame:
        // curFrame = frame;
        // do the work:
        this.notify();
        return true;
      }
      else
      {
        // ignore it
        return false;

      }

    }
  }

}
