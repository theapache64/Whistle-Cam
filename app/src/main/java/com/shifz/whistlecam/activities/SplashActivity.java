package com.shifz.whistlecam.activities;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.shifz.whistlecam.R;
import com.shifz.whistlecam.utils.PrefHelper;

public class SplashActivity extends AppCompatActivity {

    private static final long DEFAULT_SPASH_DURATION = 1000;
    private static final String KEY_IS_ALREADY_STARTED = "is_already_started";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        final PrefHelper prefHelper = PrefHelper.getInstance(this);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                final boolean isAlreadyStarted = prefHelper.getBooleanPref(KEY_IS_ALREADY_STARTED, false);
                if (isAlreadyStarted) {
                    startActivity(new Intent(SplashActivity.this, CameraActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this, SettingsActivity.class));
                }
                finish();
            }
        }, DEFAULT_SPASH_DURATION);
    }
}
