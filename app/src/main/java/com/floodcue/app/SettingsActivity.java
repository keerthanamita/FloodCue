package com.floodcue.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;

import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SettingsActivity extends FloodCueActivity {

    private static final String PREFS_NAME = "FloodCuePrefs";

    private FirebaseAuth firebaseAuth;
    private SharedPreferences preferences;
    private CredentialManager credentialManager;
    private Executor credentialExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        TextView buttonBackToHome = findViewById(R.id.buttonBackToHome);
        Button changeLanguageButton = findViewById(R.id.buttonChangeLanguage);
        Button changeUserDetailsButton = findViewById(R.id.buttonChangeUserDetails);
        Button aboutFloodcueButton = findViewById(R.id.buttonAboutFloodcue);
        Button aboutBridgefyButton = findViewById(R.id.buttonAboutBridgefy);
        Button logoutButton = findViewById(R.id.buttonLogout);

        firebaseAuth = FirebaseAuth.getInstance();
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        credentialManager = CredentialManager.create(this);
        credentialExecutor = Executors.newSingleThreadExecutor();

        buttonBackToHome.setOnClickListener(v -> finish());
        changeUserDetailsButton.setOnClickListener(v -> openUserProfile());
        changeLanguageButton.setOnClickListener(v -> openLanguagePicker());
        aboutFloodcueButton.setOnClickListener(v ->
                startActivity(new Intent(this, AboutFloodCueActivity.class)));
        aboutBridgefyButton.setOnClickListener(v -> openAboutBridgefy());
        logoutButton.setOnClickListener(v -> logoutUser());
    }

    private void openUserProfile() {
        startActivity(new Intent(this, ProfileDetailsActivity.class));
    }

    private void openLanguagePicker() {
        Intent intent = new Intent(this, LanguageActivity.class);
        intent.putExtra("LANGUAGE_EDIT_MODE", true);
        startActivity(intent);
    }

    private void openAboutBridgefy() {
        startActivity(new Intent(this, AboutBridgefyActivity.class));
    }

    private void logoutUser() {
        firebaseAuth.signOut();

        ClearCredentialStateRequest clearRequest =
                new ClearCredentialStateRequest();

        credentialManager.clearCredentialStateAsync(
                clearRequest,
                new CancellationSignal(),
                credentialExecutor,
                new CredentialManagerCallback<Void, ClearCredentialException>() {

                    @Override
                    public void onResult(@NonNull Void result) {
                        runOnUiThread(() -> finishLogout());
                    }

                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        runOnUiThread(() -> finishLogout());
                    }
                }
        );
    }

    private void finishLogout() {
        preferences.edit()
                .clear()
                .commit();

        for (String databaseName : databaseList()) {
            deleteDatabase(databaseName);
        }

        deleteDirectoryContents(getFilesDir());
        deleteDirectoryContents(getCacheDir());
        deleteDirectoryContents(getNoBackupFilesDir());
        deleteDirectoryContents(getCodeCacheDir());

        java.io.File[] externalDirs = getExternalFilesDirs(null);

        if (externalDirs != null) {
            for (java.io.File dir : externalDirs) {
                if (dir != null) {
                    deleteDirectoryContents(dir);
                }
            }
        }

        Intent intent = new Intent(
                SettingsActivity.this,
                LanguageActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void deleteDirectoryContents(java.io.File directory) {
        if (directory == null || !directory.exists()) {
            return;
        }

        java.io.File[] files = directory.listFiles();

        if (files == null) {
            return;
        }

        for (java.io.File file : files) {
            deleteRecursively(file);
        }
    }

    private void deleteRecursively(java.io.File file) {
        if (file == null || !file.exists()) {
            return;
        }

        if (file.isFile() && OfflineMapCatalog.shouldPreserveMapFile(file)) {
            return;
        }

        if (file.isDirectory()) {
            java.io.File[] children = file.listFiles();

            if (children != null) {
                for (java.io.File child : children) {
                    deleteRecursively(child);
                }
            }
        }

        file.delete();
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
