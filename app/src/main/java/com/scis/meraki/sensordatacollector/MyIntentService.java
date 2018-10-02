package com.scis.meraki.sensordatacollector;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class MyIntentService extends IntentService implements SensorEventListener{

     // TODO: add senson manager variables
    private SensorManager mSensorManager;
    private Sensor accelerometerSensor;

    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_RECORD = "com.scis.meraki.sensordatacollector.action.RECORD";
    public static final String ACTION_END = "com.scis.meraki.sensordatacollector.action.END";

    private static final String EXTRA_ACTIVITY = "com.scis.meraki.sensordatacollector.extra.ACTIVITY";


    public MyIntentService() {
        super("MyIntentService");


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
        if(mSensorManager == null){
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            accelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            mSensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    private void handleActionEnd () {
        if(mSensorManager != null){
            mSensorManager.unregisterListener(this);
        }
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.d("sensorEvent", "" + sensorEvent.values[0] + " " + sensorEvent.values[1] + " " + sensorEvent.values[2]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
