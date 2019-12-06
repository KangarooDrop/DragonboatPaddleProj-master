package com.dfrobot.angelo.blunobasicdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.SignInUIOptions;
import com.amazonaws.mobile.client.UserStateDetails;

public class AuthenticationActivity extends AppCompatActivity {

    private final String TAG = AuthenticationActivity.class.getSimpleName();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_authentication);

        AWSMobileClient.getInstance().initialize(getApplicationContext(), new Callback<UserStateDetails>() {

            @Override
            public void onResult(UserStateDetails userStateDetails) {
                Log.i(TAG, userStateDetails.getUserState().toString());
                switch (userStateDetails.getUserState()){
                    case SIGNED_IN:
                        Intent i = new Intent(AuthenticationActivity.this, MainActivity.class);
                        startActivity(i);
                        break;
                    case SIGNED_OUT:
                        showSignIn();
                        break;
                    default:
                        AWSMobileClient.getInstance().signOut();
                        showSignIn();
                        break;
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, e.toString());

                Intent i = new Intent(AuthenticationActivity.this, LaunchScreen.class);
                startActivity(i);
            }
        });

    }

    private void showSignIn() {
        try {
            AWSMobileClient.getInstance().showSignIn(this,
                    //.logo(R.mipmap.newlogo_round)
                    SignInUIOptions.builder().nextActivity(MainActivity.class).logo(R.mipmap.icon).canCancel(true).build());


        } catch (Exception e) {
            Log.e(TAG, e.toString());

            Intent i = new Intent(AuthenticationActivity.this, LaunchScreen.class);
            startActivity(i);

        }
    }






}

