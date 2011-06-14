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

import java.util.Arrays;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

// TODO: Auto-generated Javadoc
/**
 * The Class PulseGenerator.
 */
public class PulseGenerator implements Runnable
{

  //
  /** The sample rate. 44100hz is native on g1 */
  private int        sampleRate;

  /** The MI n_ puls e_ width. */
  public int         MIN_PULSE_WIDTH;

  /** The MA x_ puls e_ width. */
  public int         MAX_PULSE_WIDTH;

  /** The left channel pulse width. */
  private int        pulseWidthArray[];

  /** The pulse interval. */
  private int        pulseInterval;

  /** The buffer pulses. */
  private int        bufferPulses  = 2;

  /** The ammount modulation. */
  private int        modulation    = 200;

  /** The max volume */
  private int        volume        = Short.MAX_VALUE;

  /** Are we playing sound right now? */
  private boolean    playing       = false;

  /** Do we need to update the buffer? */
  private boolean    bufferChanged = false;

  /** The noise audio track. */
  private AudioTrack noiseAudioTrack;

  /** The bufferlength. */
  private int        bufferlength;  // 4800

  /** The audio buffer. */
  private short[]    audioBuffer;

  /** The left channel buffer. */
  private short[]    leftChannelBuffer;

  /** The right channel buffer. */
  private short[]    rightChannelBuffer;

  private static String TAG = "Servo Pulse Generator";
  
  /**
   * Instantiates a new pulse generator.
   */
  public PulseGenerator()
  {
    sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);

    MIN_PULSE_WIDTH = sampleRate / 1200;

    MAX_PULSE_WIDTH = sampleRate / 456;

    pulseWidthArray = new int[4];

    Arrays.fill(pulseWidthArray, ( MIN_PULSE_WIDTH + MAX_PULSE_WIDTH ) / 2);

    pulseInterval = sampleRate / 50;

    bufferlength = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT);

    noiseAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferlength, AudioTrack.MODE_STREAM);

    sampleRate = noiseAudioTrack.getSampleRate();

    Log.i(TAG, "BufferLength = " + Integer.toString(bufferlength));
    Log.i(TAG, "Sample Rate = " + Integer.toString(sampleRate));

    audioBuffer = new short[bufferlength];
    leftChannelBuffer = new short[bufferlength / 2];
    rightChannelBuffer = new short[bufferlength / 2];

    noiseAudioTrack.play();
  }


  private void generatePCM(int pulseWidth, int negPulseWidth, int pulseInterval, int volume, int modulation, short buffer[], int bufferLength, int lastChanged)
  {

    int i = 0;
    int j = 0;
    while (i < bufferLength)
    {
      j = 0;

      if (lastChanged % 2 == 0)
       {
        while (j < pulseWidth)// && i < bufferLength)
        {

          buffer[i] = (short) ( ( volume ) );
          i++;
          j++;
        }

        while (j < pulseInterval )// && i < bufferLength)
        {
          buffer[i] = (short) ( ( -volume ) );
          i++;
          j++;
        }
      }
     else
      {

        while (j < negPulseWidth)
        {
          // we have to modulate the signal a bit because the sound card freaks
          // out if it goes dc
          buffer[i] = (short) ( ( -volume ) );
          i++;
          j++;
        }
        while (j < pulseInterval )// && i < bufferLength)
        {
          buffer[i] = (short) ( ( volume ) );
          i++;
          j++;
        }

      }
    }

    bufferChanged = true;
  }

  public void run()
  {
    generatePCM(pulseWidthArray[0], pulseWidthArray[1], pulseInterval, volume, modulation, leftChannelBuffer, pulseInterval * bufferPulses, 0);
    generatePCM(pulseWidthArray[2], pulseWidthArray[3], pulseInterval, volume, modulation, rightChannelBuffer, pulseInterval * bufferPulses, 2);
    while (true)
    {
      int bufferlength = pulseInterval * bufferPulses * 2;
      if (playing)
      {
        for (int i = 0; i < bufferlength && bufferChanged; i += 2)
        {
          audioBuffer[i] = leftChannelBuffer[i / 2];
          audioBuffer[i + 1] = rightChannelBuffer[i / 2];
        }

      }
      else
      {
        for (int i = 0; i < bufferlength; i++)
        {
          audioBuffer[i] = (short) ( 0 );
        }
      }

      noiseAudioTrack.write(audioBuffer, 0, bufferlength);
    }
  }

  /**
   * Stop.
   */
  public void stop()
  {
    playing = false;
    noiseAudioTrack.stop();
    noiseAudioTrack.release();
  }

  /**
   * Toggle playback.
   */
  public void togglePlayback()
  {
    playing = !playing;
  }

  /**
   * Checks if is playing.
   * 
   * @return true, if is playing
   */
  public boolean isPlaying()
  {
    return playing;
  }

  /**
   * Sets the left pulse percent.
   * 
   * @param percent
   *          the new left pulse percent
   */
  public void setPulsePercent(int percent, int i)
  {

    if (i< 0 || i > 4)
    {
      Log.e(TAG,"Servo index out of bounds, should be between 0 and 3");
      return; 
    }
    
    this.pulseWidthArray[i] = MIN_PULSE_WIDTH + ( ( percent * ( MAX_PULSE_WIDTH - MIN_PULSE_WIDTH ) ) / 100 );

    if (i < 2)
    {
      generatePCM(pulseWidthArray[0], pulseWidthArray[1], pulseInterval, volume, modulation, leftChannelBuffer, pulseInterval * bufferPulses, i);
    }
    else
    {
      generatePCM(pulseWidthArray[2], pulseWidthArray[3], pulseInterval, volume, modulation, rightChannelBuffer, pulseInterval * bufferPulses, i);
    }

  }

  /**
   * Gets the  pulse percent.
   * 
   * @return the pulse percent
   */
  public int getPulsePercent(int i)
  {
    return ( ( pulseWidthArray[i] - MIN_PULSE_WIDTH ) / ( MAX_PULSE_WIDTH - MIN_PULSE_WIDTH ) ) * 100;
  }

  /**
   * Gets the pulse ms.
   * 
   * @return the  pulse ms
   */
  public float getPulseMs(int i)
  {
    return ( (float) pulseWidthArray[i] / sampleRate ) * 1000;
  }

  /**
   * Gets the pulse samples.
   * 
   * @return the pulse samples
   */
  public int getPulseSamples(int i)
  {
    return pulseWidthArray[i];
  }

}