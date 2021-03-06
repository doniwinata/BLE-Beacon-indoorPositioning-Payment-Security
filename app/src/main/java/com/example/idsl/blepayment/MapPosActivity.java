package com.example.idsl.blepayment;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;
import com.estimote.sdk.Utils;
import com.estimote.sdk.cloud.model.Device;
import com.estimote.sdk.internal.Flags;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by IDSL on 2016/11/28.
 */

public class MapPosActivity extends AppCompatActivity {
    private SharedPreferences preferences;
    private SharedPreferences.Editor editPref;


    //Bluetooth and Beacon Constant.
    private static final int REQUEST_ENABLE_BT = 1234;
    private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);


    private BeaconManager beaconManager;

    private Bitmap workingBitmap;

    private float mapXSize;
    private float mapYSize;
    //mode for calculation of observed distance
    //0: real distance, 1: Average, 2: Median
    public int Mode = 0;
    //mode for calculation of observed distance
    //0: Weight Average, 1: Pythagoras & Eucludian
    public int ModeIndoorAlgorithm = 1;
    public String detect = "no";
    //size of the map/picture in pixels

    private int widthPixels;
    private int heightPixels;
    private Boolean reset = false;
    private ImageView mapImage;

    private float[] userPos = new float[]{0, 0, 0};
    private int measurements = 0;

    final ArrayList<Integer> minorValues = new ArrayList<Integer>();
    private HashMap<Integer, ArrayList<Double>> distances = new HashMap<Integer, ArrayList<Double>>();
    private HashMap<Integer, Double> distanceAvg = new HashMap<Integer, Double>();
    private HashMap<Integer, float[]> beaconDist = new HashMap<Integer, float[]>();
    public  Button paymentButton;
    public EditText realPosition;
    public ProgressBar progress_timer;
    public MyCountDownTimer myCountDownTimer;
    public CheckBox checkbox_automatic;
    public Boolean automatic_payment = false;
    public Boolean payStart = false;
    private Boolean recordExperiment = false;

    public FirebaseDatabase database = FirebaseDatabase.getInstance();
    public DatabaseReference myRef = database.getReference();

    @Override
    protected  void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.map_position);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editPref = preferences.edit();
        //firebase
        progress_timer = (ProgressBar)findViewById(R.id.progress_timer);
        checkbox_automatic = (CheckBox) findViewById(R.id.checkbox_automatic);
        realPosition = (EditText) findViewById(R.id.realPosition);
        //retrieve the size of the map in meters.
        mapXSize = preferences.getFloat("map_x",10);
        mapYSize = preferences.getFloat("map_y",10);

        //Display the map size above the map.
        final TextView mapInfo = (TextView) findViewById(R.id.map_size);
       // mapInfo.setText("\n The Map Size(x,y): \t" + mapXSize +", " + mapYSize +"\n");

        mapImage = (ImageView) findViewById(R.id.map_picture);
        mapImage.setAdjustViewBounds(true);

        //set a scaled version of the map to the ImageView.
        final Bitmap imageRaw = BitmapFactory.decodeResource(getResources(), R.drawable.raster);
        getScreenSizes(imageRaw);
        Bitmap imageScaled = Bitmap.createScaledBitmap(imageRaw, widthPixels, heightPixels, true);
        mapImage.setImageBitmap(imageScaled);

        workingBitmap = Bitmap.createScaledBitmap(imageRaw, widthPixels, heightPixels, true);


        //Reset the distances and measurements counter to default within the scanning method.
        final Button resetButton = (Button) findViewById(R.id.reset_pos);
        paymentButton = (Button) findViewById(R.id.btn_payment);

        paymentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent paymentIntent = new Intent(MapPosActivity.this, PaymentProcess.class);

                try {
                    beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
                } catch (Exception e) {
                }
                editPref.putBoolean("intent_stop", false);
                editPref.commit();
                startActivity(paymentIntent);

            }
        });
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reset = true;
            }
        });
        final Button changeMapSettings = (Button) findViewById(R.id.map_settings);
        changeMapSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //the layout inflater is needed for multiple inputs in the dialog.
                LayoutInflater dialogInflater = LayoutInflater.from(MapPosActivity.this);

                //retrieve the minor of the beacon from adapter.
                final View textEntryView = dialogInflater.inflate(R.layout.setting_dialog_map, null);

                final EditText inputXPos = (EditText) textEntryView.findViewById(R.id.pos_x);
                final EditText inputYPos = (EditText) textEntryView.findViewById(R.id.pos_y);

                //set the default value of the input to the current value.
                inputXPos.setText(Float.toString(preferences.getFloat("map_x", 10)), TextView.BufferType.EDITABLE);
                inputYPos.setText(Float.toString(preferences.getFloat("map_y", 10)), TextView.BufferType.EDITABLE);

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MapPosActivity.this);
                dialogBuilder.setTitle("Change Settings: ");
                dialogBuilder.setView(textEntryView);

                //Set up the OK and Cancel button
                dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //save the changed x and y positions.
                        mapXSize = Float.valueOf(inputXPos.getText().toString());
                        mapYSize = Float.valueOf(inputYPos.getText().toString());
                        editPref.putFloat("map_x", mapXSize);
                        editPref.putFloat("map_y", mapYSize);
                        editPref.commit();
                    }
                });
                dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                dialogBuilder.show();

            }
        });

        //in order to show the distances with less decimals.
        final DecimalFormat df = new DecimalFormat("#0.####");

        //Blue paint to show beacons on the map.
        final Paint paintBlue = new Paint();
        paintBlue.setAntiAlias(true);
        paintBlue.setColor(Color.BLUE);
        paintBlue.setAlpha(125);

        final Paint paintRed = new Paint();
        paintRed.setAntiAlias(true);
        paintRed.setColor(Color.RED);
        paintRed.setAlpha(125);
        //Configure BeaconManager


        beaconManager = new BeaconManager(this);
        beaconManager.setForegroundScanPeriod(100,0);
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {

            @Override
            public void onBeaconsDiscovered(Region region, final List<Beacon> beaconList) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        getSupportActionBar().setSubtitle("Found Beacons : " + beaconList.size());
                        mapInfo.setText("The map size(x, y): \t " +mapXSize+ ", "+mapYSize + "\n");

                        //Reset everything to default values when the button is pressed
                        if(reset){
                           resetDistance();
                        }


                        //copy the woringBitmap for a clean sheet for the Canvas.
                        final Bitmap mutableBitmap= workingBitmap.copy(Bitmap.Config.ARGB_8888, true);
                        final Canvas canvas = new Canvas(mutableBitmap);
                        Paint paint = new Paint();
                        // canvas.drawPaint(paint);
                        paint.setColor(Color.BLACK);
                        paint.setStyle(Paint.Style.FILL);
                        paint.setTextSize(paint.getTextSize() * 8);
                        //draw a circle for each beacon on the map
                        //canvas.drawText("0,0", 2,2, paint);
                        String[] Distancex;
                        int counter = 0;
                        for (int i = 0; i < beaconList.size();i++){
                            Beacon currentBeacon = beaconList.get(i);
                            int minorVal = currentBeacon.getMinor();
                            if(minorVal < 5){
                                counter++;
                            }
                        }
                        Distancex = new String[counter];
                        counter = 0;
                        for (int i = 0; i < beaconList.size();i++){

                            Beacon currentBeacon = beaconList.get(i);
                            int minorVal = currentBeacon.getMinor();
                            if(minorVal < 5){
                            //get secret and put to preferences

                            if(preferences.getString("secretBeacon"+String.valueOf(minorVal),"")==""){
                                getSecretBeacon(String.valueOf(minorVal));
                               // Log.d("VALUE PREFERENCES",preferences.getString("secretBeacon"+String.valueOf(minorVal),null));
                            }

                            //to prevent new beacons from showing up or invalid beacons from showing.
                           // if(preferences.getFloat("x" + minorVal, -1) < 0){
                              //  continue;
                           // }

                            //check if the beacon is just found. DOuble if-statements because of the reset button.
                            if(!minorValues.contains(minorVal)){
                                minorValues.add(minorVal);
                            }
                            if(!distances.containsKey(minorVal)){
                                distances.put(minorVal, new ArrayList<Double>());
                            }
                            addDistance(currentBeacon, minorVal);

                            //predefined variables for creating a picture with drawn circles on it.
                            float[] locationBeacon = getLocation(currentBeacon);
                            //Draw a circle at the beacon position.

                                canvas.drawText(String.valueOf(currentBeacon.getMinor()), locationBeacon[0], locationBeacon[1], paint);

                            canvas.drawCircle(locationBeacon[0], locationBeacon[1], (float) 0.02* widthPixels, paintBlue);
                                Distancex[counter] = "Distance of beacon "+String.valueOf(minorVal)+" to customer's device: " + String.valueOf(distanceAvg.get(minorVal).floatValue())+"\n";
                            counter++;
                            }

                            }

                            if( Distancex.length>1){

                        Arrays.sort(Distancex);
                        for( int ix = 0; ix < Distancex.length;ix++) {
                            mapInfo.append(Distancex[ix]);
                        }
                            }
                        if(beaconList.size()>2){
                            //System.out.println(beaconList.size());
                            if(ModeIndoorAlgorithm == 0){
                        userPos = userLocation(beaconList);
                                //record
                            }else{
                                userPos = userLocationNew(beaconList);

                            }
                        mapInfo.append("User Position (x, y) " + userPos[0] + ", " + userPos[1] + "\n" + "Inside payment area ? : " +
                                detect +"\n");
                        Log.d("paint","true");
                        canvas.drawCircle(widthPixels *(userPos[0]/mapXSize), heightPixels * (userPos[1]/mapYSize),
                                (float) 0.02 * widthPixels, paintRed);
                        }
                        if(measurements > 20){
                            recordPosition(userPos[0], userPos[1]);
                            measurements = 0;
                        }
                        measurements+=1;
                        //at least 40 measurements before calculating the user position.
//                        if(measurements > 39){
//
//                            //every second a new position measurement
//                            int temp = measurements;
//                            while (temp > 9) {
//
//                                temp -= 10;
//                            }
//
//                            if(temp == 0){
//                                Log.d("tag",String.valueOf(beaconList.size()));
//                                userPos = userLocationNew(beaconList);
//
//                            }
//
//                            mapInfo.append("User Position (x, y) " + userPos[0] + ", " + userPos[1] + "\n" + "Maximum error: " +
//                            userPos[2] +"\n");
//                            Log.d("paint","true");
//                            canvas.drawCircle(widthPixels *(userPos[0]/mapXSize), heightPixels * (userPos[1]/mapYSize),
//                                    (float) 0.03 * widthPixels, paintRed);
//
//                        } else {
//                            mapInfo.append("Time until measurement: " + df.format(4 - measurements * 0.1));
//                        }
//                        measurements +=1;
                        mapImage.setImageBitmap(mutableBitmap);


                    }
                });
            }
        });



    }
    private float[] userLocationNew(List<Beacon> beaconList){
        //get 2 beacon
        int type = 0;
        for(int i =0; i< beaconList.size();i++){
            System.out.println(beaconList.size());
            int minorVal = beaconList.get(i).getMinor();

            if(minorVal < 5){
            type+=minorVal;
            float r;
            try {

                r = distanceAvg.get(minorVal).floatValue();
                Log.d("distx",String.valueOf(r));
            } catch (NullPointerException e) {
                continue;
            }
            float x = preferences.getFloat("x" + minorVal, -1);
            float y = preferences.getFloat("y" + minorVal, 0);
            float z = preferences.getFloat("z" + minorVal, 0);
            if (x >= 0 && y >= 0) {
                // Remove the height difference between the phone and beacon from the distance.
                r = (float) Math.sqrt((r * r) - (z * z));
               // if(!beaconDist.containsKey(minorVal)){

                    beaconDist.put(minorVal, new float[]{x, y, r});
                System.out.println(beaconDist.size());
                //}
            }
            }

        }
        //now we have table of all observed beacons, position x,y in catersian and also distance from device stored in beaconObs.
        //we take 2 observed beacon to find our triangle, beaconObs[1] and beaconObs[2].
        if(beaconDist.size()>=3){
            int minor_beacon_1 = 1;
            int minor_beacon_2 = 2;
            int minor_beacon_3 = 3;

            //type == 6 : 1, 2, 3
        //type == 7 : 1, 2, 4
        //type == 8 : 1, 3, 4
        //type == 9 : 2, 3, 4
            System.out.print("jumlah type = "+type);
        if(type == 6){
            System.out.print("masuk 6");
            minor_beacon_1 = 1;
            minor_beacon_2 = 2;
            minor_beacon_3 = 3;
        }else if(type == 7){
            minor_beacon_1 = 1;
            minor_beacon_2 = 2;
            minor_beacon_3 = 4;
        }else if(type == 8) {
            minor_beacon_1 = 3;
            minor_beacon_2 = 4;
            minor_beacon_3 = 1;
        }else if (type == 9){
            minor_beacon_1 = 3;
            minor_beacon_2 = 4;
            minor_beacon_3 = 2;
        }else  if(type == 10){
            minor_beacon_1 = 1;
            minor_beacon_2 = 2;
            minor_beacon_3 = 3;
        }


        //triangle
        System.out.println(beaconDist);
        float dist_b1_b2 = (float)eucludianDistance(beaconDist.get(minor_beacon_1)[0],beaconDist.get(minor_beacon_2)[0],beaconDist.get(minor_beacon_1)[1],beaconDist.get(minor_beacon_2)[1]);
        float dist_b1_dev = beaconDist.get(minor_beacon_1)[2];
        float dist_b2_dev = beaconDist.get(minor_beacon_2)[2];
        float dist_b3_dev = beaconDist.get(minor_beacon_3)[2];
        //demo
        // dist_b1_b2 = 5;
        //dist_b1_dev = 3;
        //dist_b2_dev  =4;
        //dist_b3_dev = 3;

        double d_x = ((Math.abs(Math.pow(dist_b1_dev,2)-Math.pow(dist_b2_dev,2)))-Math.pow(dist_b1_b2,2)) / (-2*dist_b1_b2);

        double d_y = Math.sqrt(Math.abs((Math.pow(dist_b1_dev,2)-Math.pow(d_x,2))));
        //c_x =(abs(d1^2-d2^2)-dist_b1_b2^2) / (-2 * dist_b1_b2)
        //c_y = sqrt(d1^2-c_x^2)
        //detect wether inside payment area, we assume that payment area is a square by using 3 beacons
        //b1************
        //*            *
        //*            *
        //*            *
        //b2**********b3

        d_x+=beaconDist.get(minor_beacon_1)[0];
        d_y+=beaconDist.get(minor_beacon_1)[1];
        Log.d("Coordinate: X: ", String.valueOf(d_x) +"|Y: "+String.valueOf(d_y));

        double dist_b3_positive = eucludianDistance((float)d_x,(float)d_y,beaconDist.get(minor_beacon_3)[0],beaconDist.get(minor_beacon_3)[1]);
        double dist_b3_negative = eucludianDistance((float)d_x,(float)d_y*-1 + 2*beaconDist.get(minor_beacon_1)[1],beaconDist.get(minor_beacon_3)[0],beaconDist.get(minor_beacon_3)[1]);
        Log.d("Coordinate: Positive: ", String.valueOf(dist_b3_positive) +"|Negative: "+String.valueOf(dist_b3_negative));
       // Log.d("Coordinate: PositiveDiff: ", Math.abs(dist_b3_negative-dist_b3_dev) +"|NegativeDiff: "+Math.abs(dist_b3_positive -dist_b3_dev));
            if(Math.abs(dist_b3_negative-dist_b3_dev) < Math.abs(dist_b3_positive -dist_b3_dev)){

                d_y = d_y*-1+2*beaconDist.get(minor_beacon_1)[1];
            }

            //d_y = d_y*-1+2*beaconDist.get(minor_beacon_1)[1];
            //d_y = d_y-beaconDist.get(minor_beacon_1)[1];
            if(d_x>=preferences.getFloat("x1",-1) && d_x <= preferences.getFloat("x2",-1) && d_y >=preferences.getFloat("y1",-1)&& d_y <=preferences.getFloat("y3",-1)){
                detect = "yes";
                paymentButton.setEnabled(true);
                startPayment();
        }else{
            detect = "no";
                detect = "no";
                paymentButton.setEnabled(false);
                if(payStart) {
                    myCountDownTimer.cancel();
                }
                progress_timer.setProgress(100);
                payStart = false;
            paymentButton.setEnabled(false);
        }


        //check if result x, greater than b2x, less than b3x, and y, greater than b1y, and less than b2y
        //if(d_x >= beaconDist.get(minor_beacon_1)[0] && d_x <=  )

        return new float[]{(float)d_x, (float)d_y, 1};
        }
        return new float[]{(float)0, (float)0, 1};
    }
