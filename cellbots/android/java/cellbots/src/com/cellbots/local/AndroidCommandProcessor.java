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

package com.cellbots.local;

import com.cellbots.eyes.EyesActivity;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.util.Log;

/**
 * Processes commands that are targeted at the Android device running on the
 * robot. This handles tasks such as the TextToSpeech, Camera, etc.
 *
 * @author clchen@google.com (Charles L. Chen)
 */
public class AndroidCommandProcessor {

    public interface ResponseListener {
        public void onResponseRequested(String response);
    }

    private Context mParent;

    private TextToSpeech mTts;

    private Compass mCompass;

    private String mRemoteEyesUrl = null;

    private ResponseListener mResponseListener;

    public AndroidCommandProcessor(Context ctx, ResponseListener responseListener) {
        mParent = ctx;
        mResponseListener = responseListener;
        mTts = new TextToSpeech(mParent, null);
        mCompass = new Compass(mParent);
    }

    public void setRemoteEyesUrl(String url) {
        mRemoteEyesUrl = url;
    }

    public String getLocalIpAddress() {
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

    public boolean processCommand(String command) {
        Log.e("processCommand", command);
        // Check if this command is intended for the Android device itself
        if (command.startsWith("speak:")) {
            if (mTts != null) {
                mTts.speak(command.replaceFirst("speak:", ""), 0, null);
            }
            return true;
        } else if (command.startsWith("location")) {
            mResponseListener.onResponseRequested(mCompass.getCurrentHeading());
        }
        return false;
    }

    public void shutdown() {
        if (mTts != null) {
            mTts.shutdown();
        }
        if (mCompass != null) {
            mCompass.shutdown();
        }
    }

}
