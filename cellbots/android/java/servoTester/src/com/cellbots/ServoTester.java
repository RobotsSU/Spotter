package com.cellbots;

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

import com.cellbots.R;

import android.app.Activity;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class ServoTester extends Activity implements OnSeekBarChangeListener
{

  public static SensorManager sensorManager;

  private LinearLayout        main;

  PulseGenerator              noise;

  GestureDetector             nGestures;

  SeekBar                     lPulseBar;

  SeekBar                     rPulseBar;

  TextView                    rPulseText;

  TextView                    lPulseText;

  SeekBar                     lPulseBar2;

  SeekBar                     rPulseBar2;

  TextView                    rPulseText2;

  TextView                    lPulseText2;

  Thread                      noiseThread;

  ToggleButton                soundToggleButton;

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.main);

    sensorManager = (SensorManager) this.getSystemService(this.SENSOR_SERVICE);

    // set up the noise thread
    noise = new PulseGenerator();
    noiseThread = new Thread(noise);

    lPulseBar = (SeekBar) findViewById(R.id.LeftServo);
    // lPulseBar.setProgress(noise.getLeftPulsePercent());
    lPulseBar.setOnSeekBarChangeListener(this);
    lPulseText = (TextView) findViewById(R.id.LeftServoValue);
    lPulseText.setText("0 Left Pos Pulse width =" + noise.getPulsePercent(0));

    lPulseBar2 = (SeekBar) findViewById(R.id.LeftServo2);
    // lPulseBar.setProgress(noise.getLeftPulsePercent());
    lPulseBar2.setOnSeekBarChangeListener(this);
    lPulseText2 = (TextView) findViewById(R.id.LeftServoValue2);
    lPulseText2.setText("1 Left Neg Pulse width =" + noise.getPulsePercent(1));
    
    rPulseBar = (SeekBar) findViewById(R.id.RightServo);
    // rPulseBar.setProgress(noise.getRightPulsePercent());
    rPulseBar.setOnSeekBarChangeListener(this);
    rPulseText = (TextView) findViewById(R.id.RightServoValue);
    rPulseText.setText("2 Right Pos Pulse width =" + noise.getPulsePercent(2));

    rPulseBar2 = (SeekBar) findViewById(R.id.RightServo2);
    // rPulseBar.setProgress(noise.getRightPulsePercent());
    rPulseBar2.setOnSeekBarChangeListener(this);
    rPulseText2 = (TextView) findViewById(R.id.RightServoValue2);
    rPulseText2.setText("3 Right Neg Pulse width =" + noise.getPulsePercent(3));

  }

  @Override
  protected void onStart()
  {
    if (!noiseThread.isAlive())
      noiseThread.start();
    super.onStart();
  }

  @Override
  protected void onDestroy()
  {
    noise.stop();
    // soundToggleButton.setChecked(false);
    // TODO Auto-generated method stub
    super.onDestroy();
  }

  public void onToggleSound(View v)
  {
    noise.togglePlayback();
  }

  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch)
  {
    if (seekBar.getId() == lPulseBar.getId())
    {
      noise.setPulsePercent(progress, 0);
      lPulseText.setText("Servo 1(L Pos) pulse width = " + noise.getPulseMs(0) + "ms (" + noise.getPulseSamples(0) + " samples)");
    }
    if (seekBar.getId() == lPulseBar2.getId())
    {
      noise.setPulsePercent(progress, 1);
      lPulseText2.setText("Servo 2(L Neg) pulse width = " + noise.getPulseMs(1) + "ms (" + noise.getPulseSamples(1) + " samples)");
    }

    if (seekBar.getId() == rPulseBar.getId())
    {
      noise.setPulsePercent(progress, 2);
      rPulseText.setText("Servo 3(R Pos) pulse width = " + noise.getPulseMs(2) + "ms (" + noise.getPulseSamples(2) + " samples)");
    }

    if (seekBar.getId() == rPulseBar2.getId())
    {
      noise.setPulsePercent(progress, 3);
      rPulseText2.setText("Servo 4(R Neg) Pulse width = " + noise.getPulseMs(3) + "ms (" + noise.getPulseSamples(3) + " samples)");
    }

  }

  public void onStartTrackingTouch(SeekBar seekBar)
  {
    // mTrackingText.setText(getString(R.string.seekbar_tracking_on));
  }

  public void onStopTrackingTouch(SeekBar seekBar)
  {
    // mTrackingText.setText(getString(R.string.seekbar_tracking_off));
  }

}