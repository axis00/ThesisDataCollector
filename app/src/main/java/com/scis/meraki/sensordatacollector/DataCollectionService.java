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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

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

    //model
    private Interpreter tflite;

    private float inferredValue;

    // Debugging
    private static final String TAG = "BluetoothService";

    //sensor related variables
    private SensorManager mSensorManager = null;
    private Sensor accelerometerSensor;
    private Sensor gyroscopeSensor;
    private static int samplingRate = 1000000;

    private static long timeInterval, prevTime, currTime;
    private static float[] dataBuffer = new float[3];
    private static int tick = 0;

    //flags
    private static boolean isGettingData = false;

    private static Context context;

    //data
    private float[] data = new float[960];
    Queue<Float> bufferQueue = new LinkedList<>();

    //bluetooth
    //private static BluetoothService btService;
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
            stopSensor();
            Log.d(TAG, "setFreq: restarting");
            startSensor();
            String res = "RES Frequency set to " + freq + "hz";
            //btService.write(res.getBytes(StandardCharsets.UTF_8));
        }
    };

    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS

    private static final String ACTION_START = "com.scis.meraki.sensordatacollector.action.STARTBT";
    private static final String ACTION_STOP = "com.scis.meraki.sensordatacollector.action.STOPBT";


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

//        if(btService == null){
//            btService = new BluetoothService(context,btHandler);
//            btService.start();
//        } else {
//            btService.start();
//        }

    }
    private void handleActionStop() {
        stopSensor();
        //btService.stop();
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

    private void stopSensor(){
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.unregisterListener(this,accelerometerSensor);
        mSensorManager = null;
        isGettingData = false;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if(!isGettingData){
            stopSensor();
            Log.e("sensor change", "onSensorChanged: ");
            return;
        }
        currTime = System.currentTimeMillis();

        bufferQueue.add(sensorEvent.values[0]);
        bufferQueue.add(sensorEvent.values[1]);
        bufferQueue.add(sensorEvent.values[2]);

        tick++;

        if(tick == 959){
            arrayAppender(data, bufferQueue);
        }

        doInference();

        //dataBuffer = sensorEvent.values[0] + " " + sensorEvent.values[1] + " " + sensorEvent.values[2] + " " + currTime + "\n";

//        Log.d(TAG, "onSensorChanged: " + dataBuffer);
        //byte[] out = dataBuffer.getBytes(StandardCharsets.UTF_8);
        //btService.write(out);

    }

    public float doInference() {
        float[] input = data;

        float[][] output = new float[1][1];

        try {
            tflite.run(input, output);
        } catch (Exception e){
            e.printStackTrace();
        }

        float inferredValue = output[0][0];

        return inferredValue;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void arrayAppender(float[] data, Queue<Float> bufferQueue){

        for(int i = 0; i < data.length; i++){
            float f = bufferQueue.remove();
            data[i] = f;
        }

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



}
