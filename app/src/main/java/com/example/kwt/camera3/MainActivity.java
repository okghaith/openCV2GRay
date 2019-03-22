package com.example.kwt.camera3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorManager;
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

    //Sensors
    private SensorManager sensorManger;
    Sensor accelerometer;
    int accelServiceFlag = 1;

    //ImageViews
    ImageView imageView_gray;
    ImageView imageView_canny;
    ImageView imageView_hough;
    ImageView imageView_mask;
    ImageView imageView_maskCanny;

    //Bitmap vars
    Bitmap grayBitmap;
    Bitmap cannyBitmap;
    Bitmap houghBitmap;
    Bitmap maskBitmap;
    Bitmap masked_cannyBitmap;

    //OpenCV vars
    Mat mRgba;
    Mat gray;
    Mat canny;
    Mat hough;
    Mat mask;
    Mat masked_canny;

    //TextViews
    TextView textXval, textYval, textZval, textLongLat;


    //Seekbar variables
    TextView text_canny_threshold1, text_canny_threshold2, text_hough_threshold, text_hough_minLength, text_hough_maxGap, textViewPolyX1, textViewPolyX2, textViewPolyX3, textViewPolyX4;
    SeekBar seek_canny_threshold1, seek_canny_threshold2, seek_hough_threshold, seek_hough_minLength, seek_hough_maxGap, seekBarX1Poly, seekBarX2Poly, seekBarX3Poly, seekBarX4Poly;
    public int canny_threshold1, canny_threshold2, hough_threshold, hough_minLength, hough_maxGap, PolyX1, PolyX2, PolyX3,PolyX4, camWidth, camHeight;
    private Bitmap bmpBlack;


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

        //Set OpenCV View Visibility and Listener
        mOpenCvCameraView = findViewById(R.id.myCameraView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        imageView_gray = findViewById(R.id.imageView_gray);
        imageView_canny = findViewById(R.id.imageView_canny);
        imageView_hough = findViewById(R.id.imageView_hough);
        imageView_mask = findViewById(R.id.imageView_Mask);
        imageView_maskCanny = findViewById(R.id.imageView_maskCanny);

        //Import Black Image and Convert to Bmp
        blackImgInit();

        //Canny Seek bars
        cannySeekBars();

        //HoughLineP Seekbars
        houghLinePSeekbars();

        //Poly Triangle Seekbars
        polySeekbars();


        //Initialize textViews + Register Broadcast Receiver
        accelInit();

    }

    private void polySeekbars() {
        seekBarX1Poly = findViewById(R.id.seekBarX1Poly);
        seekBarX1Poly.setMax(900);
        textViewPolyX1 = findViewById(R.id.textViewPolyX1);
        textViewPolyX1.setText(seekBarX1Poly.getProgress() + " / " + seekBarX1Poly.getMax());
        seekBarX1Poly.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                PolyX1 = progress;
                textViewPolyX1.setText(progress + " / " + seekBarX1Poly.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekBarX2Poly = findViewById(R.id.seekBarX2Poly);
        seekBarX2Poly.setMax(900);
        textViewPolyX2 = findViewById(R.id.textViewPolyX2);
        textViewPolyX2.setText(seekBarX2Poly.getProgress() + " / " + seekBarX2Poly.getMax());
        seekBarX2Poly.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                PolyX2 = progress;
                textViewPolyX2.setText(progress + " / " + seekBarX2Poly.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        seekBarX3Poly = findViewById(R.id.seekBarX3Poly);
        seekBarX3Poly.setMax(900);
        textViewPolyX3 = findViewById(R.id.textViewPolyX3);
        textViewPolyX3.setText(seekBarX3Poly.getProgress() + " / " + seekBarX3Poly.getMax());
        seekBarX3Poly.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                PolyX3 = progress;
                textViewPolyX3.setText(progress + " / " + seekBarX3Poly.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        seekBarX4Poly = findViewById(R.id.seekBarX4Poly);
        seekBarX4Poly.setMax(900);
        seekBarX4Poly.setProgress(100);
        textViewPolyX4 = findViewById(R.id.textViewPolyX4);
        textViewPolyX4.setText(seekBarX4Poly.getProgress() + " / " + seekBarX4Poly.getMax());
        seekBarX4Poly.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                PolyX4 = progress;
                textViewPolyX4.setText(progress + " / " + seekBarX4Poly.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void blackImgInit() {
        //upload black pic
        InputStream stream = null;
        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.black_640_360);
        try {
            stream = getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
        bmpFactoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        //H:360,W:640
        bmpBlack = BitmapFactory.decodeStream(stream, null, bmpFactoryOptions);
    }

    private void accelInit() {

        //Accel textviews
        textXval = (TextView) findViewById(R.id.xValue);
        textYval = (TextView) findViewById(R.id.yValue);
        textZval = (TextView) findViewById(R.id.zValue);
        textLongLat = (TextView) findViewById(R.id.LongLat);

        //Receives X,Y,Z values from ShakerListener broadcast
        BroadcastReceiver sensorXYZUpdate = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                textXval.setText("textXval: " + intent.getExtras().getFloat("textXval"));
                textYval.setText("textYval: " + intent.getExtras().getFloat("textYval"));
                textZval.setText("textZval: " + intent.getExtras().getFloat("textZval"));

            }
        };

        //Register listener to XYZDATA Intent Broadcast
        registerReceiver(sensorXYZUpdate, new IntentFilter("com.example.kwt.accelerometer.XYZDATA"));


        final Button serviceB = (Button) findViewById(R.id.serviceB);
        accelServiceFlag = 1;
        serviceB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (accelServiceFlag == 1) {
                    Toast.makeText(MainActivity.this, "ACTIVATED!", Toast.LENGTH_LONG).show();
                    Log.d("MSG", "Activated the Service");
                    startService(new Intent(getApplicationContext(), ShakeService.class));
                    accelServiceFlag = 0;
                } else {
                    Toast.makeText(MainActivity.this, "DEACTIVATED!", Toast.LENGTH_LONG).show();
                    stopService(new Intent(getApplicationContext(), ShakeService.class));
                    Log.d("MSG", "Deactivated the Service");
                    accelServiceFlag = 1;
                }

            }
        });
    }

    private void houghLinePSeekbars() {
        seek_hough_threshold = (SeekBar) this.findViewById(R.id.seekBar3);
        seek_hough_minLength = (SeekBar) this.findViewById(R.id.seekBar4);
        seek_hough_maxGap = (SeekBar) this.findViewById(R.id.seekBar5);

        text_hough_threshold = (TextView) findViewById(R.id.textView3);
        text_hough_threshold.setText(seek_hough_threshold.getProgress() + " / " + seek_hough_threshold.getMax());

        text_hough_minLength = (TextView) findViewById(R.id.textView4);
        text_hough_minLength.setText(seek_hough_minLength.getProgress() + " / " + seek_hough_minLength.getMax());

        text_hough_maxGap = (TextView) findViewById(R.id.textView5);
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
        seek_canny_threshold1 = (SeekBar) this.findViewById(R.id.seekBar1);
        seek_canny_threshold2 = (SeekBar) this.findViewById(R.id.seekBar2);

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
                canny_threshold2 = progress;
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
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(getApplicationContext(), "openCV did not Loaded successfully", Toast.LENGTH_SHORT).show();
        } else {
            mLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    public void onCameraViewStarted(int width, int height) {
        //H360:,W:640
        camHeight = height;
        camWidth = width;
        grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        cannyBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        houghBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        masked_cannyBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        mRgba = new Mat(width, height, CvType.CV_8UC4);
        gray = new Mat(width, height, CvType.CV_8SC1);
        canny = new Mat(width, height, CvType.CV_8SC1);
        hough = new Mat(width, height, CvType.CV_8SC1);
        masked_canny = new Mat(width, height, CvType.CV_8SC1);
        mask = new Mat(width, height, CvType.CV_8SC1);
        resetBlackImage();

    }

    private void resetBlackImage() {
        Utils.bitmapToMat(bmpBlack, mask);
        Imgproc.cvtColor(mask, mask, Imgproc.COLOR_RGB2GRAY); // from 3 channels to 1 channel
    }

    public void onCameraViewStopped() {

    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        //Log.i(TAG, "Got New Frame!");
        mRgba = inputFrame.rgba();

        Imgproc.cvtColor(mRgba, gray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.Canny(gray, canny, canny_threshold1, canny_threshold2);

        List<Point> cropMaskArray = new ArrayList<>();
        cropMaskArray.add(new Point(PolyX1, camHeight));
        cropMaskArray.add(new Point(PolyX2, PolyX4));
        cropMaskArray.add(new Point(PolyX3, camHeight));
        org.opencv.core.Point[] pointArray = new org.opencv.core.Point[cropMaskArray.size()];
        Point pt;
        for (int i = 0; i < cropMaskArray.size(); i++) {
            pt = cropMaskArray.get(i);
            pointArray[i] = new org.opencv.core.Point(pt.x, pt.y);
        }
        MatOfPoint points = new MatOfPoint(pointArray);
        resetBlackImage();
        fillConvexPoly(mask, points, new Scalar(255, 255, 255));


        Core.bitwise_and(canny, mask, masked_canny);//mask should be just 1 channel

        hough = getHoughPTransform(masked_canny, 1, Math.PI / 180, hough_threshold, hough_minLength, hough_maxGap);

        //H:360XW:640
        Utils.matToBitmap(gray, grayBitmap);
        Utils.matToBitmap(canny, cannyBitmap);
        Utils.matToBitmap(hough, houghBitmap);
        Utils.matToBitmap(mask, maskBitmap);
        Utils.matToBitmap(masked_canny, masked_cannyBitmap);


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
        Mat lines = new Mat();

        //Calculate Lines
        Imgproc.HoughLinesP(image, lines, rho, theta, threshold, minLineLength, maxLineGap);

        int[][] leftRightLines = average_HoughLinesP(image, lines);

        //Log.i(TAG, "lines.cols()" + lines.cols());

        for (int i = 0; i < lines.cols(); i++) {
            double[] val = lines.get(0, i);
            if (val == null)
                break;
            //Log.i(TAG, "val[0], val[1] val[2], val[3]" + val[0] +", "+ val[1]+", "+  val[2] +", "+  val[3]);
//            double[] parameters  = polyFit_getSlopeIntercept(val);
//            double slope = parameters[0];
//            double intercept = parameters[1];
//
//            Log.i(TAG, "X^0 = " + intercept + "\n");
//            Log.i(TAG, "X^1 = " + slope + "\n");
            Imgproc.line(mRgba, new Point(val[0], val[1]), new Point(val[2], val[3]), new Scalar(0, 0, 255), 4);
        }

//        for (int i = 0; i < leftRightLines.length; i++) {
//            int[] line = leftRightLines[i];
//            if (line == null)
//                break;
//            Imgproc.line(mRgba, new Point(line[0], line[1]), new Point(line[2], line[3]), new Scalar(0, 0, 255), 4);
//
//        }
        return mRgba;
    }

    private int[][] average_HoughLinesP(Mat image, Mat lines) {

        //because of the inverse cartesian coordinate, lines on the left will
        //have negative slope and lines on right positive slope

        ArrayList<double[]> left_fit = new ArrayList<double[]>();
        ArrayList<double[]> right_fit = new ArrayList<double[]>();
        double[] left_fit_average;
        double[] right_fit_average;

        for (int i = 0; i < lines.cols(); i++) {
            double[] twoPoints = lines.get(0, i);

            if (twoPoints == null)
                continue; //prevent crash when calculating slopeIntercept polynomial regression

            double[] parameters = polyFit_getSlopeIntercept(twoPoints);
            double slope = parameters[0];
            double intercept = parameters[1];

            if (slope < 0)
                left_fit.add(new double[]{slope, intercept});
            else
                right_fit.add(new double[]{slope, intercept});
//            Log.i(TAG, "X^0 = " + intercept + "\n");
//            Log.i(TAG, "X^1 = " + slope + "\n");
        }

        //check if there is a missing line, then add dummy line
        if (left_fit.size() == 0)
            left_fit.add(new double[]{1, 1});
        if (right_fit.size() == 0)
            right_fit.add(new double[]{-1, 0});


//        left_fit_average = average_slope_intercept(left_fit);
//        right_fit_average = average_slope_intercept(right_fit);


        double[] leftAverage = average_slope_intercept(left_fit);
        Log.i(TAG, "Left Slope AVG= " + leftAverage[0] + ", Y-Intercept AVG = " + leftAverage[1] + "\n");

        double[] rightAverage = average_slope_intercept(right_fit);
        Log.i(TAG, "Right Slope AVG= " + rightAverage[0] + ", Y-Intercept AVG = " + rightAverage[1] + "\n");


        int[] leftLineCoordinates = make_coordinates(image, leftAverage);

        int[] rightLineCoordinates = make_coordinates(image, rightAverage);

        return new int[][]{leftLineCoordinates, rightLineCoordinates};
    }

    private int[] make_coordinates(Mat image, double[] average) {
        int y1 = image.height();
        int y2 = (int) (y1 * 4 / 5);
        int x1 = (int) ((y1 - average[1]) / average[0]);
        int x2 = (int) ((y2 - average[1]) / average[0]);

        return new int[]{x1, y1, x2, y2};
    }

    private double[] average_slope_intercept(ArrayList<double[]> lines) {


        double sumSlopes = 0;
        double avgSlope = 0;
        double sumIntercepts = 0;
        double avgIntercept = 0;

        //calculate slopes and intercept average
        if (!(lines.size() == 0)) {
            for (double[] line : lines) {
                sumSlopes += line[0];
                sumIntercepts += line[1];
            }
            avgSlope = sumSlopes / lines.size();
            avgIntercept = sumIntercepts / lines.size();
        }

        return new double[]{avgSlope, avgIntercept};
    }

    // From: https://www.bragitoff.com/2017/04/polynomial-fitting-java-codeprogram-works-android-well/
    private double[] polyFit_getSlopeIntercept(double[] twoPoints) {

        System.out.print("polyfit Run\n");

        double[] x = {twoPoints[0], twoPoints[2]};        //array to store x-axis data points
        double[] y = {twoPoints[1], twoPoints[3]};         //array to store y-axis data points

/*
        http://polynomialregression.drque.net/online.php
        double[] x= {1,3};        //array to store x-axis data points
        double[] y= {2,4};         //array to store y-axis data points
*/

        int n = 1;                       //degree of polynomial to fit the data
        int N = 2;                       //no. of data points

        double X[] = new double[2 * n + 1];
        for (int i = 0; i < 2 * n + 1; i++) {
            X[i] = 0;
            for (int j = 0; j < N; j++)
                X[i] = X[i] + Math.pow(x[j], i);        //consecutive positions of the array will store N,sigma(xi),sigma(xi^2),sigma(xi^3)....sigma(xi^2n)
        }
        double B[][] = new double[n + 1][n + 2], a[] = new double[n + 1];            //B is the Normal matrix(augmented) that will store the equations, 'a' is for value of the final coefficients
        for (int i = 0; i <= n; i++)
            for (int j = 0; j <= n; j++)
                B[i][j] = X[i + j];            //Build the Normal matrix by storing the corresponding coefficients at the right positions except the last column of the matrix
        double Y[] = new double[n + 1];                    //Array to store the values of sigma(yi),sigma(xi*yi),sigma(xi^2*yi)...sigma(xi^n*yi)
        for (int i = 0; i < n + 1; i++) {
            Y[i] = 0;
            for (int j = 0; j < N; j++)
                Y[i] = Y[i] + Math.pow(x[j], i) * y[j];        //consecutive positions will store sigma(yi),sigma(xi*yi),sigma(xi^2*yi)...sigma(xi^n*yi)
        }
        for (int i = 0; i <= n; i++)
            B[i][n + 1] = Y[i];                //load the values of Y as the last column of B(Normal Matrix but augmented)
        n = n + 1;
        for (int i = 0; i < n; i++)                    //From now Gaussian Elimination starts(can be ignored) to solve the set of linear equations (Pivotisation)
            for (int k = i + 1; k < n; k++)
                if (B[i][i] < B[k][i])
                    for (int j = 0; j <= n; j++) {
                        double temp = B[i][j];
                        B[i][j] = B[k][j];
                        B[k][j] = temp;
                    }

        for (int i = 0; i < n - 1; i++)            //loop to perform the gauss elimination
            for (int k = i + 1; k < n; k++) {
                double t = B[k][i] / B[i][i];
                for (int j = 0; j <= n; j++)
                    B[k][j] = B[k][j] - t * B[i][j];    //make the elements below the pivot elements equal to zero or elimnate the variables
            }
        for (int i = n - 1; i >= 0; i--)                //back-substitution
        {                        //x is an array whose values correspond to the values of x,y,z..
            a[i] = B[i][n];                //make the variable to be calculated equal to the rhs of the last equation
            for (int j = 0; j < n; j++)
                if (j != i)            //then subtract all the lhs values except the coefficient of the variable whose value                                   is being calculated
                    a[i] = a[i] - B[i][j] * a[j];
            a[i] = a[i] / B[i][i];            //now finally divide the rhs by the coefficient of the variable to be calculated
        }
        return a;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is ...
        return false;
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * <p>
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}