package com.example.kwt.camera3;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class Accelerometer {

    Context context;

    TextView textXval ;
    TextView textYval;
    TextView textZval;
    TextView textLongLat;

    BroadcastReceiver sensorXYZUpdate;

    public Accelerometer(Context con) {
        Log.i("Accelerometer", "==== Accelerometer Monitoring Object Created ===");
        context = con;
        //mStatusView = (TextView) ((Activity)context).findViewById(R.id.status);

        //Accelerometer textviews
        textXval = (TextView) ((Activity)context).findViewById(R.id.xValue);
        textYval = (TextView) ((Activity)context).findViewById(R.id.yValue);
        textZval = (TextView) ((Activity)context).findViewById(R.id.zValue);
        textLongLat = (TextView) ((Activity)context).findViewById(R.id.LongLat);
    }

    public void InitializeAccelMeter() {

        ((Activity)context).startService(new Intent(context, ShakeService.class));
        Toast.makeText(context, "ACTIVATED!", Toast.LENGTH_LONG).show();
        Log.d("MSG", "Activated the Service");

        //Receives X,Y,Z values from ShakerListener broadcast
         sensorXYZUpdate = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                textXval.setText("textXval: " + intent.getExtras().getFloat("textXval"));
                textYval.setText("textYval: " + intent.getExtras().getFloat("textYval"));
                textZval.setText("textZval: " + intent.getExtras().getFloat("textZval"));
            }
        };

        //Register listener to XYZDATA Intent Broadcast
        ((Activity)context).registerReceiver(sensorXYZUpdate, new IntentFilter("com.example.kwt.accelerometer.XYZDATA"));
    }

    public void onDestroy(){
        ((Activity)context).unregisterReceiver(sensorXYZUpdate);
    }
}
