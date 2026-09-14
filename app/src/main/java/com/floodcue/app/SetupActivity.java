package com.floodcue.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class SetupActivity extends AppCompatActivity {

    private Button completeSetupButton;

    private SharedPreferences preferences;

    private static final String PREFS_NAME = "FloodCuePrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_setup);

        // Initialize SharedPreferences
        preferences = getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
        );

        // Connect button
        completeSetupButton = findViewById(R.id.buttonCompleteSetup);

        // Complete Setup
        completeSetupButton.setOnClickListener(v -> completeSetup());
    }

    private void completeSetup() {

        // Save setup completion status
        preferences.edit()
                .putBoolean("setup_completed", true)
                .apply();

        // Go to Home
        Intent intent = new Intent(
                SetupActivity.this,
                HomeActivity.class
        );

        startActivity(intent);
        finish();
    }
}