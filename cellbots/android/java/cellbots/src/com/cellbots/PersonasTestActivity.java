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

package com.cellbots;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.FrameLayout;

/**
 * Test harness for Personas. This avoids using the camera + adds menu options
 * to try out the various states.
 *
 * @author clchen@google.com (Charles L. Chen)
 */
public class PersonasTestActivity extends Activity {

    private FrameLayout mFrame;

    private WebView mWebView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.eyes_main);
        mFrame = (FrameLayout) findViewById(R.id.eyes_frame);
        mWebView = new WebView(this);
        mWebView.getSettings().setJavaScriptEnabled(true);
        // Use this if you want to load content locally
        // mWebView.loadUrl("content://com.cellbot.localpersonas/default/index.html");
        mWebView.loadUrl("http://personabots.appspot.com/expressions/tuby");
        mFrame.addView(mWebView);
    }

}
