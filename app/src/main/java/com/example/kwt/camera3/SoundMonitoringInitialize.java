package com.example.kwt.camera3;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.TextView;

public class SoundMonitoringInitialize {

    Context context;

    public static final int POLL_INTERVAL = 300;

    /** running state **/
    public boolean mRunning = false;

    /** config state **/
    public int mThreshold;

    public PowerManager.WakeLock mWakeLock;
    public Handler mHandler = new Handler();


    /* data source */
    public SoundMeter mSensor;

    public TextView mStatusView;


    /****************** Define runnacheckSelfPermissionble thread again and again detect noise *********/
    public Runnable mSleepTask = new Runnable() {
        public void run() {
            Log.i("Noise", "runnable mSleepTask");

            start();
        }
    };

    // Create runnable thread to Monitor Voice
    public Runnable mPollTask = new Runnable() {
        public void run() {

            double amp = mSensor.getAmplitude();
            double db = mSensor.getAmplitudeDb(amp);
            Log.i("Noise", "runnable mPollTask");
            updateDisplay("Monitoring Voice...", amp);

            if ((amp > mThreshold)) {
                callForHelp(amp, db);
                Log.i("Noise", "==== onCreate ===");
            }

            // Runnable(mPollTask) will again execute after POLL_INTERVAL
            mHandler.postDelayed(mPollTask, POLL_INTERVAL);

        }


    };

    public SoundMonitoringInitialize(   Context con) {
        Log.i("Noise", "==== Sound Monitoring Object Created ===");
        context = con;
        mStatusView = (TextView) ((Activity)context).findViewById(R.id.status);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(((Activity)context), new String[]{Manifest.permission.RECORD_AUDIO},
                    1);
        }
        InitializeSoundMeter();

        PowerManager pm = (PowerManager) ((Activity)context).getSystemService(context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "soundApp:NoiseAlert");
    }



    //---------- Microphone Monitoring functions
    public void InitializeSoundMeter() {
        //Used to record voice
        mSensor = new SoundMeter();


        Log.i("Noise", "==== Sound Monitoring Object Initialized  ===");
    }

    public void onResume() {
        Log.i("Noise", "==== onResume ===");

        initializeApplicationConstants();
      //  mDisplay.setLevel(0, mThreshold);

        if (!mRunning) {
            mRunning = true;
            start();
        }
    }

    public void onStop() {
        Log.i("Noise", "==== onStop ===");

        //Stop noise monitoring
        stop();
    }

    public void start(){

        Log.i("Noise", "==== start ===");

        mSensor.start();
        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire(1500);
        }

        //Noise monitoring start
        // Runnable(mPollTask) will execute after POLL_INTERVAL
        mHandler.postDelayed(mPollTask, POLL_INTERVAL);
    }

    public void stop(){

        Log.i("Noise", "==== Stop Noise Monitoring===");
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        mHandler.removeCallbacks(mSleepTask);
        mHandler.removeCallbacks(mPollTask);
        mSensor.stop();
        //mDisplay.setLevel(0,0);
        updateDisplay("stopped...", 0.0);
        mRunning = false;

    }



    public void initializeApplicationConstants() {
        // Set Noise Threshold
        mThreshold = 30000;//8;
    }


    public void updateDisplay(String status, double signalEMA) {
        mStatusView.setText(status);
        //    private TextView mStatusView;
        //mDisplay.setLevel((int)signalEMA, mThreshold);

    }
    public void callForHelp(double amp, double db) {
        //stop();

        // Show alert when noise thersold crossed
       // Toast.makeText(getApplicationContext(), "Noise Thershold Crossed, Amplitude: "+amp + ", DB: "+db,
         //       Toast.LENGTH_LONG).show();
        Log.i("Noise", "==== Noise Threshold Crossed, Amplitude: "+amp + ", DB: "+db+"===");

        Intent intent = new Intent("com.example.kwt.accelerometer.onNoiseThersholdCrossed");
        ((Activity)context).sendBroadcast(intent);
    }

    public void onPause() {
        mSensor.pause();

    }


}
