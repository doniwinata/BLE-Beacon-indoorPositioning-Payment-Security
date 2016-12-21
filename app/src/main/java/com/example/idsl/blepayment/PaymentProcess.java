package com.example.idsl.blepayment;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
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
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.zip.DataFormatException;

import static android.R.attr.data;

/**
 * Created by Doni on 12/15/2016.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class PaymentProcess extends AppCompatActivity {


    private Button paymentButton;


    //we set the address of Point of sales in the device name.
    private String pos_address="BLE client";
    public static final java.util.UUID SERVICE_UUID = java.util.UUID.fromString("00001111-0000-1000-8000-00805F9B34FB");
    public static final java.util.UUID CHAR_UUID = java.util.UUID.fromString("00002222-0000-1000-8000-00805F9B34FB");
    public TextView connected_device,log_view;
    public String tampLogs = "Logs:\n";
    private SharedPreferences preferences;
    private SharedPreferences.Editor editPref;
    // Initializes Bluetooth adapter.
    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 100000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    private ScanCallback mScanCallback;
    private ProgressDialog dialog;
    private List<BluetoothGattService> services;
    private BluetoothGattCharacteristic characteristicData;
    private final jPake jpake = new jPake(1);
    public byte[] round1;
    public byte[] round2;
    public byte[] round3;
    public byte[] round4;
    public Integer numPackets = 0;
    public byte[] packetData;
    public boolean packetFinish = false;
    public Integer protoclCount = 0;
    public Integer packetSize = 0;
    public Integer packetInteration = 0;

    private static int NOTIFICATION_ID = 0;
    public byte[][] packets;

    @Override
    protected void onCreate(Bundle savedInstanceState){

       super.onCreate(savedInstanceState);
        //check ble activated or not
        SystemRequirementsChecker.checkWithDefaultDialogs(this);
        setContentView(R.layout.payment);
        connected_device = (TextView) findViewById(R.id.tx_connected);
        log_view = (TextView) findViewById(R.id.tx_logs);
        log_view.setMovementMethod(new ScrollingMovementMethod());
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
                //sendData(round1);
                try {
                    updateLogs(round3);
                    sendData(round3);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (DataFormatException e) {
                    e.printStackTrace();
                }
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
                            if(btDevice.getName().equals(pos_address)){
                                connectToDevice(btDevice);
                                if(!jpake.round1) {
                                    try {

                                        round1 = jpake.jpakeRound1();

                                        Log.d("round1", String.valueOf(round1.length));
                                        updateLogs(round1);
                                        jpake.round1 = true;
                                        sendData(round1);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } catch (DataFormatException e) {
                                        e.printStackTrace();
                                    }
                                }

                            }
                        }


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

                                if(device.getName().equals(pos_address)) {
                                    Log.i("lescan detected", pos_address);

                                    connectToDevice(device);
                                    if (!jpake.round1) {
                                        try {
                                            round1 = jpake.jpakeRound1();
                                            Log.d("round1", String.valueOf(round1.length));
                                            jpake.round1=true;
                                            updateLogs(round1);

                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } catch (DataFormatException e) {
                                            e.printStackTrace();
                                        }

                                    }
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
//                if(dialog.isShowing()){
//                    mHandler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            dialog.hide();
//                        }
//                    });
//                }
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            services = mGatt.getServices();
            for(BluetoothGattService service : services){
                if( service.getUuid().equals(SERVICE_UUID)) {
                    characteristicData = service.getCharacteristic(CHAR_UUID);
                    for (BluetoothGattDescriptor descriptor : characteristicData.getDescriptors()) {
                        descriptor.setValue( BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                        mGatt.writeDescriptor(descriptor);
                    }
                    gatt.setCharacteristicNotification(characteristicData, true);
                }
            }
            sendData(round1);
//            if (dialog.isShowing()){
//                mHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        dialog.hide();
//                    }
//                });
//            }
        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            if(packetInteration<packetSize){
                Log.d("packetLength",String.valueOf(packets.length));
                Log.d("interation",packetInteration.toString());
                Log.d("packetSize", String.valueOf(packets[packetInteration].length));
                Log.d("value", new String(packets[packetInteration]));
            characteristicData.setValue(packets[packetInteration]);
            mGatt.writeCharacteristic(characteristicData);
                packetInteration++;
            }else if(jpake.round2 && packetInteration == packetSize && protoclCount == 3){
                    Log.d("packetLength",String.valueOf(packets.length));
                    Log.d("interation",packetInteration.toString());
                    Log.d("packetSize", String.valueOf(packets[packetInteration].length));
                    Log.d("value", new String(packets[packetInteration]));
                    characteristicData.setValue(packets[packetInteration]);
                    mGatt.writeCharacteristic(characteristicData);
                packetInteration++;
            }

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.i("onCharacteristicRead", characteristic.toString());
            byte[] value=characteristic.getValue();
            //read how many packets
            if(numPackets==0) {
                packetFinish=false;
                packetData = new byte[0];
                numPackets = Integer.valueOf(new String(value));
            }else{
                    packetData =combineByte(packetData,value);
                    numPackets--;
                    if(numPackets==0){
                        packetFinish=true;
                       // packetData = null;
                    }
            }
            if(packetFinish){
                System.out.println("PacketLength: "+packetData.length);
            try {
                updateLogs(packetData);
                packetData = null;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (DataFormatException e) {
                e.printStackTrace();
            }
            }
        }
    };
    public byte[] combineByte(byte[] byte1, byte[] byte2){
        byte[] combined = new byte[byte1.length + byte2.length];

        for (int i = 0; i < combined.length; ++i)
        {
            combined[i] = i < byte1.length ? byte1[i] : byte2[i - byte1.length];
        }
        return combined;
    }
    public void updateLogs(byte[] message) throws IOException, DataFormatException {
        byte[] decomp = compress.decompress(message);
        System.out.println(message.length);
        System.out.println(decomp.length);
        ByteArrayInputStream bais = new ByteArrayInputStream(decomp);
        DataInputStream in = new DataInputStream(bais);
        List<String> result = new ArrayList<String>();
        while (in.available() > 0) {
            String element = in.readUTF();
            result.add(element);
            Log.d("hasil",element);
            if(element.equals("1")){
                tampLogs+="\nround 1(X1,X2,ZKP1,ZKP2)";
            }else if(element.equals("2")){
                tampLogs+="\nround 2 Received (X3,X4,ZKP3,ZKP4)";
            }

            if(element.length()>30){
                element = "("+element.length()+")"+element.substring(0,20)+"...";
            }
            tampLogs+="\n"+element;

        }

        jpake.updateValue(result);
        protoclCount++;
        if(protoclCount==2 && jpake.round2 == false){
            //means ready for round 2
            round2 = jpake.jpakeRound2();
            Log.d("protocol", "round 2 running");
            updateLogs(round2);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            jpake.round2=true;
            sendData(round2);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(jpake.finalSKey!="" &&protoclCount == 4 && !jpake.round3 ){
                    tampLogs+="\n------------------- \nFinal Key: "+jpake.getfinalSKey()+"\n Final Nonce: "+jpake.getFinalNonce();
                    try {
                        round3 = jpake.pprotocolRound1();
                        jpake.round3 =true;
                        if(dialog.isShowing()){
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.hide();
                                }
                            });
                        }
//                        updateLogs(round3);
//                        sendData(round3);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }else if (protoclCount == 6 && !jpake.round4){
                    try {
                        round4 = jpake.pprotocolRound2();
                        jpake.round4 =true;
                        updateLogs(round4);
                        sendData(round4);
                        sendNotification("Payment Complete !");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (DataFormatException e) {
                        e.printStackTrace();
                    }
                }
                log_view.setText(tampLogs);

            }
        });

    }


    public void sendData(byte [] data){
        packetInteration =0;
        int chunksize = 20;
        Log.d("dataLength",String.valueOf(data.length));
        packetSize = (int) Math.ceil( data.length / (double)chunksize);
        Log.d("packetSize",packetSize.toString());
        packets = new byte[packetSize][chunksize];
        Integer start = 0;
        int x =0;
        if(jpake.round2 && protoclCount == 3){
            //bugs on second protocol, first byte not send, sowe made dummy bytes
            //packetSize;
            x=1;
            packets = new byte[packetSize+1][chunksize];
            packets[0] = new byte[5];
        }
        for(int i = x; i < packets.length; i++) {
            int end = start+chunksize;
            if(end>data.length){end = data.length;}
            packets[i] = Arrays.copyOfRange(data,start, end);
            start += chunksize;
        }
        characteristicData.setValue(packetSize.toString().getBytes());
        mGatt.writeCharacteristic(characteristicData);
        mGatt.executeReliableWrite();

        //packets will be send in onCharacteristicRead.
    }

    private void sendNotification(String message){
        NotificationManager mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(getString(R.string.app_name))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message))
                        .setAutoCancel(true)
                        .setContentText(message);
        Notification note = mBuilder.build();
        note.defaults |= Notification.DEFAULT_VIBRATE;
        note.defaults |= Notification.DEFAULT_SOUND;
        mNotificationManager.notify(NOTIFICATION_ID++, note);
    }

}
