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

package com.cellbots.remoteEyes;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;

/**
 * Prefs activity for the remote eyes. Sets the Remote Eyes URL.
 * 
 * @author clchen@google.com (Charles L. Chen)
 */
public class PrefsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    private EditTextPreference remoteEyesUrlPref;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        prefs = getPreferenceScreen().getSharedPreferences();
        remoteEyesUrlPref = (EditTextPreference) findPreference("REMOTE_EYES_PUT_URL");

    }

    @Override
    protected void onResume() {
        super.onResume();
        remoteEyesUrlPref.setSummary(prefs.getString("REMOTE_EYES_PUT_URL", ""));
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("REMOTE_EYES_PUT_URL")) {
            remoteEyesUrlPref.setSummary(sharedPreferences.getString(key, ""));
        }
    }

}
