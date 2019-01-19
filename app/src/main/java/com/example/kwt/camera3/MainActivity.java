package com.example.kwt.camera3;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    CameraBridgeViewBase cameraBridgeViewBase;

    Mat mat1,mat2,mat3;
    Mat Gray;
    BaseLoaderCallback baseLoaderCallback;


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.myCameraView);
        cameraBridgeViewBase.setVisibility(JavaCameraView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);

        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                switch (status)
                {
                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };


    }


    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        mat1 = inputFrame.rgba();
        Mat mRgba = mat1.t();
        Core.flip(mat1.t(),mRgba,1);
        Imgproc.resize(mRgba,mRgba,mat1.size());
        Imgproc.cvtColor(mRgba,Gray,Imgproc.COLOR_RGB2GRAY);
        return null;
    }

    @Override
    public void onCameraViewStopped() {
        mat1.release();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mat1 = new Mat(width,height,CvType.CV_8UC4);
        mat2 = new Mat(width,height,CvType.CV_8UC4);//remove
        mat3 = new Mat(width,height,CvType.CV_8UC4);//remove
        Gray = new Mat(width,height,CvType.CV_8SC1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraBridgeViewBase!=null)
        {
            cameraBridgeViewBase.disableView();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        super.onResume();
        if(!OpenCVLoader.initDebug())
        {
            Toast.makeText(getApplicationContext(),"openCV did not Loaded successfully",Toast.LENGTH_SHORT).show();
        }
        else
        {
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(cameraBridgeViewBase!=null)
        {
            cameraBridgeViewBase.disableView();
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
