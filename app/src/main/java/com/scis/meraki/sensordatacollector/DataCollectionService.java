package com.scis.meraki.sensordatacollector;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class DataCollectionService extends IntentService implements SensorEventListener{

    // Debugging
    private static final String TAG = "BluetoothService";

    //sensor related variables
    private SensorManager mSensorManager = null;
    private Sensor accelerometerSensor;
    private static int samplingRate = 1000000;

    private static long timeInterval, prevTime, currTime;
    private static String dataBuffer;

    //flags
    private static boolean isGettingData = false;

    private static Context context;

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
            stopSensor();
            Log.d(TAG, "setFreq: restarting");
            startSensor();
            String res = "RES Frequency set to " + freq + "hz";
            btService.write(res.getBytes(StandardCharsets.UTF_8));
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
        dataBuffer = "";

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
        stopSensor();
        btService.stop();
    }

    private void startSensor(){
        if(mSensorManager == null && !isGettingData){

            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            accelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(this, accelerometerSensor, samplingRate);

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

        dataBuffer = sensorEvent.values[0] + " " + sensorEvent.values[1] + " " + sensorEvent.values[2] + " " + currTime + "\n";

//        Log.d(TAG, "onSensorChanged: " + dataBuffer);
        byte[] out = dataBuffer.getBytes(StandardCharsets.UTF_8);
        btService.write(out);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }



}
