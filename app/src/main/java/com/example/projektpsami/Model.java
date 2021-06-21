package com.example.projektpsami;


import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Model {

    private Interpreter tflite;
    private int imageSizeX = 300;
    private int imageSizeY = 300;
    private Context context;
    private String[] labels;

    public Model(Context cont, String[] lab) throws IOException {
        context=cont;
        loadModelFile();
        labels = lab;
    }

    public Mat get_output(Mat mat){
        

        Bitmap img= Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, img);
        img=getResizedBitmap(img, imageSizeX, imageSizeY);

        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeOp(imageSizeY, imageSizeX, ResizeOp.ResizeMethod.BILINEAR))
                        .build();
        TensorImage tensorImage = new TensorImage(DataType.UINT8);
        tensorImage.load(img);
        tensorImage = imageProcessor.process(tensorImage);


        float[][][] output1 = new float[1][10][4];
        float[][] output2 = new float[1][10];
        float[][] output3 = new float[1][10];
        float[] output4 = new float[1];
        Object[] inputs = {tensorImage.getBuffer()};
        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(0, output1);
        outputs.put(1, output2);
        outputs.put(2, output3);
        outputs.put(3, output4);
        tflite.runForMultipleInputsOutputs(inputs, outputs);


        Mat img_mat = new Mat(mat.height(), mat.width(), CvType.CV_8UC3);
        mat.copyTo(img_mat);
        Rect rec = new Rect();
        for(int i=0; i<output4[0]; i++){
            if(output3[0][i]>0.5) {
                rec.set(new double[]{img_mat.width() * output1[0][i][1], img_mat.height() * output1[0][i][0],
                        img_mat.width() * (output1[0][i][3] - output1[0][i][1]), img_mat.height() * (output1[0][i][2] - output1[0][i][0])
                });

                Imgproc.rectangle(img_mat, new Point(rec.x, rec.y), new Point(rec.x+rec.width, rec.y+rec.height),new Scalar(255, 0, 0, 255), 2);
               Imgproc.putText(img_mat, labels[(int) output2[0][i]], new Point(mat.width() * (output1[0][i][1] + (output1[0][i][3] - output1[0][i][1]) / 2), mat.height()  * output1[0][i][0]), Core.FONT_HERSHEY_COMPLEX, 0.9, new Scalar(255, 0, 0, 255), 2);
            }
        }

    return img_mat;
    }


    private Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight){
        int width = bm.getWidth() ;
        int height = bm.getHeight();
        float scaleWidth = (float)newWidth / width;
        float scaleHeight = (float)newHeight / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false ) ;
    }


    private void loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd("neural_network.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        Long startOffset = fileDescriptor.getStartOffset();
        Long declaredLength = fileDescriptor.getDeclaredLength();
        Interpreter.Options tfliteOptions = new Interpreter.Options();
        MappedByteBuffer mappedByteBuffer = fileChannel.map( FileChannel.MapMode.READ_ONLY,
                startOffset, declaredLength);
        tflite = new Interpreter(mappedByteBuffer, tfliteOptions);
    }

}
