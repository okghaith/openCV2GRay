package com.example.kwt.camera3;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

public class SensorFusionTimer extends Service {

    private static final String TAG = "SensorFusion";
    CountDownTimer countDownTimer;
    public int counter;
    public BroadcastReceiver sensorFusionUpdate;
    public boolean onLaneDetectionLost = false;
    public boolean onAccelShake = false;
    public boolean onNoiseThersholdCrossed = false;

    public void onCreate() {

        super.onCreate();

        counter = 0;
        onLaneDetectionLost = false;
        onAccelShake = false;
        onNoiseThersholdCrossed = false;

        //Receives X,Y,Z values from ShakerListener broadcast
        sensorFusionUpdate = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("com.example.kwt.accelerometer.onLaneDetectionLost")) {

                    if(onLaneDetectionLost == false){
                        onLaneDetectionLost = true;
                        counter++;
                    }
                    Log.i(TAG, "onLaneDetectionLost Detected! counter: " + counter);

                    //update main home log box
                    Intent intent_msg = new Intent("com.example.kwt.accelerometer.statuslog");
                    intent_msg.putExtra("MSGLog","Lane Detection Lost! counter: " + counter);
                    context.sendBroadcast(intent_msg);

                }
                else if (intent.getAction().equals("com.example.kwt.accelerometer.onNoiseThersholdCrossed")) {

                    if(onNoiseThersholdCrossed == false){
                        onNoiseThersholdCrossed = true;
                        counter++;
                    }
                    Log.i(TAG, "onNoiseThersholdCrossed Detected! counter: " + counter);

                    //update main home log box
                    Intent intent_msg = new Intent("com.example.kwt.accelerometer.statuslog");
                    intent_msg.putExtra("MSGLog","Noise Thershold Cross Detected! counter: " + counter);
                    context.sendBroadcast(intent_msg);
                }
                else if (intent.getAction().equals("com.example.kwt.accelerometer.onAccelShake")) {
                    if(onAccelShake == false){
                        onAccelShake = true;
                        counter++;
                    }
                    onAccelShake = true;
                    Log.i(TAG, "onAccelShake Detected! counter: " + counter);

                    //update main home log box
                    Intent intent_msg = new Intent("com.example.kwt.accelerometer.statuslog");
                    intent_msg.putExtra("MSGLog","Shake Detected! counter: " + counter);
                    context.sendBroadcast(intent_msg);
                }
            }

        };
        //Register listener to XYZDATA Intent Broadcast
        this.registerReceiver(sensorFusionUpdate, new IntentFilter("com.example.kwt.accelerometer.onLaneDetectionLost"));
        this.registerReceiver(sensorFusionUpdate, new IntentFilter("com.example.kwt.accelerometer.onNoiseThersholdCrossed"));
        this.registerReceiver(sensorFusionUpdate, new IntentFilter("com.example.kwt.accelerometer.onAccelShake"));


        countDownTimer = new CountDownTimer(6000, 1000) {

            public void onTick(long millisUntilFinished) {
                if(counter  >= 2) {
                    DetectedEmergency();
                }
            }

            public void onFinish() {
                if(counter  >= 2) {
                    DetectedEmergency();
                }
                SensorFusionTimer.this.start(); //restart timer
            }

            private void DetectedEmergency() {
                counter = 0;
                final Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    vib.vibrate(500);
                    Intent i = new Intent();
                    i.setClass(SensorFusionTimer.this, CheckCertainty.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    Log.i(TAG, "Emergency Detected!");
                //update main home log box
                Intent intent_msg = new Intent("com.example.kwt.accelerometer.statuslog");
                intent_msg.putExtra("MSGLog","Emergency Detected!");
                SensorFusionTimer.this.sendBroadcast(intent_msg);
            }
        };//end of CountDownTimer

        SensorFusionTimer.this.start(); //start timer
    }


    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(this, "SensorFusion Service is created!",Toast.LENGTH_LONG).show();
        //update main home log box
        Intent intent_msg = new Intent("com.example.kwt.accelerometer.statuslog");
        intent_msg.putExtra("MSGLog","SensorFusion Service is created!");
        this.sendBroadcast(intent_msg);
        return null;
    }

    public void start(){
        Log.i(TAG, "SensorFusionTimer Started");
        //update main home log box
        Intent intent_msg = new Intent("com.example.kwt.accelerometer.statuslog");
        intent_msg.putExtra("MSGLog","SensorFusionTimer Started");
        this.sendBroadcast(intent_msg);
        counter = 0;
        onLaneDetectionLost = false;
        onAccelShake = false;
        onNoiseThersholdCrossed = false;
        countDownTimer.start();
    }

    public void onAccelShake(){
        counter = counter + 1;

    }

    public void onNoiseThersholdCrossed(){
        counter = counter + 1;
    }

    public void onLaneDetectionLost(){
        counter = counter + 1;
    }
}
