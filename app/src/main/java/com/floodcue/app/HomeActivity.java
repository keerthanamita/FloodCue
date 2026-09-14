package com.floodcue.app;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    private Button logoutButton;
    private GoogleSignInClient googleSignInClient;
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
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN
        )
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
        // Initialize SharedPreferences
        preferences = getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
        );

        // Logout button
        logoutButton.setOnClickListener(v -> logoutUser());
    }

    private void logoutUser() {

        googleSignInClient.signOut().addOnCompleteListener(task -> {

            firebaseAuth.signOut();

            preferences.edit()
                    .putBoolean("is_logged_in", false)
                    .apply();

            Intent intent = new Intent(
                    HomeActivity.this,
                    LoginActivity.class
            );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
        });
    }
}