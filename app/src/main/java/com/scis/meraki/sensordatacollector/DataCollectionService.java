package com.scis.meraki.sensordatacollector;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class DataCollectionService extends IntentService implements SensorEventListener{

     // TODO: add sensor manager variables
    private SensorManager mSensorManager = null;
    private Sensor accelerometerSensor;

    private static File data;
    private FileOutputStream outStream;
    private BufferedWriter dataWriter;
    private long timeInterval, prevTime, currTime;
    private String dataBuffer;

    //flags
    private static boolean isGettingData = false;

    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_RECORD = "com.scis.meraki.sensordatacollector.action.RECORD";
    public static final String ACTION_END = "com.scis.meraki.sensordatacollector.action.END";

    public static final String ACTION_STARTWRITE = "com.scis.meraki.sensordatacollector.action.STARTWRITE";
    public static final String ACTION_ENDWRITE = "com.scis.meraki.sensordatacollector.action.ENDWRITE";

    private static final String EXTRA_ACTIVITY = "com.scis.meraki.sensordatacollector.extra.ACTIVITY";
    private static final String EXTRA_POSITION = "com.scis.meraki.sensordatacollector.extra.POSITION";


    public DataCollectionService() {
        super("DataCollectionService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        prevTime = currTime = System.currentTimeMillis();

        timeInterval = 10000;
        dataBuffer = "";

    }

    public static void startActionRecord(Context context, String activity, String pos) {
        Intent intent = new Intent(context, DataCollectionService.class);
        intent.setAction(ACTION_RECORD);
        intent.putExtra(EXTRA_ACTIVITY, activity);
        intent.putExtra(EXTRA_POSITION, pos);
        context.startService(intent);
    }

    public static void startActionEnd(Context context) {
        Intent intent = new Intent(context, DataCollectionService.class);
        intent.setAction(ACTION_END);
        context.startService(intent);
    }

    public static void startActionStartWrite(Context context) {
        Intent intent = new Intent(context, DataCollectionService.class);
        intent.setAction(ACTION_STARTWRITE);
        context.startService(intent);
    }

    public static void startActionEndWrite(Context context) {
        Intent intent = new Intent(context, DataCollectionService.class);
        intent.setAction(ACTION_ENDWRITE);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
           String action = intent.getAction();
           if (action.equals(ACTION_RECORD)) {
               handleActionRecord(intent.getStringExtra(EXTRA_ACTIVITY));
           }
           if (action.equals(ACTION_END)) {
               handleActionEnd();
           }
           if (action.equals(ACTION_STARTWRITE)){
               try {
                   handleActionStartWrite();
               } catch (FileNotFoundException e) {
                   e.printStackTrace();
               }
           }
           if (action.equals(ACTION_ENDWRITE)){
               handleActionEndWrite();
           }
        }

    }

    private void handleActionRecord (String activity) {
        if(mSensorManager == null && !isGettingData){
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            accelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);
            prevTime = currTime = System.currentTimeMillis();
            isGettingData = true;
            writeActivity(activity);
        }
    }

    private void handleActionEnd () {
        isGettingData = false;
        writeEndActivity();
    }

    private void handleActionStartWrite () throws FileNotFoundException{
        data = new File(getFilesDir(), "thesis-data-" + currTime + ".dat");

        this.outStream = new FileOutputStream(data);
        this.dataWriter = new BufferedWriter(new OutputStreamWriter(outStream));

        Log.d("startwrite","Started writing");

        checkFiles();
    }

    private void handleActionEndWrite () {
        try {
            this.outStream.close();
            this.dataWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopSensor(){
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.unregisterListener(this,accelerometerSensor);
    }

    private void writeActivity(String act){
        try {
            dataWriter.write(act);
            dataWriter.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeEndActivity(){
        try {
            if (!dataBuffer.equals("")) {
                dataWriter.write(dataBuffer);
            }
            dataWriter.write("---");
            dataWriter.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        checkFiles();
    }

    private void writeData(String data){
        try {
            dataWriter.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void checkFiles(){
        File path = getFilesDir();
        File[] files = path.listFiles();

        for(File f : files){
            Log.d("File check","File Name : " + f.getName());
        }

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(!isGettingData){
            stopSensor();
            Log.e("sensor change", "onSensorChanged: ");
            return;
        }
        currTime = System.currentTimeMillis();

        dataBuffer += sensorEvent.values[0] + " " + sensorEvent.values[1] + " " + sensorEvent.values[2] + currTime;

        if (currTime - prevTime >= timeInterval) {
            currTime = prevTime;
            writeData(dataBuffer);
            Log.d("Writing", dataBuffer);
            dataBuffer = "";
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


}
