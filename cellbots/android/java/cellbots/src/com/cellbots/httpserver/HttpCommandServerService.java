/**
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
package com.cellbots.httpserver;

import com.cellbots.httpserver.HttpCommandServer.HttpCommandServerListener;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * This Android service encapsulates the local HTTP command server. The
 * functionality of this service is exposed to the clients throught he easy-to-
 * use wrapper called {@link HttpCommandServerServiceManager}. Please see
 * {@link HttpCommandServerServiceManager} to understand how to use this
 * service.
 * 
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 *
 */
public class HttpCommandServerService extends Service implements HttpCommandServerListener {

    private static final String TAG = "HttpCommandServerService";
    
    // Default path for looking up files
    private static final String ROOT = "cellbots/httpserver/files";

    private HttpCommandServer httpServer;
    
    private IHttpCommandServerServiceCallback callback;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate() {
        super.onCreate();
        httpServer = new HttpCommandServer(ROOT, HttpCommandServerServiceManager.PORT, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        if (IHttpCommandServerService.class.getName().equals(intent.getAction())) {
            return serviceBinder;
        }
        if (IRegisterCallbackService.class.getName().equals(intent.getAction())) {
            return registerCallbackBinder;
        }
        return null;
    }

    private final IHttpCommandServerService.Stub serviceBinder = new IHttpCommandServerService.Stub() {
        @Override
        public void setResponseDataByName(String name, byte[] data, String contentType) {
            httpServer.addResponseByName(name, data, contentType);
        }

        @Override
        public void setResponsePathByName(String name, String resource, String contentType) {
            httpServer.addResponseByName(name, resource, contentType);
        }

        @Override
        public byte[] getResponseByName(String name) {
            return httpServer.getResponseByName(name);
        }
        
        @Override
        public void setRoot(String root) {
            httpServer.setRoot(root);
        }
        
        @Override
        public void stopServer() {
            httpServer.stopServer();
        }
    };
    
    /**
     * Stub for exposing the service's interface.
     */
    private final IRegisterCallbackService.Stub registerCallbackBinder =
            new IRegisterCallbackService.Stub() {
        public void registerCallback(IHttpCommandServerServiceCallback cb) {
            callback = cb;
        }
        public void unregisterCallback(IHttpCommandServerServiceCallback cb) {
            if (callback == cb)
                callback = null;
        }

    };

    @Override
    public void onRequest(String request, String[] keys, String[] values, byte[] data) {
        if (callback != null) {
            try {
                callback.OnRequest(request, keys, values, data);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
