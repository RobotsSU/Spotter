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

import com.cellbots.communication.AbstractCommChannel.CommMessageListener;

import java.util.HashMap;

/**
 * This class acts as a easy-to-use wrapper for using and managing different
 * communication channels.
 * 
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 *
 */
public class CommunicationManager {
    
    public static final int CHANNEL_NONE = 0x00;
    
    public static final int CHANNEL_HTTP = 0x01;
    
    public static final int CHANNEL_GAE = 0x01 << 1;
    
    public static final int CHANNEL_XMPP = 0x01 << 2;
    
    public static final int CHANNEL_LOCAL_HTTP = 0x01 << 3;
    
    public static final int CHANNEL_JINGLE = 0x01 << 4;
    
    private HashMap<String, AbstractCommChannel> mChannelMap =
            new HashMap<String, AbstractCommChannel>();
    
    public CommunicationManager() {
        
    }
    
    /**
     * Add a new HTTP relay channel.
     * 
     * @param name Name of the channel.
     * @param receivingUrl Receive message by polling this URL.
     * @param sendingUrl Send messages by PUTing to this URL.
     * @param listener The listener to received callback for this HTTP channel.
     */
    public void addHttpChannel(String name, String receivingUrl, String sendingUrl,
            CommMessageListener listener) {
        mChannelMap.put(name, new CustomHttpCommChannel(receivingUrl, sendingUrl, listener, name));
    }

    /**
     * Add a new XMPP channel.
     * 
     * @param name Name of the channel.
     * @param username Username with which to sign-in.
     * @param passwd Account password. 
     * @param listener The listener to received callback for this XMPP channel.
     */
    public void addXmppCommChannel(String name, String username, String passwd,
            String targetUsername, CommMessageListener listener) {
        mChannelMap.put(name, new XmppCommChannel(username, passwd, targetUsername, listener, name));
    }
    
    /**
     * Add a new AppEngine-based HTTP relay channel.
     * 
     * @param name Name of the channel.
     * @param url The base url for receiving and sending messages. 
     * @param listener The listener to received callback for this XMPP channel.
     */
    public void addAppEngineCommChannel(String name, String url, CommMessageListener listener) {
        mChannelMap.put(name, new AppEngineCommChannel(name, url, listener));
    }

    /**
     * Connect all added channels.
     */
    public void connectAll() {
        for (String channel : mChannelMap.keySet()) {
            mChannelMap.get(channel).connect();
        }
    }
    
    /**
     * Disconnect all channels.
     */
    public void disconnectAll() {
        for (String channel : mChannelMap.keySet()) {
            mChannelMap.get(channel).disconnect();
        }
    }
    
    /**
     * Start listening for messages on the specified channel.
     * 
     * @param channelName Channel name on which to listen for messages.
     * @param pollWaitTime Duration in ms to wait for before polling for the
     *     next message.
     * @param returnStream Whether to return an InputStream for the received
     *     message through the CommMessageListener.onMessage() callback. 
     * @return true if the request channel exists and the request for listening
     *     went through successfully.
     */
    public boolean listenForMessages(String channelName, long pollWaitTime, boolean returnStream) {
        if (!mChannelMap.containsKey(channelName)) return false;
        mChannelMap.get(channelName).listenForMessages(pollWaitTime, returnStream);
        return true;
    }
    
    /**
     * Sends the message over the specified channel.
     * 
     * @param channelName The channel name to send the message via.
     * @param message The message to send.
     * @param type The mime type of the message content, ex: "text/text",
     *     "image/jpg", "text/html", etc.
     * @return true, if the specified channel exists and the request for
     *     sending the message went through successfully.
     */
    public boolean sendMessage(String channelName, String message, String type) {
        if (!mChannelMap.containsKey(channelName)) return false;
        mChannelMap.get(channelName).sendMessage(message, type);
        return true;
    }
}
