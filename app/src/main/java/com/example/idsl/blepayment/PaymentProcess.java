package com.example.idsl.blepayment;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;

import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Doni on 12/15/2016.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class PaymentProcess extends AppCompatActivity {
   // private static final int REQUEST_ENABLE_BT = 1234;
    private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);

    private Button paymentButton;
    //we set the address of Point of sales in the device name.
    private String pos_address="BLE client";
    public static final java.util.UUID SERVICE_UUID = java.util.UUID.fromString("00001111-0000-1000-8000-00805F9B34FB");
    public static final java.util.UUID CHAR_UUID = java.util.UUID.fromString("00002222-0000-1000-8000-00805F9B34FB");


    private SharedPreferences preferences;
    private SharedPreferences.Editor editPref;
    // Initializes Bluetooth adapter.
    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 30000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    private ScanCallback mScanCallback;
    private ProgressDialog dialog;
    private List<BluetoothGattService> services;
    private BluetoothGattCharacteristic characteristicData;

    @Override
    protected void onCreate(Bundle savedInstanceState){

       super.onCreate(savedInstanceState);
        //check ble activated or not
        SystemRequirementsChecker.checkWithDefaultDialogs(this);

        setContentView(R.layout.payment);
        dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        dialog.setMessage("Loading");
        dialog.show();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editPref = preferences.edit();

        mHandler = new Handler();
        //scanning process and check whether enter payment area, if yes so we enable our payment button
        // and establishing ble connection to pos
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        paymentButton = (Button) findViewById(R.id.payment_button);
        paymentButton.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {

                characteristicData.setValue("test".getBytes());
                mGatt.writeCharacteristic(characteristicData);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SystemRequirementsChecker.checkWithDefaultDialogs(this);
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<ScanFilter>();
            }
            scanLeDevice(true);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onDestroy() {
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            Log.d("scan le device", "true");
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {

                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    } else {
                        mScanCallbackfunc();
                        Log.d("sdk","masuk");
                        mLEScanner.stopScan(mScanCallback);

                    }
                }
            }, SCAN_PERIOD);
            if (Build.VERSION.SDK_INT < 21) {
                Log.d("start less than 21", "le scan");

                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                mScanCallbackfunc();
                Log.d("sdk","masuk");
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {

                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                mScanCallbackfunc();
                Log.d("sdk","masuk");
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }

    @Override
    protected void onStart(){
        super.onStart();
        SystemRequirementsChecker.checkWithDefaultDialogs(this);


    }

        private void mScanCallbackfunc(){
            if (Build.VERSION.SDK_INT >= 21) {
                mScanCallback = new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        Log.i("callbackType", String.valueOf(callbackType));
                        Log.i("result", result.toString());
                        Log.i("onLeScan", result.toString());

                        BluetoothDevice btDevice = result.getDevice();
                        if(btDevice.getName()!=null){
                            Log.i("btName", btDevice.getName());
                        }
                        connectToDevice(btDevice);

                    }

                    @Override
                    public void onBatchScanResults(List<ScanResult> results) {
                        for (ScanResult sr : results) {
                            Log.i("ScanResult - Results", sr.toString());
                        }
                    }

                    @Override
                    public void onScanFailed(int errorCode) {
                        Log.e("Scan Failed", "Error Code: " + errorCode);
                    }
                };
            }
        }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("onLeScan", device.toString());
                            if(device.getName()!=null){
                            Log.i("btName", device.getName());

                                if(device.getName().equals(pos_address)){
                                    Log.i("lescan detected",pos_address);
                                    connectToDevice(device);
                                }
                            }

                        }
                    });
                }
            };

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false);// will stop after first device detection
        }
    }


    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(newState == BluetoothProfile.STATE_CONNECTED) {
                mGatt.discoverServices();
            }else{
                if(dialog.isShowing()){
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            dialog.hide();
                        }
                    });
                }
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            services = mGatt.getServices();

            for(BluetoothGattService service : services){
                if( service.getUuid().equals(SERVICE_UUID)) {
                    Log.d("Andrey", "Uuid = " + service.getUuid().toString());

                    characteristicData = service.getCharacteristic(CHAR_UUID);
                    for (BluetoothGattDescriptor descriptor : characteristicData.getDescriptors()) {
                        descriptor.setValue( BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                        mGatt.writeDescriptor(descriptor);
                    }
                    gatt.setCharacteristicNotification(characteristicData, true);
                }
            }
            if (dialog.isShowing()){
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        dialog.hide();
                    }
                });
            }


        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());
            byte[] value=characteristic.getValue();
            String v = new String(value);
            Log.i("onCharacteristicRead", "Value: " + v);
        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
           // super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d("onCharacteristicWrite","before execute");
            mGatt.executeReliableWrite();
           /// gatt.setCharacteristicNotification(characteristic, true);
            gatt.readCharacteristic(characteristic);

            //setNotifySensor(mGatt);
            Log.d("charnya",new String(characteristicData.getValue()));
            Log.d("onCharacteristicRead","after execute");


        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

        }
    };


}
