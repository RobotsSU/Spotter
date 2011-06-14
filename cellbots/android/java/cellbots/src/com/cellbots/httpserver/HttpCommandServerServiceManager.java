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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.cellbots.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * This class provides an easy-to-use wrapper for the HttpCommandServerService.
 * 
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 *
 */
public class HttpCommandServerServiceManager {

    public static final String EXTERNAL_STORAGE = Environment.getExternalStorageDirectory() + "";
    
    public static final String REL_ROOT_DIR = "cellbots/files";
    
    public static final int PORT = 8080;
    
    public static final String LOCAL_IP = "127.0.0.1";
    
    private static final String TAG = "HttpCommandServerServiceManager";
    
    private IHttpCommandServerService httpServerService = null;
    
    private IRegisterCallbackService registerCallbackService = null;
    
    private Context context;
    
    private HttpRequestListener requestListener = null;
    
    private ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
        
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            httpServerService = IHttpCommandServerService.Stub.asInterface(service);
            try {
                httpServerService.setRoot(REL_ROOT_DIR);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            requestListener.onConnected();
        }
    };

    private ServiceConnection registerCallbackConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
        
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            registerCallbackService = IRegisterCallbackService.Stub.asInterface(service);
            try {
                registerCallbackService.registerCallback(mCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "Error registering callback.");
            }
        }
    };
    
    public static void copyLocalServerFiles(Context ct) {
        String[] fileNames = ct.getResources().getStringArray(R.array.copy_files);
        File dir = new File(EXTERNAL_STORAGE + "/" + REL_ROOT_DIR);
        if (!dir.exists()) dir.mkdirs();
        copyFiles(ct, fileNames, EXTERNAL_STORAGE + "/" + REL_ROOT_DIR);
    }
   
    private static void copyFiles(Context ct, String[] fileNames, String path) {
        for (String name : fileNames) {
            try {
                copyFile(ct.getAssets().open(name), path + "/" + name);
            } catch (IOException e) {
                Log.e(TAG, "Error copying file " + name);
            }
        }
    }
   
    private static void copyFile(InputStream stream, String path) {
        try {
            File file = new File(path);
            if (!file.getParentFile().exists()) return;
            FileOutputStream op = new FileOutputStream(file);
            byte[] data = new byte[1024];
            while (true) {
                int bytesRead = stream.read(data);
                if (bytesRead == -1) break;
                op.write(data, 0, bytesRead);
            }
            stream.close();
        } catch (IOException e) {
            Log.e(TAG, "Error copying Logbot resource file.");
        }
    }
    
    /**
     * Returns the IP address of this device.
     * @return IP address as a String
     */
    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                 en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                     enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("", ex.toString());
        }
        return null;
    }
    
    public HttpCommandServerServiceManager(Context ct, HttpRequestListener listener) {
        context = ct;
        requestListener = listener;
        HttpCommandServerServiceManager.copyLocalServerFiles(ct);
        context.bindService(new Intent(IHttpCommandServerService.class.getName()),
                serviceConn, Context.BIND_AUTO_CREATE);
        context.bindService(new Intent(IRegisterCallbackService.class.getName()),
                registerCallbackConn, Context.BIND_AUTO_CREATE);
    }
    
    public void disconnect() {
        if (httpServerService != null) {
            try {
                httpServerService.stopServer();
            } catch (RemoteException e) {
            	Log.e(TAG, "Error stopping HTTP server.");
            }
        }
        if (registerCallbackService != null) {
            try {
                registerCallbackService.unregisterCallback(mCallback);
            } catch (RemoteException e) {
            	Log.e(TAG, "Error unregistering callback.");
            }
        }
        if (serviceConn != null)
            context.unbindService(serviceConn);
        if (registerCallbackConn != null)
            context.unbindService(registerCallbackConn);
        serviceConn = null;
        registerCallbackConn = null;
    }
    
    public void setResponseByName(String name, byte[] data, String contentType) {
        if (httpServerService != null) {
            try {
                httpServerService.setResponseDataByName(name, data, contentType);
            } catch (RemoteException e) {
            	Log.e(TAG, "Error calling remote method to set response.");
            }
        }        
    }
    
    public void setResponseByName(String name, String resource, String contentType) {
        if (httpServerService != null) {
            try {
                httpServerService.setResponsePathByName(name, resource, contentType);
            } catch (RemoteException e) {
            	Log.e(TAG, "Error calling remote method to set response.");
            }
        }    
    }
    
    public byte[] getResponseByName(String name) {
        if (httpServerService != null) {
            try {
                return httpServerService.getResponseByName(name);
            } catch (RemoteException e) {
                Log.e(TAG, "Error calling remote method to set response.");
            }
        }
        return null;
    }

    public void setRootDir(String root) {
        if (httpServerService != null) {
            try {
                httpServerService.setRoot(root);
            } catch (RemoteException e) {
            	Log.e(TAG, "Error calling remote method to set root directory.");
            }
        }        
    }

    private IHttpCommandServerServiceCallback mCallback =
        new IHttpCommandServerServiceCallback.Stub() {
            @Override
            public void OnRequest(String request, String[] keys, String[] values, byte[] data) {
                Log.d(TAG, "*** Param request received: " + request);
                if (requestListener != null) {
                    requestListener.onRequest(request, keys, values, data);
                }
            }
    };

    /**
     * Implement this interface to receive callbacks when the HTTP server is
     * connected and when PUT/POST requests are received.  
     */
    public interface HttpRequestListener {
        public void onRequest(String req, String[] keys, String[] values, byte[] data);
        public void onConnected();
    }
}
