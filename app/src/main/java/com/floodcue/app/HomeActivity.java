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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
        android.widget.Button btnViewSavedDetails =
                findViewById(R.id.btnViewSavedDetails);

        android.widget.ScrollView savedDetailsScroll =
                findViewById(R.id.savedDetailsScroll);

        android.widget.TextView txtSavedDetails =
                findViewById(R.id.txtSavedDetails);

        btnViewSavedDetails.setOnClickListener(v -> {

            com.google.firebase.auth.FirebaseUser user =
                    com.google.firebase.auth.FirebaseAuth
                            .getInstance().getCurrentUser();

            if (user == null) {
                txtSavedDetails.setText("Please login first.");
                savedDetailsScroll.setVisibility(android.view.View.VISIBLE);
                return;
            }

            UserProfile profile =
                    ProfileStorage.getProfile(this, user.getUid());

            if (profile == null) {
                txtSavedDetails.setText(
                        "No local profile found on this device."
                );
            } else {
                String details =
                        "LOCAL SAVED PROFILE\n\n" +
                                "Name: " + profile.full_name +
                                "\nBlood Group: " + profile.blood_group +
                                "\nAllergies: " + profile.allergies +
                                "\nOther Allergies: " + profile.other_allergies +
                                "\nMedications: " + profile.medications +
                                "\nMedical Notes: " + profile.medical_notes +
                                "\n\nPRIMARY CONTACT\n" +
                                "Name: " + profile.primary_contact_name +
                                "\nRelationship: " + profile.primary_contact_relationship +
                                "\nPhone: " + profile.primary_contact_phone +
                                "\nAlternate Phone: " + profile.primary_contact_alt_phone +
                                "\n\nSECOND CONTACT\n" +
                                "Name: " + profile.second_contact_name +
                                "\nRelationship: " + profile.second_contact_relationship +
                                "\nPhone: " + profile.second_contact_phone +
                                "\nAlternate Phone: " + profile.second_contact_alt_phone +
                                "\n\nTHIRD CONTACT\n" +
                                "Name: " + profile.third_contact_name +
                                "\nRelationship: " + profile.third_contact_relationship +
                                "\nPhone: " + profile.third_contact_phone +
                                "\nAlternate Phone: " + profile.third_contact_alt_phone +
                                "\n\nAssistance: " + profile.special_assistance +
                                "\nCountry: " + profile.country +
                                "\nState: " + profile.state +
                                "\nDistrict: " + profile.district +
                                "\nApp Language: " + profile.app_language +
                                "\nSurvival Language: " + profile.survival_language +
                                "\nSetup Completed: " + profile.setup_completed;

                txtSavedDetails.setText(details);
            }

            savedDetailsScroll.setVisibility(android.view.View.VISIBLE);
        });
        logoutButton = findViewById(R.id.buttonLogout);
        Button offlineMapsButton = findViewById(R.id.buttonOfflineMaps);
        offlineMapsButton.setOnClickListener(v -> openOfflineMaps());

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

        if (savedInstanceState == null) {
            maybePromptForOfflineMap();
        }
    }

    private void maybePromptForOfflineMap() {
        if (OfflineMapCatalog.hasAnyDownloaded(this)) {
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.offline_maps_title)
                .setMessage(R.string.offline_maps_download_prompt)
                .setNegativeButton(R.string.offline_maps_cancel, null)
                .setPositiveButton(
                        R.string.offline_maps_go,
                        (dialog, which) -> openOfflineMaps()
                )
                .show();
    }

    private void openOfflineMaps() {
        startActivity(new Intent(this, OfflineMapsActivity.class));
    }

    private void logoutUser() {

        // 1. Sign out from Firebase
        firebaseAuth.signOut();

        // 2. Clear Credential Manager state
        ClearCredentialStateRequest clearRequest =
                new ClearCredentialStateRequest();

        credentialManager.clearCredentialStateAsync(
                clearRequest,
                new CancellationSignal(),
                credentialExecutor,
                new CredentialManagerCallback<Void,
                        ClearCredentialException>() {

                    @Override
                    public void onResult(@NonNull Void result) {
                        runOnUiThread(() -> finishLogout());
                    }

                    @Override
                    public void onError(
                            @NonNull ClearCredentialException e) {

                        // Continue logout even if credential clearing fails
                        runOnUiThread(() -> finishLogout());
                    }
                }
        );
    }
    private void finishLogout() {

        // Clear FloodCue app preferences
        preferences.edit()
                .clear()
                .commit();

        // Delete local app databases
        for (String databaseName : databaseList()) {
            deleteDatabase(databaseName);
        }

        // Clear app-private files and cache
        deleteDirectoryContents(getFilesDir());
        deleteDirectoryContents(getCacheDir());
        deleteDirectoryContents(getNoBackupFilesDir());
        deleteDirectoryContents(getCodeCacheDir());

        // Clear app-specific external files
        java.io.File[] externalDirs = getExternalFilesDirs(null);

        if (externalDirs != null) {
            for (java.io.File dir : externalDirs) {
                if (dir != null) {
                    deleteDirectoryContents(dir);
                }
            }
        }

        // Go back to LanguageActivity and clear previous screens
        Intent intent = new Intent(
                HomeActivity.this,
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