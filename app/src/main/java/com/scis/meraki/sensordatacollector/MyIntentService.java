package com.scis.meraki.sensordatacollector;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class MyIntentService extends IntentService implements SensorEventListener{

     // TODO: add senson manager variables
    private SensorManager mSensorManager = null;
    private Sensor accelerometerSensor;

    private File data;
    private long timeInterval, prevTime, currTime;

    //flags
    private static boolean isGettingData = false;

    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_RECORD = "com.scis.meraki.sensordatacollector.action.RECORD";
    public static final String ACTION_END = "com.scis.meraki.sensordatacollector.action.END";

    public static final String ACTION_STARTWRITE = "com.scis.meraki.sensordatacollector.action.STARTWRITE";
    public static final String ACTION_ENDWRITE = "com.scis.meraki.sensordatacollector.action.ENDWRITE";

    private static final String EXTRA_ACTIVITY = "com.scis.meraki.sensordatacollector.extra.ACTIVITY";


    public MyIntentService() {
        super("MyIntentService");

        prevTime = currTime = System.currentTimeMillis();
        data = new File(getFilesDir(), "data-" + currTime + ".json");
        timeInterval = 10000;


    }

    public static void startActionRecord(Context context, String activity) {
        Intent intent = new Intent(context, MyIntentService.class);
        intent.setAction(ACTION_RECORD);
        intent.putExtra(EXTRA_ACTIVITY, activity);
        context.startService(intent);
    }

    public static void startActionEnd(Context context) {
        Intent intent = new Intent(context, MyIntentService.class);
        intent.setAction(ACTION_END);
        context.startService(intent);
    }

    public static void startActionStartWrite(Context context) {
        Intent intent = new Intent(context, MyIntentService.class);
        intent.setAction(ACTION_RECORD);
        context.startService(intent);
    }

    public static void setActionStartEnd(Context context) {
        Intent intent = new Intent(context, MyIntentService.class);
        intent.setAction(ACTION_RECORD);
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
        }

    }

    private void handleActionRecord (String activity) {
        if(mSensorManager == null && !isGettingData){
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            accelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);
            prevTime = currTime = System.currentTimeMillis();
            isGettingData = true;
        }
    }

    private void handleActionEnd () {
       isGettingData = false;
    }

    private void handleActionStartWrite () {

    }

    private void handleActionEndWrite () {

    }

    private void stopSensor(){
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.unregisterListener(this,accelerometerSensor);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(!isGettingData){
            stopSensor();
            Log.e("sensor change", "onSensorChanged: ");
            return;
        }

        currTime = System.currentTimeMillis();
        Log.d("sensorEvent", "" + sensorEvent.values[0] + " " + sensorEvent.values[1] + " " + sensorEvent.values[2]);
    }

    public void writeData(float x, float y, float z, long time) {
        String data = "{" +
                "x : " + x + " , " +
                "y : " + y + " , " +
                "z : " + z + " , " +
                "time : " + time +
                "}";
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


}
