package com.scis.meraki.sensordatacollector;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.nfc.Tag;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;

import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Queue;
import java.util.LinkedList;
import java.lang.Float;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class DataCollectionService extends IntentService implements SensorEventListener{

    public static final String TAG = "TAG";

    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS

    private static final String ACTION_START = "com.scis.meraki.sensordatacollector.action.STARTBT";
    private static final String ACTION_STOP = "com.scis.meraki.sensordatacollector.action.STOPBT";

    //model
    private Interpreter tflite;
    String modelFile = "CNN_3.tflite";

    //sensor related variables
    private SensorManager mSensorManager = null;
    private Sensor accelerometerSensor;
    private static int samplingRate = 20000;

    private static long timeInterval, prevTime, currTime;

    private int accTick = 0;

    //flags
    private static boolean isGettingData = false;

    private static Context context;

    //inputData
    private static int windowSize = 100;
    private static int dims = 3;

    private static float[][] accData = new float[3][windowSize];

    private static float[] inputData = new float[windowSize * dims];

    private static float[][] accSensorData = new float[3][windowSize];

    public DataCollectionService() {
        super("DataCollectionService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        prevTime = currTime = System.currentTimeMillis();

        timeInterval = 1000;

        //create tflite object, loaded from model file
//        try {
//            tflite = new Interpreter(loadModelFile());
//        } catch (Exception ex){
//            ex.printStackTrace();
//        }

    }

    public static void startActionStart(Context c) {
        context = c;
        Intent intent = new Intent(context, DataCollectionService.class);
        intent.setAction(ACTION_START);
        context.startService(intent);
    }

    public static void startActionStop(Context c) {
        context = c;
        Intent intent = new Intent(context, DataCollectionService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
           String action = intent.getAction();
           if (action.equals(ACTION_START)) {
               handleActionStart();
           }
           if (action.equals(ACTION_STOP)) {
               handleActionStop();
           }
        }

    }
    private void handleActionStart() {

        startSensor();

    }
    private void handleActionStop() {

        stopSensors();

    }

    private void startSensor(){
        if(mSensorManager == null && !isGettingData){

            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            accelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(this, accelerometerSensor, samplingRate);

            isGettingData = true;

        }
    }

    private void stopSensors(){
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.unregisterListener(this,accelerometerSensor);
        mSensorManager = null;
        isGettingData = false;
    }

//    private void addAccData(float[][] data){
//        accData = data;
//        concatenateData();
//    }
//
//    private void concatenateData(){
//
//        for(int i = 0; i < windowSize; i++){
//            inputData[i] = accData[0][i];
//        }
//        for (int i = 0; i < windowSize; i++){
//            inputData[i +  windowSize] = accData[1][i];
//        }
//        for (int i = 0; i < windowSize; i++){
//            inputData[i + 2 * windowSize] = accData[2][i];
//        }
//
//
//        Log.d("data", "inputData : " + inputData.toString() );
//        Log.d("data", "accData : " + accData.toString() );
//
//        accData = null;
//
//        float[] result = doInference(inputData)[0];
//
//        Log.d(TAG, "printArray: " + printArray(inputData).substring(0,3000));
//        Log.d(TAG, "printArray: " + printArray(inputData).substring(3000));
//        Log.d(TAG, "probabilities: " + printArray(result));
//    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if(!isGettingData){
            stopSensors();
            Log.e("sensor change", "onSensorChanged: ");
            return;
        }

        printArray(sensorEvent.values);

//        if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
//
//            if (accTick >= windowSize) {
//                accTick = 0;
//                addAccData(deepClone(accSensorData));
//            } else {
//                accSensorData[0][accTick] = sensorEvent.values[1] / 9.80665f;
//                accSensorData[1][accTick] = sensorEvent.values[0] / 9.80665f;
//                accSensorData[2][accTick] = sensorEvent.values[2] / 9.80665f;
//
//                accTick++;
//            }
//        }
    }

//    public float[][] doInference(float[] input) {
//
//        float[][] output = new float[1][12];
//
//        try {
//            tflite.run(input, output);
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//
//        return output;
//    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

//    // Memory-map the model file from assets
//    private MappedByteBuffer loadModelFile() throws IOException {
//        // Open the model using an input stream, and memory map it to load
//        AssetFileDescriptor fileDescriptor = this.getAssets().openFd(modelFile);
//        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
//        FileChannel fileChannel = inputStream.getChannel();
//        long startOffset = fileDescriptor.getStartOffset();
//        long declaredLength = fileDescriptor.getDeclaredLength();
//        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
//    }

    public String printArray(float[] array) {
        String res = "";

        for (int i = 0; i < array.length; i++){
            Log.d(TAG, "printArray: " + array[i]);
            res += array[i] + (i == array.length-1 ? "" : ",");

        }

        return res;
    }
//
//    public int arrMax(float[] array) {
//        int highest = 0;
//
//        for (int i = 1; i < array.length; i++) {
//            if (array[i] > array[highest]){
//                highest = i;
//            }
//        }
//        Log.d(TAG, "arrMax: " + highest);
//        return highest;
//    }
//
//    public float[][] deepClone(float[][] array) {
//        if (array == null) {
//            return null;
//        }
//
//        final float[][] result = new float[array.length][];
//        for (int i = 0; i < array.length; i++) {
//            result[i] = Arrays.copyOf(array[i], array[i].length);
//        }
//        return result;
//    }

}
