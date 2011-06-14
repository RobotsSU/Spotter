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

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Allow a user to select a bluetooth device to use for the robot connection.
 * This code also allows a user to scan for bluetooth devices as well as pair
 * with them.
 * 
 * @author css@google.com (Charles Spirakis)
 */
public class FindBluetoothDevicesActivity extends Activity {
    private static final String TAG = "FindBluetoothDevicesActivity";
    
    public static final int RESULT_OK = Activity.RESULT_OK;

    public static final int RESULT_CANCEL = Activity.RESULT_CANCELED;

    public static final int RESULT_FAILURE = 100;

    public static final String EXTRA_BLUETOOTH_NAME = "BT_NAME";

    public static final String EXTRA_BLUETOOTH_MAC = "BT_MAC";

    private static final int INTENT_ENABLE_BLUETOOTH = 1;
    
    private ListView mPairedDevicesListView;

    private ListView mDiscoveredListView;

    private Button mScanDevices;

    private Button mCancel;

    private BluetoothAdapter mBtAdapter;

    // Can be empty, but can never be null.
    ArrayList<HashMap<String, Object>> mDiscoveredDevices =
            new ArrayList<HashMap<String, Object>>();

    private Activity mActivity;

    // mBtDevices should never be null. It is ok for it to be an empty set, but
    // it should never be null.
    private Set<BluetoothDevice> mBtDevices = new HashSet<BluetoothDevice>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.find_bluetooth_devices);

        setTitle("Select a device");

        mActivity = this;

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        mPairedDevicesListView = (ListView) findViewById(R.id.blue_paired);
        mPairedDevicesListView.setOnItemClickListener(mDeviceClickListener);

        mScanDevices = (Button) findViewById(R.id.blue_scan);
        mScanDevices.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Scan for devices");
                mScanDevices.setEnabled(false);
                // TODO(css): Instead of wiping this list with each scan,
                // could walk the list when an item is found and add it if it
                // doesn't already exist.
                mDiscoveredDevices.clear();
                mDiscoveredListView.setAdapter(new SimpleAdapter(mActivity, mDiscoveredDevices,
                        R.layout.bluetooth_device_list_row, new String[] {
                                "line1", "line2", "front"
                        }, new int[] {
                                R.id.text1, R.id.text2, R.id.icon1
                        }));
                doDiscovery();
            }
        });

        mCancel = (Button) findViewById(R.id.blue_cancel);
        mCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCEL);
                finish();
            }
        });

        mDiscoveredListView = (ListView) findViewById(R.id.new_devices);
        mDiscoveredListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBtAdapter == null) {
            Log.e(TAG, "No bluetooth default adapter");
            setResult(FindBluetoothDevicesActivity.RESULT_FAILURE);
            finish();
        }

        if (!mBtAdapter.isEnabled()) {
            Log.w(TAG, "Need bluetooth enabled to find bluetooth devices.");
            Intent i = new Intent();
            i.setClass(this, com.cellbots.CheckBluetoothEnabledActivity.class);
            startActivityForResult(i, INTENT_ENABLE_BLUETOOTH);
        } else {
            mBtDevices = mBtAdapter.getBondedDevices();
            // Guarantee non-null result. Use size() to determine if the set
            // is empty or not.
            if (mBtDevices == null) {
                mBtDevices = new HashSet<BluetoothDevice>();
            }
            refreshDeviceList();
        }
    }

    private void refreshDeviceList() {
        ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
        for (BluetoothDevice device : mBtDevices) {
            String name = device.getName();
            String description = device.getAddress();
            HashMap<String, Object> entryMap = new HashMap<String, Object>();
            entryMap.put("line1", name);
            entryMap.put("line2", description);
            int majorClass = BluetoothClass.Device.Major.UNCATEGORIZED;
            if (device.getBluetoothClass() != null) {
                majorClass = device.getBluetoothClass().getMajorDeviceClass();
            }
            switch (majorClass) {
                case BluetoothClass.Device.Major.COMPUTER:
                    entryMap.put("front", R.drawable.device_computer);
                    break;
                case BluetoothClass.Device.Major.PHONE:
                    entryMap.put("front", R.drawable.device_phone);
                    break;
                case BluetoothClass.Device.Major.NETWORKING:
                case BluetoothClass.Device.Major.PERIPHERAL:
                    entryMap.put("front", R.drawable.device_comm);
                    break;
                case BluetoothClass.Device.Major.WEARABLE:
                case BluetoothClass.Device.Major.HEALTH:
                    entryMap.put("front", R.drawable.device_health);
                    break;
                case BluetoothClass.Device.Major.UNCATEGORIZED:
                default:
                    entryMap.put("front", R.drawable.device_unknown);
            }
            list.add(entryMap);
        }

        mPairedDevicesListView.setAdapter(new SimpleAdapter(this, list,
                R.layout.bluetooth_device_list_row, new String[] {
                        "line1", "line2", "front"
                }, new int[] {
                        R.id.text1, R.id.text2, R.id.icon1
                }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        Log.d(TAG, "doDiscovery()");

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle("Scanning...");

        // Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case INTENT_ENABLE_BLUETOOTH:
                if (resultCode == CheckBluetoothEnabledActivity.RESULT_SUCCESS) {
                    mBtDevices = mBtAdapter.getBondedDevices();
                    // Guarantee non-null result. Use size() to determine if the
                    // set is empty or not.
                    if (mBtDevices == null) {
                        mBtDevices = new HashSet<BluetoothDevice>();
                    }
                    refreshDeviceList();
                } else {
                    Log.w(TAG, "User did not enable bluetooth");
                    Toast.makeText(this, "Bluetooth must be enabled to find bluetooth devices,",
                            Toast.LENGTH_LONG).show();
                }
                break;
            default:
                Log.e(TAG, "Unepxected request code:" + requestCode);
        }
    }

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed
                // already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    String name = device.getName();
                    String description = device.getAddress();
                    HashMap<String, Object> entryMap = new HashMap<String, Object>();
                    entryMap.put("line1", name);
                    entryMap.put("line2", description);
                    int majorClass = BluetoothClass.Device.Major.UNCATEGORIZED;
                    if (device.getBluetoothClass() != null) {
                        majorClass = device.getBluetoothClass().getMajorDeviceClass();
                    }
                    switch (majorClass) {
                        case BluetoothClass.Device.Major.COMPUTER:
                            entryMap.put("front", R.drawable.device_computer);
                            break;
                        case BluetoothClass.Device.Major.PHONE:
                            entryMap.put("front", R.drawable.device_phone);
                            break;
                        case BluetoothClass.Device.Major.NETWORKING:
                        case BluetoothClass.Device.Major.PERIPHERAL:
                            entryMap.put("front", R.drawable.device_comm);
                            break;
                        case BluetoothClass.Device.Major.WEARABLE:
                        case BluetoothClass.Device.Major.HEALTH:
                            entryMap.put("front", R.drawable.device_health);
                            break;
                        case BluetoothClass.Device.Major.UNCATEGORIZED:
                        default:
                            entryMap.put("front", R.drawable.device_unknown);
                    }
                    mDiscoveredDevices.add(entryMap);
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle("Select a device");
                mScanDevices.setEnabled(true);

                if (mDiscoveredDevices.size() <= 0) {
                    // TODO(css): What to display if there are no
                    // discovered devices?
                }
            }

            mDiscoveredListView.setAdapter(new SimpleAdapter(mActivity, mDiscoveredDevices,
                    R.layout.bluetooth_device_list_row, new String[] {
                            "line1", "line2", "front"
                    }, new int[] {
                            R.id.text1, R.id.text2, R.id.icon1
                    }));
        }
    };

    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();

            // NOTE: Walking this view is heavily dependant on the structure
            // from bluetooth_device_list_row.xml
            // Any changes there affects the code here.
            LinearLayout topLayout = (LinearLayout) v;

            LinearLayout oneLine = (LinearLayout) topLayout.getChildAt(0);

            LinearLayout nameMacSet = (LinearLayout) oneLine.getChildAt(1);

            String name = ((TextView) nameMacSet.getChildAt(0)).getText().toString();

            String mac = ((TextView) nameMacSet.getChildAt(1)).getText().toString();

            // This device might or might not be on the paired list.
            // If it isn't paired, force pairing by trying to connect to the
            // device.
            boolean isPaired = false;

            for (BluetoothDevice device : mBtDevices) {
                String bondName = device.getName();
                String bondAddress = device.getAddress();

                if (bondName.equals(name) && bondAddress.equals(mac)) {
                    isPaired = true;
                    break;
                }
            }

            // Try forcing the pairing.
            if (!isPaired) {
                try {
                    BluetoothDevice device = mBtAdapter.getRemoteDevice(mac);
                    try {
                        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(UUID
                                .fromString("00001101-0000-1000-8000-00805F9B34FB"));
                        socket.connect();
                        socket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to connect to device: "
                                + name + " " + mac);
                        if (e.getLocalizedMessage() != null) {
                            Log.e(TAG, "connect details: " + name
                                  + " " + mac + " " + e.getLocalizedMessage());
                        }
                        showPairingProblemDialog(name, mac);
                        return;
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Bluetooth device doesn't exist: " + name + " " + mac);
                    showPairingProblemDialog(name, mac);
                    return;
                }
            }

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_BLUETOOTH_NAME, name);
            intent.putExtra(EXTRA_BLUETOOTH_MAC, mac);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    private void showPairingProblemDialog(String name, String addr) {
        AlertDialog alertDialog = new AlertDialog.Builder(mActivity).create();
        alertDialog.setTitle("Problem with pairing");
        alertDialog.setMessage("There was a problem pairing to "
                + name + " (" + addr + ")");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
        alertDialog.show();
    }
}
