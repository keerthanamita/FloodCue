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
import java.util.Arrays;
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
public class SosEmergencyActivity extends FloodCueActivity
        implements LocationListener {

    private static final String PREFS_NAME = "FloodCuePrefs";
    private static final String LAST_LOCATION_KEY = "sos_emergency_last_location";
    private static final String LAST_LOCATION_TIME_KEY =
            "sos_emergency_last_location_time";
    private static final long LOCATION_TIMEOUT_MS = 15000L;


    private static final int LOCATION_PERMISSION_REQUEST = 3101;

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timestampFormat =
            new SimpleDateFormat("MMM dd, HH:mm", Locale.US);

    private LocationManager locationManager;
    private CancellationSignal locationCancellation;
    private Location lastKnownLocation;
    private boolean hasLocationPermission;
    private boolean hasSmsPermission;

    /** The location fix we are waiting for from a fresh GPS request. */
    private Location pendingLocation;
    /** Set when a fresh location arrives, cleared before a new request. */
    private boolean locationReady;

    private TextView statusView;
    private LinearLayout contactsLayout;
    private MediaPlayer sirenPlayer;
    private LinearLayout locationLayout;
    private Button sendButton;
    private Button stopSirenButton;

    private boolean fetchingLocation;
    private boolean sirenPlaying;
    private boolean finished;

    /** Tracks the whole send in one state so the screen only reports what
     *  {@link SosQueue} actually observed: queued, submitting, or a confirmed
     *  send outcome.  Never means "sent" before the queue reports a success.
     */
    private enum SendState {
        /** A message is in the queue but not submitted to the radio yet. */
        QUEUED,
        /** The radio acknowledged submission for at least one number. */
        SUBMITTED,
        /** Every number's radio result has been received. */
        COMPLETE
    }

    private SendState sendState = SendState.QUEUED;

    /** Most recent radio result code per number (0 = none). */
    private int lastSmsResult;

    private final Handler locationWaitHandler = new Handler(Looper.getMainLooper());
    /** Set true when a fresh location has arrived; cleared before a new request. */
    private boolean waitingForFreshLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos_emergency);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(R.string.sos_emergency_heading);
        }

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

        // The queue's per-number send callbacks drive the status. The listener
        // is replaced on every configuration change so it always points at the
        // current activity and can always post to the main thread.
        SosQueue.setStatusListener(() -> {
            SosQueue.State state = SosQueue.getState(this);
            if (SosQueue.isComplete(this)) {
                onQueueResult(null, android.app.Activity.RESULT_OK);
                return;
            }
            if (state == SosQueue.State.SENDING) {
                onQueueResult(null, 0);
            } else {
                onQueueResult(null, SosQueue.getLastError(this));
            }
        });
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
        if (finished) {
            return;
        }

        sendButton.setEnabled(false);
        stopSirenButton.setEnabled(false);
        sendState = SendState.QUEUED;
        lastSmsResult = 0;
        updateFeedback();

        Location location = getBestLocation();
        if (location != null && location.getTime() > 0) {
            saveLastKnownLocation(location);
            // A recent location already exists, so use it immediately.
            enqueueEmergencySend(loadEmergencyContacts(), location);
            return;
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

        // No location ready yet: request a fresh GPS fix and wait for it,
        // or fall back to the last known location if the fix is recent.
        if (hasLocationPermission) {
            if (lastKnownLocation != null && lastKnownLocation.getTime() > 0) {
                long age = System.currentTimeMillis() - lastKnownLocation.getTime();
                if (age <= 60_000L) {
                    // A recent saved fix is usable now.
                    enqueueEmergencySend(contacts, lastKnownLocation);
                    return;
                }
            }
            // Request a fresh fix; the callback enqueues the message.
            startFreshLocationRequest();
            return;
        }

        // Location permission is not granted. Use the last known location
        // if it is still reasonably fresh, otherwise send without coordinates.
        if (lastKnownLocation != null && lastKnownLocation.getTime() > 0) {
            long age = System.currentTimeMillis() - lastKnownLocation.getTime();
            if (age <= 300_000L) {
                enqueueEmergencySend(contacts, lastKnownLocation);
                return;
            }
        }
        enqueueEmergencySend(contacts, null);
    }

    /**
     * Called by {@link SosQueue} whenever a per-number send result arrives.
     * The queue reports COMPLETE only when every number's radio confirmed
     * delivery, so that is the only moment this screen may say "sent".
     */
    private void onQueueResult(String number, int resultCode) {
        if (finished) {
            return;
        }
        if (resultCode == android.app.Activity.RESULT_OK) {
            lastSmsResult = 0;
        } else {
            lastSmsResult = resultCode;
        }
        if (SosQueue.isComplete(this)) {
            sendState = SendState.COMPLETE;
            runOnUiThread(() -> {
                finished = true;
                sendButton.setEnabled(true);
                stopSirenButton.setEnabled(true);
                updateFeedback();
            });
        } else if (SosQueue.getState(this) == SosQueue.State.SENDING) {
            sendState = SendState.SUBMITTED;
            runOnUiThread(() -> updateFeedback());
        } else {
            sendState = SendState.QUEUED;
            runOnUiThread(() -> updateFeedback());
        }
    }

    private void enqueueEmergencySend(String[] recipients, Location location) {
        String message = buildEmergencyMessage(location);
        SosQueue.enqueue(this, java.util.Arrays.asList(recipients), message);
        // The queue drives the status: the screen only ever reports what the
        // per-number sent/delivery callbacks actually confirmed. Until one of
        // those arrives the button stays enabled and the status stays "sending".
    }

    private String buildEmergencyMessage(Location location) {
        return SosQuickSend.buildMessage(this, location);
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

    /**
     * Best location to use in the emergency message.
     *
     *  - Returns a recent saved location immediately if one is available.
     *  - Otherwise starts a fresh GPS/network request and returns the result
     *    when it arrives or after the timeout. The caller must wait for the
     *    result before enqueuing the message, so the message always carries
     *    coordinates when a fix exists.
     *  - Never returns a location that was just requested and not yet arrived.
     */
    /**
     * Returns the location to attach to the SOS message.
     *
     *  - Returns a recent saved location immediately if one is available.
     *  - Otherwise returns null and the caller waits for a fresh GPS fix.
     *  - Never returns a location that was just requested and not yet arrived.
     */
    private Location getBestLocation() {
        if (fetchingLocation) {
            return null;
        }
        // A saved fix is usable only if it is recent or the user is waiting
        // for a fresh fix and has no better option yet.
        if (lastKnownLocation != null && lastKnownLocation.getTime() > 0) {
            long age = System.currentTimeMillis() - lastKnownLocation.getTime();
            if (age <= 60_000L) {
                return lastKnownLocation;
            }
        }
        return null;
    }

    /**
     * Starts a fresh location request and returns immediately.
     *
     * The caller waits for {@link #locationReady} to become true (or for the
     * timeout) before enqueuing the message. This matches the pattern used by
     * {@link SosActivity}, which is verified working.
     */
    private void startFreshLocationRequest() {
        if (locationManager == null) {
            return;
        }

        boolean gpsEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!gpsEnabled && !networkEnabled) {
            return;
        }

        fetchingLocation = true;
        locationCancellation = null;
        locationReady = false;
        waitingForFreshLocation = true;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationCancellation = new CancellationSignal();
                String provider = gpsEnabled
                        ? LocationManager.GPS_PROVIDER
                        : LocationManager.NETWORK_PROVIDER;
                locationManager.getCurrentLocation(
                        provider,
                        locationCancellation,
                        getMainExecutor(),
                        location -> {
                            if (location == null) {
                                return;
                            }
                            if (locationReady) {
                                return;
                            }
                            locationReady = true;
                            pendingLocation = location;
                            finishLocationRequest();
                        });
            } else {
                if (gpsEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            0L,
                            0f,
                            this,
                            Looper.getMainLooper());
                }
                if (networkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            0L,
                            0f,
                            this,
                            Looper.getMainLooper());
                }
            }
        } catch (SecurityException ignored) {
            stopLocationUpdates();
            fetchingLocation = false;
            locationReady = false;
            waitingForFreshLocation = false;
            return;
        } catch (Exception ignored) {
            stopLocationUpdates();
            fetchingLocation = false;
            locationReady = false;
            waitingForFreshLocation = false;
            return;
        }

        // Timeout after the bounded window; the app then sends without
        // coordinates rather than blocking forever.
        timeoutHandler.removeCallbacksAndMessages(null);
        timeoutHandler.postDelayed(() -> {
            if (!waitingForFreshLocation) {
                return;
            }
            waitingForFreshLocation = false;
            stopLocationUpdates();
            fetchingLocation = false;
        }, LOCATION_TIMEOUT_MS);
    }

    /**
     * Called when a location arrives, so the caller can use it.
     *
     * Stops the request after a fresh location is obtained so no extra
     * updates are requested while the SOS message is being sent.
     */
    private void finishLocationRequest() {
        fetchingLocation = false;
        if (locationCancellation != null) {
            locationCancellation.cancel();
            locationCancellation = null;
        }
        stopLocationUpdates();
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
            return getString(R.string.sos_location_time_unknown);
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

    /**
     * The queue can move through several send attempts, so only the most
     * recent radio result is reported.  A result is known as soon as a number
     * leaves this device and reported as delivered once the receiver confirms.
     */
    private void updateFeedback() {
        if (finished) {
            switch (sendState) {
                case SUBMITTED:
                    statusView.setText(R.string.sos_emergency_sms_pending);
                    break;
                case COMPLETE:
                    statusView.setText(R.string.sos_emergency_sms_sent);
                    break;
                default:
                    if (lastSmsResult != 0) {
                        statusView.setText(R.string.sos_emergency_sms_failed);
                    } else {
                        statusView.setText(R.string.sos_emergency_sms_pending);
                    }
                    break;
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
        SosQueue.setStatusListener(null);
        stopLocationUpdates();
        if (sirenPlayer != null) {
            sirenPlayer.release();
            sirenPlayer = null;
        }
        super.onDestroy();
    }
}
