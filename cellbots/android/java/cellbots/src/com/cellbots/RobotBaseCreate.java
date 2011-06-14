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

import com.cellbots.local.CellDroidManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.Set;

/**
 * Base class that includes methods useful for various robot profile
 * creation activities.
 * 
 * @author css@google.com (Charles Spirakis)
 *
 */
public class RobotBaseCreate extends Activity {

    private static final String TAG = "RobotBaseCreate";

    protected static final Uri BARCODE_SCANNER_MARKET_URI = Uri.parse(
            "market://search?q=pname:com.google.zxing.client.android");

    protected static final int INTENT_START_SCANNER = 100;

    protected RobotList mRobotProfiles;

    protected SharedPreferences mPrefs;

    protected boolean mIsEdit;

    protected String mName;

    protected Bundle mExtras;

    // Commenting out App Engine until that piece is ready.
    protected String[] mCommChoices = new String[] { /*"AppEngine",*/ "Google Talk", "Custom HTTP" };

    protected String[] mCellbotTypes;

    protected String[] mCellbotBts;

    /**
     * Do the basics that are required for creation classes. This includes
     * getting the list of robots available, determining if this was
     * an edit or create call, getting the name passed to us (if available),
     * etc.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = getSharedPreferences(LauncherActivity.PREFERENCES_NAME, MODE_PRIVATE);
        mRobotProfiles = new RobotList(mPrefs);
        mRobotProfiles.load();

        mExtras = getIntent().getExtras();
        if (mExtras != null) {
            mIsEdit = mExtras.getBoolean(RobotSelectionActivity.EXTRA_EDIT, false);
            mName = mExtras.getString(RobotSelectionActivity.EXTRA_NAME);
            if (mName == null) {
                mName = "";
            }
        } else {
            // Missing extras means we are creating which also means
            // we have no default name to start with.
            mIsEdit = false;
            mName = "";
        }
    }

    protected void setupCancelButton(Activity me, int id) {
        final Activity self = me;
        Button cancelButton = (Button) findViewById(id);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                self.setResult(RobotSelectionActivity.RESULT_CANCEL);
                self.finish();
            }
        });
    }

    protected void setupRobotTypeList(Context ctx, Spinner spin) {
        mCellbotTypes = CellDroidManager.getControllers(ctx);
        if (mCellbotTypes == null) {
            mCellbotTypes = new String[0];
        }
        buildSpinner(spin, mCellbotTypes);
    }

    protected void setupBluetoothPairList(Spinner spin) {
        mCellbotBts = getPairedBluetooth();
        buildSpinner(spin, mCellbotBts);
    }

    protected void setupCommSpinner(Spinner cspin) {
        buildSpinner(cspin, mCommChoices);
    }

    /**
     * Start up the barcode scanner. If the scanner can't be found,
     * provide a dialog alert allowing the user to install via
     * the marketplace
     * 
     * @param me activity that is initiating the scanner
     * @param id layout id for the button which initiates the scan request
     */
    protected void setupQrScanButton(Activity me, int id) {
        final Activity self = me;
        Button scanButton = (Button) findViewById(id);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch scanner, result returned to #onActivityResult.
                Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                try {
                    startActivityForResult(intent, INTENT_START_SCANNER);
                } catch (ActivityNotFoundException ex) {
                    // TODO(ptucker): i18n
                    // Scanner not found, prompt to install it.
                    new AlertDialog.Builder(self).setTitle("Error")
                            .setMessage("Barcode Scanner not found.")
                            .setPositiveButton("Install", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivity(new Intent(
                                            Intent.ACTION_VIEW, BARCODE_SCANNER_MARKET_URI));
                                }
                            }).create().show();
                }
            }
        });
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // If it wasn't for us or it was for us and the user cancel'ed out,
        // then let the parent handle it and just leave.
        if ((requestCode != INTENT_START_SCANNER) || (resultCode != Activity.RESULT_OK)) {
            super.onActivityResult(requestCode, resultCode, intent);
            return;
        }

        // Assume this is the QR code result.
        String scannedResult = intent.getStringExtra("SCAN_RESULT");
        if (scannedResult == null || scannedResult.length() == 0) {
            // TODO(ptucker): i18n
            Toast.makeText(getApplicationContext(), "No ID found.", Toast.LENGTH_LONG).show();
            super.onActivityResult(requestCode, resultCode, intent);
            return;
        }

        RobotEntry entry = new RobotEntry();
        entry.applySerialized(scannedResult);
        robotScanResult(entry);
    }

    /**
     * After a scan has occurred, this method is called with the values that
     * were scanned in. Should be overridden by children who want the
     * scanned in data.
     *
     * @param scannedEntry scanned in RobotEntry. Only the data from the scan
     * is included. The callee should take what they need and fill in defaults
     * for missing values that are needed.
     */
    protected void robotScanResult(RobotEntry scannedEntry) {
        Log.e(TAG, "This method should be overridden for children "
                + "that want to scan robot profiles");
    }

    /**
     * Builds a spinner from a Spinner and a list of string.
     * 
     * @param spinner The Spinner
     * @param list list of strings. Cannot be null, but can be an array of
     *  zero length
     */
    protected void buildSpinner(Spinner spinner, String[] list) {
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
                this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (String item : list) {
            adapter.add(item);
        }
        spinner.setAdapter(adapter);
    }

    protected String getStringFromField(Object object) {
        if (object != null) {
            return object.toString();
        } else {
            return "";
        }
    }
    
    public static String[] getPairedBluetooth() {
        BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            return new String[0];
        }
        Set<BluetoothDevice> devices = mBtAdapter.getBondedDevices();
        if (devices == null || devices.size() == 0) {
            return new String[0];
        }
        String[] deviceNames = new String[devices.size()];
        int i = 0;
        for (BluetoothDevice d : devices) {
            deviceNames[i++] = RobotEntry.generateNameMacPair(d.getName(), d.getAddress());
        }
        return deviceNames;
    }
}
