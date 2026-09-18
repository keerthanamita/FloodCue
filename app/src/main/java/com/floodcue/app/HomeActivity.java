package com.floodcue.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;

import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity {

    private Button logoutButton;

    private FirebaseAuth firebaseAuth;
    private SharedPreferences preferences;

    private CredentialManager credentialManager;
    private Executor credentialExecutor;

    private static final String PREFS_NAME = "FloodCuePrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        logoutButton = findViewById(R.id.buttonLogout);

        firebaseAuth = FirebaseAuth.getInstance();

        preferences = getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
        );

        credentialManager = CredentialManager.create(this);
        credentialExecutor = Executors.newSingleThreadExecutor();

        logoutButton.setOnClickListener(
                v -> logoutUser()
        );
    }

    private void logoutUser() {

        // Sign out from Firebase first.
        firebaseAuth.signOut();

        // Clear Credential Manager state.
        ClearCredentialStateRequest clearRequest =
                new ClearCredentialStateRequest();

        credentialManager.clearCredentialStateAsync(
                clearRequest,
                new CancellationSignal(),
                credentialExecutor,
                new CredentialManagerCallback<Void,
                        ClearCredentialException>() {

                    @Override
                    public void onResult(
                            @NonNull Void result) {

                        runOnUiThread(
                                () -> finishLogout()
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull ClearCredentialException e) {

                        // Firebase is already signed out.
                        // Continue logout even if credential-state
                        // clearing fails.
                        runOnUiThread(
                                () -> finishLogout()
                        );
                    }
                }
        );
    }

    private void finishLogout() {

        preferences.edit()
                .putBoolean("is_logged_in", false)
                .apply();

        Intent intent = new Intent(
                HomeActivity.this,
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (credentialExecutor instanceof java.util.concurrent.ExecutorService) {
            ((java.util.concurrent.ExecutorService) credentialExecutor)
                    .shutdown();
        }
    }
}