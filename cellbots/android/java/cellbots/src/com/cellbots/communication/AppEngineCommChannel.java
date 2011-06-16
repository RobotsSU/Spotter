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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.PostMethod;

/*
These are the original import, which generate errors in HttpConnection
import org.apache.http.HttpConnection;
*/

import android.os.Looper;
import android.util.Log;

/**
 * Implements sending and receiving messages via an AppEngine-based HTTP relay.
 * TODO (chaitanyag): Move App Engine client implementation here.
 * 
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 *
 */
public class AppEngineCommChannel extends AbstractCommChannel {

    private static final String TAG = "AppEngineCommandController";

    private String mHttpCmdUrl;

    private CommMessageListener mMessageListener;

    private boolean stopReading;

    private HttpConnection mConnection;

    private HttpState mHttpState;

    private final int port = 80;

    private String startCmdStr = "\"cmd\":\"";

    public AppEngineCommChannel(String name, String cmdUrl, CommMessageListener listener) {
        super(name);
        mMessageListener = listener;
        mHttpCmdUrl = cmdUrl;
        stopReading = false;
        mHttpState = new HttpState();
    }

    @Override
    public void listenForMessages(long waitTimeBetweenPolling, boolean returnStream) {
        stopReading = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                while (!stopReading) {
                    try {
                        resetConnection();
                        PostMethod post = new PostMethod(mHttpCmdUrl);
                        post.setParameter("msg", "{}");
                        int result = post.execute(mHttpState, mConnection);
                        String response = post.getResponseBodyAsString();
                        int cmdStart = response.indexOf(startCmdStr);
                        if (cmdStart != -1) {
                            String command = response.substring(cmdStart + startCmdStr.length());
                            command = command.substring(0, command.indexOf("\""));
                            Log.e("command", command);
                            mMessageListener.onMessage(new CommMessage(command, null, "text/text",
                                    null, null, mChannelName, CommunicationManager.CHANNEL_GAE));
                        }
                        // Thread.sleep(200);
                    } catch (MalformedURLException e) {
                        Log.e(TAG, "Error processing URL: " + e.getMessage());
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading command from URL: " + mHttpCmdUrl + " : "
                                + e.getMessage());
                    }
                }
            }
        }).start();
    }

    private void resetConnection() {
        if (mHttpCmdUrl == null || mHttpCmdUrl.equals("")) {
            return;
        }
        try {
            mConnection = new HttpConnection(new URL(mHttpCmdUrl).getHost(), port);
            mConnection.open();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disconnect() {
        stopReading = true;
    }

    @Override
    public void connect() {
    }

    @Override
    public void sendMessage(String message, String type) {
    }

    @Override
    public void setMessageListener(CommMessageListener listener) {
        mMessageListener = listener;
    }

    @Override
    public void setMessageTarget(String target) {
        mHttpCmdUrl = target;
    }

    @Override
    public void sendMessage(byte[] message, String type) {
    }

}
