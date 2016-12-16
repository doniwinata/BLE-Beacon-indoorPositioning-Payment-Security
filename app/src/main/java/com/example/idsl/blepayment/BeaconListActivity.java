package com.example.idsl.blepayment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IDSL on 2016/11/28.
 */

public class BeaconListActivity extends Activity {

    private static final int REQUEST_ENABLE_BT = 1234;
    private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);

    private BeaconManager beaconManager;
    private BeaconListAdapter listViewAdapter;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editPref = preferences.edit();

        //set the custom listview adapter

        listViewAdapter = new BeaconListAdapter(this);
        ListView beaconListView = (ListView) findViewById(R.id.device_list);
        beaconListView.setAdapter(listViewAdapter);
        beaconListView.setClickable(true);

        //set a dialogAlerter to change values of a beaocn
        //pending

        //configure beacon manager

        beaconManager = new BeaconManager(this);
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, final List<Beacon> beaconList) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //the beacon are sorted based on distance from the user.
                        getActionBar().setSubtitle("Found beacons: "+ beaconList.size());
                        List<Beacon>  sortedBeaconList =
                    }
                });

            }
        });
    }
    //sort the beacons based on their minor.

    private List<Beacon> sortBeaconOnMinor(List<Beacon> beaconList){
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
    }

}
