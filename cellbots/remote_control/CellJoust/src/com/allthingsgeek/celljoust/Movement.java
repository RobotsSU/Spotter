package com.allthingsgeek.celljoust;

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

import com.cellbots.CellbotProtos.ControllerState;

import android.view.KeyEvent;

/**
 * Singleton class to control servos in response to key or console events
 */
public class Movement {
	private PulseGenerator noise;
	private static Movement instance;
	private int speed = 20;
	private int offset = 0;

	private Movement() {
		noise = PulseGenerator.getInstance();
	}

	public static Movement getInstance() {
		if (instance == null) {
			instance = new Movement();
		}
		return instance;
	}

	/**
	 * @return the offset
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * @param offset the offset to set
	 */
	public void setOffset(int o) {
		this.offset = o;
	}

	public void driveFoward() {
		driveFoward(50);
	}

	public void driveBackward() {
		driveBackward(50);
	}

	public void driveFoward(int ms) {
		int left = speed, right = speed;
		if (offset < 0)
			right += offset;
		else if (offset > 0)
			left -= offset;
		noise.setServo(0, 50 + left, ms);
		noise.setServo(2, 50 + right, ms);
		noise.unpause();
	}

	public void driveBackward(int ms) {
		int left = speed, right = speed;
		if (offset < 0)
			right += offset;
		else if (offset > 0)
			left -= offset;
		noise.setServo(0, 50 - left, ms);
		noise.setServo(2, 50 - right, ms);
		noise.unpause();
	}

	public void stop() {
	    
		noise.setServo(0, 50, 1);
		noise.setServo(2, 50, 1);
	    noise.pause();
	}

	public void turnLeft() {
		noise.setServo(0, 50 - speed, 25);
		noise.setServo(2, 50 + speed, 25);
		noise.unpause();
	}

	public void turnRight() {
		noise.setServo(0, 50 + speed, 25);
		noise.setServo(2, 50 - speed, 25);
		noise.unpause();
	}

	public void setSpeed(int s) {
		speed = s;
	}
	
	public String processControllerStateEvent(ControllerState cs)
	{
	  for (ControllerState.KeyEvent ev: cs.getKeyEventList())
      {
         if (ev.getKeyDown())
        {
          return processKeyDownEvent(Integer.parseInt(ev.getKeyCode()));
        }
         
         if (ev.getKeyUp())
         {
           processKeyUpEvent(Integer.parseInt(ev.getKeyCode()));
         }
      }
	  
	  return "";
	  
	  
	}

	public void processTextCommand(String string) {
		if (string.startsWith("w")) {
			driveFoward();
		}
		if (string.startsWith("s")) {
			driveBackward();
		}
		if (string.startsWith("a")) {
			turnLeft();
		}
		if (string.startsWith("d")) {
			turnRight();
		}
		if (string.startsWith("-")) {
			speed--;
		}
		if (string.startsWith("+")) {
			speed++;
		}
		if (string.startsWith(" ")) {
			stop();
		}
	}

	public String processKeyDownEvent(int keyCode) {
	   
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_UP:
		case KeyEvent.KEYCODE_W:
			driveFoward();
			return "f\n";
		case KeyEvent.KEYCODE_DPAD_DOWN:
		case KeyEvent.KEYCODE_S:
			driveBackward();
			return "b\n";
		case KeyEvent.KEYCODE_DPAD_LEFT:
		case KeyEvent.KEYCODE_A:
			turnLeft();
			return "l\n";
		case KeyEvent.KEYCODE_DPAD_RIGHT:
		case KeyEvent.KEYCODE_D:
			turnRight();
			return "r\n";
		case KeyEvent.KEYCODE_P:
			if (speed < 50)
				speed++;
			return "p\n";
		case KeyEvent.KEYCODE_M:
			if (speed > 0)
				speed--;
			return "m\n";
		case KeyEvent.KEYCODE_DPAD_CENTER:
		case KeyEvent.KEYCODE_SPACE:
			stop();
			return "s\n";
		}
		return null;
	}
	
	public boolean processKeyUpEvent(int keyCode) {
      
      switch (keyCode) {
      case KeyEvent.KEYCODE_DPAD_UP:
      case KeyEvent.KEYCODE_W:
          stop();
          return true;
      case KeyEvent.KEYCODE_DPAD_DOWN:
      case KeyEvent.KEYCODE_S:
        stop();
          return true;
      case KeyEvent.KEYCODE_DPAD_LEFT:
      case KeyEvent.KEYCODE_A:
        stop();
          return true;
      case KeyEvent.KEYCODE_DPAD_RIGHT:
      case KeyEvent.KEYCODE_D:
        stop();
          return true;
     
      }
      return false;
  }
}
