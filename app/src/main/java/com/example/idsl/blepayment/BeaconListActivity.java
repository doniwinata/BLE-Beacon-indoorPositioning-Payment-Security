package com.example.idsl.blepayment;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IDSL on 2016/11/28.
 */

public class BeaconListActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1234;
    private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);

    private BeaconManager beaconManager;
    private BeaconListAdapter listViewAdapter;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("new intent","success");
        super.onCreate(savedInstanceState);
        Log.d("new intent","success1");
        setContentView(R.layout.main);
        Log.d("new intent","success2");
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Log.d("new intent","success3");

        // ActionBar actionBar =  getSupportActionBar();
        //actionBar.setDisplayHomeAsUpEnabled(true);


       // getActionBar().setDisplayHomeAsUpEnabled(true);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editPref = preferences.edit();
        Log.d("new intent","success3");
        //set the custom listview adapter

        listViewAdapter = new BeaconListAdapter(this);

        ListView beaconListView = (ListView) findViewById(R.id.device_list);
        beaconListView.setAdapter(listViewAdapter);
        beaconListView.setClickable(true);
        Log.d("new intent","success4");
        //set a dialogAlerter to change values of a beaocn
        beaconListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parentView, View childView, int position, long id) {
                Intent intentSingleBeacon = new Intent(BeaconListActivity.this, SingleBeaconActivity.class);
                intentSingleBeacon.putExtra("getBeacon", listViewAdapter.getItem(position));
                startActivity(intentSingleBeacon);
            }
        });

        //configure beacon manager

        beaconManager = new BeaconManager(this);
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, final List<Beacon> beaconList) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //the beacon are sorted based on distance from the user.

                        getSupportActionBar().setSubtitle("Found beacons: "+ beaconList.size());
                        List<Beacon> sortedBeaconList = sortBeaconsOnMinor(beaconList);

                        // Refresh the listViewAdapter with new beacons.
                        listViewAdapter.replaceWith(sortedBeaconList);
                    }
                });

            }
        });
    }
    //sort the beacons based on their minor.

    private List<Beacon> sortBeaconsOnMinor (List<Beacon> beaconList){
        Map<Integer, Beacon> map1 = new HashMap<Integer, Beacon>();
        List<Integer> minorValueList = new ArrayList<Integer>();
        for(int i =0; i <beaconList.size();i++){
            minorValueList.add(beaconList.get(i).getMinor());
            map1.put(minorValueList.get(i),beaconList.get(i));
        }

        //sort the list with minor values and rebuild the beacon list using the HashMap.
        Collections.sort(minorValueList);
        List<Beacon> sortedBeaconList = new ArrayList<Beacon>();
        for(int i = 0; i< beaconList.size();i++){
            sortedBeaconList.add(map1.get(minorValueList.get(i)));
        }
        return sortedBeaconList;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(item.getItemId() == android.R.id.home){
            final Intent intentHome = new Intent(BeaconListActivity.this, MainActivity.class);
            try {
                beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
            } catch (Exception e) {
            }
            editPref.putBoolean("intent_stop", false);
            editPref.commit();

            startActivity(intentHome);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy(){
        beaconManager.disconnect();
        super.onDestroy();
    }

    @Override
    protected void onStart(){
        super.onStart();
        SystemRequirementsChecker.checkWithDefaultDialogs(this);

        connectToService();
    }
    @Override
    protected void onStop(){
        Boolean intentStop = preferences.getBoolean("intent_stop",true);
        if(intentStop){
            try{
                beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
            }catch (Exception e){}
        }
        editPref.remove("intent_stop");
        editPref.commit();
        super.onStop();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Request Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                connectToService();
            } else {
                getSupportActionBar().setSubtitle("Bluetooth not enabled");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void connectToService() {
        // Retrieve the beacons.
        getSupportActionBar().setSubtitle("Scanning...");
        listViewAdapter.replaceWith(Collections.<Beacon>emptyList());
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startRanging(ALL_ESTIMOTE_BEACONS_REGION);
                } catch (Exception e) {
                    getSupportActionBar().setSubtitle("Can't start ranging");
                }
            }
        });
    }

}
