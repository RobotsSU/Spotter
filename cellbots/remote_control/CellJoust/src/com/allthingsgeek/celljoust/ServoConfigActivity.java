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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.GestureDetector;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * Activity to help configure the ServoBot program
 */
public class ServoConfigActivity extends Activity implements OnSeekBarChangeListener
{
  // public static SensorManager sensorManager;

  SeekBar         lPulseBar;

  EditText        serverUrl;

  EditText        robotId;

  SeekBar         lPulseBar2;

  SeekBar         rPulseBar;

  SeekBar         rPulseBar2;

  SeekBar         lrOffset;

  TextView        rPulseText;

  TextView        rPulseText2;

  TextView        lPulseText;

  TextView        lPulseText2;

  TextView        lrOffsetText;

  ToggleButton    soundToggleButton;

  PulseGenerator  noise;

  Thread          noiseThread;

  GestureDetector nGestures;

  Movement        mover;

  /*
   * (non-Javadoc)
   * 
   * @see android.app.Activity#onCreate(android.os.Bundle)
   */
  @Override
  protected void onCreate(Bundle savedInstanceState)
  {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.servos);

    SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);

    noise = PulseGenerator.getInstance();
    mover = Movement.getInstance();

    lPulseBar = (SeekBar) findViewById(R.id.LeftServo);
    lPulseBar.setProgress( ( noise.getPulsePercent(0) - 25 ) * 2);
    lPulseBar.setOnSeekBarChangeListener(this);
    lPulseText = (TextView) findViewById(R.id.LeftServoValue);
    lPulseText.setText("Servo 1(L Wheel) = " + noise.getPulsePercent(0) + "% (" + noise.getPulseSamples(0) + " samples)");

    lPulseBar2 = (SeekBar) findViewById(R.id.LeftServo2);
    lPulseBar2.setProgress( ( noise.getPulsePercent(1) - 25 ) * 2);
    lPulseBar2.setOnSeekBarChangeListener(this);
    lPulseText2 = (TextView) findViewById(R.id.LeftServoValue2);
    lPulseText2.setText("Servo 2(L Arm) = " + noise.getPulsePercent(1) + "% (" + noise.getPulseSamples(1) + " samples)");

    rPulseBar = (SeekBar) findViewById(R.id.RightServo);
    rPulseBar.setProgress( ( noise.getPulsePercent(2) - 25 ) * 2);
    rPulseBar.setOnSeekBarChangeListener(this);
    rPulseText = (TextView) findViewById(R.id.RightServoValue);
    rPulseText.setText("Servo 3(R Wheel) = " + noise.getPulsePercent(2) + "% (" + noise.getPulseSamples(2) + " samples)");

    rPulseBar2 = (SeekBar) findViewById(R.id.RightServo2);
    rPulseBar2.setProgress( ( noise.getPulsePercent(3) - 25 ) * 2);
    rPulseBar2.setOnSeekBarChangeListener(this);
    rPulseText2 = (TextView) findViewById(R.id.RightServoValue2);
    rPulseText2.setText("Servo 4(R Arm) = " + noise.getPulsePercent(3) + "% (" + noise.getPulseSamples(3) + " samples)");

    lrOffset = (SeekBar) findViewById(R.id.LROffset);
    lrOffset.setProgress(50 + mover.getOffset() * 2);
    lrOffset.setOnSeekBarChangeListener(this);
    lrOffsetText = (TextView) findViewById(R.id.WheelOffestValue);
    lrOffsetText.setText("Left vs Right Offset = " + mover.getOffset());

    // soundToggleButton = (ToggleButton) findViewById(R.id.ToggleSound);
    // soundToggleButton.setChecked(!noise.isPaused());
    noise.pause(false);
  }

  /*
   * (non-Javadoc)
   * 
   * @see android.app.Activity#onStop()
   */
  @Override
  protected void onStop()
  {
    super.onStop();
    SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
    SharedPreferences.Editor editor = settings.edit();
    editor.putInt("servo1Percent", noise.getPulsePercent(0));
    editor.putInt("servo2Percent", noise.getPulsePercent(1));
    editor.putInt("servo3Percent", noise.getPulsePercent(2));
    editor.putInt("servo4Percent", noise.getPulsePercent(3));
    editor.putInt("wheelOffset", mover.getOffset());
    // Commit the edits!
    editor.commit();
    noise.pause(true);
    finish();
  }
  
  

  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch)
  {
    if (seekBar.getId() == lPulseBar.getId())
    {
      noise.setOffsetPulsePercent(progress / 2 + 25, 0);
      lPulseText.setText("Servo 1(L Wheel) = " + noise.getPulsePercent(0) + "% (" + noise.getPulseSamples(0) + " samples)");
    }
    if (seekBar.getId() == lPulseBar2.getId())
    {
      noise.setOffsetPulsePercent(progress / 2 + 25, 1);
      lPulseText2.setText("Servo 2(L Arm) = " + noise.getPulsePercent(1) + "% (" + noise.getPulseSamples(1) + " samples)");
    }
    if (seekBar.getId() == rPulseBar.getId())
    {
      noise.setOffsetPulsePercent(progress / 2 + 25, 2);
      rPulseText.setText("Servo 3(R Wheel) = " + noise.getPulsePercent(2) + "% (" + noise.getPulseSamples(2) + " samples)");
    }
    if (seekBar.getId() == rPulseBar2.getId())
    {
      noise.setOffsetPulsePercent(progress / 2 + 25, 3);
      rPulseText2.setText("Servo 4(R Arm) = " + noise.getPulsePercent(3) + "% (" + noise.getPulseSamples(3) + " samples)");
    }
    if (seekBar.getId() == lrOffset.getId())
    {
      mover.setOffset( ( progress / 2 + 25 ) - 50);
      lrOffsetText.setText("Left vs Right Offset = " + mover.getOffset());
    }
  }

  public void onStartTrackingTouch(SeekBar seekBar)
  {
    // TODO Auto-generated method stub

  }

  public void onStopTrackingTouch(SeekBar seekBar)
  {
    // TODO Auto-generated method stub

  }
}
