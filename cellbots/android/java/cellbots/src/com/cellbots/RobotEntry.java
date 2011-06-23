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

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

import com.cellbots.httpserver.HttpCommandServerServiceManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An robot profile, union of fields necessary for local control (bluetooth) and
 * remote (XMPP, HTTP, appengine).
 *
 * @author ptucker@google.com (Philip Tucker)
 */
public class RobotEntry {
    
    private static final String TAG = "RobotEntry";
    
    /**
     * A robot entry can have multiple elements filled in. Define a usage field
     * which lets the outside world know what fields matter.
     */
    public enum Usage {
        UNKNOWN("Unknown"),
        DIRECT_CONTROL("Phone is talking directly to the robot via bluetooth"),
        BRAIN("Phone is acting as a brain and/or relay between the robot and the internet"),
        WEB_CONTROL("Phone is talking via the internet to a relay on a robot");

        String description;

        private Usage(String desc) {
            this.description = desc;
        }
    };

    //
    // Robot Identification
    //
    private String name = "!";

    private String cellbotType = "!";

    private boolean enableWiFi = false;

    //
    // XMPP
    //
    private String gmail = "!";

    private String passwd = "!";

    //
    // HTTP
    //
    private String url = "!";
    
    private boolean useLocalHttp = false;

    //
    // AppEngine
    //
    private String agentId = "!";

    //
    // Local Control
    //
    private String bluetoothName = "!";
    
    // The mac address is unique, the name is not. Also, with the
    // mac address we can create a BluetoothDevice, but we can't with
    // just the name.
    private String bluetoothAddr = "!";

    //
    // Intended usage for this profile entry
    //
    private Usage intendedUsage = Usage.UNKNOWN;
    
    private static final Pattern NAME_MAC_SPLITTER =
            Pattern.compile("(.*) \\[([a-zA-z0-9:]+)\\]$");

    /**
     * Generate a string that includes the bluetooth name and mac.
     * 
     * @return string with the name and mac
     */
    public String createNameMacPair() {
        return generateNameMacPair(bluetoothName, bluetoothAddr);
    }
    
    /**
     * Allow an external entity that has a name and a mac generate the
     * same string as the nameMacPair() above.
     * 
     * @param name bluetooth name
     * @param mac bluetooth mac address
     * @return string with the name and mac
     */
    public static String generateNameMacPair(String name, String mac) {
        // Only append the bluetooth address if it is a valid address
        if (!BluetoothAdapter.checkBluetoothAddress(mac)) {
            return name;
        } else {
            return name + " [" + mac + "]";
        }
    }
    
    /**
     * Split up the name and mac strings from a string that was created via
     * generateNameMacPair()
     * 
     * @param nameMac String generated from generateNameMacPair
     * @return String array - [0] = name [1] = mac address
     */
    public static String[] splitNameMacPair(String nameMac) {
        String[] ret = new String[2];

        Matcher m = NAME_MAC_SPLITTER.matcher(nameMac);
        if (m.matches()) {
            if (m.groupCount() == 2) {
                ret[0] = m.group(1);
                if (BluetoothAdapter.checkBluetoothAddress(m.group(2))) {
                    ret[1] = m.group(2);
                } else {
                    Log.w(TAG, "Problem splitting - bad mac address: " + nameMac);
                    ret[1] = "";
                }
            } else {
                Log.e(TAG, "Problem splitting - count wrong: "
                        + nameMac + " (" + m.groupCount() + ")");
                ret[0] = nameMac;
                ret[1] = "";
            }
        } else {
            Log.w(TAG, "Problem splitting - no match: " + nameMac);
            ret[0] = nameMac;
            ret[1] = "";
        }
        
        return ret;
    }

     // TODO(ptucker): protocol-specific factory methods

    public static RobotEntry newDirectRobotEntry(
            String name, String type, String bt, String btAddr) {
        return new RobotEntry(
                name, type, false, "!", "!", "!", false, "!", bt, Usage.DIRECT_CONTROL, btAddr);
    }

