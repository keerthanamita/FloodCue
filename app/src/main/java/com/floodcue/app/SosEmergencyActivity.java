package com.floodcue.app;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * One-tap emergency SOS.
 *
 * <ul>
 *  <li>Sends the emergency SMS to the personal emergency contacts saved during
 *      setup and to the numbers chosen on the SOS Emergency Message screen.</li>
 *  <li>Activates the app siren.</li>
 *  <li>Attaches the current GPS coordinate plus a Google Maps link.</li>
 * </ul>
 *
 * Reuses the existing location pattern from {@link SosActivity}, keeps
 * everything on the device, and never claims delivery it cannot confirm.
 */
public class SosEmergencyActivity extends AppCompatActivity
        implements LocationListener {

    private static final String PREFS_NAME = "FloodCuePrefs";
    private static final String LAST_LOCATION_KEY = "sos_emergency_last_location";
    private static final String LAST_LOCATION_TIME_KEY =
            "sos_emergency_last_location_time";
    private static final long LOCATION_TIMEOUT_MS = 15000L;

    private static final String EMERGENCY_SMS_BODY =
            SosQuickSend.EMERGENCY_SMS_BODY;

    private static final int LOCATION_PERMISSION_REQUEST = 3101;

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timestampFormat =
            new SimpleDateFormat("MMM dd, HH:mm", Locale.US);

    private LocationManager locationManager;
    private CancellationSignal locationCancellation;
    private Location lastKnownLocation;
    private boolean hasLocationPermission;
    private boolean hasSmsPermission;

    private TextView statusView;
    private LinearLayout contactsLayout;
    private MediaPlayer sirenPlayer;
    private LinearLayout locationLayout;
    private Button sendButton;
    private Button stopSirenButton;

    private boolean fetchingLocation;
    private boolean sendingSms;
    private boolean sirenPlaying;
    private boolean finished;

    private Location pendingLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos_emergency);

        statusView = findViewById(R.id.sosEmergencyStatus);
        contactsLayout = findViewById(R.id.sosEmergencyContacts);
        locationLayout = findViewById(R.id.sosEmergencyLocation);
        sendButton = findViewById(R.id.buttonSendEmergencySOS);
        stopSirenButton = findViewById(R.id.buttonStopSiren);

        findViewById(R.id.buttonBackFromEmergencySOS)
                .setOnClickListener(v -> finish());

        stopSirenButton.setOnClickListener(v -> toggleSiren());
        sendButton.setOnClickListener(v -> sendEmergencySos());

        loadLastKnownLocation();
        checkPermissions();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (lastKnownLocation != null) {
            try {
                outState.putByteArray(
                        "lastKnownLocation",
                        null);
            } catch (Exception ignored) {
            }
        }
        outState.putLong(LAST_LOCATION_TIME_KEY,
                lastKnownLocation.getTime());
    }

    private void loadLastKnownLocation() {
        SharedPreferences prefs =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lat = prefs.getString("sos_emergency_lat", null);
        String lon = prefs.getString("sos_emergency_lon", null);
        String time = prefs.getString("sos_emergency_time", null);
        if (lat != null && lon != null && time != null) {
            try {
                double latitude = Double.parseDouble(lat);
                double longitude = Double.parseDouble(lon);
                long timeMs = Long.parseLong(time);
                if (latitude != 0.0 || longitude != 0.0) {
                    Location saved = new Location("sos_emergency_last");
                    saved.setLatitude(latitude);
                    saved.setLongitude(longitude);
                    saved.setTime(timeMs);
                    lastKnownLocation = saved;
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void saveLastKnownLocation(Location location) {
        if (location == null) {
            return;
        }
        SharedPreferences prefs =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putString("sos_emergency_lat",
                        String.valueOf(location.getLatitude()))
                .putString("sos_emergency_lon",
                        String.valueOf(location.getLongitude()))
                .putString("sos_emergency_time",
                        String.valueOf(location.getTime()))
                .apply();
    }

    private void checkPermissions() {
        hasLocationPermission = ContextCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        hasSmsPermission = SosQueue.hasSmsPermission(this);

        if (hasLocationPermission) {
            loadLastKnownLocation();
        }

        if (hasSmsPermission) {
            updateFeedback();
        }
    }

    private void sendEmergencySos() {
        if (sendingSms || finished) {
            return;
        }

        sendButton.setEnabled(false);
        stopSirenButton.setEnabled(false);
        sendingSms = true;
        finished = false;
        updateFeedback();

        Location location = getBestLocation();
        if (location != null && location.getTime() > 0) {
            saveLastKnownLocation(location);
        }

        String[] contacts = loadEmergencyContacts();
        if (contacts.length == 0) {
            finished = true;
            sendButton.setEnabled(true);
            stopSirenButton.setEnabled(true);
            showStatus(R.string.sos_emergency_no_contacts);
            return;
        }

        if (!hasSmsPermission) {
            finished = true;
            sendButton.setEnabled(true);
            stopSirenButton.setEnabled(true);
            showStatus(R.string.sos_emergency_perm_denied);
            Toast.makeText(this, R.string.sos_emergency_perm_denied,
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (!isSimReady()) {
            finished = true;
            sendButton.setEnabled(true);
            stopSirenButton.setEnabled(true);
            showStatus(R.string.sos_emergency_no_sim);
            return;
        }

        final String[] recipients = contacts;
        if (location != null) {
            enqueueEmergencySend(recipients, location);
        } else {
            enqueueEmergencySend(recipients, null);
        }
    }

    private void enqueueEmergencySend(String[] recipients, Location location) {
        new Thread(() -> {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                String message = buildEmergencyMessage(location);
                for (String recipient : recipients) {
                    smsManager.sendTextMessage(
                            recipient, null, message, null, null);
                }
                runOnUiThread(() -> {
                    finished = true;
                    sendingSms = false;
                    sendButton.setEnabled(true);
                    stopSirenButton.setEnabled(true);
                    showStatus(R.string.sos_emergency_sms_sent);
                    Toast.makeText(
                            SosEmergencyActivity.this,
                            R.string.sos_emergency_sms_sent,
                            Toast.LENGTH_LONG).show();
                });
            } catch (RuntimeException failed) {
                runOnUiThread(() -> {
                    finished = true;
                    sendingSms = false;
                    sendButton.setEnabled(true);
                    stopSirenButton.setEnabled(true);
                    showStatus(R.string.sos_emergency_sms_failed);
                    Toast.makeText(
                            SosEmergencyActivity.this,
                            R.string.sos_emergency_sms_failed,
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private String buildEmergencyMessage(Location location) {
        StringBuilder body = new StringBuilder(EMERGENCY_SMS_BODY);

        if (location != null) {
            body.append("\n\n");
            body.append("My location: ");
            body.append(formatCoordinate(location.getLatitude()));
            body.append(", ");
            body.append(formatCoordinate(location.getLongitude()));
            body.append(" (");
            body.append(formatTimestamp(location.getTime()));
            body.append(")");
            body.append("\nhttps://maps.google.com/maps?q=");
            body.append(location.getLatitude());
            body.append(",");
            body.append(location.getLongitude());
        } else {
            body.append("\n\nNo GPS coordinates available.");
        }
        return body.toString();
    }

    private void toggleSiren() {
        if (sirenPlaying) {
            sirenPlayer.pause();
            sirenPlayer.release();
            sirenPlayer = null;
            sirenPlaying = false;
            updateFeedback();
            return;
        }

        if (sirenPlayer == null) {
            try {
                sirenPlayer = MediaPlayer.create(
                        getApplicationContext(), R.raw.siren);
            } catch (RuntimeException failedToCreate) {
                sirenPlayer = null;
            }
            if (sirenPlayer == null) {
                showStatus(R.string.siren_unavailable);
                return;
            }
            sirenPlayer.setLooping(true);
            sirenPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build());
        }

        try {
            sirenPlayer.start();
            sirenPlaying = true;
            updateFeedback();
        } catch (IllegalStateException failedToStart) {
            Toast.makeText(this, R.string.siren_unavailable,
                    Toast.LENGTH_LONG).show();
        }
    }

    private Location getBestLocation() {
        if (fetchingLocation) {
            return null;
        }
        if (pendingLocation != null && pendingLocation.getTime() > 0) {
            return pendingLocation;
        }
        if (lastKnownLocation != null && lastKnownLocation.getTime() > 0) {
            return lastKnownLocation;
        }
        if (hasLocationPermission) {
            return requestFreshLocationNoUi();
        }
        return null;
    }

    private Location requestFreshLocationNoUi() {
        if (locationManager == null) {
            return null;
        }

        boolean gpsEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!gpsEnabled && !networkEnabled) {
            return null;
        }

        fetchingLocation = true;
        Location result = null;
        boolean locationReceived = false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationCancellation = new CancellationSignal();
                String provider = gpsEnabled
                        ? LocationManager.GPS_PROVIDER
                        : LocationManager.NETWORK_PROVIDER;
                locationManager.getCurrentLocation(
                        provider, locationCancellation, getMainExecutor(),
                        location -> {
                            if (location == null) {
                                return;
                            }
                            pendingLocation = location;
                        });
            } else {
                if (gpsEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            0L, 0f, this, Looper.getMainLooper());
                }
                if (networkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            0L, 0f, this, Looper.getMainLooper());
                }
            }
        } catch (SecurityException ignored) {
            return null;
        } catch (Exception ignored) {
            return null;
        }

        timeoutHandler.removeCallbacksAndMessages(null);
        timeoutHandler.postDelayed(() -> {
            if (!fetchingLocation) {
                return;
            }
            stopLocationUpdates();
            fetchingLocation = false;
            if (result == null) {
                locationLayout.setVisibility(View.VISIBLE);
                TextView unavailable = new TextView(this);
                unavailable.setText(R.string.sos_emergency_location_unavailable);
                locationLayout.addView(unavailable);
            }
        }, LOCATION_TIMEOUT_MS);

        return result;
    }

    private void stopLocationUpdates() {
        timeoutHandler.removeCallbacksAndMessages(null);
        if (locationCancellation != null) {
            locationCancellation.cancel();
            locationCancellation = null;
        }
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Personal emergency contacts from profile setup plus the numbers the user
     * picked on the SOS Emergency Message screen, so both SOS paths reach the
     * same people.
     */
    private String[] loadEmergencyContacts() {
        return SosQuickSend.recipients(this).toArray(new String[0]);
    }

    private String formatCoordinate(double value) {
        return String.format(Locale.US, "%.6f", value);
    }

    private String formatTimestamp(long timeMs) {
        if (timeMs <= 0) {
            return "time not recorded";
        }
        return timestampFormat.format(new Date(timeMs));
    }

    private boolean isSimReady() {
        TelephonyManager manager = (TelephonyManager)
                getSystemService(TELEPHONY_SERVICE);
        return manager != null
                && manager.getSimState() == TelephonyManager.SIM_STATE_READY;
    }

    private void showStatus(int stringId) {
        statusView.setText(stringId);
        statusView.setVisibility(View.VISIBLE);
    }

    private void updateFeedback() {
        if (finished) {
            if (sendingSms && !finished) {
                statusView.setText(R.string.sos_emergency_feeding_sms);
            } else {
                statusView.setText(R.string.sos_emergency_sms_pending);
            }
            contactsLayout.setVisibility(View.GONE);
            locationLayout.setVisibility(View.GONE);
            return;
        }

        if (sirenPlaying) {
            statusView.setText(R.string.sos_emergency_siren);
            sirenPlaying = true;
            contactsLayout.setVisibility(View.GONE);
            locationLayout.setVisibility(View.GONE);
            return;
        }

        contactsLayout.setVisibility(View.VISIBLE);
        contactsLayout.removeAllViews();
        String[] contacts = loadEmergencyContacts();
        TextView header = new TextView(this);
        header.setText(R.string.sos_emergency_contact);
        header.setTextSize(14);
        header.setTextColor(getColor(R.color.text_secondary));
        contactsLayout.addView(header);
        TextView count = new TextView(this);
        count.setText(String.valueOf(contacts.length) + " contact(s)");
        count.setTextSize(15);
        count.setTextColor(getColor(R.color.text_primary));
        contactsLayout.addView(count);
        for (String contact : contacts) {
            TextView row = new TextView(this);
            row.setText(contact);
            row.setTextSize(15);
            row.setTextColor(getColor(R.color.text_primary));
            row.setPadding(0, 4, 0, 4);
            contactsLayout.addView(row);
        }

        locationLayout.setVisibility(View.VISIBLE);
        locationLayout.removeAllViews();
        TextView locHeader = new TextView(this);
        locHeader.setText(R.string.sos_emergency_location);
        locHeader.setTextSize(14);
        locHeader.setTextColor(getColor(R.color.text_secondary));
        locationLayout.addView(locHeader);
        if (lastKnownLocation != null && lastKnownLocation.getTime() > 0) {
            TextView lastKnownText = new TextView(this);
            String text = getString(
                    R.string.sos_emergency_location_last_known,
                    formatCoordinate(lastKnownLocation.getLatitude()),
                    formatTimestamp(lastKnownLocation.getTime()));
            lastKnownText.setText(text);
            lastKnownText.setTextSize(15);
            lastKnownText.setTextColor(getColor(R.color.text_primary));
            locationLayout.addView(lastKnownText);
            TextView mapLink = new TextView(this);
            mapLink.setText("https://maps.google.com/maps?q="
                    + formatCoordinate(lastKnownLocation.getLatitude())
                    + "," + formatCoordinate(lastKnownLocation.getLongitude()));
            mapLink.setTextSize(14);
            mapLink.setTextColor(getColor(R.color.link_color));
            mapLink.setLinksClickable(true);
            locationLayout.addView(mapLink);
        } else {
            TextView unavailable = new TextView(this);
            unavailable.setText(R.string.sos_emergency_location_unavailable);
            unavailable.setTextSize(15);
            unavailable.setTextColor(getColor(R.color.text_primary));
            locationLayout.addView(unavailable);
        }
        statusView.setText(R.string.sos_emergency_location_available);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (fetchingLocation) {
            if (location != null && location.getTime() > 0) {
                pendingLocation = location;
            }
            if (location != null) {
                stopLocationUpdates();
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    protected void onDestroy() {
        stopLocationUpdates();
        if (sirenPlayer != null) {
            sirenPlayer.release();
            sirenPlayer = null;
        }
        super.onDestroy();
    }
}
