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

import android.util.Log;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

/**
 * Implements communication via XMPP. Current limits to GTalk accounts.
 * 
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 *
 */
public class XmppCommChannel extends AbstractCommChannel {
    private static final String TAG = "XmppCommChannel";

    private static final String SERVICE = "gmail.com";

    private String mUsername;

    private String mPassword;
    
    private String mTargetUser;

    private XMPPConnection mConnection;

    private CommMessageListener mMessageListener;

    public XmppCommChannel(String username, String password, String targetUsername,
            CommMessageListener listener, String name) {
        super(name);
        int index = username.indexOf('@');
        mUsername = index >= 0 ? username.substring(0, index) : username;
        index = password.indexOf('@');
        mPassword = index >= 0 ? password.substring(0, index) : password;
        mUsername += "@" + SERVICE;
        mTargetUser = targetUsername;
        mMessageListener = listener;
    }

    @Override
    public void connect() {
        // Create a connection
        mConnection = new XMPPConnection(SERVICE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mConnection.connect();
                    Log.i(TAG, "Connected to " + mConnection.getHost());
                    mConnection.login(mUsername, mPassword);
                    Log.i(TAG, "Logged in as " + mConnection.getUser());
                    // Set the status to available
                    Presence presence = new Presence(Presence.Type.available);
                    mConnection.sendPacket(presence);
                    if (mMessageListener != null) {
                        mMessageListener.onConnected(mChannelName,
                                CommunicationManager.CHANNEL_XMPP);
                    }
                } catch (XMPPException ex) {
                    Log.e(TAG, "Failed to connect/login as " + mUsername);
                    Log.e(TAG, ex.toString());
                    mConnection = null;
                    if (mMessageListener != null) {
                      mMessageListener.onConnectError(mChannelName,
                              CommunicationManager.CHANNEL_XMPP);
                    }
                }
            }
        }).start();
    }

    @Override
    public void disconnect() {
        if (mConnection != null)
            mConnection.disconnect();
        if (mMessageListener != null)
            mMessageListener.onDisconnected(mChannelName, CommunicationManager.CHANNEL_XMPP);
    }

    @Override
    public void sendMessage(String message, String type) {
        send(mTargetUser, message);
    }

    @Override
    public void sendMessage(byte[] message, String type) {
        sendMessage(new String(message), type);
    }

    private void send(String to, String message) {
        if (to == null) {
            return;
        }
        int index = to.indexOf('@');
        to = index >= 0 ? to.substring(0, index) : to;
        if (mConnection == null)
            return;
        Message msg = new Message(to + "@" + SERVICE, Message.Type.chat);
        msg.setBody(message);
        mConnection.sendPacket(msg);
    }

    @Override
    public void setMessageListener(CommMessageListener listener) {
        mMessageListener = listener;
    }

    @Override
    public void setMessageTarget(String target) {
        mTargetUser = target;
    }
    
    @Override
    public void listenForMessages(final long waitTimeBetweenPolling, boolean returnStream) {
        // Add a packet listener to get messages sent to us
        PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
        mConnection.addPacketListener(new PacketListener() {
            public void processPacket(Packet packet) {
                Message message = (Message) packet;
                if (message.getBody() != null && mMessageListener != null) {
                    mMessageListener.onMessage(
                            new CommMessage(message.getBody(), null, "text/text",
                            message.getFrom(), message.getTo(), mChannelName,
                            CommunicationManager.CHANNEL_XMPP));
                }
            }
        }, filter);
    }
}
