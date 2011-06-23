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

import com.cellbots.RobotEntry.Usage;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Select the robot that will be used. We determine what it will be used for
 * based on user actions after the robot is selected.
 *
 * @author css@google.com (Charles Spirakis)
 */
public class RobotSelectionActivity extends ListActivity {

    private static final String TAG = "RobotSelectionActivity";

    // Parameters that can be passed to create/edit activities
    public static final String EXTRA_CREATE_NOW = "create_now";

    public static final String EXTRA_EDIT = "edit";

    public static final String EXTRA_NAME = "robo_name";

    public static final String EXTRA_TYPE = "robo_type";

    public static final String EXTRA_BT = "robo_blue";
    
    public static final String EXTRA_SHOW_WEBONLY_BUTTON = "show_webonly";

    // Status result from create/edit activities
    public static final int RESULT_CANCEL = 0;

    public static final int RESULT_SAVE = 1;

    public static final int RESULT_START_WEB_CREATE = 2;

    // Internal intent to match results with activities
    private enum SelectionIntents {
        INTENT_CREATE,
        INTENT_DIRECT_CREATE,
        INTENT_DIRECT_EDIT,
        INTENT_BRAIN_CREATE,
        INTENT_BRAIN_EDIT,
        INTENT_WEB_CREATE,
        INTENT_WEB_EDIT,
        INTENT_BLUECHECK_CREATE,
        INTENT_BLUECHECK_DIRECT_CONTROL,
        INTENT_BLUECHECK_DIRECT_EDIT,
        INTENT_BLUECHECK_BRAIN_CONTROL,        
        INTENT_BLUECHECK_BRAIN_EDIT,
        INTENT_BLUECHECK_BRAIN_CREATE;
    };
    
    private SelectionIntents[] mAllSelectionIntents = SelectionIntents.values();

    // Menu button items
    private static final int PREFS_ID = Menu.FIRST;

    protected RobotList mRobotProfiles;

    private SharedPreferences mPrefs;

    private static SharedPreferences mGlobalPrefs;
    
    private Activity mActivity;
    
    private LinearLayout noCellbotsMsgView;
    
    // Used by the various edit/controls to pass the name to the actual
    // edit/control acitivity because the bluetooth checker is between
    // the selection of the robot profile and the actual doing something
    // with it.
    private String mExtraName;
    
    private String mExtraType;
    
    private String mExtraBt;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mGlobalPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        mActivity = this;
        setContentView(R.layout.robot_selection);
        final Activity self = mActivity;

