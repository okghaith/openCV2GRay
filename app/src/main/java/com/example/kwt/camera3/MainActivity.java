package com.example.kwt.camera3;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.WindowManager;
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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;


public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "MyActivity_OpenCVTest";
    private CameraBridgeViewBase mOpenCvCameraView;
    ImageView imageView_gray;
    ImageView imageView_canny;
    ImageView imageView_hough;
    Bitmap grayBitmap;
    Bitmap cannyBitmap;
    Bitmap houghBitmap;
    Mat mRgba;
    Mat gray;
    Mat canny;
    Mat hough;

    TextView text_canny_threshold1, text_canny_threshold2;
    SeekBar seek_canny_threshold1, seek_canny_threshold2;
    public int canny_threshold1, canny_threshold2;

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

        //Canny Seek bars
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
        grayBitmap = Bitmap.createBitmap(width,height,Bitmap.Config.RGB_565);
        cannyBitmap = Bitmap.createBitmap(width,height,Bitmap.Config.RGB_565);
        houghBitmap =  Bitmap.createBitmap(width,height,Bitmap.Config.RGB_565);
        mRgba = new Mat(width,height,CvType.CV_8UC4);
        gray = new Mat(width,height,CvType.CV_8SC1);
        canny = new Mat(width,height,CvType.CV_8SC1);
        hough = new Mat(width,height,CvType.CV_8SC1);
    }

    public void onCameraViewStopped() {

    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        //Log.i(TAG, "Got New Frame!");
        mRgba = inputFrame.rgba();

        Imgproc.cvtColor(mRgba, gray , Imgproc.COLOR_RGB2GRAY);
        Imgproc.Canny(gray, canny, canny_threshold1, canny_threshold2);

        hough = getHoughPTransform(canny,1, Math.PI / 180, 50, 5,50);

        Utils.matToBitmap(gray, grayBitmap);
        Utils.matToBitmap(canny,cannyBitmap);
        Utils.matToBitmap(hough,houghBitmap);


        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                imageView_gray.setImageBitmap(grayBitmap);
                imageView_canny.setImageBitmap(cannyBitmap);
                imageView_hough.setImageBitmap(houghBitmap);

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


    /**
     * A native method that is implemented by the 'native-lib' native library,

     * which is packaged with this application.
     */
    public native String stringFromJNI();
}