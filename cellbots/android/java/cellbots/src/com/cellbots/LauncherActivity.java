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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * The initial splash screen that a user sees when starting up the cellbot
 * application.
 *
 * @author css@google.com (Charles Spirakis)
 */
public class LauncherActivity extends Activity {
    private static final String TAG = "CellbotLauncher";

    public static final String PREF_SHOW_SPLASH = "SHOW_SPLASH";

    public static final String PREFERENCES_NAME = "cellbotRobotPreferences";

    private SharedPreferences mRobotPrefs;
    
    private SharedPreferences mGlobalPrefs;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launcher);
        final Activity self = this;

        // See if the user wants this page shown
        mGlobalPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        mRobotPrefs = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        boolean showSplash = mGlobalPrefs.getBoolean(PREF_SHOW_SPLASH, true);
        
        final RobotList robotProfiles = new RobotList(mRobotPrefs);
        robotProfiles.load();
        
        Button continueButton = (Button) findViewById(R.id.launcher_continue);
        continueButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                boolean currentCheck = mGlobalPrefs.getBoolean(PREF_SHOW_SPLASH, true);

                Intent i = new Intent();
                i.setClass(self, com.cellbots.RobotSelectionActivity.class);

                if (robotProfiles.size() > 0) {
                    Log.d(TAG, "Has bots, starting bot list");
                    i.putExtra(RobotSelectionActivity.EXTRA_CREATE_NOW, false);
                } else {
                    Log.d(TAG, "No bots, starting bot create");
                    i.putExtra(RobotSelectionActivity.EXTRA_CREATE_NOW, true);
                }

                startActivity(i);

                // If the user doesn't want the splash screen any longer
                // don't leave it lying around.
                if (!currentCheck) {
                    finish();
                }
            }
        });

        if (!showSplash) {
            Intent i = new Intent();
            i.setClass(self, com.cellbots.RobotSelectionActivity.class);

            if (robotProfiles.size() > 0) {
                Log.d(TAG, "Has bots, starting bot list");
                i.putExtra(RobotSelectionActivity.EXTRA_CREATE_NOW, false);
            } else {
                Log.d(TAG, "No bots, starting bot create");
                i.putExtra(RobotSelectionActivity.EXTRA_CREATE_NOW, true);
            }

            startActivity(i);
            finish();
        }
    }
}
