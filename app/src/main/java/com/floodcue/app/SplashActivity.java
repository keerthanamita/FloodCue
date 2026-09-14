package com.floodcue.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private TextView loadingStatus;

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private android.content.SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        loadingStatus = findViewById(R.id.loadingStatus);

        preferences =
                getSharedPreferences("FloodCuePrefs", MODE_PRIVATE);

        initializeApp();
    }

    private void initializeApp() {

        // Temporary initialization
        // Future real loading functions will be added here.

        updateLoadingStatus("Preparing FloodCue...");

        mainHandler.postDelayed(() -> {

            updateLoadingStatus("Loading local resources...");

            mainHandler.postDelayed(() -> {

                updateLoadingStatus("Preparing emergency data...");

                mainHandler.postDelayed(() -> {

                    updateLoadingStatus("FloodCue ready");

                    mainHandler.postDelayed(
                            this::openNextScreen,
                            500
                    );

                }, 500);

            }, 500);

        }, 500);
    }

    private void updateLoadingStatus(String message) {
        loadingStatus.setText(message);
    }

    private void openNextScreen() {

        boolean setupCompleted =
                preferences.getBoolean("setup_completed", false);

        boolean isLoggedIn =
                preferences.getBoolean("is_logged_in", false);

        Intent intent;

        if (!isLoggedIn) {

            // User is not logged in
            intent = new Intent(
                    SplashActivity.this,
                    LoginActivity.class
            );

        } else if (!setupCompleted) {

            // User is logged in but setup is not completed
            intent = new Intent(
                    SplashActivity.this,
                    SetupActivity.class
            );

        } else {

            // User is logged in and setup is completed
            intent = new Intent(
                    SplashActivity.this,
                    HomeActivity.class
            );
        }

        startActivity(intent);
        finish();
    }
}