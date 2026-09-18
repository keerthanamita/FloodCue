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

        preferences = getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
        );

        completeSetupButton =
                findViewById(R.id.buttonCompleteSetup);

        completeSetupButton.setOnClickListener(
                v -> completeSetup()
        );
    }

    private void completeSetup() {

        preferences.edit()
                .putBoolean("setup_completed", true)
                .apply();

        Intent intent = new Intent(
                SetupActivity.this,
                HomeActivity.class
        );

        startActivity(intent);
        finish();
    }
}