package com.cellbots.cellserv.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;

public class AndroidClickHandler implements ChangeHandler, MouseUpHandler, MouseDownHandler, ClickHandler, KeyPressHandler
{

  WiimoteServiceAsync wiiService;

  private int         keyCode;

  public AndroidClickHandler(WiimoteServiceAsync service, int code)
  {
    wiiService = service;
    keyCode = code;
  }

  public AndroidClickHandler(WiimoteServiceAsync service)
  {
    wiiService = service;
  }

  public void onClick(ClickEvent event)
  {
    wiiService.handleButtonDown(keyCode, WiimoteEntry.BOT_ID, new AsyncCallback<Integer>()
    {
      public void onFailure(Throwable caught)
      {
        GWT.log(caught.getMessage());
      }

      public void onSuccess(Integer result)
      {

      }
    });

  }

  public void onMouseUp(MouseUpEvent event)
  {
    wiiService.handleButtonDown(keyCode, WiimoteEntry.BOT_ID, new AsyncCallback<Integer>()
    {
      public void onFailure(Throwable caught)
      {
        GWT.log(caught.getMessage());
      }

      public void onSuccess(Integer result)
      {

      }
    });

  }

  public void onMouseDown(MouseDownEvent event)
  {
    wiiService.handleButtonDown(keyCode, WiimoteEntry.BOT_ID, new AsyncCallback<Integer>()
    {
      public void onFailure(Throwable caught)
      {
        GWT.log(caught.getMessage());
      }

      public void onSuccess(Integer result)
      {

      }
    });

  }

  public void onChange(ChangeEvent event)
  {

  }

  public void onKeyPress(KeyPressEvent event)
  {
    final TextBox t = (TextBox) event.getSource();
    event.stopPropagation();
    if (event.getCharCode() == '\n' || event.getCharCode() == '\r')
      wiiService.handleTextCommand(t.getValue(), WiimoteEntry.BOT_ID, new AsyncCallback<Integer>()
      {
        public void onFailure(Throwable caught)
        {
          GWT.log(caught.getMessage());
        }

        public void onSuccess(Integer result)
        {
          t.setText("");
        }
      });

  }
}
