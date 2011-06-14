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

package com.cellbots.remote;

import android.content.Context;
import android.widget.FrameLayout;

/**
 * Generic UI view for handling the controls.
 *
 * @author clchen@google.com (Charles L. Chen)
 */
public class UiView extends FrameLayout {
    public interface UiEventListener {
        public static final int INTERFACE_DPAD = 0;

        public static final int INTERFACE_JOYSTICK = 1;

        public static final int INTERFACE_ACCELEROMETER = 2;

        public static final int INTERFACE_VOICE = 3;

        public static final int ACTION_FORWARD = 0; // Optional value: [0-100]

        public static final int ACTION_BACKWARD = 1; // Optional value: [0-100]

        public static final int ACTION_LEFT = 2; // Optional value: [0-100]

        public static final int ACTION_RIGHT = 3; // Optional value: [0-100]

        public static final int ACTION_SPEAK = 4; // Required value: Text to be
                                                  // spoken

        public static final int ACTION_STRING = 5; // Required value: Command to
                                                   // send to robot

        public static final int ACTION_TAKE_PICTURE = 6; // No values
        

        public static final int ACTION_GET_LOCATION = 7; // No values

        public static final int ACTION_CHANGE_PERSONA = 8; // Required value: text for expression

        /**
         * Causes the robot to take an action as defined by the ACTION_XXX
         * values above. Currently the only string value allowed is the text for
         * the ACTION_SPEAK. There are plans in the future to allow for the
         * options to the movement commands. TODO(css): Change this to the
         * public document URL when it is available since this code is intended
         * to be open sourced.
         *
         * @see https://docs.google.com/a/google.com/document/d/1Kl11k3YO9BsmoRIY4JEjZZoZX0BdYy_3AHC1GyVNuQQ/edit#
         *@param action An ACTION_XXX value
         * @param values Value to pass along to the action.
         */
        public void onActionRequested(int action, String values);

        /**
         * Send a wheel velocity command to a robot based on the requested
         * direction and speed.
         *
         * @param direction valid values are -180 to +180 where: -180 to 0 means
         *            left 0 to +180 means right
         * @param speed valid values are 0-100 which is a percentage of the
         *            robots speed.
         */
        public void onWheelVelocitySetRequested(float direction, float speed);

        /**
         * Causes the robot to stop moving.
         */
        public void onStopRequested();

        /**
         * Request to switch to a different interface.
         *
         * @param interfaceId The ID of the interface to switch to.
         */
        public void onSwitchInterfaceRequested(int interfaceId);
        
        /**
         * Request to show a popup window
         */
        public void onPopupWindowRequested();
    }

    public UiView(Context context, UiEventListener uiEventListener) {
        super(context);
    }

}
