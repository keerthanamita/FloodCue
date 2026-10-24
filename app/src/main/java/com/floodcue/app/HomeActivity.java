package com.floodcue.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class HomeActivity extends AppCompatActivity {

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
        Button settingsButton = findViewById(R.id.buttonSettings);
        offlineMapsButton.setOnClickListener(v -> openOfflineMaps());
        emergencyContactsButton.setOnClickListener(v -> openEmergencyContacts());
        settingsButton.setOnClickListener(v -> openSettings());

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

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }
}