package com.floodcue.app;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SosActivity extends AppCompatActivity implements LocationListener {

    private static final String PREFS_NAME = "FloodCuePrefs";
    private static final String DRAFT_NUMBERS_KEY = "sos_draft_numbers";
    private static final String DRAFT_MESSAGE_KEY = "sos_draft_message";
    private static final int LOCATION_PERMISSION_REQUEST = 2101;
    private static final long LOCATION_TIMEOUT_MS = 15000L;

    private enum LocationKind {
        NONE,
        CURRENT,
        LAST_KNOWN
    }

    private final Handler locationTimeoutHandler = new Handler(Looper.getMainLooper());
    private final List<String> recipients = new ArrayList<>();

    private SharedPreferences preferences;
    private LocationManager locationManager;
    private CancellationSignal locationCancellation;

    private EditText inputNumber;
    private EditText inputMessage;
    private TextView numberError;
    private TextView recipientsEmpty;
    private ChipGroup recipientChips;
    private TextView locationStatus;
    private TextView locationError;
    private TextView preview;
    private TextView sendHint;
    private Button addNumberButton;
    private Button getLocationButton;
    private Button useLastKnownButton;
    private Button sendButton;

    private LocationKind selectedLocationKind = LocationKind.NONE;
    private double selectedLatitude;
    private double selectedLongitude;
    private long selectedLocationTimeMs;
    private Location lastKnownLocation;
    private boolean fetchingLocation;
    private boolean launchingSms;
    private boolean defaultMessageInitialized;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        TextView backButton = findViewById(R.id.buttonBackToHome);
        inputNumber = findViewById(R.id.inputSosNumber);
        inputMessage = findViewById(R.id.inputSosMessage);
        numberError = findViewById(R.id.textSosNumberError);
        recipientsEmpty = findViewById(R.id.textSosRecipientsEmpty);
        recipientChips = findViewById(R.id.chipGroupSosRecipients);
        locationStatus = findViewById(R.id.textSosLocationStatus);
        locationError = findViewById(R.id.textSosLocationError);
        preview = findViewById(R.id.textSosPreview);
        sendHint = findViewById(R.id.textSosSendHint);
        addNumberButton = findViewById(R.id.buttonAddSosNumber);
        getLocationButton = findViewById(R.id.buttonGetSosLocation);
        useLastKnownButton = findViewById(R.id.buttonUseLastKnownLocation);
        sendButton = findViewById(R.id.buttonSendSos);

        backButton.setOnClickListener(v -> finish());
        addNumberButton.setOnClickListener(v -> addNumbersFromInput());
        getLocationButton.setOnClickListener(v -> startCurrentLocationRequest());
        useLastKnownButton.setOnClickListener(v -> useLastKnownLocation());
        sendButton.setOnClickListener(v -> confirmAndOpenSms());
        inputNumber.setOnEditorActionListener((v, actionId, event) -> {
            addNumbersFromInput();
            return true;
        });
        inputMessage.addTextChangedListener(new SimpleTextWatcher(this::refreshPreviewAndSendState));

        restoreDraft(savedInstanceState);
        restoreLocationState(savedInstanceState);
        refreshPreviewAndSendState();
    }

    @Override
    protected void onPause() {
        saveDraft();
        stopLocationUpdates();
        if (fetchingLocation) {
            fetchingLocation = false;
            getLocationButton.setEnabled(true);
            if (selectedLocationKind == LocationKind.NONE) {
                locationStatus.setText(R.string.sos_location_idle);
            }
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        launchingSms = false;
        refreshPreviewAndSendState();
    }

    @Override
    protected void onDestroy() {
        stopLocationUpdates();
        locationTimeoutHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("recipients", new ArrayList<>(recipients));
        outState.putString("message", inputMessage.getText().toString());
        outState.putString("locationKind", selectedLocationKind.name());
        outState.putDouble("latitude", selectedLatitude);
        outState.putDouble("longitude", selectedLongitude);
        outState.putLong("locationTime", selectedLocationTimeMs);
        outState.putBoolean("defaultMessageInitialized", defaultMessageInitialized);
    }

    private void restoreDraft(Bundle savedInstanceState) {
        List<String> savedNumbers;
        String savedMessage;

        if (savedInstanceState != null) {
            savedNumbers = savedInstanceState.getStringArrayList("recipients");
            savedMessage = savedInstanceState.getString("message");
            defaultMessageInitialized =
                    savedInstanceState.getBoolean("defaultMessageInitialized", false);
        } else {
            savedNumbers = splitStoredNumbers(preferences.getString(DRAFT_NUMBERS_KEY, ""));
            savedMessage = preferences.getString(DRAFT_MESSAGE_KEY, null);
        }

        if (savedNumbers != null) {
            for (String number : savedNumbers) {
                addRecipient(number, false);
            }
        }

        if (savedMessage != null && !savedMessage.trim().isEmpty()) {
            inputMessage.setText(savedMessage);
            defaultMessageInitialized = true;
        } else if (!defaultMessageInitialized) {
            inputMessage.setText(R.string.sos_default_message);
            defaultMessageInitialized = true;
        }
    }

    private void restoreLocationState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        String kindValue = savedInstanceState.getString("locationKind", LocationKind.NONE.name());
        try {
            selectedLocationKind = LocationKind.valueOf(kindValue);
        } catch (IllegalArgumentException ignored) {
            selectedLocationKind = LocationKind.NONE;
        }

        selectedLatitude = savedInstanceState.getDouble("latitude");
        selectedLongitude = savedInstanceState.getDouble("longitude");
        selectedLocationTimeMs = savedInstanceState.getLong("locationTime");
        bindSelectedLocation();
    }

    private void saveDraft() {
        preferences.edit()
                .putString(DRAFT_NUMBERS_KEY, String.join(",", recipients))
                .putString(DRAFT_MESSAGE_KEY, inputMessage.getText().toString())
                .apply();
    }

    private void addNumbersFromInput() {
        hideNumberError();
        String raw = inputNumber.getText().toString().trim();
        if (raw.isEmpty()) {
            return;
        }

        String[] parts = raw.split("[,;]+");
        boolean addedAny = false;
        for (String part : parts) {
            if (addRecipient(part, true)) {
                addedAny = true;
            } else if (parts.length == 1) {
                return;
            }
        }

        if (addedAny) {
            inputNumber.setText("");
        }
        refreshPreviewAndSendState();
    }

    private boolean addRecipient(String rawNumber, boolean showErrors) {
        String normalized = normalizePhoneNumber(rawNumber);
        if (!isValidPhoneNumber(normalized)) {
            if (showErrors) {
                showNumberError(getString(R.string.sos_number_invalid, rawNumber.trim()));
            }
            return false;
        }

        for (String existing : recipients) {
            if (existing.equals(normalized)) {
                if (showErrors) {
                    showNumberError(getString(R.string.sos_number_duplicate));
                }
                return false;
            }
        }

        recipients.add(normalized);
        Chip chip = new Chip(this, null, com.google.android.material.R.attr.chipStyle);
        chip.setText(normalized);
        chip.setCloseIconVisible(true);
        chip.setClickable(true);
        chip.setCheckable(false);
        chip.setOnCloseIconClickListener(v -> removeRecipient(normalized, chip));
        recipientChips.addView(chip);
        return true;
    }

    private void removeRecipient(String number, Chip chip) {
        recipients.remove(number);
        recipientChips.removeView(chip);
        hideNumberError();
        Toast.makeText(this, R.string.sos_number_removed, Toast.LENGTH_SHORT).show();
        refreshPreviewAndSendState();
    }

    private void startCurrentLocationRequest() {
        if (fetchingLocation) {
            return;
        }

        hideLocationError();
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST
            );
            return;
        }

        requestFreshLocation();
    }

    private void requestFreshLocation() {
        if (locationManager == null) {
            showLocationError(getString(R.string.sos_location_unavailable));
            return;
        }

        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled =
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!gpsEnabled && !networkEnabled) {
            showLocationError(getString(R.string.sos_location_gps_off));
            loadLastKnownIfAvailable();
            return;
        }

        fetchingLocation = true;
        getLocationButton.setEnabled(false);
        locationStatus.setText(R.string.sos_location_loading);
        loadLastKnownIfAvailable();

        locationTimeoutHandler.removeCallbacksAndMessages(null);
        locationTimeoutHandler.postDelayed(this::onLocationTimeout, LOCATION_TIMEOUT_MS);

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
                        this::onFreshLocation
                );
            } else {
                if (gpsEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            0L,
                            0f,
                            this,
                            Looper.getMainLooper()
                    );
                }
                if (networkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            0L,
                            0f,
                            this,
                            Looper.getMainLooper()
                    );
                }
            }
        } catch (SecurityException exception) {
            fetchingLocation = false;
            getLocationButton.setEnabled(true);
            showLocationError(getString(R.string.sos_location_permission_denied));
        } catch (Exception exception) {
            fetchingLocation = false;
            getLocationButton.setEnabled(true);
            showLocationError(getString(R.string.sos_location_unavailable));
        }
    }

    private void onLocationTimeout() {
        if (!fetchingLocation) {
            return;
        }
        stopLocationUpdates();
        fetchingLocation = false;
        getLocationButton.setEnabled(true);
        showLocationError(getString(R.string.sos_location_timeout));
        loadLastKnownIfAvailable();
        refreshPreviewAndSendState();
    }

    private void onFreshLocation(Location location) {
        if (location == null) {
            return;
        }
        stopLocationUpdates();
        fetchingLocation = false;
        getLocationButton.setEnabled(true);
        hideLocationError();
        selectedLocationKind = LocationKind.CURRENT;
        selectedLatitude = location.getLatitude();
        selectedLongitude = location.getLongitude();
        selectedLocationTimeMs = location.getTime() > 0
                ? location.getTime()
                : System.currentTimeMillis();
        bindSelectedLocation();
        refreshPreviewAndSendState();
    }

    private void useLastKnownLocation() {
        if (lastKnownLocation == null) {
            return;
        }
        hideLocationError();
        selectedLocationKind = LocationKind.LAST_KNOWN;
        selectedLatitude = lastKnownLocation.getLatitude();
        selectedLongitude = lastKnownLocation.getLongitude();
        selectedLocationTimeMs = lastKnownLocation.getTime() > 0
                ? lastKnownLocation.getTime()
                : System.currentTimeMillis();
        bindSelectedLocation();
        refreshPreviewAndSendState();
    }

    private void loadLastKnownIfAvailable() {
        if (!hasLocationPermission() || locationManager == null) {
            useLastKnownButton.setVisibility(View.GONE);
            return;
        }

        Location best = null;
        try {
            Location gps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location network =
                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            best = newerLocation(gps, network);
        } catch (SecurityException ignored) {
            best = null;
        }

        lastKnownLocation = best;
        boolean showLastKnown = best != null && selectedLocationKind != LocationKind.CURRENT;
        useLastKnownButton.setVisibility(showLastKnown ? View.VISIBLE : View.GONE);
        if (showLastKnown && selectedLocationKind == LocationKind.NONE && !fetchingLocation) {
            locationStatus.setText(
                    getString(R.string.sos_location_last_known_available)
                            + "\n"
                            + formatCoordinates(
                            best.getLatitude(),
                            best.getLongitude(),
                            best.getTime()
                    )
            );
        }
    }

    private void bindSelectedLocation() {
        if (selectedLocationKind == LocationKind.CURRENT) {
            locationStatus.setText(getString(
                    R.string.sos_location_current,
                    formatCoordinate(selectedLatitude),
                    formatCoordinate(selectedLongitude),
                    formatTimestamp(selectedLocationTimeMs)
            ));
        } else if (selectedLocationKind == LocationKind.LAST_KNOWN) {
            locationStatus.setText(getString(
                    R.string.sos_location_last_known,
                    formatCoordinate(selectedLatitude),
                    formatCoordinate(selectedLongitude),
                    formatTimestamp(selectedLocationTimeMs)
            ));
        } else {
            locationStatus.setText(R.string.sos_location_idle);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        onFreshLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_PERMISSION_REQUEST) {
            return;
        }

        if (hasLocationPermission()) {
            requestFreshLocation();
            return;
        }

        showLocationError(getString(R.string.sos_location_permission_denied));
        refreshPreviewAndSendState();
    }

    private void confirmAndOpenSms() {
        if (launchingSms || !canSend()) {
            return;
        }

        String numbersList = formatRecipientListForDialog();
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sos_confirm_title)
                .setMessage(getString(R.string.sos_confirm_message, numbersList))
                .setNegativeButton(R.string.sos_confirm_cancel, null)
                .setPositiveButton(
                        R.string.sos_confirm_continue,
                        (dialog, which) -> openSmsApp()
                )
                .show();
    }

    private void openSmsApp() {
        if (launchingSms || !canSend()) {
            return;
        }

        launchingSms = true;
        sendButton.setEnabled(false);
        saveDraft();

        String smsBody = buildSmsBody();
        String joinedRecipients = String.join(",", recipients);
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
        smsIntent.setData(Uri.parse("smsto:" + joinedRecipients));
        smsIntent.putExtra("sms_body", smsBody);
        smsIntent.putExtra("address", joinedRecipients);
        smsIntent.putExtra(Intent.EXTRA_TEXT, smsBody);

        try {
            if (smsIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(smsIntent);
                Toast.makeText(this, R.string.sos_sms_opened, Toast.LENGTH_LONG).show();
                return;
            }

            Intent fallback = new Intent(Intent.ACTION_VIEW);
            fallback.setData(Uri.parse("sms:" + joinedRecipients));
            fallback.putExtra("sms_body", smsBody);
            fallback.putExtra("address", joinedRecipients);
            if (fallback.resolveActivity(getPackageManager()) != null) {
                startActivity(fallback);
                Toast.makeText(this, R.string.sos_sms_opened, Toast.LENGTH_LONG).show();
                return;
            }

            launchingSms = false;
            Toast.makeText(this, R.string.sos_sms_unavailable, Toast.LENGTH_LONG).show();
            refreshPreviewAndSendState();
        } catch (Exception exception) {
            launchingSms = false;
            Toast.makeText(this, R.string.sos_sms_failed, Toast.LENGTH_LONG).show();
            refreshPreviewAndSendState();
        }
    }

    private void refreshPreviewAndSendState() {
        recipientsEmpty.setVisibility(recipients.isEmpty() ? View.VISIBLE : View.GONE);

        StringBuilder previewText = new StringBuilder();
        if (recipients.isEmpty()) {
            previewText.append(getString(R.string.sos_preview_no_recipients));
        } else {
            previewText.append(getString(
                    R.string.sos_preview_recipients,
                    String.join(", ", recipients)
            ));
        }
        previewText.append("\n\n");
        previewText.append(inputMessage.getText().toString().trim());
        previewText.append("\n\n");
        previewText.append(buildLocationPreviewLine());
        previewText.append("\n\n");
        previewText.append(getString(R.string.sos_preview_sms_note));
        preview.setText(previewText.toString());

        boolean enabled = canSend() && !launchingSms && !fetchingLocation;
        sendButton.setEnabled(enabled);
        sendButton.setAlpha(enabled ? 1f : 0.45f);
        sendHint.setVisibility(enabled ? View.GONE : View.VISIBLE);
        if (!canSend()) {
            sendHint.setText(R.string.sos_send_disabled_reason);
        }
    }

    private String buildSmsBody() {
        StringBuilder body = new StringBuilder(inputMessage.getText().toString().trim());
        if (selectedLocationKind != LocationKind.NONE) {
            body.append("\n\n");
            if (selectedLocationKind == LocationKind.LAST_KNOWN) {
                body.append(getString(
                        R.string.sos_preview_location_last_known,
                        formatCoordinate(selectedLatitude),
                        formatCoordinate(selectedLongitude),
                        formatTimestamp(selectedLocationTimeMs)
                ));
            } else {
                body.append(getString(
                        R.string.sos_preview_location,
                        formatCoordinate(selectedLatitude),
                        formatCoordinate(selectedLongitude),
                        formatTimestamp(selectedLocationTimeMs)
                ));
            }
        }
        if (recipients.size() > 1) {
            body.append("\n\nIntended recipients: ");
            body.append(String.join(", ", recipients));
        }
        return body.toString();
    }

    private String buildLocationPreviewLine() {
        if (selectedLocationKind == LocationKind.CURRENT) {
            return getString(
                    R.string.sos_preview_location,
                    formatCoordinate(selectedLatitude),
                    formatCoordinate(selectedLongitude),
                    formatTimestamp(selectedLocationTimeMs)
            );
        }
        if (selectedLocationKind == LocationKind.LAST_KNOWN) {
            return getString(
                    R.string.sos_preview_location_last_known,
                    formatCoordinate(selectedLatitude),
                    formatCoordinate(selectedLongitude),
                    formatTimestamp(selectedLocationTimeMs)
            );
        }
        return getString(R.string.sos_preview_no_location);
    }

    private boolean canSend() {
        return !recipients.isEmpty() && !inputMessage.getText().toString().trim().isEmpty();
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void stopLocationUpdates() {
        locationTimeoutHandler.removeCallbacksAndMessages(null);
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

    private void showNumberError(String message) {
        numberError.setText(message);
        numberError.setVisibility(View.VISIBLE);
    }

    private void hideNumberError() {
        numberError.setVisibility(View.GONE);
    }

    private void showLocationError(String message) {
        locationError.setText(message);
        locationError.setVisibility(View.VISIBLE);
    }

    private void hideLocationError() {
        locationError.setVisibility(View.GONE);
    }

    private String formatRecipientListForDialog() {
        StringBuilder list = new StringBuilder();
        for (String number : recipients) {
            list.append("• ").append(number).append("\n");
        }
        return list.toString().trim();
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }

        String number = phoneNumber.trim()
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "")
                .replace(".", "");

        if (number.startsWith("00") && number.length() > 2) {
            number = "+" + number.substring(2);
        }

        if (number.startsWith("+")) {
            return "+" + number.substring(1).replaceAll("[^0-9]", "");
        }

        number = number.replaceAll("[^0-9]", "");
        if (number.matches("[6-9][0-9]{9}")) {
            return "+91" + number;
        }
        return number;
    }

    private boolean isValidPhoneNumber(String number) {
        if (number == null || number.isEmpty()) {
            return false;
        }
        if (number.startsWith("+")) {
            String digits = number.substring(1);
            return digits.matches("[0-9]{8,15}");
        }
        return number.matches("[0-9]{8,15}");
    }

    private List<String> splitStoredNumbers(String stored) {
        List<String> values = new ArrayList<>();
        if (stored == null || stored.trim().isEmpty()) {
            return values;
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String part : stored.split("[,;]+")) {
            String normalized = normalizePhoneNumber(part);
            if (isValidPhoneNumber(normalized)) {
                unique.add(normalized);
            }
        }
        values.addAll(unique);
        return values;
    }

    private Location newerLocation(Location first, Location second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.getTime() >= second.getTime() ? first : second;
    }

    private String formatCoordinate(double value) {
        return String.format(Locale.US, "%.6f", value);
    }

    private String formatTimestamp(long timeMs) {
        long value = timeMs > 0 ? timeMs : System.currentTimeMillis();
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
                .format(new Date(value));
    }

    private String formatCoordinates(double latitude, double longitude, long timeMs) {
        return getString(
                R.string.sos_location_last_known,
                formatCoordinate(latitude),
                formatCoordinate(longitude),
                formatTimestamp(timeMs)
        );
    }

    private static final class SimpleTextWatcher implements TextWatcher {
        private final Runnable onChange;

        private SimpleTextWatcher(Runnable onChange) {
            this.onChange = onChange;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            onChange.run();
        }
    }
}
