package com.example.kwt.camera3;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
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


public class MainActivity extends AppCompatActivity implements CvCameraViewListener2 {
    private static final String TAG = "MyActivity_OpenCVTest";
    private CameraBridgeViewBase mOpenCvCameraView;

    private SensorManager sensorManger;
    Sensor accelerometer;

    ImageView imageView_gray;
    ImageView imageView_canny;
    ImageView imageView_hough;
    ImageView imageView_mask;
    ImageView imageView_maskCanny;
    Bitmap grayBitmap;
    Bitmap cannyBitmap;
    Bitmap houghBitmap;
    Bitmap maskBitmap;
    Bitmap masked_cannyBitmap;
    Mat mRgba;
    Mat gray;
    Mat canny;
    Mat hough;
    Mat mask;
    Mat masked_canny;


    TextView xValue, yValue, zValue, LongLat ;
    int flag = 1;

    TextView text_canny_threshold1, text_canny_threshold2, text_hough_threshold,text_hough_minLength,text_hough_maxGap,text_poly_point1,text_poly_point2,text_poly_point3;
    SeekBar seek_canny_threshold1, seek_canny_threshold2, seek_hough_threshold,seek_hough_minLength,seek_hough_maxGap,seek_poly_point1,seek_poly_point2,seek_poly_point3;
    public int canny_threshold1, canny_threshold2, hough_threshold,hough_minLength,hough_maxGap;
    private Bitmap bmp;