//    public float[] findLocationTriangle(){
//
//    }
    private float[] userLocation(List<Beacon> beaconsList) {
        float x_user = 0;
        float y_user = 0;
        float r_user = 1;

        // Check for legitimate beacons, those who have been selected within the area.
        ArrayList<float[]> circleArray = new ArrayList<float[]>();
        for (int i = 0; i < beaconsList.size(); i++) {

            int minorVal = beaconsList.get(i).getMinor();
            if(minorVal < 5){
            float r;
            try {
             //   Log.d("minorVal: "+ String.valueOf(minorVal),String.valueOf(distanceAvg.size()));
                r = distanceAvg.get(minorVal).floatValue();

            } catch (NullPointerException e) {
                continue;
            }

            float x = preferences.getFloat("x" + minorVal, -1);
            float y = preferences.getFloat("y" + minorVal, 0);
            float z = preferences.getFloat("z" + minorVal, 0);

            if (x >= 0 && y >= 0) {
                // Remove the height difference between the phone and beacon from the distance.
                r = (float) Math.sqrt((r * r) - (z * z));
                circleArray.add(new float[]{x, y, r});
                x_user += x;
                y_user += y;
            }
            }
        }

        // Only calculate the position when 2 or more beacons are available.
        float circleNumber = circleArray.size();
        if (circleNumber < 2) {
            return new float[]{x_user, y_user, r_user};
        }

        // The average position between all the valid circles.
        x_user = x_user / circleNumber;
        y_user /= circleNumber;

        float prev1Error = 0;
        float currentError = 1000000;

        // If the last error is the same as the current error no better value will be calculated.
        while (prev1Error != currentError) {
            prev1Error = currentError;
            // Calculate the position up, down, left and right of the current one to calculate its error there.
            ArrayList<float[]> newPositions = new ArrayList<float[]>();
            newPositions.add(new float[]{(float) (x_user + 0.1), y_user});
            newPositions.add(new float[]{(float) (x_user - 0.1), y_user});
            newPositions.add(new float[]{x_user, (float) (y_user + 0.1)});
            newPositions.add(new float[]{x_user, (float) (y_user - 0.1)});

            // For each position in a direction calculate the error.
            for (float[] direction : newPositions) {
                float error = 0;
                ArrayList<Float> dist = new ArrayList<Float>();

                // The error on a certain position for each beacon.
                for (int i = 0; i < circleNumber; i++) {
                    float[] circlePos = circleArray.get(i);
                    dist.add((float) (Math.sqrt(Math.pow(circlePos[0] - direction[0], 2) +
                            Math.pow(circlePos[1] - direction[1], 2)) - circlePos[2]));
                    error += Math.pow(dist.get(dist.size() - 1), 2);
                }
                error = (float) Math.sqrt(error);

                // If the error is smaller we take the values.
                if (error < currentError) {
                    Collections.sort(dist);
                    r_user = dist.get(dist.size() - 1);
                    x_user = direction[0];
                    y_user = direction[1];
                    currentError = error;
                }
            }
        }
        if(x_user >= preferences.getFloat("x1", -1) && x_user <= preferences.getFloat("x2", -1) && y_user >= preferences.getFloat("y1", -1) && y_user <= preferences.getFloat("y3", -1)){
            detect = "yes";
            paymentButton.setEnabled(true);
            //run timer
            //connect
            startPayment();


        }else{
            detect = "no";
            detect = "no";
            paymentButton.setEnabled(false);
            if(payStart) {
                myCountDownTimer.cancel();
            }
            progress_timer.setProgress(100);
            payStart = false;
            paymentButton.setEnabled(false);
        }

        return new float[]{x_user, y_user, r_user};
    }

    /*
     * Add a distance to its value and calculate the distance to the beacon over the multiple measurements.
     */
    private void addDistance(Beacon beacon, int minorVal) {

        // Add the distance to the ArrayList and maintain a certain size.
        double distance = Utils.computeAccuracy(beacon);
        distance = distance*1.8;
        if(Mode == 0){
        distanceAvg.put(minorVal,distance);
        }
        else{
        ArrayList<Double> distanceToBeacon = distances.get(minorVal);
//        if(distanceToBeacon.size()>50){
//            distances.remove(minorVal);
//        }
        distanceToBeacon.add(distance);
        if (distanceToBeacon.size() > 40
                ) {
            distanceToBeacon.remove(0);
        }
            if(Mode == 1){
        average(distanceToBeacon, minorVal);
            }else{
                median(distanceToBeacon, minorVal);
            }
        }
    }

    /*
     * Calculate the distance to beacon by taking the median.
     */
    private void median(ArrayList<Double> distanceToBeacon, int minorVal) {
        Collections.sort(distanceToBeacon);
        int half = (int) (0.5 * distanceToBeacon.size());
        if (distanceToBeacon.size() == 1) {
            half = 0;
        }
        distanceAvg.put(minorVal, distanceToBeacon.get(half));
    }

    /*
     * Calculate the distance to beacon by taking the average.
     */
    private void average(ArrayList<Double> distanceToBeacon, int minorVal) {
        double total = 0;

        for (double element : distanceToBeacon) {
            total += element;
        }
        distanceAvg.put(minorVal, total / distanceToBeacon.size());
    }

    /*
     * Retrieve and calculate the location of a beacon on the picture.
     */
    private float[] getLocation(Beacon beacon) {
        int minorVal = beacon.getMinor();

        // Get the position of the beacon.
        float beaconX = preferences.getFloat("x" + minorVal, 0);

        float beaconY = preferences.getFloat("y" + minorVal, 0);
        //set for development only !!!
        //if(beaconX)
        if(beaconX == 0 || beaconY == 0) {
            if (minorVal == 1) {
                editPref.putFloat("x1", 4);
                editPref.putFloat("y1", 4);
                editPref.putFloat("z1", 0);
                beaconX = 4;
                beaconY = 4;
            } else if (minorVal == 2) {

                editPref.putFloat("x2", 6);
                editPref.putFloat("y2", 4);
                editPref.putFloat("z2", 0);
                beaconX = 6;
                beaconY = 4;
            } else if (minorVal == 3){

                editPref.putFloat("x3", 4);
                editPref.putFloat("y3", 6);
                editPref.putFloat("z3", 0);
                beaconX = 4;
                beaconY = 6;
            } else if (minorVal == 4) {
                editPref.putFloat("x4", 6);
                editPref.putFloat("y4", 6);
                editPref.putFloat("z4", 0);
            }
            editPref.commit();
        }


        // Calculate the ratio of the beacon position with the map
        float ratioX = beaconX / mapXSize;
        float ratioY = beaconY / mapYSize;

        // Calculate the number of pixels it has to move.
        float locationPixelsX = (ratioX * widthPixels);
        float locationPixelsY = (ratioY * heightPixels);

        return new float[]{locationPixelsX, locationPixelsY};
    }
    private void getScreenSizes(Bitmap image_raw) {

        // Retrieve the height of the status bar
        int statusbar_height = getStatusBarHeight();

        // Correction for the padding. 2 pixels above and below each ImageView
        int padding_height = 0;

        // Retrieve screen size
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int height_screen = metrics.heightPixels - statusbar_height - padding_height;
        int width_screen = (int) (metrics.widthPixels * 0.9);

        // Retrieve picture size and calculate ratio
        int height_image = image_raw.getHeight();
        int width_image = image_raw.getWidth();
        float ratio = (float) (height_image) / width_image;

        // Scaling depending on the lowest ratio width/height between image and picture.
        if (ratio < (float) (height_screen) / width_screen) {
            widthPixels = width_screen;
            heightPixels = (int) (height_image * (float) (width_screen) / width_image);
        } else {
            widthPixels = (int) (width_image * (float) (height_screen) / height_image);
            heightPixels = height_screen;
        }
    }

    private int getStatusBarHeight() {
        int status_bar_height = 0;
        int resource_id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resource_id > 0) {
            status_bar_height = getResources().getDimensionPixelSize(resource_id);
        }
        return status_bar_height;
    }

    private double eucludianDistance(float x1, float y1, float x2, float y2){
        return Math.sqrt(Math.pow((x1-x2),2)+ Math.pow((y1-y2),2));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan_menu, menu);
       // MenuItem refreshItem = menu.findItem(R.id.refresh);
        //refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
        return true;
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
                getSupportActionBar().setSubtitle("Bluetooth not enabled");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void connectToService() {
        // Retrieve the beacons.
        getSupportActionBar().setSubtitle("Scanning...");
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btn_average:
                ModeIndoorAlgorithm = 0;
                resetDistance();
                return true;

            case R.id.btn_triangle:
                ModeIndoorAlgorithm = 1;
                resetDistance();
                return true;
            case R.id.btn_distance_average:
                Mode = 3;
                resetDistance();
                return true;

            case R.id.btn_distance_median:

                Mode = 2;
                resetDistance();
                return true;
            case R.id.btn_distance_real:
                //mode is 0
                Mode = 0;
                resetDistance();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
    public void resetDistance(){
        measurements = 0;
        distances = new HashMap<Integer, ArrayList<Double>>();
        distanceAvg = new HashMap<Integer, Double>();
        reset = false;
    }

    public void getSecretBeacon(final String minorVal){
        Log.d("masukFirebase",minorVal);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("secretBeacon");
        //get value from firebase
        final String[] result = {""};
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.

                String value = dataSnapshot.child(minorVal).child("secret").getValue(String.class);
                String POS = dataSnapshot.child(minorVal).child("POS_ADDRESS").getValue(String.class);


                editPref.putString("secretBeacon"+minorVal, value);
                editPref.commit();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("Error", "Failed to read value.", error.toException());
            }
        });

    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_indoor_pythagoras:
                if (checked)
                    ModeIndoorAlgorithm = 1;
                    resetDistance();
                    Toast.makeText(getApplicationContext(),"Algorithm changed to Pythagoras and Eucludian Distance", Toast.LENGTH_SHORT);
                    break;
            case R.id.radio_indoor_average:
                if (checked)
                    ModeIndoorAlgorithm = 0;
                    resetDistance();
                    Toast.makeText(getApplicationContext(),"Algorithm changed to Weight Average", Toast.LENGTH_SHORT);
                    break;
        }
    }

    public void onCheckboxClicked(View view) {
        // Is the button now checked?
        boolean checked = ((CheckBox) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.checkbox_automatic:
                if (checked){
                    if(payStart) {
                        myCountDownTimer.cancel();
                    }
                    paymentButton.setVisibility(View.GONE);
                    automatic_payment = true;
                    progress_timer.setProgress(100);

                    payStart = false;
                }else{
                    if(payStart){
                        myCountDownTimer.cancel();
                    }
                    automatic_payment = false;
                    progress_timer.setProgress(100);
                    payStart = false;
                 paymentButton.setVisibility(View.VISIBLE);
                }

                Toast.makeText(getApplicationContext(),"Algorithm changed to Pythagoras and Eucludian Distance", Toast.LENGTH_SHORT);
                break;
            case R.id.record_experiment:
                if (checked){

                    recordExperiment= true;
                }else{
                    recordExperiment = false;
                }

                Toast.makeText(getApplicationContext(),"Algorithm changed to Pythagoras and Eucludian Distance", Toast.LENGTH_SHORT);
                break;

        }
    }

    public void onRadioButtonDistanceClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_real_distance:
                if (checked)
                    Mode = 0;
                resetDistance();
                Toast.makeText(getApplicationContext(),"Calculation Algorithm changed to Real Distance", Toast.LENGTH_SHORT);
                break;
            case R.id.radio_average:
                if (checked)
                    Mode = 1;
                resetDistance();
                Toast.makeText(getApplicationContext(),"Calculation Algorithm changed to Average", Toast.LENGTH_SHORT);
                break;
            case R.id.radio_median:
                if (checked)
                    Mode = 2;
                resetDistance();
                Toast.makeText(getApplicationContext(),"Calculation Algorithm changed to Median", Toast.LENGTH_SHORT);
                break;
        }
    }



    public void startPayment(){
        if(!payStart && automatic_payment) {
            payStart = true;
            progress_timer.setProgress(100);
            myCountDownTimer = new MyCountDownTimer(10000, 500);
            myCountDownTimer.start();
        }
    }

    public void recordPosition(float x, float y){
        if(recordExperiment)
            compress.captureResult(String.valueOf(ModeIndoorAlgorithm),System.currentTimeMillis(),realPosition.getText().toString(),x, y,detect,myRef);
    }

    public class MyCountDownTimer extends CountDownTimer {

        public MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
           // textCounter.setText(String.valueOf(millisUntilFinished));
            int progress = (int) (millisUntilFinished/100);
            progress_timer.setProgress(progress);
        }

        @Override
        public void onFinish() {
            //textCounter.setText("Task completed");


            payStart = true;
            progress_timer.setProgress(0);
            Intent paymentIntent = new Intent(MapPosActivity.this, PaymentProcess.class);

            try {
                beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
            } catch (Exception e) {
            }
            editPref.putBoolean("intent_stop", false);
            editPref.commit();
            startActivity(paymentIntent);
        }



    }



}
