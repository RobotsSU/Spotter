package com.cellbots.cellserv.server;

import java.util.HashMap;

import com.cellbots.CellbotProtos;

public class StateHolder
{

  private CellbotProtos.PhoneState              phoneState;

  private CellbotProtos.ControllerState         controllerState;

  private CellbotProtos.AudioVideoFrame         avFrame;
  
  private static boolean MERGE_PHONE_STATE     = true;

  private CellbotProtos.ControllerState.Builder csBuilder = CellbotProtos.ControllerState.newBuilder();
  
  private static HashMap<String, StateHolder> instances = new HashMap<String, StateHolder>();

  // private MemcacheService phoneStates =
  // MemcacheServiceFactory.getMemcacheService();

  private StateHolder()
  {
  }

  public static StateHolder getInstance(String botID)
  {

    if (! instances.containsKey(botID))
    {
      instances.put(botID, new StateHolder());
    }
    return instances.get(botID);
  }

  public void setPhoneState(CellbotProtos.PhoneState ps)
  {
    
    if(phoneState != null && MERGE_PHONE_STATE )
    {
      //we merge sensor data because it may not come in that often.
      phoneState = CellbotProtos.PhoneState.newBuilder(phoneState).mergeFrom(ps).build();
    }
    else {
      phoneState = ps;
    }
  }

  public void setVideoFrame(CellbotProtos.AudioVideoFrame av)
  {
    avFrame = av;
  }

  public CellbotProtos.PhoneState getPhoneState()
  {
    return phoneState;
  }

  public CellbotProtos.ControllerState getControllerState()
  {
    controllerState = csBuilder.build();
    csBuilder = null;
    return controllerState;
  }

  public boolean newVideoFrameAvilble()
  {
    return avFrame != null;// && instance.avFrame.getTimestamp() !=
  }

  public boolean newPhoneStateAvilble()
  {
    return phoneState != null;// && instance.phoneState.getTimestamp()
  }

  public byte[] getVideoFrame()
  {
    if (avFrame != null && avFrame.hasData())
      return avFrame.getData().toByteArray();
    else
      return null;
  }

  public boolean newControllerStateAvailble()
  {
    return csBuilder != null && csBuilder.getKeyEventCount() > 0;
  }

  public int addKeyEvent(com.cellbots.CellbotProtos.ControllerState.KeyEvent.Builder key)
  {
    if (csBuilder == null)
      csBuilder = CellbotProtos.ControllerState.newBuilder();

    csBuilder.setTimestamp(System.currentTimeMillis());
    csBuilder.addKeyEvent(key);
    return csBuilder.getKeyEventCount();
  }
  
  public int addKeyTxtCommand(String command)
  {
    csBuilder.setTxtCommand(command);
    return 1;
  }
}
