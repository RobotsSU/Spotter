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

import com.cellbots.remote.DpadView;
import com.cellbots.remote.UiView;
import com.cellbots.remote.UiView.UiEventListener;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;
import android.widget.LinearLayout;

/**
 * Dump whatever UI layouts here to test how they look since many of these
 * layouts will not render correctly/at all in the Android layout tool.
 *
 * @author clchen@google.com (Charles L. Chen)
 * @author keertip@google.com (Keerti Parthasarathy)
 */
public class UITestActivity extends Activity implements UiEventListener {

    private LinearLayout mControlsLayout;

    private UiView mUiView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remote_main);
        mControlsLayout = (LinearLayout) findViewById(R.id.controls_layout);
        Display disp = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        mUiView = new DpadView(this, this, true, disp.getWidth(), disp.getHeight());
        mControlsLayout.addView(mUiView);
    }

    /*
     * (non-Javadoc)
     * @see com.cellbots.remote.UiView.UiEventListener#onActionRequested(int,
     * java.lang.String)
     */
    @Override
    public void onActionRequested(int action, String values) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * @see com.cellbots.remote.UiView.UiEventListener#onStopRequested()
     */
    @Override
    public void onStopRequested() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * @see
     * com.cellbots.remote.UiView.UiEventListener#onWheelVelocitySetRequested
     * (float, float)
     */
    @Override
    public void onWheelVelocitySetRequested(float direction, float speed) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * @see
     * com.cellbots.remote.UiView.UiEventListener#onSwitchInterfaceRequested
     * (int)
     */
    @Override
    public void onSwitchInterfaceRequested(int interfaceId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPopupWindowRequested() {

    }

}
