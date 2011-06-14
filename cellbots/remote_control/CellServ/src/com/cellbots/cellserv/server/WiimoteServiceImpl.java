package com.cellbots.cellserv.server;

import com.cellbots.CellbotProtos.ControllerState.KeyEvent;
import com.cellbots.cellserv.client.WiimoteService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;


public class WiimoteServiceImpl extends RemoteServiceServlet implements WiimoteService
{

  /**
   * 
   */
  private static final long serialVersionUID = -1101119412732207496L;

  public int handleButtonDown(int buttonid, String botid)
  {
    // TODO Auto-generated method stub
    System.out.println("Got Button Down : " + buttonid);

    KeyEvent.Builder key = KeyEvent.newBuilder();

    key.setKeyDown(true);
    key.setKeyCode("" + buttonid);
    return    StateHolder.getInstance(botid).addKeyEvent(key);
    
  }

  public int handleButtonUp(int buttonid,String botid)
  {
    // TODO Auto-generated method stub
    System.out.println("Got Button Up : " + buttonid);

    KeyEvent.Builder key = KeyEvent.newBuilder();

    key.setKeyUp(true);
    key.setKeyCode("" + buttonid);

    return StateHolder.getInstance(botid).addKeyEvent(key);
  }

  public int handleTextCommand(String command, String botid)
  {
    return StateHolder.getInstance(botid).addKeyTxtCommand(command);
  }
  
 

}