    public static RobotEntry newLocalRobotEntry(String name, String gmail, String passwd,
            String url, boolean useLocal, String agent, String type,
            String bt, boolean wifi, String btAddr) {
        return new RobotEntry(
                name, type, wifi, gmail, passwd, url, useLocal, agent, bt, Usage.BRAIN, btAddr);
    }

    public static RobotEntry newRemoteRobotEntry(
            String name, String gmail, String url, String agent) {
        return new RobotEntry(
                name, "!", false, gmail, "!", url, false, agent, "!", Usage.WEB_CONTROL, "!");
    }

    public RobotEntry() {
    }

    public RobotEntry(String nameStr, String cellbotTypeStr, boolean wifiBool, String gmailStr,
            String passwdStr, String urlStr, boolean useLocal, String agentIdStr, String btStr,
            Usage usage, String btAddr) {
        name = cleanUpString(nameStr);
        cellbotType = cleanUpString(cellbotTypeStr);
        enableWiFi = wifiBool;

        gmail = cleanUpString(gmailStr);
        passwd = cleanupPasswd(passwdStr);

        url = cleanUpString(urlStr);
        useLocalHttp = useLocal;

        agentId = cleanUpString(agentIdStr);

        bluetoothName = cleanUpString(btStr);
        
        bluetoothAddr = cleanUpString(btAddr);
        // If the bluetooth address provided is not valid, then
        // put in the "not there" marker.
        if (!BluetoothAdapter.checkBluetoothAddress(bluetoothAddr)) {
            bluetoothAddr = "!";
        }

        intendedUsage = usage;
    }

    public void setName(String nameStr) {
        name = cleanUpString(nameStr);
    }

    public void setGmail(String gmailStr) {
        gmail = cleanUpString(gmailStr);
    }

    public void setPasswd(String passwdStr) {
        passwd = cleanupPasswd(passwdStr);
    }

    public void setUrl(String urlStr) {
        url = cleanUpString(urlStr);
    }
    
    public void setUseLocalHttpServer(boolean useLocal) {
        useLocalHttp = useLocal;
    }
    
    public void setAgentId(String agentIdStr) {
        agentId = cleanUpString(agentIdStr);
    }

    public void setBluetoothName(String btStr) {
        bluetoothName = cleanUpString(btStr);
    }

    public void setBluetoothAddr(String btStr) {
        bluetoothAddr = cleanUpString(btStr);
        // If the bluetooth address provided is not valid, then
        // put in the "not there" marker.
        if (!BluetoothAdapter.checkBluetoothAddress(bluetoothAddr)) {
            bluetoothAddr = "!";
        }
    }

    public void setEnableWifi(boolean wifiBool) {
        enableWiFi = wifiBool;
    }

    public void setType(String typeStr) {
        cellbotType = cleanUpString(typeStr);
    }

    public void setIntendedUsage(Usage usage) {
        intendedUsage = usage;
    }

    public String getName() {
        return name.replaceAll("!", "");
    }

    public String getGmail() {
        return gmail.replaceAll("!", "");
    }

    public String getPasswd() {
        // TODO: Make sure the assumption that jabber passwords must be longer
        // than 1 character is true.
        if (passwd.equals("!")) {
            return "";
        }
        return passwd;
    }

    public String getUrl() {
        updateUrl();
        return url.replaceAll("!", "");
    }
    
    public boolean getUseLocalHttpServer() {
        return useLocalHttp;
    }

    public String getAgentId() {
        return agentId.replaceAll("!", "");
    }

    public String getType() {
        return cellbotType.replaceAll("!", "");
    }

    public String getBluetoothName() {
        return bluetoothName.replaceAll("!", "");
    }

    public String getBluetoothAddr() {
        return bluetoothAddr.replaceAll("!", "");
    }

    public boolean getEnableWiFi() {
        return enableWiFi;
    }

    public Usage getIntendedUsage() {
        return intendedUsage;
    }

    private static String cleanUpString(String original) {
        if (original == null) {
            original = "";
        }
        String cleaned = original.replaceAll("\n", "").replaceAll("|", "").replaceAll("!", "");
        if (cleaned.length() < 1) {
            cleaned = "!";
        }
        return cleaned;
    }

