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

    public void onCreate() {

        super.onCreate();

        Toast.makeText(this, "SensorFusion Service is created!",Toast.LENGTH_LONG).show();

        counter = 0;

        //Receives X,Y,Z values from ShakerListener broadcast
        sensorFusionUpdate = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("com.example.kwt.accelerometer.onLaneDetectionLost")) {
                    counter = counter + 1;
                    Log.i(TAG, "onLaneDetectionLost Detected! counter: " + counter);

                }
                else if (intent.getAction().equals("com.example.kwt.accelerometer.onNoiseThersholdCrossed")) {
                    counter = counter + 1;
                    Log.i(TAG, "onNoiseThersholdCrossed Detected! counter: " + counter);

                }
                else if (intent.getAction().equals("com.example.kwt.accelerometer.onAccelShake")) {
                    counter = counter + 1;
                    Log.i(TAG, "onAccelShake Detected! counter: " + counter);

                }
            }
        };
        //Register listener to XYZDATA Intent Broadcast
        this.registerReceiver(sensorFusionUpdate, new IntentFilter("com.example.kwt.accelerometer.onLaneDetectionLost"));
        this.registerReceiver(sensorFusionUpdate, new IntentFilter("com.example.kwt.accelerometer.onNoiseThersholdCrossed"));
        this.registerReceiver(sensorFusionUpdate, new IntentFilter("com.example.kwt.accelerometer.onAccelShake"));


        countDownTimer = new CountDownTimer(6000, 1000) {

            public void onTick(long millisUntilFinished) {

            }

            public void onFinish() {
                if(counter  > 3){
                    final Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    vib.vibrate(500);
                    Intent i = new Intent();
                    i.setClass(SensorFusionTimer.this, CheckCertainty.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    Log.i(TAG, "Emergency Detected!");
                }
                SensorFusionTimer.this.start(); //restart timer
            }
        };//end of CountDownTimer


        SensorFusionTimer.this.start(); //start timer
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void start(){
        Log.i(TAG, "SensorFusionTimer Started");
        counter = 0;
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
