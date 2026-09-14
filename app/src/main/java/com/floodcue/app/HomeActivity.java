package com.floodcue.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    private Button logoutButton;

    private FirebaseAuth firebaseAuth;
    private SharedPreferences preferences;

    private static final String PREFS_NAME = "FloodCuePrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        // Connect Logout button
        logoutButton = findViewById(R.id.buttonLogout);

        // Initialize Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize SharedPreferences
        preferences = getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
        );

        // Logout button
        logoutButton.setOnClickListener(v -> logoutUser());
    }

    private void logoutUser() {

        // Sign out from Firebase
        firebaseAuth.signOut();

        // Clear local login state
        preferences.edit()
                .putBoolean("is_logged_in", false)
                .apply();

        // Go to Login screen
        Intent intent = new Intent(
                HomeActivity.this,
                LoginActivity.class
        );

        // Prevent going back to Home
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}