    private static String cleanupPasswd(String original) {
        if (original == null) {
            original = "";
        }
        String passwd = original.replaceAll("\n", "").replaceAll("|", "");
        if (passwd.length() < 1) {
            passwd = "!";
        }
        return passwd;
    }
    
    private void updateUrl() {
        url = useLocalHttp ? "http://" + HttpCommandServerServiceManager.getLocalIpAddress() +
                ":8080/" : url;
    }

    /**
     * Serialize a robot entry based on the usage. Leave out fields
     * that aren't necessary for a scanner (i.e. leave out passwords)
     * 
     * @return
     */
    public String serializeForUsage() {
        String s;
        updateUrl();
        switch (intendedUsage) {
            case DIRECT_CONTROL:
                s = name + "|" + cellbotType + "|" + "!" + "|" + "!" + "|" + "!" + "|" + "!"
                + "|" + "!" + "|" + bluetoothName + "|" + intendedUsage.ordinal()
                + "|" + "!" + "|" + bluetoothAddr;
                break;
            case BRAIN:
                s = name + "|" + cellbotType + "|" + enableWiFi + "|" + gmail
                + "|" + "!" + "|" + url + "|" + agentId + "|" + bluetoothName
                + "|" + intendedUsage.ordinal() + "|" + useLocalHttp
                + "|" + bluetoothAddr;
                break;
            case WEB_CONTROL:
                s = name + "|" + "!" + "|" + "!" + "|" + gmail + "|" + "!" + "|" + url
                + "|" + agentId + "|" + "!" + "|" + intendedUsage.ordinal()
                + "|" + "!" + "|" + "!";
                break;
            default:
                Log.e(TAG, "Problem serializing: " + name + " bad usage " + intendedUsage);
                s = "";
        }
        return s;
    }

    /**
     * Fully serialize a robot entry. This means including stuff like the
     * password. Someone who deserializes this should get an exact copy of
     * the robot entry.
     * 
     * @return
     */
    public String serialize() {
        updateUrl();
        return name + "|" + cellbotType + "|" + enableWiFi + "|" + gmail + "|" + passwd + "|"
                + url + "|" + agentId + "|" + bluetoothName + "|" + intendedUsage.ordinal()
                + "|" + useLocalHttp + "|" + bluetoothAddr;
    }

    public RobotEntry applySerialized(String serialized) {
        if (serialized == null || serialized.length() == 0) {
            return this;
        }
        String[] tokens = serialized.split("\\|");
        if (tokens.length > 0 && tokens[0].length() > 0) {
            setName(tokens[0]);
        }
        if (tokens.length > 1 && tokens[1].length() > 0) {
            setType(tokens[1]);
        }
        if (tokens.length > 2 && tokens[2].length() > 0) {
            setEnableWifi(Boolean.parseBoolean(tokens[2]));
        }
        if (tokens.length > 3 && tokens[3].length() > 0) {
            setGmail(tokens[3]);
        }
        if (tokens.length > 4 && tokens[4].length() > 0) {
            setPasswd(tokens[4]);
        }
        if (tokens.length > 5 && tokens[5].length() > 0) {
            setUrl(tokens[5]);
        }
        if (tokens.length > 6 && tokens[6].length() > 0) {
            setAgentId(tokens[6]);
        }
        if (tokens.length > 7 && tokens[7].length() > 0) {
            setBluetoothName(tokens[7]);
        }
        if (tokens.length > 8 && tokens[8].length() > 0) {
            Usage value = Usage.values()[Integer.parseInt(tokens[8])];
            setIntendedUsage(value);
        }
        if (tokens.length > 9 && tokens[9].length() > 0) {
            setUseLocalHttpServer(Boolean.parseBoolean(tokens[9]));
        }
        if (tokens.length > 10 && tokens[10].length() > 0) {
            setBluetoothAddr(tokens[10]);
        }
        return this;
    }

    public static RobotEntry deserialize(String serialized) {
        if (serialized == null || serialized.length() == 0) {
            return null;
        }
        return new RobotEntry().applySerialized(serialized);
    }

    @Override
    public String toString() {
        return serialize();
    }
}
