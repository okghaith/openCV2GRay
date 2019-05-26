package com.example.kwt.camera3;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.opencv.imgproc.Imgproc.fillConvexPoly;

public class LaneDetection implements CameraBridgeViewBase.CvCameraViewListener2 {
    Context context;
    private CameraBridgeViewBase mOpenCvCameraView;
    private BaseLoaderCallback mLoaderCallback;
    private static final String TAG = "LaneDetection";

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

    private Bitmap bmpBlack;

    public LaneDetection(Context con) {
        context = con;

        //OpenCV Initialization
        mLoaderCallback = new BaseLoaderCallback(context) {
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

        //set default values
        hough_threshold = 50;
        hough_minLength= 60;
        hough_maxGap= 80;
        canny_threshold1 = 100;
        canny_threshold2 = 150;
        PolyX1 = 400;
        PolyX2 = 190;
        PolyX3 = 0;
        PolyX4 = 52;


        //Set OpenCV View Visibility and Listener
        mOpenCvCameraView = ((Activity)context).findViewById(R.id.myCameraView);
//        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        imageView_hough = ((Activity)context).findViewById(R.id.imageView_hough);

        //Import Black Image and Convert to Bmp
        blackImgInit();
    }



    private void blackImgInit() {
        //upload black pic
        InputStream stream = null;
        Uri uri = Uri.parse("android.resource://" + ((Activity)context).getPackageName() + "/" + R.drawable.black_image_352_288);
        try {
            stream = ((Activity)context).getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
        bmpFactoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        //H:360,W:640, https://dummyimage.com/
        bmpBlack = BitmapFactory.decodeStream(stream, null, bmpFactoryOptions);
    }

    private void resetBlackImage() {
        Utils.bitmapToMat(bmpBlack, mask);
        Imgproc.cvtColor(mask, mask, Imgproc.COLOR_RGB2GRAY); // from 3 channels to 1 channel
    }

    protected void onPause() {
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    protected void onResume() {
        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(context, "openCV did not Loaded successfully", Toast.LENGTH_SHORT).show();
        } else {
            mLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }

    }

    protected void onDestroy() {
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }


    @Override
    public void onCameraViewStarted(int width, int height) {

        //update main home log box
        Intent intent_msg = new Intent("com.example.kwt.accelerometer.statuslog");
        intent_msg.putExtra("MSGLog","Camera Initialized");
        context.sendBroadcast(intent_msg);

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

    public void onCameraViewStopped() {

    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
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

        ((Activity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView_hough.setImageBitmap(houghBitmap);
            }
        });

        return mRgba;
    }


    public Mat getHoughPTransform(Mat masked_canny, double rho, double theta, int threshold, double minLineLength, double maxLineGap) {

        Mat lines = new Mat();
        //Calculate Lines
        Imgproc.HoughLinesP(masked_canny, lines, rho, theta, threshold, minLineLength, maxLineGap);

        Log.i(TAG+"1", "Shape: " + lines.size() + ", lines.cols(): "+ lines.cols() +",lines.rows():"+lines.rows()+"\n lines.dump() = " + lines.dump());

        //Drawing Green lines on the image
        for (int i = 0; i < lines.rows(); i++) {
            double[] points = lines.get(i, 0);
            if (points == null)
                continue;
            double x1, y1, x2, y2;
            x1 = points[0];
            y1 = points[1];
            x2 = points[2];
            y2 = points[3];

            Point pt1 = new Point(x1, y1);
            Point pt2 = new Point(x2, y2);

            //Drawing Green lines on an image
            Imgproc.line(mRgba, pt1, pt2, new Scalar(0, 255, 0), 1);
        }

        //Draw Red Average Line
        int[][] avgLeftRightLines = average_HoughLinesP(mRgba, lines);

        Log.i("leftRight","leftRightLines.length: " + avgLeftRightLines.length);

        int[] emptyLine = {0,0,0,0};
        int emptyLineCount = 0;

        for (int i = 0; i < avgLeftRightLines.length; i++) {
            int[] line = avgLeftRightLines[i];

            Log.i("leftRight"+"1", Arrays.toString(line));

            if(line == emptyLine){
                emptyLineCount++;
                continue;
            }

            //red color line
            Imgproc.line(mRgba, new Point(line[0], line[1]), new Point(line[2], line[3]), new Scalar(255, 0, 0), 3);
        }

        if (emptyLineCount == 2){
            Intent intent = new Intent("com.example.kwt.accelerometer.onLaneDetectionLost");
            context.sendBroadcast(intent);
            Log.i("leftRight", "Left Right Lines = 0, Broadcast sent");
        }
        return mRgba;
    }

    private int[][] average_HoughLinesP(Mat image, Mat lines) {

        //because of the inverse cartesian coordinate, lines on the left will
        //have negative slope and lines on right positive slope

        ArrayList<double[]> left_fit_lines = new ArrayList<double[]>();
        ArrayList<double[]> right_fit_lines = new ArrayList<double[]>();
        double[] left_fit_average;
        double[] right_fit_average;

        for (int i = 0; i < lines.rows(); i++) {
            double[] line = lines.get(i, 0);

            if (line == null)
                continue; //prevent crash when calculating slopeIntercept polynomial regression

            double[] parameters = polyFit_getSlopeIntercept(line);
            double intercept = parameters[0];
            double slope = parameters[1];

            Log.i(TAG+"1", "Line: "+ Arrays.toString(line)+", Slope:"+ slope + ", Intercept:"+ intercept);

            if (slope < -0.2 ) //ignore horizantal lines
                left_fit_lines.add(new double[]{slope, intercept});
            else if(slope > 0.2) //ignore horizantal lines
                right_fit_lines.add(new double[]{slope, intercept});
            else {
                //left_fit_lines.add(new double[]{0, 0});
                //right_fit_lines.add(new double[]{0, 0});
                Log.i(TAG+"_CrntLine", "Ignored Line\n");
            }
            Log.i(TAG+"_CrntLine", "X^0(Intercept-Y) = " + intercept + ", X^1(Slope M) = " + slope + "\n");

        }


        double[] leftAverage = average_slope_intercept(left_fit_lines);
        Log.i(TAG, "Left Slope AVG= " + leftAverage[0] + ", Y-Intercept AVG = " + leftAverage[1] + "\n");

        double[] rightAverage = average_slope_intercept(right_fit_lines);
        Log.i(TAG, "Right Slope AVG= " + rightAverage[0] + ", Y-Intercept AVG = " + rightAverage[1] + "\n");

        int[] leftLineCoordinates =  {0,0,0,0};
        int[] rightLineCoordinates = {0,0,0,0};

        if(leftAverage[0] != 0 || leftAverage[1] != 0 ) {
            leftLineCoordinates = make_coordinates(image, leftAverage);
        }
        if(rightAverage[0] != 0 || rightAverage[1] != 0 ) {
            rightLineCoordinates = make_coordinates(image, rightAverage);
        }

        Log.i("leftRight"+"2", "leftLineCoordinates" + Arrays.toString(leftLineCoordinates));
        Log.i("leftRight"+"2", "rightLineCoordinates" + Arrays.toString(rightLineCoordinates));

        return new int[][]{leftLineCoordinates, rightLineCoordinates};
    }

    private int[] make_coordinates(Mat image, double[] average) {

        //Log.i("make_coordinates", "rightLineCoordinates" + Arrays.toString(rightLineCoordinates));
        int y1 = image.height(); //height (bottom of the image)
        int y2 = (int) (y1 * 2 / 5);
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
    // return {Intercept, Slope}
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
}
