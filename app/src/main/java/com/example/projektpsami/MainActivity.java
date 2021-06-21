package com.example.projektpsami;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;

//imports to add
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    enum Choices {NOTHING, EMBOSS, CANNY, MONOCHROME, INVERT, CUSTOM, CARTOON, RECOGNIZE};
    Choices choice = Choices.NOTHING;

    Button recognizeButton;
    Button customEffectButton;
    Button cartoonEffectButton;
    Button monochromeEffectButton;
    Button embossEffectButton;
    Button invertEffectButton;
    Button edgeDetectorButton;


    boolean front = false;
    BaseLoaderCallback baseLoaderCallback;
    CameraBridgeViewBase cameraBridgeViewBase;



    Mat frame;
    Mat outputFrame;
    Mat gray;
    Mat blur;
    Mat edge;

    String[] labels = new String[90];
    Model tfliteModel;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.javaCameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);
        cameraBridgeViewBase.setScaleY( (float) (16.0/12.0) );
        cameraBridgeViewBase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(front){
                    cameraBridgeViewBase.disableView();
                    front = false;
                    cameraBridgeViewBase.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
                    cameraBridgeViewBase.enableView();
                }
                else{
                    front = true;
                    cameraBridgeViewBase.disableView();
                    cameraBridgeViewBase.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
                    cameraBridgeViewBase.enableView();
                }

            }
        });




        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(getAssets().open("labels.txt")));
            String mLine;
            int i=0;
            while (
                    (mLine = reader.readLine()) != null) {
                labels[i] = mLine;
                i++;
            }
        } catch (IOException e) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e) {
                }
            }
        }




        try {
            tfliteModel = new Model(this, labels);
        } catch (IOException e) {
            e.printStackTrace();
        }


        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                switch (status){
                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };

        if(!OpenCVLoader.initDebug()){
            Log.d("TAG", "OpenCV-FAIL");
        }
        else{
            Log.d("TAG", "OpenCV-OK");
            baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS);
        }

        customEffectButton = findViewById(R.id.buttonCustomEffect);
        cartoonEffectButton = findViewById(R.id.buttonCartoonEffect);
        monochromeEffectButton = findViewById(R.id.buttonMonochromeEffect);
        embossEffectButton = findViewById(R.id.buttonEmbossEffect);
        invertEffectButton = findViewById(R.id.buttonInvert);
        edgeDetectorButton = findViewById(R.id.buttonEdgeDetector);
        recognizeButton = findViewById(R.id.buttonRecognize);


        customEffectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(choice == Choices.CUSTOM){
                    choice = Choices.NOTHING;
                }
                else{
                    choice = Choices.CUSTOM;
                }

            }
        });
        cartoonEffectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(choice == Choices.CARTOON){
                    choice = Choices.NOTHING;
                }
                else{
                    choice = Choices.CARTOON;
                }
            }
        });
        monochromeEffectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(choice == Choices.MONOCHROME){
                    choice = Choices.NOTHING;
                }
                else{
                    choice = Choices.MONOCHROME;
                }
            }
        });
        embossEffectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(choice == Choices.EMBOSS){
                    choice = Choices.NOTHING;
                }
                else{
                    choice = Choices.EMBOSS;
                }
            }
        });
        invertEffectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(choice == Choices.INVERT){
                    choice = Choices.NOTHING;
                }
                else{
                    choice = Choices.INVERT;
                }
            }
        });
        edgeDetectorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(choice == Choices.CANNY){
                    choice = Choices.NOTHING;
                }
                else{
                    choice = Choices.CANNY;
                }
            }
        });
        recognizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(choice == Choices.RECOGNIZE){
                    choice = Choices.NOTHING;
                }
                else{
                    choice = Choices.RECOGNIZE;
                }

            }
        });


    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        frame = new Mat(height, width, CvType.CV_8UC4);
        outputFrame = new Mat(height, width, CvType.CV_8UC3);
        gray = new Mat(height, width, CvType.CV_8UC1);
        blur = new Mat(height, width, CvType.CV_8UC1);
        edge = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        frame.release();
        outputFrame.release();
        gray.release();
        blur.release();
        edge.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        frame = inputFrame.rgba();

        Imgproc.cvtColor(frame, outputFrame, Imgproc.COLOR_RGBA2RGB);


        if(front){
            Core.rotate(outputFrame, outputFrame, Core.ROTATE_90_COUNTERCLOCKWISE);
        }
        else{

            Core.rotate(outputFrame, outputFrame, Core.ROTATE_90_CLOCKWISE);
        }

        switch(choice){
            case NOTHING:
                break;
            case EMBOSS:
                Mat embossMat= new Mat(3,3,CvType.CV_16SC1);
                embossMat.put(0, 0, 0, -1, -1, 1, 0, -1, 1, 1, 0);
                Imgproc.filter2D(outputFrame, outputFrame, outputFrame.depth(), embossMat);
                break;
            case CANNY:
                Imgproc.cvtColor(outputFrame, gray, Imgproc.COLOR_RGB2GRAY);
                Imgproc.Canny(gray, outputFrame, 50, 100);
                break;
            case MONOCHROME:
                Imgproc.cvtColor(outputFrame, outputFrame, Imgproc.COLOR_RGB2GRAY);
                break;
            case INVERT:
                Imgproc.cvtColor(outputFrame, gray, Imgproc.COLOR_RGB2GRAY);
                Core.bitwise_not(gray, gray);
                Imgproc.cvtColor(gray, outputFrame, Imgproc.COLOR_GRAY2RGB);
                break;
            case CUSTOM:
                Imgproc.cvtColor(outputFrame, gray, Imgproc.COLOR_RGB2GRAY);
                Core.bitwise_not(gray, gray);
                Imgproc.GaussianBlur(gray, blur, new Size(15, 15), 0);
                Core.bitwise_xor(gray, blur, blur);
                Imgproc.cvtColor(blur, blur, Imgproc.COLOR_GRAY2RGB);
                Core.bitwise_and(outputFrame, blur, outputFrame);
                break;
            case CARTOON:
                Imgproc.cvtColor(outputFrame, gray, Imgproc.COLOR_RGB2GRAY);
                Imgproc.adaptiveThreshold(gray, edge,255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 9, 4);
                Imgproc.cvtColor(edge, edge, Imgproc.COLOR_GRAY2RGB);
                Core.bitwise_and(outputFrame, edge, outputFrame);
                break;
            case RECOGNIZE:
                outputFrame=tfliteModel.get_output(outputFrame);
                break;

        }



        Imgproc.resize(outputFrame, outputFrame, inputFrame.rgba().size());

        return outputFrame;
    }






    @Override
    protected void onResume() {
        cameraBridgeViewBase.enableView();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraBridgeViewBase!=null){
            cameraBridgeViewBase.disableView();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(cameraBridgeViewBase!=null){
            cameraBridgeViewBase.disableView();
        }
    }


}