package com.example.kwt.camera3;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.fillConvexPoly;


public class Home extends AppCompatActivity {
    private static final String TAG = "Home_OpenCVTest";
    private CameraBridgeViewBase mOpenCvCameraView;

    private SoundMonitoringInitialize soundMonitoringInitialize;
    private Accelerometer accelerometer;
    private LaneDetection laneDetection;
    public BroadcastReceiver statuslog;

    //TextViews
    TextView textGforce;
    TextView txtstatuslog;

    public int gForce;
    String phoneNumber;


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        Log.i(TAG, "called onCreate");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        txtstatuslog = findViewById(R.id.txtstatuslog);

        //Default values
        phoneNumber = "+64211484398";

        //Initialize Accelerometer background Service
        accelerometer = new Accelerometer(this);
        accelerometer.InitializeAccelMeter();

        //Initialize Microphone monitoring
        soundMonitoringInitialize = new SoundMonitoringInitialize(this);

        //Initialize Lane Detection
        laneDetection = new LaneDetection(this);

        //Start sensorFusionTimer service
        startService(new Intent(this, SensorFusionTimer.class));


        statuslog = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                txtstatuslog.append(intent.getStringExtra("MSGLog") + "\n");
            }
        };

        this.registerReceiver(statuslog, new IntentFilter("com.example.kwt.accelerometer.statuslog"));
    }


    @Override
    protected void onPause() {

        super.onPause();
        laneDetection.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();
        laneDetection.onResume();
        soundMonitoringInitialize.onResume();

    }
    @Override
    protected void onStop() {
        super.onStop();
       soundMonitoringInitialize.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        laneDetection.onDestroy();
        accelerometer.onDestroy();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is ...
        return false;
    }


    public native String stringFromJNI();

    public void onOpenSettings(View view) {

        Intent getSettings = new Intent(this, Settings.class);
        final int result = 1;
        getSettings.putExtra("Caller", "homepage");

        startActivityForResult(getSettings, result);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //G-Force Value and phone number
        String phoneNumber = data.getStringExtra("phoneNumber");
        String g_Force = data.getStringExtra("gForce");
        gForce = Integer.parseInt(g_Force);
        textGforce = findViewById(R.id.g_forceVal);
        if ( g_Force != null)
            textGforce.setText(g_Force);
        if (phoneNumber == null)
            phoneNumber = "+64211484398";

        int[] HoughLinePSettings = data.getIntArrayExtra("HoughLinePSettings");
        laneDetection.hough_threshold = HoughLinePSettings[0];
        laneDetection.hough_minLength= HoughLinePSettings[1];
        laneDetection.hough_maxGap= HoughLinePSettings[2];

        int[] cannySettings  = data.getIntArrayExtra("cannySettings");
        laneDetection.canny_threshold1 = cannySettings[0];
        laneDetection.canny_threshold2 = cannySettings[1];

        int[] PolyMaskSettings  = data.getIntArrayExtra("PolyMaskSettings");
        laneDetection.PolyX1 = PolyMaskSettings[0];
        laneDetection.PolyX2 = PolyMaskSettings[1];
        laneDetection.PolyX3 = PolyMaskSettings[2];
        laneDetection.PolyX4 = PolyMaskSettings[3];
    }
}