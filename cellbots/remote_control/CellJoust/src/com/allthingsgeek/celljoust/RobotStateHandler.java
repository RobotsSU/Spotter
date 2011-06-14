package com.allthingsgeek.celljoust;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import com.cellbots.CellbotProtos;
import com.cellbots.CellbotProtos.ControllerState;
import com.cellbots.CellbotProtos.PhoneState;
import com.cellbots.CellbotProtos.PhoneState.Builder;
import com.cellbots.sensors.SensorListenerImpl;
import com.google.protobuf.ByteString;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/*
 * This is a class to store the state of the robot.
 */
public class RobotStateHandler extends Thread
{
  private BTCommThread                          bTcomThread;

  private Handler                               uiHandler;

  private static RobotStateHandler              instance                = null;

  public boolean                                listening               = false;

  public static String                          TAG                     = "RobotStateHandler";

  public static String                          ROBOT_ID                = "Pokey";

  private CellbotProtos.PhoneState              state;

  private Movement                              mover;

  HttpClient                                    httpclient;

  InetSocketAddress                             clientAddress           = null;

  private CellbotProtos.AudioVideoFrame.Builder avFrame;

  ControllerState                               controllerState;

  long                                          lastControllerTimeStamp = 0;

  public Handler                                handler;


  public RobotStateHandler(Handler h)
  {
    uiHandler = h;

    avFrame = CellbotProtos.AudioVideoFrame.newBuilder();
    avFrame.setFrameNumber(0);
    Random generator = new Random(System.currentTimeMillis());
    ROBOT_ID = Integer.toHexString(generator.nextInt()).toUpperCase();

    mover = Movement.getInstance();


  }

  public void onBtDataRecive(String data)
  {
    /*
     * Log.i(TAG, "got bt data:" + data);
     * 
     * //state.blueToothConnected = true; Date date = new Date();
     * //state.lastBtTimestamp = date.getTime();
     * 
     * if(data.startsWith("L")) { //state.message += data; } else {
     * 
     * String[] botData = data.split(" ");
     * 
     * try {
     * 
     * state.botBatteryLevel = Integer.parseInt(botData[0]); state.damage =
     * Integer.parseInt(botData[1]); state.servoSpeed =
     * Integer.parseInt(botData[2]); state.strideOffset =
     * Integer.parseInt(botData[3]); state.turretAzimuth =
     * Integer.parseInt(botData[4]); state.turretElevation =
     * Integer.parseInt(botData[5]); state.sonarDistance =
     * Integer.parseInt(botData[6]); state.irDistance =
     * Integer.parseInt(botData[7]); state.lampOn = Integer.parseInt(botData[8])
     * == 1; state.laserOn = Integer.parseInt(botData[9]) == 1; state.rGunOn =
     * Integer.parseInt(botData[10]) == 1; state.lGunOn =
     * Integer.parseInt(botData[11]) == 1; state.moving =
     * Integer.parseInt(botData[12]) == 1;
     * 
     * } catch(Exception e) { Log.e(TAG, "Error parsing robot data: " + data +
     * " e:",e); } }
     */
  }

  public void onBtDataError()
  {
    /*
     * if(state.blueToothConnected) { Message say = uiHandler.obtainMessage();
     * say.obj = "Danger! Danger! Bluetooth error!"; say.sendToTarget(); }
     * state.blueToothConnected = false;
     */
  }

  @Override
  public void run()
  {
    try
    {
      // preparing a looper on current thread
      // the current thread is being detected implicitly
      Looper.prepare();

      // now, the handler will automatically bind to the
      // Looper that is attached to the current thread
      // You don't need to specify the Looper explicitly
      
      

      handler = new Handler()
      {

        @Override
        public void handleMessage(Message msg)
        {
          // utterTaunt((String) msg.obj);
          if (msg.obj instanceof PhoneState)
          {
            state = (PhoneState) msg.obj;

            try
            {
              
              httpclient = new DefaultHttpClient();
              
              HttpPost post = new HttpPost("http://"+MainActivity.putUrl + "/robotState");
              
              post.setEntity(new ByteArrayEntity(state.toByteArray()));

              HttpResponse resp = httpclient.execute(post);

              HttpEntity ent = resp.getEntity();
              
              if(ent==null)
                return;
              
              InputStream resStream = ent.getContent();

              ControllerState cs = ControllerState.parseFrom(resStream);

              String txt = mover.processControllerStateEvent(cs);

              if (bTcomThread != null && cs != null && cs.getTimestamp() != lastControllerTimeStamp)
              {
                if (cs.hasTxtCommand())
                {
                  lastControllerTimeStamp = cs.getTimestamp();
                  Message btMsg = bTcomThread.handler.obtainMessage();
                  btMsg.obj = cs;
                  btMsg.sendToTarget();
                }
                else if (txt != null)
                {
                  lastControllerTimeStamp = cs.getTimestamp();
                  Message btMsg = bTcomThread.handler.obtainMessage();
                  btMsg.obj = txt;
                  btMsg.sendToTarget();
                }

              }

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
            catch (NullPointerException e)
            {
              Log.e(TAG,"npe",e);
              e.printStackTrace();
            }


          }
        }

      };

      
      Log.i(TAG, "Robot State handler is bound to - " + handler.getLooper().getThread().getName());
      // After the following line the thread will start
      // running the message loop and will not normally
      // exit the loop unless a problem happens or you
      // quit() the looper (see below)
      Looper.loop();

      Log.i(TAG, "Thread exiting gracefully");
    }
    catch (Throwable t)
    {
      Log.e(TAG, "Thread halted due to an error", t);
    }
  }

}
