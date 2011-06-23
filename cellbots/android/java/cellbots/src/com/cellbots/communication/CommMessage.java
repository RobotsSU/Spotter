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

import java.io.InputStream;

/**
 * Class for storing a message.
 * 
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 *
 */
public class CommMessage {
    
    private String mMessage;
    
    private InputStream mMsgStream;
    
    private String mFrom;
    
    private String mTo;
    
    private int mCommChannel;
    
    private String mChannelName;
    
    private String mType;
    
    public CommMessage(String msg, InputStream strm, String type, String from, String to,
            String name, int channel) {
        mMessage = msg;
        mMsgStream = strm;
        mType = type;
        mFrom = from;
        mTo = to;
        mChannelName = name;
        mCommChannel = channel;
    }

    /**
     * @return the mMessage
     */
    public String getMessage() {
        return mMessage;
    }

    /**
     * @param mMessage the mMessage to set
     */
    public void setMessage(String mMessage) {
        this.mMessage = mMessage;
    }

    /**
     * @return the message type
     */
    public String getMessageType() {
        return mType;
    }

    /**
     * @param type the message type
     */
    public void setMessageType(String type) {
        this.mType = type;
    }
    
    /**
     * @return the mFrom
     */
    public String getFrom() {
        return mFrom;
    }

    /**
     * @param mFrom the mFrom to set
     */
    public void setFrom(String mFrom) {
        this.mFrom = mFrom;
    }

    /**
     * @return the mTo
     */
    public String getTo() {
        return mTo;
    }

    /**
     * @param mTo the mTo to set
     */
    public void setTo(String mTo) {
        this.mTo = mTo;
    }

    /**
     * @return the mCommChannel
     */
    public int getCommChannel() {
        return mCommChannel;
    }

    /**
     * @param mCommChannel the mCommChannel to set
     */
    public void setCommChannel(int mCommChannel) {
        this.mCommChannel = mCommChannel;
    }
    
    /**
     * @return the mChannelName
     */
    public String getChannelName() {
        return mChannelName;
    }

    /**
     * @param channelName the channel name to set
     */
    public void setmCommChannel(String channelName) {
        this.mChannelName = channelName;
    }
    
    /**
     * @return the mMsgStream
     */
    public InputStream getMessageInputStream() {
        return mMsgStream;
    }

    /**
     * @param mMsgStream the mMsgStream to set
     */
    public void setMessageInputStream(InputStream mMsgStream) {
        this.mMsgStream = mMsgStream;
    }
}
