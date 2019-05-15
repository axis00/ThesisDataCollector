package com.scis.meraki.sensordatacollector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.channels.FileChannel;
import java.util.Scanner;


public class MainActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startBTService = findViewById(R.id.start_bt_button);
        Button stopBTService = findViewById(R.id.stop_bt_button);


        startBTService.setOnClickListener(startListener);
        stopBTService.setOnClickListener(stopListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private View.OnClickListener startListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH)
                    == PackageManager.PERMISSION_GRANTED){
                DataCollectionService.startActionStart(MainActivity.this);
            }

        }
    };

    private View.OnClickListener stopListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            DataCollectionService.startActionStop(MainActivity.this);
        }
    };



}
