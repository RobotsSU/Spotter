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

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.util.ArrayList;

/**
 * Data model for a list of robot entries, a selected entry, and a pending entry
 * to be added. Also includes utility methods for launching QR scanner and
 * applying result to selected robot (probably not the best place for this).
 *
 * @author ptucker@google.com (Philip Tucker)
 */
public class RobotList {
    private final SharedPreferences prefs;

    private ArrayList<RobotEntry> robots;

    public RobotList(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    /**
     * Add an entry if it doesn't already exist based on the name. If it does
     * exist, update the entry that is there instead. There are no guarantees
     * that the order of the RobotList is maintained (i.e. don't hold onto an
     * index and assume the index will never change between calls to add/remote
     * methods).
     *
     * @param entry The entry to be added/updated.
     */
    public boolean addEntry(RobotEntry entry) {
        for (int i = 0; i < robots.size(); i++) {
            if (robots.get(i).getName().equals(entry.getName())) {
                return false;
            }
        }
        robots.add(entry);
        return true;
    }
    
    public boolean replaceEntry(int index, RobotEntry entry) {
        int existingIndex = findIndexByName(entry.getName());
        if (index >= robots.size() || index < 0 ||
            (existingIndex >= 0 && existingIndex != index)) {
            return false;
        }
        robots.remove(index);
        robots.add(index, entry);
        return true;
    }
    
    public RobotEntry getEntry(int index) {
        return robots.get(index);
    }

    public RobotEntry remove(int index) {
        return robots.remove(index);
    }

    /**
     * Find the robot entry that matches the given name.
     *
     * @param name The name to search for
     * @return If there is a match, return the index of the RobotEntry. If there
     *         is not a match, return -1
     */
    public int findIndexByName(String name) {
        for (int i = 0; i < robots.size(); i++) {
            if (robots.get(i).getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public int size() {
        return robots.size();
    }

    public void load() {
        robots = new ArrayList<RobotEntry>();
        String rawPrefsString = prefs.getString("ROBOTS_LIST", "");
        String[] entries = rawPrefsString.split("\n");
        for (int i = 0; i < entries.length; i++) {
            RobotEntry entry = RobotEntry.deserialize(entries[i]);
            if (entry != null) {
                robots.add(entry);
            }
        }
    }

    public void save() {
        String output = "";
        for (int i = 0; i < robots.size(); i++) {
            RobotEntry entry = robots.get(i);
            output = output + entry.serialize() + "\n";
        }
        Editor editor = prefs.edit();
        editor.putString("ROBOTS_LIST", output);
        editor.commit();
    }
}
