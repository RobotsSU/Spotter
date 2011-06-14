/*
 * Copyright (C) 2011 Google Inc.
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

package com.cellbots.logger;

import android.content.Context;
import android.media.MediaRecorder;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * View that handles the video recording functionality. Parts of this code were
 * takenfrommemofy'stutorialat:http://memofy.com/memofy/show/2008c618f15fc61801ca038cbfe138/how-to-use-mediarecorder-in-android
 *
 * @author clchen@google.com (Charles L. Chen)
 */
public abstract class AbstractCamcorderPreview extends SurfaceView implements
        SurfaceHolder.Callback, MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {

    public AbstractCamcorderPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public abstract void initializeRecording();

    public abstract void releaseRecorder() throws IOException;

    public abstract void startRecording();

    public abstract void stopRecording();
}
