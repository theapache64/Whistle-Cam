package com.shifz.whistlecam;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.shifz.whistlecam.CameraActivity;
import com.shifz.whistlecam.R;

public class SplashActivity extends AppCompatActivity {

    private static final long DEFAULT_SPASH_DURATION = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, CameraActivity.class));
                finish();
            }
        }, DEFAULT_SPASH_DURATION);
    }
}
