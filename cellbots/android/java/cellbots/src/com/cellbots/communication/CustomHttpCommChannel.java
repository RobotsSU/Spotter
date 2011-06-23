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
package com.cellbots.communication;

import android.os.Looper;
import android.util.Log;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.http.client.ClientProtocolException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * This class sends and reads text from the specified URLs.
 *
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 * @author clchen@google.com (Charles L. Chen)
 */
public class CustomHttpCommChannel extends AbstractCommChannel {
    
    private static final String TAG = "CustomHttp";

    private String inUrl;

    private String outUrl;

    private CommMessageListener mMessageListener;

    private String prevCmd;

    private boolean stopReading;
    
    private boolean doDisconnect = false;
    
    private boolean errorConnecting = false;

    private URL commandUrl;

    private SendHttpTask mSendHttpTask;
    
    private Thread listenThread;

    public CustomHttpCommChannel(String receivingUrl, String sendingUrl,
            CommMessageListener listener, String name) {
        super(name);
        mMessageListener = listener;
        inUrl = receivingUrl;
        outUrl = sendingUrl;
        stopReading = false;
        if (sendingUrl != null) {
            new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Looper.prepare();
                            mSendHttpTask = new SendHttpTask(outUrl);
                        }
                    }).start();
        }
    }

    @Override
    public void connect() {
        new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Wait while connection is not done for sending
                        // messages. No need to wait if no outUrl is specified.
                        while ((mSendHttpTask == null || !mSendHttpTask.isReady()) && 
                               !errorConnecting && outUrl != null) {
                            try {
                              Thread.sleep(100);
                            } catch (InterruptedException e) {
                              e.printStackTrace();
                            }
                        }
                        // Call only if the handler has not already requested
                        // disconnecting this channel.
                        if (mMessageListener != null && !doDisconnect) {
                          if (!errorConnecting) {
                              mMessageListener.onConnected(mChannelName,
                                      CommunicationManager.CHANNEL_HTTP);
                          } else {
                              mMessageListener.onConnectError(mChannelName,
                                  CommunicationManager.CHANNEL_HTTP);
                          }
                        }
                    }
                }).start();
    }
    
    @Override
    public void listenForMessages(final long waitTimeBetweenPolling, final boolean returnStream) {
        if (inUrl == null || doDisconnect) return;
        stopReading = false;
        listenThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                while (!stopReading) {
                    try {
                        if (waitTimeBetweenPolling >= 0)
                            Thread.sleep(waitTimeBetweenPolling);
                        if (commandUrl == null) {
                            commandUrl = new URL(inUrl);
                        }
                        URLConnection cn = commandUrl.openConnection();
                        cn.connect();
                        if (returnStream) {
                            if (mMessageListener != null) {
                                mMessageListener.onMessage(new CommMessage(null,
                                        cn.getInputStream(), null, null, null, mChannelName,
                                        CommunicationManager.CHANNEL_HTTP));
                            }
                        } else {
                            BufferedReader rd = new BufferedReader(
                                    new InputStreamReader(cn.getInputStream()), 1024);
                            String cmd = rd.readLine();
                            if (cmd != null && !cmd.equals(prevCmd) && mMessageListener != null) {
                                prevCmd = cmd;
                                // TODO (chaitanyag): Change this after we come
                                // up with a better protocol. The first (space
                                // separated) token in the command string could
                                // be a timestamp. This is useful if the same
                                // commands are sent back to back. For example,
                                // the controller sends consecutive "hu"
                                // (head up) commands to tilt the head up in
                                // small increments.
                                if (cmd.indexOf(' ') >= 0) {
                                    try {
                                        Long.parseLong(cmd.substring(0, cmd.indexOf(' ')));
                                        cmd = cmd.substring(cmd.indexOf(' ') + 1);
                                    } catch (NumberFormatException e) {
                                    }
                                }
                                mMessageListener.onMessage(new CommMessage(cmd, null, null, null,
                                        null, mChannelName, CommunicationManager.CHANNEL_HTTP));
                            }
                        }
                        if (waitTimeBetweenPolling < 0) {   // Do not repeat this loop
                            break;
                        }
                    } catch (MalformedURLException e) {
                        Log.e(TAG, "Error processing URL: " + e.getMessage());
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading command from URL: " + commandUrl + " : "
                                + e.getMessage());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        listenThread.start();
    }

    @Override
    public void disconnect() {
        doDisconnect = true;
        stopReading = true;
        if (listenThread != null)
            listenThread.interrupt();
        if (mMessageListener != null)
            mMessageListener.onDisconnected(mChannelName, CommunicationManager.CHANNEL_HTTP);
    }

    @Override
    public void sendMessage(String message, String type) {
        if (errorConnecting) {
            if (mMessageListener != null)
                mMessageListener.onConnectError(mChannelName, CommunicationManager.CHANNEL_HTTP);
            return;
        }
        if (outUrl != null && mSendHttpTask.isReady()) {
            mSendHttpTask = new SendHttpTask(outUrl);
            mSendHttpTask.execute(System.currentTimeMillis() + " " + message);
        }
    }

    @Override
    public void sendMessage(byte[] message, String type) {
        sendMessage(new String(message), type);
    }

    private class SendHttpTask extends UserTask<String, Void, Void> {

        private HttpConnection mConnection;

        private HttpState mHttpState;

        private final int mPort = 80;

        private boolean isReady = false;

        private String mPutUrl = "";

        public SendHttpTask(String putUrl) {
            super();
            mPutUrl = putUrl;
            mHttpState = new HttpState();
            errorConnecting = !resetConnection();
        }

        @Override
        @SuppressWarnings("unchecked")
        public Void doInBackground(String... params) {
            if (mConnection == null) {
                errorConnecting = true;
                return null;
            }
            if (params == null || params.length == 0 || mPutUrl == null || mPutUrl.equals(""))
                return null;
            isReady = false;
            try {
                PutMethod put = new PutMethod(mPutUrl);
                put.setRequestBody(new ByteArrayInputStream(params[0].getBytes()));
                put.execute(mHttpState, mConnection);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                resetConnection();
            } catch (IOException e) {
                e.printStackTrace();
                resetConnection();
            } finally {
                isReady = true;
                if (mConnection != null)
                    mConnection.close();
            }
            return null;
        }

        public boolean isReady() {
            return isReady;
        }

        private boolean resetConnection() {
            if (mPutUrl == null || mPutUrl.equals(""))
                return true;
            try {
                int port = new URL(mPutUrl).getPort();
                mConnection = new HttpConnection(new URL(mPutUrl).getHost(),
                        port == -1 ? mPort : port);
                mConnection.open();
                isReady = true;
                return true;
            } catch (MalformedURLException e) {
                Log.e(TAG, "Cannot initiate connection - Malformed URL.");
                mConnection = null;
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(TAG, "Error initiating connection.");
                mConnection = null;
                e.printStackTrace();
            }
            return false;
        }
    }
    
    @Override
    public void setMessageListener(CommMessageListener listener) {
        mMessageListener = listener;
    }

    @Override
    public void setMessageTarget(String target) {
        outUrl = target;
    }
}
