package com.cellbots.sensors;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import com.allthingsgeek.celljoust.RobotStateHandler;
import com.cellbots.CellbotProtos;
import com.cellbots.CellbotProtos.PhoneState;
import com.cellbots.CellbotProtos.PhoneState.WIFI.Builder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;

public class SensorListenerImpl implements SensorListener
{
  private CellbotProtos.PhoneState.Builder state;

  private WifiManager                      wifi;

  private long                             lastTimetamp  = 0;

  private int                              maxUpdateRate = 100;

  private Handler                          stateReciver;

  // public SensorManager getSensorManager();

  public SensorListenerImpl(Handler h, WifiManager w)
  {
    stateReciver = h;
    state = CellbotProtos.PhoneState.newBuilder();
    wifi =w;
  }

  public void onBottomUp()
  {
    // Toast.makeText(this, "Bottom UP", 1000).show();
  }

  public void onLeftUp()
  {
    // Toast.makeText(this, "Left UP", 1000).show();
  }

  public void onRightUp()
  {
    // / Toast.makeText(this, "Right UP", 1000).show();
  }

  public void onTopUp()
  {
    // / Toast.makeText(this, "Top UP", 1000).show();
  }

  /**
   * onShake callback
   */
  public void onShake(float force)
  {
    // Toast.makeText(this, "Phone shaked : " + force, 1000).show();
  }

  /**
   * onAccelerationChanged callback
   */
  public void onAccelerationChanged(float x, float y, float z)
  {

    CellbotProtos.PhoneState.Accelerometer.Builder b = CellbotProtos.PhoneState.Accelerometer.newBuilder();

    b.setX(x);
    b.setY(y);
    b.setZ(z);
    state.setAccelerometer(b);

    sendPhoneState();
  }

  /**
   * onCompassChanged callback
   */
  public void onCompassChanged(float x, float y, float z)
  {
    CellbotProtos.PhoneState.Compass.Builder b = CellbotProtos.PhoneState.Compass.newBuilder();

    b.setX(x);
    b.setY(y);
    b.setZ(z);
    state.setCompass(b);
    sendPhoneState();
  }

  public void onOrientationChanged(float azimuth, float pitch, float roll)
  {
    CellbotProtos.PhoneState.Orientation.Builder b = CellbotProtos.PhoneState.Orientation.newBuilder();

    b.setAzimuth(azimuth);
    b.setPitch(pitch);
    b.setRoll(roll);

    state.setOrientation(b);
    sendPhoneState();

  }

  public BroadcastReceiver mBatInfoReceiver  = new BroadcastReceiver()
                                             {
                                               public void onReceive(Context arg0, Intent intent)
                                               {
                                                 state.setPhoneBatteryLevel(intent.getIntExtra("level", 0));
                                                 state.setPhoneBatteryTemp(intent.getIntExtra("temperature", 0));
                                                 sendPhoneState();
                                               }
                                             };

  public BroadcastReceiver mWifiInfoReceiver = new BroadcastReceiver()
                                             {

                                               @Override
                                               public void onReceive(Context context, Intent intent)
                                               {
                                                 WifiInfo info = wifi.getConnectionInfo();
                                                 Builder ws = CellbotProtos.PhoneState.WIFI.newBuilder();

                                                 ws.setStrength(info.getRssi());
                                                 ws.setKbps(info.getLinkSpeed());

                                                 ws.setIp(info.getIpAddress());
                                                 // ws.setSsid(info.getBSSID());

                                                 // state.setWifiStrength();

                                                 // state.setWifiSpeed(info.getLinkSpeed());

                                                 sendPhoneState();
                                               }

                                             };

  public void onLightLevelChanged(float level)
  {
    state.setLightLevel(level);
    sendPhoneState();
  }

  private synchronized void sendPhoneState()
  {

    long now = System.currentTimeMillis();
    if (now - lastTimetamp > maxUpdateRate)
    {
      lastTimetamp = now;
    }
    state.setTimestamp(now);
    state.setBotID(RobotStateHandler.ROBOT_ID);
    PhoneState ps = state.build();

    stateReciver.obtainMessage(0, ps).sendToTarget();
    state = CellbotProtos.PhoneState.newBuilder(ps);

  }

}
