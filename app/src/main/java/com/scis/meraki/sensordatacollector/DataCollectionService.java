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

    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS

    private static final String ACTION_START = "com.scis.meraki.sensordatacollector.action.STARTBT";
    private static final String ACTION_STOP = "com.scis.meraki.sensordatacollector.action.STOPBT";

    //model
    private Interpreter tflite;

    // Debugging
    private static final String TAG = "BluetoothService";

    //sensor related variables
    private SensorManager mSensorManager = null;
    private Sensor accelerometerSensor;
    private Sensor gyroscopeSensor;
    private static int samplingRate = 20000;

    private static long timeInterval, prevTime, currTime;
    private static String dataBuffer;
    private static int tick = 0;

    private int accTick = 0;
    private int gyroTick = 0;

    //flags
    private static boolean isGettingData = false;

    private static Context context;

    //inputData
    private static int windowSize = 100;
    private static int dims = 6;

    private static float[] gyroData = null;
    private static float[] accData = null;

    private static float[] inputData = new float[windowSize * dims];

    private static float[] accSensorData = new float[windowSize * 3];
    private static float[] gyroSensorData = new float[windowSize * 3];

    Queue<Float> bufferQueue = new LinkedList<>();

    //bluetooth
    private static BluetoothService btService;
    @SuppressLint("HandlerLeak")
    private final Handler btHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case Constants.MESSAGE_TOAST:

                    Toast.makeText(context, msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();

                    break;

                case Constants.MESSAGE_READ:

                    byte[] data = new byte[msg.arg1];
                    byte[] buffer = (byte[])msg.obj;
                    for(int i = 0; i < msg.arg1; i++){
                        data[i] = buffer[i];
                    }

                    String dataStr = new String(data, StandardCharsets.UTF_8);

                    Log.d(TAG, "handleMessage: " + dataStr);

                    interpretData(dataStr);
                    
                    break;
            }
        }

        private void interpretData(String data){
            if(data.charAt(0) == 'C'){
                String[] command = data.split(" ");
                String op = command[1];

                switch(op){
                    case  Constants.OP_SETFREQ:
                        float freq = Float.parseFloat(command[2]);
                        setFreq(freq);
                        break;
                }

            }
        }

        private void setFreq(float freq){
            samplingRate = (int)(1000000/freq);
            stopSensors();
            Log.d(TAG, "setFreq: restarting");
            startSensor();
            String res = "RES Frequency set to " + freq + "hz";
            btService.write(res.getBytes(StandardCharsets.UTF_8));
        }
    };



    public DataCollectionService() {
        super("DataCollectionService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        prevTime = currTime = System.currentTimeMillis();

        timeInterval = 1000;

        //create tflite object, loaded from model file
        try {
            tflite = new Interpreter(loadModelFile());
        } catch (Exception ex){
            ex.printStackTrace();
        }

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

        if(btService == null){
            btService = new BluetoothService(context,btHandler);
            btService.start();
        } else {
            btService.start();
        }

    }
    private void handleActionStop() {
        stopSensors();
        btService.stop();
    }

    private void startSensor(){
        if(mSensorManager == null && !isGettingData){

            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            accelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(this, accelerometerSensor, samplingRate);

            gyroscopeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            mSensorManager.registerListener(this, gyroscopeSensor, samplingRate);

            isGettingData = true;

        }
    }

    private void stopSensors(){
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.unregisterListener(this,accelerometerSensor);
        mSensorManager = null;
        isGettingData = false;
    }

    private void addGyroData(float[] data){
        this.gyroData = data;

        if(accData != null) {
            concatenateData();
        }
    }

    private void addAccData(float[] data){
        this.accData = data;

        if(gyroData != null){
            concatenateData();
        }
    }

    private void concatenateData(){

        for(int i = 0; i < windowSize; i++){
            inputData[i * 6] = accData[i * 3];
            inputData[i * 6 + 1] = accData[i * 3 + 1];
            inputData[i * 6 + 2] = accData[i * 3 + 2];
            inputData[i * 6 + 3] = gyroData[i * 3];
            inputData[i * 6 + 4] = gyroData[i * 3 + 1];
            inputData[i * 6 + 5] = gyroData[i * 3 + 2];
        }

        Log.d("data", "inputData : " + inputData.toString() );
        Log.d("data", "accData : " + accData.toString() );
        Log.d("data", "gyroData : " + gyroData.toString() );

        accData = null;
        gyroData = null;

        dataBuffer = "" + arrMax(doInference(inputData)[0]);

        Log.d(TAG, "onSensorChanged: " + dataBuffer);
        byte[] out = dataBuffer.getBytes(StandardCharsets.UTF_8);
        btService.write(out);

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if(!isGettingData){
            stopSensors();
            Log.e("sensor change", "onSensorChanged: ");
            return;
        }

        if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){

            if (accTick >= windowSize) {
                accTick = 0;
                addAccData(accSensorData.clone());
            } else {
                accSensorData[accTick * 3] = sensorEvent.values[0] / 9.80665f;
                accSensorData[accTick * 3 + 1] = sensorEvent.values[1] / 9.80665f;
                accSensorData[accTick * 3 + 2] = sensorEvent.values[2] / 9.80665f;
                accTick++;
            }
        } if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

            if (gyroTick >= windowSize) {
                gyroTick = 0;
                addGyroData(gyroSensorData.clone());
            } else {
                gyroSensorData[gyroTick * 3] = sensorEvent.values[0];
                gyroSensorData[gyroTick * 3 + 1] = sensorEvent.values[1];
                gyroSensorData[gyroTick * 3 + 2] = sensorEvent.values[2];
                gyroTick++;
            }
        }

//        currTime = System.currentTimeMillis();
//
//        bufferQueue.add(sensorEvent.values[0]);
//        bufferQueue.add(sensorEvent.values[1]);
//        bufferQueue.add(sensorEvent.values[2]);
//
//        tick++;
//
//        if(tick == 159){
//            arrayAppender(inputData, bufferQueue);
//        }




    }

    public float[][] doInference(float[] input) {

        float[][] output = new float[1][12];

        try {
            tflite.run(input, output);
        } catch (Exception e){
            e.printStackTrace();
        }

//        printArray(output[0]);
        return output;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    // Memory-map the model file from assets
    private MappedByteBuffer loadModelFile() throws IOException {
        // Open the model using an input stream, and memory map it to load
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("CNN_Model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

//    public String printArray(float[] array) {
//        String res = "";
//
//        for (int i = 0; i < array.length; i++){
////            Log.d(TAG, "printArray: " + array[i]);
//            res += array[i] + (i == array.length-1 ? "" : ",");
//
//        }
//
//        return res;
//    }

    public int arrMax(float[] array) {
        int highest = 0;

        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[highest]){
                highest = i;
            }
        }
        Log.d(TAG, "arrMax: " + highest);
        return highest;
    }



}
