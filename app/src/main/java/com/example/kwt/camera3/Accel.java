package com.example.kwt.camera3;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;

public class Accel {

    Context context;


    public Accel(Context con) {
        Log.i("Accel", "==== Accel Monitoring Object Created ===");
        context = con;
        //mStatusView = (TextView) ((Activity)context).findViewById(R.id.status);
    }
}
