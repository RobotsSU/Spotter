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

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * A simple Activity for choosing which mode to launch the data logger in.
 *
 * @author clchen@google.com (Charles L. Chen)
 */
public class LauncherActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        final Activity self = this;
        Button launchVideoFrontButton = (Button) findViewById(R.id.launchVideoFront);
        launchVideoFrontButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(self, LoggerActivity.class);
                i.putExtra(LoggerActivity.EXTRA_MODE, LoggerActivity.MODE_VIDEO_FRONT);
                startActivity(i);
                finish();
            }
        });
        Button launchVideoBackButton = (Button) findViewById(R.id.launchVideoBack);
        launchVideoBackButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(self, LoggerActivity.class);
                i.putExtra(LoggerActivity.EXTRA_MODE, LoggerActivity.MODE_VIDEO_BACK);
                startActivity(i);
                finish();
            }
        });
        final EditText pictureDelayEditText = (EditText) findViewById(R.id.pictureDelay);
        Button launchPictureButton = (Button) findViewById(R.id.launchPicture);
        launchPictureButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(self, LoggerActivity.class);
                i.putExtra(LoggerActivity.EXTRA_MODE, LoggerActivity.MODE_PICTURES);
                int delay = 30;
                try {
                    delay = Integer.parseInt(pictureDelayEditText.getText().toString());
                } catch (Exception e) {
                    Toast.makeText(self,
                            "Error parsing picture delay time. Using default delay of 30 seconds.",
                            1).show();
                }
                i.putExtra(LoggerActivity.EXTRA_PICTURE_DELAY, delay);
                startActivity(i);
                finish();
            }
        });
        // The code we are using for taking video through the front camera
        // relies on APIs added in SDK 9. Don't offer the front video option to
        // users on devices older than that.
        if (Build.VERSION.SDK_INT < 9) {
            launchVideoFrontButton.setVisibility(View.GONE);
        }
    }

}
