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

        Log.w("buns", "shit giggles");

        setContentView(R.layout.activity_authentication);

        AWSMobileClient.getInstance().initialize(getApplicationContext(), new Callback<UserStateDetails>() {

            @Override
            public void onResult(UserStateDetails userStateDetails) {
                Log.i(TAG, userStateDetails.getUserState().toString());
                switch (userStateDetails.getUserState()){
                    case SIGNED_IN:
                        Intent i = new Intent(AuthenticationActivity.this, MainActivity.class);
                        startActivity(i);
                        Log.w("buns", "shit www");
                        break;
                    case SIGNED_OUT:
                        Log.w("buns", "shit two point 2");
                        showSignIn();


                        break;
                    default:
                        Log.w("buns", "shit ksksk");
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


        Log.w("buns", "shit two");

    }

    private void showSignIn() {
        try {
            AWSMobileClient.getInstance().showSignIn(this,
                    //.logo(R.mipmap.newlogo_round)
                    SignInUIOptions.builder().nextActivity(MainActivity.class).canCancel(true).build());


        } catch (Exception e) {
            Log.e(TAG, e.toString());

            Intent i = new Intent(AuthenticationActivity.this, LaunchScreen.class);
            startActivity(i);

        }
    }






}

