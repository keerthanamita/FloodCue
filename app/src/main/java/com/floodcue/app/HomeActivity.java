package com.floodcue.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HomeActivity extends FloodCueActivity {

    /**
     * Held statically so the siren keeps sounding across rotations and while
     * other FloodCue screens are open; it is released when the app is left.
     */
    private static MediaPlayer sirenPlayer;

    private Button sirenButton;
    private Button sosButton;

    private static final int SOS_PERMISSION_REQUEST = 5101;
    /** Guards against an accidental double tap sending two emergency messages. */
    private static final long SOS_SEND_COOLDOWN_MS = 15_000L;
    /** How long the message waits for a first GPS fix before sending without one. */
    private static final long SOS_LOCATION_WAIT_MS = 6_000L;

    private final Handler sosHandler = new Handler(Looper.getMainLooper());
    private LocationListener sosLocationListener;
    private Runnable sosLocationTimeout;
    private List<String> pendingSosRecipients;
    private boolean waitingForSosLocation;
    private long lastSosSendAt;

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
                txtSavedDetails.setText(R.string.home_sign_in_saved_details);
                savedDetailsScroll.setVisibility(android.view.View.VISIBLE);
                return;
            }

            UserProfile profile =
                    ProfileStorage.getProfile(this, user.getUid());

            if (profile == null) {
                txtSavedDetails.setText(
                        R.string.home_no_local_profile
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
                                "\nSurvival Language: " + profile.survival_language;

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

        Button checklistButton = findViewById(R.id.buttonChecklist);
        Button tipsButton = findViewById(R.id.buttonTips);
        Button weatherButton = findViewById(R.id.buttonWeather);
        weatherButton.setOnClickListener(v -> openWeather());
        Button floodAlertButton = findViewById(R.id.buttonFloodAlert);
        floodAlertButton.setOnClickListener(v -> openFloodAlerts());
        Button offlineCommunicationButton = findViewById(R.id.buttonOfflineCommunication);
        offlineCommunicationButton.setOnClickListener(v -> openOfflineCommunication());

        Button emergencyGuideButton = findViewById(R.id.buttonEmergencyGuide);
        emergencyGuideButton.setOnClickListener(v -> openEmergencyGuide());
        Button sosEmergencyButton = findViewById(R.id.buttonSosEmergency);
        sosEmergencyButton.setOnClickListener(v -> openSosEmergency());
        checklistButton.setOnClickListener(v -> openChecklist());
        tipsButton.setOnClickListener(v -> openTips());

        sosButton = findViewById(R.id.buttonSos);
        sosButton.setOnClickListener(v -> toggleQuickSos());

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

    private void openSosEmergency() {
        startActivity(new Intent(this, SosEmergencyActivity.class));
    }

    private void openWeather() {
        startActivity(new Intent(this, WeatherActivity.class));
    }

    private void openFloodAlerts() {
        startActivity(new Intent(this, FloodAlertActivity.class));
    }

    private void openEmergencyGuide() {
        startActivity(new Intent(this, EmergencyGuideActivity.class));
    }

    private void openOfflineCommunication() {
        startActivity(new Intent(this, OfflineCommunicationActivity.class));
    }

    private void openChecklist() {
        startActivity(new Intent(this, ChecklistActivity.class));
    }

    private void openTips() {
        startActivity(new Intent(this, TipsActivity.class));
    }

    /**
     * The red home SOS button. One tap starts the alarm and sends the
     * emergency message on its own; the next tap stops the alarm. Nothing else
     * is asked of the user, because an SMS that already left this device
     * cannot be recalled.
     */
    private void toggleQuickSos() {
        boolean alarmSounding = sirenPlayer != null && sirenPlayer.isPlaying();
        if (alarmSounding) {
            toggleSiren();
            updateSirenButton();
            Toast.makeText(getApplicationContext(),
                    R.string.home_sos_siren_stopped, Toast.LENGTH_SHORT).show();
            return;
        }

        // Sound first: it needs no permission and is the fastest help.
        toggleSiren();
        updateSirenButton();

        if (!SosQueue.hasSmsPermission(this) || !hasLocationPermission()) {
            requestSosPermissions();
            return;
        }
        startQuickSos();
    }

    private void requestSosPermissions() {
        List<String> missing = new ArrayList<>();
        if (!SosQueue.hasSmsPermission(this)) {
            missing.add(Manifest.permission.SEND_SMS);
        }
        if (!hasLocationPermission()) {
            missing.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (missing.isEmpty()) {
            startQuickSos();
            return;
        }
        ActivityCompat.requestPermissions(this,
                missing.toArray(new String[0]), SOS_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != SOS_PERMISSION_REQUEST) {
            return;
        }
        if (!SosQueue.hasSmsPermission(this)) {
            // The alarm keeps sounding; without SMS permission nothing can be sent.
            Toast.makeText(getApplicationContext(),
                    R.string.home_sos_sms_permission, Toast.LENGTH_LONG).show();
            return;
        }
        if (!hasLocationPermission()) {
            Toast.makeText(getApplicationContext(),
                    R.string.home_sos_location_permission, Toast.LENGTH_SHORT).show();
        }
        startQuickSos();
    }

    private void startQuickSos() {
        long now = System.currentTimeMillis();
        if (now - lastSosSendAt < SOS_SEND_COOLDOWN_MS) {
            return;
        }
        Set<String> recipients = SosQuickSend.recipients(this);
        if (recipients.isEmpty()) {
            Toast.makeText(getApplicationContext(),
                    R.string.home_sos_no_contacts, Toast.LENGTH_LONG).show();
            return;
        }
        lastSosSendAt = now;
        pendingSosRecipients = new ArrayList<>(recipients);

        Location known = SosQuickSend.bestLastKnown(this, hasLocationPermission());
        if (known != null) {
            dispatchSos(known);
            return;
        }
        waitForSosLocation();
    }

    /** Gives the GPS a short chance to produce a fix before sending anyway. */
    private void waitForSosLocation() {
        if (!hasLocationPermission()) {
            dispatchSos(null);
            return;
        }
        String provider = SosQuickSend.enabledProvider(this);
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (provider == null || manager == null) {
            dispatchSos(null);
            return;
        }

        waitingForSosLocation = true;
        sosLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (!waitingForSosLocation) {
                    return;
                }
                waitingForSosLocation = false;
                stopSosLocationRequest();
                dispatchSos(location);
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                if (waitingForSosLocation) {
                    waitingForSosLocation = false;
                    stopSosLocationRequest();
                    dispatchSos(null);
                }
            }
        };
        try {
            manager.requestLocationUpdates(provider, 0L, 0f,
                    sosLocationListener, Looper.getMainLooper());
        } catch (SecurityException | IllegalArgumentException failed) {
            waitingForSosLocation = false;
            sosLocationListener = null;
            dispatchSos(null);
            return;
        }

        sosLocationTimeout = () -> {
            if (!waitingForSosLocation) {
                return;
            }
            waitingForSosLocation = false;
            stopSosLocationRequest();
            dispatchSos(null);
        };
        sosHandler.postDelayed(sosLocationTimeout, SOS_LOCATION_WAIT_MS);
    }

    private void stopSosLocationRequest() {
        sosHandler.removeCallbacksAndMessages(null);
        sosLocationTimeout = null;
        if (sosLocationListener == null) {
            return;
        }
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager != null) {
            try {
                manager.removeUpdates(sosLocationListener);
            } catch (SecurityException ignored) {
            }
        }
        sosLocationListener = null;
    }

    /** Hands the message to the queue, which sends now and retries when a network returns. */
    private void dispatchSos(Location location) {
        List<String> recipients = pendingSosRecipients;
        pendingSosRecipients = null;
        if (recipients == null || recipients.isEmpty()) {
            return;
        }
        if (location == null) {
            Toast.makeText(getApplicationContext(),
                    R.string.home_sos_location_unavailable, Toast.LENGTH_SHORT).show();
        }
        SosQueue.enqueue(getApplicationContext(), recipients,
                SosQuickSend.buildMessage(location));
        Toast.makeText(getApplicationContext(),
                getString(R.string.home_sos_started, recipients.size()),
                Toast.LENGTH_LONG).show();
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void updateSirenButton() {
        boolean playing = sirenPlayer != null && sirenPlayer.isPlaying();
        sirenButton.setText(playing ? R.string.home_siren_stop : R.string.home_siren);
        if (sosButton != null) {
            sosButton.setText(playing ? R.string.home_sos_stop : R.string.home_sos);
        }
    }

    @Override
    protected void onDestroy() {
        // Leaving the screen mid-wait must not silently drop the SOS message.
        if (waitingForSosLocation) {
            waitingForSosLocation = false;
            stopSosLocationRequest();
            dispatchSos(null);
        } else {
            stopSosLocationRequest();
        }
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
