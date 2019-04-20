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
import android.support.annotation.Nullable;
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

import static org.opencv.imgproc.Imgproc.compareHist;
import static org.opencv.imgproc.Imgproc.fillConvexPoly;


public class Home extends AppCompatActivity implements CvCameraViewListener2 {
    private static final String TAG = "Home_OpenCVTest";
    private CameraBridgeViewBase mOpenCvCameraView;

    //Sensors
    BroadcastReceiver sensorXYZUpdate;

    //ImageViews
    ImageView imageView_hough;

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
    TextView textXval, textYval, textZval, textLongLat, textGforce;

    public int canny_threshold1, canny_threshold2, hough_threshold, hough_minLength, hough_maxGap, PolyX1, PolyX2, PolyX3,PolyX4, camWidth, camHeight,
            gForce;
    String phoneNumber;
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
        setContentView(R.layout.home);
        Log.i(TAG, "called onCreate");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Default values
        phoneNumber = "+64211484398";
        gForce = 10000;
        hough_threshold = 70;
        hough_minLength= 35;
        hough_maxGap= 22;
        canny_threshold1 = 70;
        canny_threshold2 = 70;
        PolyX1 = 300;
        PolyX2 = 200;
        PolyX3 = 70;
        PolyX4 = 125;

        //Set OpenCV View Visibility and Listener
        mOpenCvCameraView = findViewById(R.id.myCameraView);
//        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        imageView_hough = findViewById(R.id.imageView_hough);

        //Import Black Image and Convert to Bmp
        blackImgInit();

        //Initialize textViews + Register Broadcast Receiver
        accelInit();
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
        //H:360,W:640, https://dummyimage.com/
        bmpBlack = BitmapFactory.decodeStream(stream, null, bmpFactoryOptions);
    }

    private void accelInit() {

        //Accel textviews
        textXval = (TextView) findViewById(R.id.xValue);
        textYval = (TextView) findViewById(R.id.yValue);
        textZval = (TextView) findViewById(R.id.zValue);
        textLongLat = (TextView) findViewById(R.id.LongLat);

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
        registerReceiver(sensorXYZUpdate, new IntentFilter("com.example.kwt.accelerometer.XYZDATA"));

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
        this.unregisterReceiver(sensorXYZUpdate);
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
                imageView_hough.setImageBitmap(houghBitmap);
            }
        });


        return mRgba;
    }


    public Mat getHoughPTransform(Mat image, double rho, double theta, int threshold, double minLineLength, double maxLineGap) {
        Mat lines = new Mat();

        //Calculate Lines
        Imgproc.HoughLinesP(image, lines, rho, theta, threshold, minLineLength, maxLineGap);


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
        int[][] leftRightLines = average_HoughLinesP(image, lines);

        for (int i = 0; i < leftRightLines.length; i++) {
            int[] line = leftRightLines[i];
            if (line == null)
                continue;
            Log.i(TAG, "line[0], line[1] line[2], line[3]" + line[0] +", "+ line[1]+", "+  line[2] +", "+  line[3]);
            Imgproc.line(mRgba, new Point(line[0], line[1]), new Point(line[2], line[3]), new Scalar(255, 0, 0), 4);

        }
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
//        if (left_fit.size() == 0)
//            left_fit.add(new double[]{1, 1});
//        if (right_fit.size() == 0)
//            right_fit.add(new double[]{-1, 0});


//        left_fit_average = average_slope_intercept(left_fit);
//        right_fit_average = average_slope_intercept(right_fit);


        double[] leftAverage = average_slope_intercept(left_fit);
        Log.i(TAG, "Left Slope AVG= " + leftAverage[0] + ", Y-Intercept AVG = " + leftAverage[1] + "\n");

        double[] rightAverage = average_slope_intercept(right_fit);
        Log.i(TAG, "Right Slope AVG= " + rightAverage[0] + ", Y-Intercept AVG = " + rightAverage[1] + "\n");

        int[] leftLineCoordinates = {0,0,0,0,0};
        int[] rightLineCoordinates = {0,0,0,0};

        if(leftAverage[0] != 0 || leftAverage[1] != 0 ) {
            leftLineCoordinates = make_coordinates(image, leftAverage);
            return new int[][]{leftLineCoordinates, {0, 0, 0, 0}};
        }
        if(leftAverage[0] != 0 || leftAverage[1] != 0 ) {
            rightLineCoordinates = make_coordinates(image, rightAverage);
            return new int[][]{{0, 0, 0, 0}, rightLineCoordinates};
        }

        return new int[][]{{0,0, 0, 0}, {0, 0, 0,0}};
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

    public void onOpenSettings(View view) {

        Intent getSettings = new Intent(this, Settings.class);
        final int result = 1;
        getSettings.putExtra("Caller", "homepage");

        startActivityForResult(getSettings, result);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        int[] cannySettings  = data.getIntArrayExtra("cannySettings");
        int[] PolyMaskSettings  = data.getIntArrayExtra("PolyMaskSettings");
        int[] HoughLinePSettings = data.getIntArrayExtra("HoughLinePSettings");

        String phoneNumber = data.getStringExtra("phoneNumber");
        String g_Force = data.getStringExtra("gForce");
        gForce = Integer.parseInt(g_Force);
        textGforce = findViewById(R.id.g_forceVal);

        if ( g_Force != null)
            textGforce.setText(g_Force);
        if (phoneNumber == null)
            phoneNumber = "+64211484398";

        hough_threshold = HoughLinePSettings[0];
        hough_minLength= HoughLinePSettings[1];
        hough_maxGap= HoughLinePSettings[2];

        canny_threshold1 = cannySettings[0];
        canny_threshold2 = cannySettings[1];

        PolyX1 = PolyMaskSettings[0];
        PolyX2 = PolyMaskSettings[1];
        PolyX3 = PolyMaskSettings[2];
        PolyX4 = PolyMaskSettings[3];
    }
}