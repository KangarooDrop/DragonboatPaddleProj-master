package com.dfrobot.angelo.blunobasicdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class LaunchScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch_screen);


        Button buttonSkip = findViewById(R.id.buttonSkip);					//initial the button for scanning the BLE device
        buttonSkip.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub


                Intent i = new Intent(LaunchScreen.this, MainActivity.class);
                startActivity(i);										//Alert Dialog for selecting the BLE device
            }
        });


        Button buttonLogin = findViewById(R.id.buttonLogin);					//initial the button for scanning the BLE device
        buttonLogin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                Intent i = new Intent(LaunchScreen.this, AuthenticationActivity.class);
                startActivity(i);						//Alert Dialog for selecting the BLE device
            }
        });

    }
}
