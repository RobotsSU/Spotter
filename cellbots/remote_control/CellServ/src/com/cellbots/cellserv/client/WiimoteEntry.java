package com.cellbots.cellserv.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.gen2.event.dom.client.LoadEvent;
import com.google.gwt.gen2.event.dom.client.LoadHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LoadListener;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.widgetideas.client.ProgressBar;
import com.google.gwt.widgetideas.graphics.client.Color;
import com.google.gwt.widgetideas.graphics.client.GWTCanvas;

public class WiimoteEntry implements EntryPoint
{

  static String        SENSORSTATE_URL   = "/robotState";

  static String        VIDEO_URL         = "/video";

  public static String BOT_ID            = "";

  final static Label   messageLabel      = new Label("Did you forget ?BOTID=yourbotname");

  final static Label   cmdLabel          = new Label("Type Cmd:");

  final GWTCanvas      canvas            = new GWTCanvas(128, 64);

  final TextArea       debugConsole      = new TextArea();

  int                  framePoleInterval = 200;

  boolean              loadingImg        = false;

  public void dbg(String msg)
  {
    debugConsole.setText(debugConsole.getText() + "\n" + msg);
  }

  @SuppressWarnings("deprecation")
  public void onModuleLoad()
  {

    String path = Window.Location.getPath();
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1);
    SENSORSTATE_URL = path + SENSORSTATE_URL;

    BOT_ID = Window.Location.getParameter("BOTID");

    Window.setTitle(BOT_ID + " Cellserv");
    final WiimoteServiceAsync wiiService = GWT.create(WiimoteService.class);
    final VerticalPanel mainPanel = new VerticalPanel();
    final VerticalPanel controlPanel = new VerticalPanel();
    final HorizontalPanel horizontalPanel = new HorizontalPanel();
    final HorizontalPanel hudPanel = new HorizontalPanel();
    final HorizontalPanel cmdPanel = new HorizontalPanel();

    final TextBox txtCommand = new TextBox();

    // txtCommand.setWidth("30em");
    debugConsole.setWidth("95%");
    debugConsole.setHeight("95%");

    txtCommand.addKeyPressHandler(new AndroidClickHandler(wiiService));

    final Timer elapsedTimer;
    final Timer sensorTimer;

    final Button fwdButton = new Button("FWD");
    fwdButton.addClickHandler(new AndroidClickHandler(wiiService, AndroidKeyCode.KEYCODE_DPAD_UP));
    final Button bkwdButton = new Button("BKWD");
    bkwdButton.addClickHandler(new AndroidClickHandler(wiiService, AndroidKeyCode.KEYCODE_DPAD_DOWN));
    final Button leftButton = new Button("LEFT");
    leftButton.addClickHandler(new AndroidClickHandler(wiiService, AndroidKeyCode.KEYCODE_DPAD_LEFT));
    final Button rightButton = new Button("RIGHT");
    rightButton.addClickHandler(new AndroidClickHandler(wiiService, AndroidKeyCode.KEYCODE_DPAD_RIGHT));
    final Button stopButton = new Button("STOP");
    stopButton.addClickHandler(new AndroidClickHandler(wiiService, AndroidKeyCode.KEYCODE_DPAD_CENTER));

    final Image videoImage = new Image(VIDEO_URL);

    videoImage.addErrorHandler(new ErrorHandler()
    {
      public void onError(ErrorEvent event)
      {
        dbg("could not load video frame");
      }
    });

    videoImage.setUrl("video");

    fwdButton.setWidth("100%");
    bkwdButton.setWidth("100%");

    horizontalPanel.add(leftButton);
    horizontalPanel.add(stopButton);
    horizontalPanel.add(rightButton);

    controlPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
    controlPanel.add(fwdButton);
    controlPanel.add(horizontalPanel);
    controlPanel.add(bkwdButton);
    controlPanel.setHeight("64px");

    hudPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
    hudPanel.setWidth("100%");
    hudPanel.add(controlPanel);

    hudPanel.add(canvas);
    hudPanel.add(debugConsole);
    hudPanel.setCellWidth(debugConsole, "50%");
    hudPanel.setCellWidth(controlPanel, "20%");

    cmdPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
    cmdPanel.setWidth("100%");
    cmdPanel.add(cmdLabel);
    txtCommand.setWidth("32em");
    cmdPanel.add(txtCommand);
    hudPanel.setCellWidth(txtCommand, "60%");
    cmdPanel.add(messageLabel);

    mainPanel.setWidth("600px");
    mainPanel.setBorderWidth(2);
    mainPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    mainPanel.add(videoImage);

    mainPanel.add(hudPanel);
    mainPanel.add(cmdPanel);

    RootPanel.get().add(mainPanel);

    // Create a new timer
    elapsedTimer = new Timer()
    {
      public void run()
      {
        loadingImg = true;
        videoImage.setUrl("video?BOTID=" + BOT_ID + "&ts=" + System.currentTimeMillis());
      }
    };
    
  /*  videoImage.addLoadListener(new LoadListener()
    {

      //do a bit of throttleing 
      public void onLoad(Widget sender)
      {
        // TODO Auto-generated method stub
        if (framePoleInterval > 50)
          framePoleInterval = framePoleInterval - 10;
        loadingImg = false;
        elapsedTimer.scheduleRepeating(framePoleInterval);
      }
      public void onError(Widget sender)
      {
        // TODO Auto-generated method stub
        loadingImg = false;
        if (framePoleInterval < 4000)
          framePoleInterval = framePoleInterval + 10;
        elapsedTimer.scheduleRepeating(framePoleInterval);

      }
    });*/


    // Create a new timer
    sensorTimer = new Timer()
    {
      public void run()
      {
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, SENSORSTATE_URL + "?BOTID=" + BOT_ID);
        try
        {
          builder.setHeader("Content-Type", "application/json");

          builder.sendRequest(null, new RequestCallback()
          {
            public void onError(Request request, Throwable exception)
            {
              dbg("Couldn't retrieve JSON");
            }

            public void onResponseReceived(Request request, Response response)
            {
              if (response.getStatusCode() == 200)
                showPhoneState(PhoneState.parse(response.getText()));
              else
                dbg("Couldn't retrieve JSON");
            }
          });
        }
        catch (RequestException e)
        {
          dbg("Couldn't retrieve JSON");
        }
      }
    };
    elapsedTimer.scheduleRepeating(framePoleInterval);
    sensorTimer.scheduleRepeating(framePoleInterval*2);


    drawCompass(0);
    drawBattery(50);
  }
   
  void drawCompass(double angle)
  {
    double rad = ( Math.PI * 2 * ( ( angle + 180 ) / 360.0 ) );

    canvas.saveContext();
    canvas.translate(64, 32);

    canvas.rotate(rad);
    canvas.setFillStyle(Color.WHITE);
    canvas.fillRect(-32, -32, 64, 64);
    canvas.scale(.75, .75);
 
    canvas.setLineWidth(3);
    canvas.setStrokeStyle(Color.BLACK);
    canvas.beginPath();
    canvas.moveTo(0, 28);
    canvas.lineTo(12, -28);
    canvas.lineTo(0, -18);
    canvas.lineTo(-12, -28);
    canvas.closePath();
    canvas.stroke();
    canvas.restoreContext();
  }

  void drawBattery(double percent)
  {
    canvas.setFillStyle(Color.GREEN);
    canvas.fillRect(2, 4, 12, 60);

    canvas.setFillStyle(Color.WHITE);
    canvas.fillRect(2, 4, 12, ((100-percent)/100.0)*60.0);
    
    canvas.setLineWidth(2);
    canvas.setStrokeStyle(Color.BLACK);
    canvas.strokeRect(2, 4, 12, 60);
  }

  void showPhoneState(PhoneState state)
  {
    if (state.hasOrientation())
    {
      // messageLabel.setText("Azimuth="+state.getOrientation().getAzimuth());
      drawCompass(state.getOrientation().getAzimuth());
    }
    
    if (state.hasPhoneBatteryLevel())
    {
      // messageLabel.setText("Azimuth="+state.getOrientation().getAzimuth());
      drawBattery(state.getPhoneBatteryLevel());
    }
    if (state.hasBotID())
    {
      BOT_ID = state.getBotID();
      Window.setTitle(BOT_ID + " Cellserv");
    }
  }

  static void displayError(String error)
  {
    messageLabel.setText(error);
  }

}