        ImageButton addButton = (ImageButton) findViewById(R.id.selection_add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClass(self, com.cellbots.RobotCreationActivity.class);
                startActivityForResult(i, SelectionIntents.INTENT_CREATE.ordinal());
            }
        });

        // See if we should jump directly into the create direct control screen
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            boolean mCreateNow = extras.getBoolean(RobotSelectionActivity.EXTRA_CREATE_NOW, false);
            if (mCreateNow) {
                Intent i = new Intent();
                i.setClass(self, com.cellbots.RobotCreationActivity.class);
                startActivityForResult(i, SelectionIntents.INTENT_CREATE.ordinal());
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshRobotList();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        SelectionIntents si;

        try {
            si = mAllSelectionIntents[requestCode];
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "resultCode not in enum list: " + resultCode);
            return;
        }

        Intent i = new Intent();

        switch (si) {
            case INTENT_CREATE:
            case INTENT_DIRECT_CREATE:
            case INTENT_DIRECT_EDIT:
            case INTENT_BRAIN_CREATE:
            case INTENT_BRAIN_EDIT:
            case INTENT_WEB_CREATE:
            case INTENT_WEB_EDIT:
                switch (resultCode) {
                    case RESULT_CANCEL:
                        // Nothing to do when the user cancels.
                        break;
                    case RESULT_SAVE:
                        mRobotProfiles.load();
                        refreshList();
                        break;
                    case RESULT_START_WEB_CREATE:
                        i.setClass(mActivity, com.cellbots.RobotCreateWebRemoteActivity.class);
                        i.putExtra(RobotSelectionActivity.EXTRA_EDIT, false);
                        startActivityForResult(i, SelectionIntents.INTENT_WEB_CREATE.ordinal());
                        break;
                    default:
                        Log.e(TAG, "Unexpected result: " + requestCode + "." + resultCode);
                }
                break;
            case INTENT_BLUECHECK_CREATE:
                switch (resultCode) {
                    case CheckBluetoothEnabledActivity.RESULT_CANCEL:
                    case CheckBluetoothEnabledActivity.RESULT_FAILURE:
                        // If bluetooth is off, send the user back to the
                        // selection
                        // screen (by basically doing nothing).
                        break;
                    case CheckBluetoothEnabledActivity.RESULT_SUCCESS:
                        i.setClass(mActivity, com.cellbots.RobotCreationActivity.class);
                        i.putExtra(RobotSelectionActivity.EXTRA_EDIT, false);
                        startActivityForResult(i, SelectionIntents.INTENT_DIRECT_CREATE.ordinal());
                        break;
                    case CheckBluetoothEnabledActivity.RESULT_WEB_ONLY:
                        i.setClass(mActivity, com.cellbots.RobotCreateWebRemoteActivity.class);
                        i.putExtra(RobotSelectionActivity.EXTRA_EDIT, false);
                        startActivityForResult(i, SelectionIntents.INTENT_WEB_CREATE.ordinal());
                        break;
                    default:
                        Log.e(TAG, "Unexpected result: " + requestCode + "." + resultCode);
                }
                break;
            case INTENT_BLUECHECK_BRAIN_EDIT:
                switch (resultCode) {
                    case CheckBluetoothEnabledActivity.RESULT_CANCEL:
                    case CheckBluetoothEnabledActivity.RESULT_FAILURE:
                        // If bluetooth is off, send the user back to the
                        // selection
                        // screen (by basically doing nothing).
                        break;
                    case CheckBluetoothEnabledActivity.RESULT_SUCCESS:
                        i.setClass(mActivity, com.cellbots.RobotCreateBrainActivity.class);
                        i.putExtra(RobotSelectionActivity.EXTRA_EDIT, true);
                        i.putExtra(RobotSelectionActivity.EXTRA_NAME, mExtraName);
                        startActivityForResult(i, SelectionIntents.INTENT_BRAIN_EDIT.ordinal());
                        break;
                    case CheckBluetoothEnabledActivity.RESULT_WEB_ONLY:
                        break;
                    default:
                        Log.e(TAG, "Unexpected result: " + requestCode + "." + resultCode);
                }
                break;
            case INTENT_BLUECHECK_DIRECT_EDIT:
                switch (resultCode) {
                    case CheckBluetoothEnabledActivity.RESULT_CANCEL:
                    case CheckBluetoothEnabledActivity.RESULT_FAILURE:
                        // If bluetooth is off, send the user back to the
                        // selection
                        // screen (by basically doing nothing).
                        break;
                    case CheckBluetoothEnabledActivity.RESULT_SUCCESS:
                        i.setClass(mActivity, com.cellbots.RobotCreationActivity.class);
                        i.putExtra(RobotSelectionActivity.EXTRA_EDIT, true);
                        i.putExtra(RobotSelectionActivity.EXTRA_NAME, mExtraName);
                        startActivityForResult(i, SelectionIntents.INTENT_DIRECT_EDIT.ordinal());
                        break;
                    case CheckBluetoothEnabledActivity.RESULT_WEB_ONLY:
                        break;
                    default:
                        Log.e(TAG, "Unexpected result: " + requestCode + "." + resultCode);
                }
                break;
            case INTENT_BLUECHECK_DIRECT_CONTROL:
                switch (resultCode) {
                    case CheckBluetoothEnabledActivity.RESULT_CANCEL:
                    case CheckBluetoothEnabledActivity.RESULT_FAILURE:
                        // If bluetooth is off, send the user back to the
                        // selection
                        // screen (by basically doing nothing).
                        break;
                    case CheckBluetoothEnabledActivity.RESULT_SUCCESS:
                        i.setClass(mActivity, com.cellbots.directcontrol.CellDroidActivity.class);
                        i.putExtra(EXTRA_NAME, mExtraName);
                        startActivity(i);
                        break;
                    case CheckBluetoothEnabledActivity.RESULT_WEB_ONLY:
                        break;
                    default:
                        Log.e(TAG, "Unexpected result: " + requestCode + "." + resultCode);
                }
                break;
            case INTENT_BLUECHECK_BRAIN_CONTROL:
                switch (resultCode) {
                    case CheckBluetoothEnabledActivity.RESULT_CANCEL:
                    case CheckBluetoothEnabledActivity.RESULT_FAILURE:
                        // If bluetooth is off, send the user back to the
                        // selection
                        // screen (by basically doing nothing).
                        break;
                    case CheckBluetoothEnabledActivity.RESULT_SUCCESS:
                        i.setClass(mActivity, com.cellbots.local.CellDroidActivity.class);
                        i.putExtra(EXTRA_NAME, mExtraName);
                        startActivity(i);
                        break;
                    case CheckBluetoothEnabledActivity.RESULT_WEB_ONLY:
                        break;
                    default:
                        Log.e(TAG, "Unexpected result: " + requestCode + "." + resultCode);
                }
                break;
            case INTENT_BLUECHECK_BRAIN_CREATE:
                switch (resultCode) {
                    case CheckBluetoothEnabledActivity.RESULT_CANCEL:
                    case CheckBluetoothEnabledActivity.RESULT_FAILURE:
                        // If bluetooth is off, send the user back to the
                        // selection
                        // screen (by basically doing nothing).
                        break;
                    case CheckBluetoothEnabledActivity.RESULT_SUCCESS:
                        // Start the sharing activity
                        i.setClass(mActivity, com.cellbots.RobotCreateBrainActivity.class);
                        i.putExtra(RobotSelectionActivity.EXTRA_EDIT, false);
                        i.putExtra(EXTRA_NAME, mExtraName);
                        i.putExtra(RobotSelectionActivity.EXTRA_TYPE, mExtraType);
                        i.putExtra(RobotSelectionActivity.EXTRA_BT, mExtraBt);
                        startActivityForResult(i, SelectionIntents.INTENT_BRAIN_CREATE.ordinal());
                        break;
                    case CheckBluetoothEnabledActivity.RESULT_WEB_ONLY:
                        break;
                    default:
                        Log.e(TAG, "Unexpected result: " + requestCode + "." + resultCode);
                }
                break;
            default:
                Log.e(TAG, "Unexpected resultCode: " + resultCode);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, PREFS_ID, 0, R.string.settings).setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(0, PREFS_ID + 1, 0, R.string.about_menu)
                .setIcon(android.R.drawable.ic_menu_info_details);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case PREFS_ID:
                Intent intent = new Intent(this, com.cellbots.PrefsActivity.class);
                startActivity(intent);
                break;
            case (PREFS_ID + 1):
                new AlertDialog.Builder(RobotSelectionActivity.this).setTitle(R.string.about_dialog_title)
                        .setMessage(R.string.splash_text)
                        .setPositiveButton(android.R.string.cancel, null).create().show();
                break;
            default:
                Log.e(TAG, "bad menu item number: " + item.getItemId());
        }

        return super.onOptionsItemSelected(item);
    }

    private void refreshRobotList() {
        mPrefs = getSharedPreferences(LauncherActivity.PREFERENCES_NAME, MODE_PRIVATE);
        mRobotProfiles = new RobotList(mPrefs);
        mRobotProfiles.load();
        refreshList();
        noCellbotsMsgView = (LinearLayout) findViewById(R.id.no_cellbots_msg_view);
        if (mRobotProfiles.size() > 0) {
            noCellbotsMsgView.setLayoutParams(
                    new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, 0));
            noCellbotsMsgView.setVisibility(View.INVISIBLE);
        } else {
            noCellbotsMsgView.setLayoutParams(
                    new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                    LayoutParams.FILL_PARENT));
            noCellbotsMsgView.setVisibility(View.VISIBLE);
        }
    }
    
    private void showLongPressOptionsPopup(final int botIndex) {
        AlertDialog.Builder optionsPopupDialog = new AlertDialog.Builder(this);
        final Activity self = mActivity;
        final RobotEntry selectedBot = mRobotProfiles.getEntry(botIndex);
        final Usage selectedUsage = selectedBot.getIntendedUsage();

        optionsPopupDialog.setTitle("Cellbot options");

        // All have edit/delete, but some have share and some have Show QR.
        // Make the menu show what we want based on which bot was
        // long-pressed.
        String[] qrOptions = {
                "Show QR Code", "Edit", "Delete"
        };

        String[] directOptions = {
                "Share", "Edit", "Delete"
        };

        String[] options = directOptions;
        if (selectedUsage != Usage.DIRECT_CONTROL) {
            options = qrOptions;
        }

        optionsPopupDialog.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent i = new Intent();
                switch (which) {
                    case 0:
                        // SHARE / Show QR code
                        Log.d(TAG, "Share: " + selectedBot.getName());
                        switch (selectedUsage) {
                            case DIRECT_CONTROL:
                                mExtraName = selectedBot.getName() + "_shared";
                                mExtraType = selectedBot.getType();
                                mExtraBt = selectedBot.createNameMacPair();
                                i.setClass(self, com.cellbots.CheckBluetoothEnabledActivity.class);
                                i.putExtra(RobotSelectionActivity.EXTRA_SHOW_WEBONLY_BUTTON, false);
                                startActivityForResult(i,
                                        SelectionIntents.INTENT_BLUECHECK_BRAIN_CREATE.ordinal());
                                break;
                            case BRAIN:
                            case WEB_CONTROL:
                                // Send a new robot entry to generate QR code so
                                // that only the minimal necessary info is
                                // included in the QR code. 
                                generateQrUri(RobotSelectionActivity.this, 
                                        RobotEntry.newRemoteRobotEntry(selectedBot.getName(),
                                        selectedBot.getGmail(), selectedBot.getUrl(),
                                        selectedBot.getAgentId()));
                                break;
                            default:
                                Log.d(TAG, "Unexpected share/usage: " + selectedBot.getName() + "("
                                        + selectedUsage + ")");
                        }
                        break;
                    case 1:
                        // EDIT
                        Log.d(TAG, "Editing: " + selectedBot.getName());
                        switch (selectedUsage) {
                            case DIRECT_CONTROL:
                                mExtraName = selectedBot.getName();
                                i.setClass(self, com.cellbots.CheckBluetoothEnabledActivity.class);
                                i.putExtra(RobotSelectionActivity.EXTRA_SHOW_WEBONLY_BUTTON, false);
                                startActivityForResult(i,
                                        SelectionIntents.INTENT_BLUECHECK_DIRECT_EDIT.ordinal());
                                break;
                            case BRAIN:
                                mExtraName = selectedBot.getName();
                                i.setClass(self, com.cellbots.CheckBluetoothEnabledActivity.class);
                                i.putExtra(RobotSelectionActivity.EXTRA_SHOW_WEBONLY_BUTTON, false);
                                startActivityForResult(i,
                                        SelectionIntents.INTENT_BLUECHECK_BRAIN_EDIT.ordinal());
                                break;
                            case WEB_CONTROL:
                                i.setClass(self, com.cellbots.RobotCreateWebRemoteActivity.class);
                                i.putExtra(RobotSelectionActivity.EXTRA_EDIT, true);
                                i.putExtra(RobotSelectionActivity.EXTRA_NAME,
                                        selectedBot.getName());
                                startActivityForResult(i,
                                        SelectionIntents.INTENT_WEB_EDIT.ordinal());
                                break;
                            default:
                                Log.d(TAG, "Unexpected edit/usage: " + selectedBot.getName() + "("
                                        + selectedUsage + ")");
                        }
                        break;
                    case 2:
                        // DELETE
                        Log.d(TAG, "Deleting: " + selectedBot.getName());
                        deleteEntryPopup(botIndex);
                        break;
                    default:
                        Log.e(TAG, "Unexpected popup option seen: " + which);
                }
            }
        });
        optionsPopupDialog.setCancelable(true);
        optionsPopupDialog.create().show();
    }

    public void deleteEntryPopup(final int entryIndex) {
        Builder deleteDialog = new Builder(this);
        RobotEntry entry = mRobotProfiles.getEntry(entryIndex);
        deleteDialog.setTitle("Delete " + entry.getName() + "?");
        deleteDialog.setPositiveButton("Delete it", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                mRobotProfiles.remove(entryIndex);
                mRobotProfiles.save();
                refreshRobotList();
            }
        });

        deleteDialog.setNegativeButton("Leave it", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        deleteDialog.setCancelable(false);

        deleteDialog.create().show();
    }

    public void refreshList() {
        ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
        for (int i = 0; i < mRobotProfiles.size(); i++) {
            RobotEntry entry = mRobotProfiles.getEntry(i);
            String description = "";
            if (entry.getType().length() > 0) {
                description = description + "Type: " + entry.getType() + "\n";
            }
            if (entry.getAgentId().length() > 0) {
                description = description + "AgentID: " + entry.getAgentId() + "\n";
            } else if (entry.getGmail().length() > 0) {
                description = description + "Google Talk: " + entry.getGmail() + "\n";
            } else if (entry.getUrl().length() > 0) {
                description = description + "Custom HTTP: " + entry.getUrl() + "\n";
            }
            HashMap<String, Object> entryMap = new HashMap<String, Object>();
            entryMap.put("line1", entry.getName());
            entryMap.put("line2", description);
            Usage usage = entry.getIntendedUsage();
            switch (usage) {
                case WEB_CONTROL:
                    entryMap.put("front", R.drawable.web_small);
                    break;
                case BRAIN:
                    entryMap.put("front", R.drawable.brain_small);
                    break;
                case DIRECT_CONTROL:
                default:
                    entryMap.put("front", R.drawable.remote_small);
            }
            list.add(entryMap);
        }

        setListAdapter(
                new SimpleAdapter(this, list, R.layout.robot_selection_list_row,
                        new String[] { "line1", "line2", "front" },
                        new int[] { R.id.text1, R.id.text2, R.id.icon1 }));

        final Context self = this;
        getListView().setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                // User has selected a robot to control, so
                // transition to the direct control or remote control depending
                // on entry.GetIntendedUsage()...
                Intent i = new Intent();
                RobotEntry entry = mRobotProfiles.getEntry(arg2);
                Usage usage = entry.getIntendedUsage();
                switch (usage) {
                    case DIRECT_CONTROL:
                        mExtraName = entry.getName();
                        i.setClass(self, com.cellbots.CheckBluetoothEnabledActivity.class);
                        i.putExtra(RobotSelectionActivity.EXTRA_SHOW_WEBONLY_BUTTON, false);
                        startActivityForResult(i,
                                SelectionIntents.INTENT_BLUECHECK_DIRECT_CONTROL.ordinal());
                        break;
                    case BRAIN:
                        mExtraName = entry.getName();
                        i.setClass(self, com.cellbots.CheckBluetoothEnabledActivity.class);
                        i.putExtra(RobotSelectionActivity.EXTRA_SHOW_WEBONLY_BUTTON, false);
                        startActivityForResult(i,
                                SelectionIntents.INTENT_BLUECHECK_BRAIN_CONTROL.ordinal());
                        break;
                    case WEB_CONTROL:
                        i.setClass(self, com.cellbots.remote.CellbotRCActivity.class);
                        i.putExtra(EXTRA_NAME, entry.getName());
                        startActivity(i);
                        break;
                    default:
                        Log.e(TAG, "Unexpected usage: " + usage);
                }
            }
        });

        getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                showLongPressOptionsPopup(arg2);
                return true;
            }
        });
    }

    public static void generateQrUri(Context ct, RobotEntry entry) {
        WebView webView = new WebView(ct);
        webView.loadData(ct.getResources().getString(R.string.show_qr_text) + "<br><img src='" +
                "http://chart.apis.google.com/chart?chs=250x250&cht=qr&choe=UTF-8&chl="
                + URLEncoder.encode(entry.serializeForUsage()) + "'></img>",
                "text/html", "UTF-8");
        new AlertDialog.Builder(ct).setTitle("Share").setView(webView)
                .setPositiveButton(android.R.string.cancel, null)
                .create().show();
    }
    
    /**
     * Get the shared preferences so that others can determine global preferences
     * even if they don't have a context or view. To avoid memory leaks,
     * callers should not store the value.
     * 
     * @return SharedPreferences from a call to
     *      PreferenceManager.getDefaultSharedPreferences() 
     */
    public static SharedPreferences getSharedPreferences() {
        return mGlobalPrefs;
    }

}
