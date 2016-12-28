package com.example.idsl.blepayment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;
import com.estimote.sdk.Utils;
import com.estimote.sdk.eddystone.Eddystone;
import com.estimote.sdk.repackaged.android_21.ScanRecord;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1234;
    private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);

    private Button beaconlistButton;

    private ArrayList<Integer> minorValues = new ArrayList<Integer>();
    private HashMap<Integer, ArrayList<Double>> distances = new HashMap<Integer, ArrayList<Double>>();

    private BeaconManager beaconManager;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editPref = preferences.edit();

        beaconlistButton = (Button) findViewById(R.id.beacon_list);

        //A button to BeaconListActivity, to get a list of all beacons.
        beaconlistButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
            final Intent beaconListIntent = new Intent(MainActivity.this, BeaconListActivity.class);
                try {
                    beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
                } catch (Exception e) {
                }
                editPref.putBoolean("intent_stop", false);
                editPref.commit();
                startActivity(beaconListIntent);

            }
        });

        final Button mapPosButton = (Button) findViewById(R.id.map_position);

        //A button to MapPosActivity, to see your location on a map.
        mapPosButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                final Intent mapPosItent = new Intent(MainActivity.this, MapPosActivity.class);

                //stop Ranging/Searching for beacons.
                try {
                    beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
                } catch (Exception e) {
                }
                editPref.putBoolean("intent_stop", false);
                editPref.commit();
                startActivity(mapPosItent);

            }
        });


        final TableLayout.LayoutParams paramTableRow = new TableLayout.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT);
        paramTableRow.setMargins(50, 0, 0, 0);
        final TableRow.LayoutParams paramTextView = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);

        //go to payment
        final Button payment = (Button) findViewById(R.id.payment_activity);
        payment.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent paymentIntent = new Intent(MainActivity.this, PaymentProcess.class);

                    try {
                    beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
                } catch (Exception e) {
                }
                editPref.putBoolean("intent_stop", false);
                editPref.commit();
                startActivity(paymentIntent);
            }
        });

        //Decimal format to show the distance with only 4 decimals.
        final DecimalFormat df = new DecimalFormat("#0.####");
//        final Button measureButton = (Button) findViewById(R.id.single_measure);
//
//        //The Button will calculate the distance on each beacon and show this in a TextView.
//
//        measureButton.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View v){
//                distanceTable.removeAllViews();
//
//                TableRow textRow = new TableRow(MainActivity.this);
//                textRow.setLayoutParams(paramTableRow);
//
//                TextView minorStringText = new TextView(MainActivity.this);
//                minorStringText.setText("Minor");
//                minorStringText.setLayoutParams(paramTextView);
//
//                TextView avgStringText = new TextView(MainActivity.this);
//                avgStringText.setText("Average");
//                avgStringText.setLayoutParams(paramTextView);
//
//                TextView medStringText = new TextView(MainActivity.this);
//                medStringText.setText("Median");
//                medStringText.setLayoutParams(paramTextView);
//
//                textRow.addView(minorStringText);
//                textRow.addView(avgStringText);
//                textRow.addView(medStringText);
//                distanceTable.addView(textRow);
//
//                // Calculate the distance for each beacon and add this to the TextView.
//                for (int i = 0; i < minorValues.size(); i ++) {
//                    int minorVal = minorValues.get(i);
//                    ArrayList<Double> dist = distances.get(minorVal);
//                    double avg = average(dist);
//                    double med = median(dist);
//
//                    TableRow newRow = new TableRow(MainActivity.this);
//                    newRow.setLayoutParams(paramTableRow);
//
//                    TextView minorText = new TextView(MainActivity.this);
//                    minorText.setText(String.valueOf(minorVal));
//                    minorText.setLayoutParams(paramTextView);
//
//                    TextView avgText = new TextView(MainActivity.this);
//                    avgText.setText(String.valueOf(df.format(avg)));
//                    avgText.setLayoutParams(paramTextView);
//
//                    TextView medText = new TextView(MainActivity.this);
//                    medText.setText(String.valueOf(df.format(med)));
//                    medText.setLayoutParams(paramTextView);
//
//                    newRow.addView(minorText);
//                    newRow.addView(avgText);
//                    newRow.addView(medText);
//                    distanceTable.addView(newRow);
//                };
//            }
//        });





        beaconManager = new BeaconManager(this);
        beaconManager.setForegroundScanPeriod(100,0);
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, final List<Beacon> beacons) {
                runOnUiThread(new Runnable(){
                    @Override
                    public void run(){
                        beaconlistButton.setText("List of beacons (Found: " + beacons.size() +")");
                        //add distances to the ArrayLists.

                        for(int i = 0; i <beacons.size(); i++){
                            Beacon currentBeacon = beacons.get(i);
                            Double distance = Utils.computeAccuracy(currentBeacon);
                            int minor = currentBeacon.getMinor();

                            //check if the beacon is just found.

                            if(!distances.containsKey(minor)){
                                minorValues.add(minor);
                                distances.put(minor, new ArrayList<Double>());
                            }

                            //Add the distance to the ArrayList and maintain a certain size.

                            ArrayList<Double> distanceToBeacon = distances.get(minor);
                            distanceToBeacon.add(distance);

                            if(distanceToBeacon.size() > 100){
                                distanceToBeacon.remove(0);
                            }
                            distances.put(minor, distanceToBeacon);
                        }
                    }
                });

            }
        });


        ///////////////////


    }

    /*
   * Calculate the distance to beacon by taking the median.
   */
    private double median(ArrayList<Double> distanceToBeacon) {
        Collections.sort(distanceToBeacon);
        int half = (int) (0.5 * distanceToBeacon.size());
        if (distanceToBeacon.size() == 1) {
            half = 0;
        }
        return distanceToBeacon.get(half);
    }

    /*
     * Calculate the distance to beacon by taking the average.
     */
    private double average(ArrayList<Double> distanceToBeacon) {
        double total = 0;

        for (double element : distanceToBeacon) {
            total += element;
        }
        return total / distanceToBeacon.size();
    }

    @Override
    protected void onDestroy() {
        beaconManager.disconnect();

        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        SystemRequirementsChecker.checkWithDefaultDialogs(this);

        connectToService();
    }


    @Override
    protected void onResume() {
        super.onResume();

        SystemRequirementsChecker.checkWithDefaultDialogs(this);


    }

    @Override
    protected void onPause(){

        super.onPause();
    }

    @Override
    protected void onStop() {
        // Quit asking for results.
        Boolean intentStop = preferences.getBoolean("intent_stop", true);
        if (intentStop) {
            try {
                beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
            } catch (Exception e) {
            }
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
                beaconlistButton.setText("List of beacons (No Bluetooth)");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void connectToService() {
        // Retrieve the beacons.
        beaconlistButton.setText("List of beacons (Scanning...)");
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startRanging(ALL_ESTIMOTE_BEACONS_REGION);
                } catch (Exception e) {
                    beaconlistButton.setText("List of beacons (Error)");
                }
            }
        });
    }
}
