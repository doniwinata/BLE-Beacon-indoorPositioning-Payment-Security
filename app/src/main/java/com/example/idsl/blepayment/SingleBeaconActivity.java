package com.example.idsl.blepayment;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.connection.DeviceConnection;
import com.estimote.sdk.connection.DeviceConnectionProvider;

/**
 * Created by IDSL on 2016/11/30.
 */

public class SingleBeaconActivity  extends Activity{
    private Beacon beacon;
    private DeviceConnectionProvider connection;
    private int power;
    private int interval;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.single_beacon_screen);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor editPref = preferences.edit();

        //get beacon from the intent.
        beacon = getIntent().getParcelableExtra("getBeacon");

        //set up read/write connection to the beacon.
       // connection = new DeviceConnection(this, beacon, )
    }

    // sets up a connection to the beacon and its events on authentication or its failure.
    /*private DeviceConnectionProvider.ConnectionProviderCallback createConnectionCallback() {
        return new DeviceConnectionProvider.ConnectionProviderCallback() {
            @Override
            public void onConnectedToService() {
                Log.d("Connect", "Connected to device");
            }
            @Override
            public void onAuthenticated( final DeviceConnection connection){
                connection.edit()

            }
        }

    }*/
}
