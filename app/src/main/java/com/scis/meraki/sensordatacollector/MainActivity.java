package com.scis.meraki.sensordatacollector;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    boolean isWriting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startWriteButton  = (Button)findViewById(R.id.start_write_button);
        Button stopWriteButton = (Button)findViewById(R.id.end_write_button);

        Button startButton = (Button)findViewById(R.id.record_button);
        Button endButton = (Button) findViewById(R.id.stop_button);

        final TextView action = (TextView) findViewById(R.id.action_text);
        final TextView pos = (TextView) findViewById(R.id.position_text);

        startWriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isWriting){
                    isWriting = true;
                    DataCollectionService.startActionStartWrite(MainActivity.this);
                }
            }
        });

        stopWriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isWriting){
                    isWriting = false;
                    DataCollectionService.startActionEndWrite(MainActivity.this);
                }
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isWriting) {
                    DataCollectionService.startActionRecord(MainActivity.this,
                            action.getText().toString(),pos.getText().toString());
                }
            }
        });


        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DataCollectionService.startActionEnd(MainActivity.this);
            }
        });
    }
}
