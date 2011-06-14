/*
 * Robot control console. Copyright (C) 2010 Darrell Taylor & Eric Hokanson
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.allthingsgeek.celljoust;

import com.allthingsgeek.celljoust.R;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 *  Activity to help configure the ServoBot program 
 */
public class SetupActivity extends Activity  {
//	public static SensorManager sensorManager;
	
	SeekBar lPulseBar;
	EditText serverUrl;
	EditText robotId;
	SeekBar lPulseBar2;
	SeekBar rPulseBar;
	SeekBar rPulseBar2;
	SeekBar lrOffset;
	TextView rPulseText;
	TextView rPulseText2;
	TextView lPulseText;
	TextView lPulseText2;
	TextView lrOffsetText;
	ToggleButton soundToggleButton;
	PulseGenerator noise;
	Thread noiseThread;
	GestureDetector nGestures;
	Movement mover;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	  

		super.onCreate(savedInstanceState);
		setContentView(R.layout.setup);

		SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
		serverUrl = (EditText) findViewById(R.id.serverUrl);
		serverUrl.setText(settings.getString("REMOTE_EYES_PUT_URL", "celljoust.appspot.com"));
		
	    robotId = (EditText) findViewById(R.id.robotID);
	    robotId.setText(settings.getString("ROBOT_ID", RobotStateHandler.ROBOT_ID));
		
	    noise = PulseGenerator.getInstance();
	    mover = Movement.getInstance();
	    
	  
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
		SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		
		editor.putString("REMOTE_EYES_PUT_URL", serverUrl.getText().toString().replace("http://", "").replace("HTTP://", ""));
		editor.putString("ROBOT_ID", robotId.getText().toString());
		RobotStateHandler.ROBOT_ID = robotId.getText().toString();
		// Commit the edits!
		editor.commit();
		finish();
	}



	
	
	public void launchServoAdjuster(View v)
	{
      Intent i = new Intent(this, ServoConfigActivity.class);
      startActivity(i);
	}
}
