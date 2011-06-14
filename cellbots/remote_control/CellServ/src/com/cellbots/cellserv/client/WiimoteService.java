package com.cellbots.cellserv.client;

import com.google.gwt.user.client.rpc.RemoteService;

//Add this to the "imports"
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
 
// Add this above the interface declaration
@RemoteServiceRelativePath("wiimote")
 


public interface WiimoteService extends RemoteService
{
  public int handleButtonDown(int buttonid, String botid);
  public int handleButtonUp(int buttonid, String botid);
  public int handleTextCommand(String command, String botid);
  //public String getPhoneState();
  
}
