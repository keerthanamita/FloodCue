package com.floodcue.app;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class HomeActivity extends AppCompatActivity {

    /**
     * Held statically so the siren keeps sounding across rotations and while
     * other FloodCue screens are open; it is released when the app is left.
     */
    private static MediaPlayer sirenPlayer;

    private Button sirenButton;

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
        Button offlineMapsButton = findViewById(R.id.buttonOfflineMaps);
        Button emergencyContactsButton = findViewById(R.id.buttonEmergencyContacts);
        Button sosEmergencyMessageButton = findViewById(R.id.buttonSosEmergencyMessage);
        Button settingsButton = findViewById(R.id.buttonSettings);
        offlineMapsButton.setOnClickListener(v -> openOfflineMaps());
        emergencyContactsButton.setOnClickListener(v -> openEmergencyContacts());
        sosEmergencyMessageButton.setOnClickListener(v -> openSosEmergencyMessage());
        settingsButton.setOnClickListener(v -> openSettings());

        sirenButton = findViewById(R.id.buttonSiren);
        sirenButton.setOnClickListener(v -> toggleSiren());
        updateSirenButton();

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

    private void openEmergencyContacts() {
        startActivity(new Intent(this, EmergencyContactsActivity.class));
    }

    private void openSosEmergencyMessage() {
        startActivity(new Intent(this, SosActivity.class));
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    /** Taps once to sound the siren, again to stop it. */
    private void toggleSiren() {
        if (sirenPlayer != null && sirenPlayer.isPlaying()) {
            sirenPlayer.pause();
            updateSirenButton();
            return;
        }

        if (sirenPlayer == null) {
            try {
                sirenPlayer = MediaPlayer.create(getApplicationContext(), R.raw.siren);
            } catch (RuntimeException failedToCreate) {
                sirenPlayer = null;
            }
            if (sirenPlayer == null) {
                Toast.makeText(this, R.string.siren_unavailable, Toast.LENGTH_LONG).show();
                return;
            }
            sirenPlayer.setLooping(true);
            // Alarm stream: plays at the alarm volume even if media is muted.
            sirenPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
        }

        try {
            sirenPlayer.start();
        } catch (IllegalStateException failedToStart) {
            Toast.makeText(this, R.string.siren_unavailable, Toast.LENGTH_LONG).show();
            return;
        }
        updateSirenButton();
    }

    private void updateSirenButton() {
        boolean playing = sirenPlayer != null && sirenPlayer.isPlaying();
        sirenButton.setText(playing ? R.string.home_siren_stop : R.string.home_siren);
    }

    @Override
    protected void onDestroy() {
        // Leave the app entirely: release so no unstoppable siren keeps blaring.
        if (isFinishing() && sirenPlayer != null) {
            try {
                sirenPlayer.stop();
            } catch (IllegalStateException ignored) {
            }
            sirenPlayer.release();
            sirenPlayer = null;
        }
        super.onDestroy();
    }
}