    //OpenCV Initialization
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded Successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "called onCreate");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = findViewById(R.id.myCameraView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

        imageView_gray = findViewById(R.id.imageView_gray);
        imageView_canny = findViewById(R.id.imageView_canny);
        imageView_hough = findViewById(R.id.imageView_hough);
        imageView_mask = findViewById(R.id.imageView_Mask);
        imageView_maskCanny = findViewById(R.id.imageView_maskCanny);

        //Canny Seek bars
        cannySeekBars();

        //HoughLineP Seekbars
        houghLinePSeekbars();


        //upload black pic
        InputStream stream = null;

        Uri uri = Uri.parse("android.resource://" + getPackageName() +"/"+ R.drawable.black_640_360);
        try {
            stream = getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
        bmpFactoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        //H:360,W:640
        bmp = BitmapFactory.decodeStream(stream, null, bmpFactoryOptions);
//        imageView_mask.setImageBitmap(bmp);

//        mask  = new Mat(640,360,CvType.CV_8SC1);
//        maskBitmap =   Bitmap.createBitmap(640,360,Bitmap.Config.RGB_565);
//
        xValue=(TextView)findViewById(R.id.xValue);
        yValue=(TextView)findViewById(R.id.yValue);
        zValue=(TextView)findViewById(R.id.zValue);
        LongLat = (TextView) findViewById(R.id.LongLat);

        BroadcastReceiver sensorXYZUpdate = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                xValue.setText("xValue: " +  intent.getExtras().getFloat("xValue"));
                yValue.setText("yValue: " +   intent.getExtras().getFloat("yValue"));
                zValue.setText("zValue: " +  intent.getExtras().getFloat("zValue"));

            }
        };


        registerReceiver(sensorXYZUpdate, new IntentFilter("com.example.kwt.accelerometer.XYZDATA"));


        //   Log.d(TAG, "onCreate: Initializing Sensor Services");
        // sensorManger = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        //accelerometer = sensorManger.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //sensorManger.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        //Log.d(TAG, "onCreate: Registered accelerometer listner");

        final Button serviceB=(Button)findViewById(R.id.serviceB);
        flag=1;
        serviceB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flag == 1) {
                    Toast.makeText(MainActivity.this, "ACTIVATED!", Toast.LENGTH_LONG).show();
                    Log.d("MSG", "Activated the Service");
                    startService(new Intent(getApplicationContext(), ShakeService.class));
                    flag = 0;
                } else {
                    Toast.makeText(MainActivity.this, "DEACTIVATED!", Toast.LENGTH_LONG).show();
                    stopService(new Intent(getApplicationContext(), ShakeService.class));
                    Log.d("MSG", "Deactivated the Service");
                    flag = 1;
                }

            }
        });



    }


    private void houghLinePSeekbars() {
        seek_hough_threshold = (SeekBar)this.findViewById(R.id.seekBar3);
        seek_hough_minLength = (SeekBar)this.findViewById(R.id.seekBar4);
        seek_hough_maxGap = (SeekBar)this.findViewById(R.id.seekBar5);

        text_hough_threshold=(TextView)findViewById(R.id.textView3);
        text_hough_threshold.setText(seek_hough_threshold.getProgress() + " / " + seek_hough_threshold.getMax());

        text_hough_minLength=(TextView)findViewById(R.id.textView4);
        text_hough_minLength.setText(seek_hough_minLength.getProgress() + " / " + seek_hough_minLength.getMax());

        text_hough_maxGap=(TextView)findViewById(R.id.textView5);
        text_hough_maxGap.setText(seek_hough_maxGap.getProgress() + " / " + seek_hough_maxGap.getMax());

        seek_hough_threshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                hough_threshold = progress;
                text_hough_threshold.setText(progress + " / " + seek_hough_threshold.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seek_hough_minLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                hough_minLength = progress;
                text_hough_minLength.setText(progress + " / " + seek_hough_minLength.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seek_hough_maxGap.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                hough_maxGap = progress;
                text_hough_maxGap.setText(progress + " / " + seek_hough_maxGap.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void cannySeekBars() {
        seek_canny_threshold1 =(SeekBar)this.findViewById(R.id.seekBar1);
        seek_canny_threshold2 =(SeekBar)this.findViewById(R.id.seekBar2);

        text_canny_threshold1 = (TextView) findViewById(R.id.textView1);
        text_canny_threshold1.setText(seek_canny_threshold1.getProgress() + " / " + seek_canny_threshold1.getMax());

        text_canny_threshold2 = (TextView) findViewById(R.id.textView2);
        text_canny_threshold2.setText(seek_canny_threshold2.getProgress() + " / " + seek_canny_threshold2.getMax());


        seek_canny_threshold1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                canny_threshold1 = progress;
                text_canny_threshold1.setText(progress + " / " + seek_canny_threshold1.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        seek_canny_threshold2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                canny_threshold2 =progress;
                text_canny_threshold2.setText(progress + " / " + seek_canny_threshold2.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mOpenCvCameraView!=null)
        {
            mOpenCvCameraView.disableView();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(!OpenCVLoader.initDebug())
        {
            Toast.makeText(getApplicationContext(),"openCV did not Loaded successfully",Toast.LENGTH_SHORT).show();
        }
        else
        {
            mLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mOpenCvCameraView!=null)
        {
            mOpenCvCameraView.disableView();
        }
    }


    public void onCameraViewStarted(int width, int height) {
        //H360:,W:640
        grayBitmap = Bitmap.createBitmap(width,height,Bitmap.Config.RGB_565);
        cannyBitmap = Bitmap.createBitmap(width,height,Bitmap.Config.RGB_565);
        houghBitmap =  Bitmap.createBitmap(width,height,Bitmap.Config.RGB_565);
        maskBitmap =   Bitmap.createBitmap(width,height,Bitmap.Config.RGB_565);
        masked_cannyBitmap =  Bitmap.createBitmap(width,height,Bitmap.Config.RGB_565);

        mRgba = new Mat(width,height,CvType.CV_8UC4);
        gray = new Mat(width,height,CvType.CV_8SC1);
        canny = new Mat(width,height,CvType.CV_8SC1);
        hough = new Mat(width,height,CvType.CV_8SC1);
        masked_canny =  new Mat(width,height,CvType.CV_8SC1);
        mask  = new Mat(width,height,CvType.CV_8SC1);
        Utils.bitmapToMat(bmp, mask);
        Imgproc.cvtColor(mask, mask , Imgproc.COLOR_RGB2GRAY); // from 3 channels to 1 channel
        //mask  = new Mat(width,height,CvType.CV_8SC1);//Mat.zeros(width, height, CvType.CV_8UC3); //new Mat(width, height, CvType.CV_8SC1, new Scalar(0));
    }

    public void onCameraViewStopped() {

    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        //Log.i(TAG, "Got New Frame!");
        mRgba = inputFrame.rgba();

        Imgproc.cvtColor(mRgba, gray , Imgproc.COLOR_RGB2GRAY);
        Imgproc.Canny(gray, canny, canny_threshold1, canny_threshold2);

        List<Point> cropMaskArray = new ArrayList<>();
        cropMaskArray.add(new Point(100, 360));
        cropMaskArray.add(new Point(320, 180));
        cropMaskArray.add(new Point(540, 360));
        org.opencv.core.Point [] pointArray = new org.opencv.core.Point[cropMaskArray.size()];
        Point pt;
        for(int i = 0; i < cropMaskArray.size(); i++){
            pt = cropMaskArray.get(i);
            pointArray[i] = new org.opencv.core.Point(pt.x, pt.y);
        }
        MatOfPoint points = new MatOfPoint(pointArray);
        fillConvexPoly(mask, points, new Scalar(255, 255, 255));


        Core.bitwise_and(canny, mask, masked_canny);//mask should be just 1 channel

        hough = getHoughPTransform(masked_canny,1, Math.PI / 180, hough_threshold, hough_minLength,hough_maxGap);

        //H:360XW:640
        Utils.matToBitmap(gray, grayBitmap);
        Utils.matToBitmap(canny,cannyBitmap);
        Utils.matToBitmap(hough,houghBitmap);
        Utils.matToBitmap(mask,maskBitmap);
        Utils.matToBitmap(masked_canny,masked_cannyBitmap);


        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                imageView_gray.setImageBitmap(grayBitmap);
                imageView_canny.setImageBitmap(cannyBitmap);
                imageView_hough.setImageBitmap(houghBitmap);
                imageView_mask.setImageBitmap(maskBitmap);
                imageView_maskCanny.setImageBitmap(masked_cannyBitmap);

            }
        });


        return mRgba;
    }


    public Mat getHoughPTransform(Mat image, double rho, double theta, int threshold, double minLineLength, double maxLineGap) {
        Mat result = image.clone();
        Mat lines = new Mat();


        Imgproc.HoughLinesP(image, lines, rho, theta, threshold, minLineLength, maxLineGap);

        Log.i(TAG, "lines.cols()" + lines.cols());

        for (int i = 0; i < lines.cols(); i++) {
            double[] val = lines.get(0, i);
            if (val == null)
                break;
            Log.i(TAG, "val[0], val[1] val[2], val[3]" + val[0] +", "+ val[1]+", "+  val[2] +", "+  val[3]);
            Imgproc.line(mRgba, new Point(val[0], val[1]), new Point(val[2], val[3]), new Scalar(0, 0, 255), 4);
        }
        return mRgba;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is ...
        return false;
    }

    /* @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.d(TAG, "onSensorChanged: X: " + sensorEvent.values[0] +" Y: " +sensorEvent.values[1] + " Z: " +sensorEvent.values[2]);

        xValue.setText("xValue: " + sensorEvent.values[0]);
        yValue.setText("yValue: " + sensorEvent.values[1]);
        zValue.setText("zValue: " + sensorEvent.values[2]);

    }*/

    /**
     * A native method that is implemented by the 'native-lib' native library,

     * which is packaged with this application.
     */
    public native String stringFromJNI();
}