package com.cellbots.cellserv.client;


import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>GreetingService</code>.
 */

public interface WiimoteServiceAsync 
{

  public void handleButtonDown(int buttonid, String botid, AsyncCallback<Integer> callback);
  public void handleButtonUp(int buttonid, String botid, AsyncCallback<Integer> callback);
  
  public void handleTextCommand(String command, String botid, AsyncCallback<Integer> callback);
  
 // public void getPhoneState(AsyncCallback<String> callback);
}
