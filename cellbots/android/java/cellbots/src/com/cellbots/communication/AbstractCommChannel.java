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

/**
 * Abstract class for a communication channel for sending and receiving
 * messages. Extend this class to implement a channel based on communication
 * method of choice, ex: HTTP, XMPP, Telnet, libjingle, etc.
 * 
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 *
 */
public abstract class AbstractCommChannel {
    
    protected String mChannelName;
    
    /**
     * Create new channel with the specified name;
     * @param name The channel name.
     */
    public AbstractCommChannel(String name) {
        mChannelName = name;
    }
    
    /**
     * Returns the name of this communication channel.
     * @return returns the channel name.
     */
    public String getChannelName() {
        return mChannelName;
    }
    
    /**
     * Sets the name of this communication channel.
     * @param name
     */
    public void setChannelName(String name) {
        mChannelName = name;
    }

    /**
     * Initializes and connects this channel so that it is ready to send and
     * receive message.
     */
    public abstract void connect();
    
    /**
     * Disconnects and tears down this channel. Call this method in your app's
     * onDestroy() or while exiting the app.
     */
    public abstract void disconnect();
    
    /**
     * Sets the listener for getting callbacks when messages are received on
     * this channel.
     * 
     * @param listener The message listener.
     */
    public abstract void setMessageListener(CommMessageListener listener);
    
    /**
     * Starts listening for messages on this channel.
     * 
     * @param waitTimeBetweenPolling Duration in ms for which to wait before
     *     polling for a new message.
     * @param returnStream Whether to return an InputStream for the received
     *     message through the CommMessageListener.onMessage() callback.
     */
    public abstract void listenForMessages(final long waitTimeBetweenPolling,
            final boolean returnStream);
    
    /**
     * Sets the target to send messages to. See implementations specific
     * documentation to know what the target means in that context.
     * 
     * @param target
     */
    public abstract void setMessageTarget(String target);
    
    /**
     * Sends the specified message string over this channel.
     * 
     * @param message
     * @param type
     */
    public abstract void sendMessage(String message, String type);
    
    /**
     * Sends the specified message byte array over this channel.
     * 
     * @param message
     * @param type
     */
    public abstract void sendMessage(byte[] message, String type);

    /**
     * Implement this interface to receive callbacks from this channel.
     * 
     * @author chaitanyag@google.com (Chaitanya Gharpure)
     *
     */
    public interface CommMessageListener {
        public void onMessage(CommMessage msg);
        
        public void onConnectError(String channelName, int channel);

        public void onConnected(String channelName, int channel);

        public void onDisconnected(String channelName, int channel);

    }
}
