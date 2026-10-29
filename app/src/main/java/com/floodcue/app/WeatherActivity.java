package com.floodcue.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lightweight weather view focused on rainfall and severe weather.
 * Fetches from Open-Meteo (no API key required), caches the latest
 * successful response in SharedPreferences, and labels cached data with
 * its last-updated time so it is never mistaken for live data.
 *
 * Works offline for cached readings; forecasts are guidance only and
 * cannot confirm flooding.
 */
public class WeatherActivity extends FloodCueActivity {

    private static final String PREFS_NAME = "FloodCuePrefs";
    private static final String KEY_CACHE = "weather_cache";
    private static final String KEY_UPDATED_AT = "weather_updated_at";
    private static final String KEY_SELECTED_LAT = "weather_selected_lat";
    private static final String KEY_SELECTED_LON = "weather_selected_lon";

    private static final long CACHE_TTL_MS = 15 * 60 * 1000L;
    private static final long LOCATION_TIMEOUT_MS = 12_000L;
    private static final int LOCATION_PERMISSION_REQUEST = 4101;
    private static final int FORECAST_HOURS = 24;

    private static final String OPEN_METEO_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=%1$f&longitude=%2$f"
                    + "&current=temperature_2m,weather_code,rain,wind_speed_10m,wind_direction_10m"
                    + ",relativehumidity_2m,apparent_temperature,pressure_msl,cloud_cover,wind_gusts_10m"
                    + "&hourly=rain,rain_probability,visibility"
                    + "&temperature_unit=celsius&wind_speed_unit=ms&timezone=auto&forecast_days=2";

    private static final String[] COMPASS = {
            "N", "NE", "E", "SE", "S", "SW", "W", "NW"
    };

    private TextView headerText;
    private TextView statusText;
    private TextView tempText;
    private TextView conditionText;
    private TextView rainText;
    private TextView rainProbText;
    private TextView rain24hText;
    private TextView windText;
    private TextView hourlyText;
    private RecyclerView detailsGrid;
    private WeatherDetailAdapter detailAdapter;
    private TextView warningsText;
    private TextView cachedText;
    private TextView locationErrorText;
    private Button refreshButton;
    private Button locationButton;
    private Button warningHelpButton;

    private double selectedLatitude;
    private double selectedLongitude;
    private boolean hasSelectedLocation;
    private boolean locationPermissionGranted;
    private boolean loading;
    private boolean waitingForFix;

    private LocationManager locationManager;
    private LocationListener activeLocationListener;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable locationTimeout = this::onLocationTimeout;

    private final Runnable cacheNoteClearer = () -> cachedText.setVisibility(View.GONE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        headerText = findViewById(R.id.weatherHeader);
        statusText = findViewById(R.id.weatherStatus);
        tempText = findViewById(R.id.weatherTemp);
        conditionText = findViewById(R.id.weatherCondition);
        rainText = findViewById(R.id.weatherRain);
        rainProbText = findViewById(R.id.weatherRainProb);
        rain24hText = findViewById(R.id.weatherRain24h);
        windText = findViewById(R.id.weatherWind);
        hourlyText = findViewById(R.id.weatherHourly);
        detailsGrid = findViewById(R.id.weatherDetailsGrid);
        detailsGrid.setLayoutManager(new GridLayoutManager(this, 2));
        detailAdapter = new WeatherDetailAdapter();
        detailsGrid.setAdapter(detailAdapter);
        warningsText = findViewById(R.id.weatherWarnings);
        cachedText = findViewById(R.id.weatherCached);
        locationErrorText = findViewById(R.id.weatherLocationError);
        refreshButton = findViewById(R.id.buttonWeatherRefresh);
        locationButton = findViewById(R.id.buttonWeatherSelectLocation);
        warningHelpButton = findViewById(R.id.buttonWeatherWarningHelp);

        findViewById(R.id.buttonBackFromWeather).setOnClickListener(v -> finish());
        refreshButton.setOnClickListener(v -> refreshWeather());
        locationButton.setOnClickListener(v -> showLocationOptions());
        warningHelpButton.setOnClickListener(v -> openWarningHelp());

        loadSelectedLocation();
        updateLocationButtonText();
        boolean haveCache = loadCachedWeather();

        locationPermissionGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (hasSelectedLocation) {
            refreshWeather();
        } else if (locationPermissionGranted) {
            obtainCurrentLocation();
        } else if (!haveCache) {
            showStatus(R.string.weather_location_request);
        }
    }

    // ------------------------------------------------------------ location

    private void loadSelectedLocation() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lat = prefs.getString(KEY_SELECTED_LAT, null);
        String lon = prefs.getString(KEY_SELECTED_LON, null);
        if (lat == null || lon == null) {
            return;
        }
        try {
            selectedLatitude = Double.parseDouble(lat);
            selectedLongitude = Double.parseDouble(lon);
            hasSelectedLocation = isValidCoordinate(selectedLatitude, selectedLongitude);
        } catch (NumberFormatException ignored) {
            hasSelectedLocation = false;
        }
    }

    private void saveSelectedLocation(double lat, double lon) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(KEY_SELECTED_LAT, String.valueOf(lat))
                .putString(KEY_SELECTED_LON, String.valueOf(lon))
                .apply();
        selectedLatitude = lat;
        selectedLongitude = lon;
        hasSelectedLocation = true;
        updateLocationButtonText();
    }

    private void updateLocationButtonText() {
        if (hasSelectedLocation) {
            locationButton.setText(getString(
                    R.string.weather_selected_location, selectedLatitude, selectedLongitude));
        } else {
            locationButton.setText(R.string.weather_select_location);
        }
    }

    private static boolean isValidCoordinate(double lat, double lon) {
        return lat >= -90d && lat <= 90d && lon >= -180d && lon <= 180d
                && !(lat == 0d && lon == 0d);
    }

    private void showLocationOptions() {
        CharSequence[] options = new CharSequence[]{
                getString(R.string.weather_use_current_location),
                getString(R.string.weather_enter_coordinates)
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.weather_choose_location)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        obtainCurrentLocation();
                    } else {
                        showManualLocationDialog();
                    }
                })
                .setNegativeButton(R.string.weather_cancel, null)
                .show();
    }

    private void showManualLocationDialog() {
        LinearLayout fields = new LinearLayout(this);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(padding, padding / 2, padding, 0);

        EditText latitudeInput = new EditText(this);
        latitudeInput.setHint(R.string.weather_lat_hint);
        latitudeInput.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_SIGNED
                | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (hasSelectedLocation) {
            latitudeInput.setText(String.format(Locale.US, "%.4f", selectedLatitude));
        }

        EditText longitudeInput = new EditText(this);
        longitudeInput.setHint(R.string.weather_lon_hint);
        longitudeInput.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_SIGNED
                | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (hasSelectedLocation) {
            longitudeInput.setText(String.format(Locale.US, "%.4f", selectedLongitude));
        }

        fields.addView(latitudeInput);
        fields.addView(longitudeInput);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.weather_manual_title)
                .setView(fields)
                .setNegativeButton(R.string.weather_cancel, null)
                .setPositiveButton(R.string.weather_use_location, (dialog, which) -> {
                    try {
                        double lat = Double.parseDouble(latitudeInput.getText().toString().trim());
                        double lon = Double.parseDouble(longitudeInput.getText().toString().trim());
                        if (!isValidCoordinate(lat, lon)) {
                            showLocationError(R.string.weather_invalid_coordinates);
                            return;
                        }
                        locationErrorText.setVisibility(View.GONE);
                        saveSelectedLocation(lat, lon);
                        refreshWeather();
                    } catch (NumberFormatException invalid) {
                        showLocationError(R.string.weather_invalid_coordinates);
                    }
                })
                .show();
    }

    private void obtainCurrentLocation() {
        if (!locationPermissionGranted) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        }
        if (locationManager == null) {
            showLocationError(R.string.weather_location_fix_failed);
            return;
        }

        showStatus(R.string.weather_getting_location);

        Location known = bestLastKnownLocation();
        if (known != null) {
            onLocationObtained(known);
            return;
        }

        String provider = enabledProvider();
        if (provider == null) {
            showLocationError(R.string.weather_location_fix_failed);
            return;
        }

        waitingForFix = true;
        activeLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                onLocationObtained(location);
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                if (waitingForFix) {
                    onLocationTimeout();
                }
            }
        };
        try {
            locationManager.requestLocationUpdates(
                    provider, 0L, 0f, activeLocationListener, Looper.getMainLooper());
            mainHandler.postDelayed(locationTimeout, LOCATION_TIMEOUT_MS);
        } catch (SecurityException | IllegalArgumentException failed) {
            waitingForFix = false;
            activeLocationListener = null;
            showLocationError(R.string.weather_location_fix_failed);
        }
    }

    @SuppressLint("MissingPermission")
    private Location bestLastKnownLocation() {
        Location best = null;
        for (String provider : new String[]{
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER}) {
            try {
                if (!locationManager.isProviderEnabled(provider)) {
                    continue;
                }
                Location candidate = locationManager.getLastKnownLocation(provider);
                if (candidate != null && (best == null
                        || candidate.getTime() > best.getTime())) {
                    best = candidate;
                }
            } catch (SecurityException | IllegalArgumentException ignored) {
                // Provider unavailable or permission revoked: try the next one.
            }
        }
        return best;
    }

    private String enabledProvider() {
        for (String provider : new String[]{
                LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    return provider;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    private void onLocationObtained(Location location) {
        stopWaitingForFix();
        locationErrorText.setVisibility(View.GONE);
        saveSelectedLocation(location.getLatitude(), location.getLongitude());
        refreshWeather();
    }

    private void onLocationTimeout() {
        stopWaitingForFix();
        showLocationError(R.string.weather_location_fix_failed);
    }

    private void stopWaitingForFix() {
        waitingForFix = false;
        mainHandler.removeCallbacks(locationTimeout);
        if (activeLocationListener != null && locationManager != null) {
            try {
                locationManager.removeUpdates(activeLocationListener);
            } catch (SecurityException ignored) {
            }
            activeLocationListener = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_PERMISSION_REQUEST) {
            return;
        }
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
            locationErrorText.setVisibility(View.GONE);
            obtainCurrentLocation();
        } else {
            locationPermissionGranted = false;
            showLocationError(R.string.weather_location_denied);
        }
    }

    // ------------------------------------------------------------- fetching

    private void refreshWeather() {
        if (loading) {
            return;
        }
        if (!hasSelectedLocation) {
            showStatus(R.string.weather_no_location);
            return;
        }
        showLoading();

        final double latitude = selectedLatitude;
        final double longitude = selectedLongitude;
        new Thread(() -> {
            String raw = null;
            try {
                raw = fetchWeather(latitude, longitude);
            } catch (IOException networkDown) {
                raw = null;
            }
            final String response = raw;
            final WeatherData fresh =
                    response == null ? null : parseJson(response, System.currentTimeMillis());
            if (fresh != null) {
                saveWeatherCache(response);
            }
            runOnUiThread(() -> {
                hideLoading();
                if (fresh != null) {
                    renderWeather(fresh, false);
                } else {
                    showCachedOrEmpty(R.string.weather_network_error);
                }
            });
        }).start();
    }

    private String fetchWeather(double latitude, double longitude) throws IOException {
        URL url = new URL(String.format(Locale.US, OPEN_METEO_URL, latitude, longitude));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(8_000);
        connection.setReadTimeout(8_000);
        try {
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            reader.close();
            return body.toString();
        } finally {
            connection.disconnect();
        }
    }

    // ------------------------------------------------------------- rendering

    private boolean loadCachedWeather() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String raw = prefs.getString(KEY_CACHE, null);
        if (raw == null) {
            renderEmptyStatus();
            showStatus(R.string.weather_no_data);
            return false;
        }
        WeatherData cached = parseJson(raw, prefs.getLong(KEY_UPDATED_AT, 0L));
        if (cached == null) {
            renderEmptyStatus();
            showStatus(R.string.weather_no_data);
            return false;
        }
        renderWeather(cached, true);
        return true;
    }

    /** Network failed: keep the cached reading visible and explain why. */
    private void showCachedOrEmpty(int errorRes) {
        if (!loadCachedWeather()) {
            renderEmptyStatus();
        }
        showStatus(errorRes);
    }

    private void renderEmptyStatus() {
        headerText.setText(R.string.weather_header);
        tempText.setText(R.string.weather_unavailable);
        conditionText.setText(R.string.weather_unavailable);
        rainText.setText(R.string.weather_unavailable);
        rainProbText.setText(R.string.weather_unavailable);
        rain24hText.setText(R.string.weather_unavailable);
        windText.setText(R.string.weather_unavailable);
        hourlyText.setText(R.string.weather_hourly);
        renderDetails(null);
        warningsText.setVisibility(View.GONE);
        hideCacheNote();
    }

    private void renderWeather(WeatherData data, boolean fromCache) {
        headerText.setText(R.string.weather_header);
        tempText.setText(getString(R.string.weather_temp, data.temperature));
        conditionText.setText(data.weatherCode >= 0
                ? getString(R.string.weather_condition, weatherCodeName(data.weatherCode))
                : getString(R.string.weather_unavailable));

        rainText.setText(data.currentRain >= 0d
                ? getString(R.string.weather_rain, data.currentRain)
                : getString(R.string.weather_unavailable));

        rainProbText.setText(data.rainProbability >= 0
                ? getString(R.string.weather_rain_prob, data.rainProbability)
                : getString(R.string.weather_unavailable));

        rain24hText.setText(data.rainNext24h >= 0d
                ? getString(R.string.weather_rain_24h, data.rainNext24h)
                : getString(R.string.weather_unavailable));

        windText.setText(data.windSpeed >= 0d
                ? getString(R.string.weather_wind, data.windSpeed,
                directionName(data.windDirection))
                : getString(R.string.weather_unavailable));

        hourlyText.setText(String.format(Locale.US, "%s\n%s",
                getString(R.string.weather_hourly), hourlyForecast(data)));

        renderDetails(data);

        // No official alert feed is bundled, so alerts are never invented.
        warningsText.setVisibility(View.GONE);

        if (fromCache) {
            showCacheNote(data.updatedAt);
        } else {
            hideCacheNote();
        }
        statusText.setVisibility(View.GONE);
        locationErrorText.setVisibility(View.GONE);
    }

    private void showCacheNote(long updatedAt) {
        String when = updatedAt > 0L
                ? new java.text.SimpleDateFormat("MMM dd, HH:mm", Locale.US)
                .format(new java.util.Date(updatedAt))
                : getString(R.string.weather_unavailable);
        String freshness = isStale(updatedAt)
                ? getString(R.string.weather_using_cache)
                : getString(R.string.weather_cache_fresh);
        cachedText.setText(getString(R.string.weather_cache_note, freshness, when));
        cachedText.setVisibility(View.VISIBLE);
    }

    private void hideCacheNote() {
        mainHandler.removeCallbacks(cacheNoteClearer);
        cachedText.setVisibility(View.GONE);
    }

    private static boolean isStale(long updatedAt) {
        return updatedAt <= 0L
                || System.currentTimeMillis() - updatedAt > CACHE_TTL_MS;
    }

    private void showLoading() {
        loading = true;
        refreshButton.setEnabled(false);
        showStatus(R.string.weather_loading);
    }

    private void hideLoading() {
        loading = false;
        refreshButton.setEnabled(true);
    }

    private boolean isLoading() {
        return loading;
    }

    private void showStatus(int messageRes) {
        statusText.setText(messageRes);
        statusText.setVisibility(View.VISIBLE);
    }

    private void showLocationError(int messageRes) {
        locationErrorText.setText(messageRes);
        locationErrorText.setVisibility(View.VISIBLE);
    }

    private String weatherCodeName(int code) {
        String[] weatherCodes = getResources().getStringArray(R.array.weather_condition_names);
        if (code < 0 || code >= weatherCodes.length) {
            return getString(R.string.weather_unavailable);
        }
        return weatherCodes[code];
    }

    private String directionName(int degrees) {
        if (degrees < 0 || degrees > 360) {
            return getString(R.string.weather_unavailable);
        }
        int index = (int) Math.round(degrees / 45.0) % COMPASS.length;
        return COMPASS[index] + " (" + degrees + "°)";
    }

    private String hourlyForecast(WeatherData data) {
        int start = currentIndex(data);
        if (start < 0 || data.hourlyTimes.isEmpty() || data.hourlyRain.isEmpty()) {
            return getString(R.string.weather_unavailable);
        }
        int end = Math.min(start + FORECAST_HOURS, data.hourlyTimes.size());
        StringBuilder builder = new StringBuilder();
        String previousDay = "";
        for (int i = start; i < end; i++) {
            String iso = data.hourlyTimes.get(i);
            String day = iso.length() >= 10 ? iso.substring(0, 10) : "";
            if (!day.equals(previousDay)) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(day).append('\n');
                previousDay = day;
            }
            String hour = iso.length() >= 16 ? iso.substring(11, 16) : iso;
            double millimetres = i < data.hourlyRain.size() ? data.hourlyRain.get(i) : 0d;
            builder.append("  ")
                    .append(hour)
                    .append("  ")
                    .append(String.format(Locale.US, "%.1f mm", millimetres))
                    .append('\n');
        }
        return builder.toString().trim();
    }

    /** Index in the hourly arrays that matches the current time. */
    private int currentIndex(WeatherData data) {
        if (data.currentTime == null || data.currentTime.isEmpty()) {
            return data.hourlyTimes.isEmpty() ? -1 : 0;
        }
        for (int i = 0; i < data.hourlyTimes.size(); i++) {
            if (data.currentTime.equals(data.hourlyTimes.get(i))) {
                return i;
            }
        }
        // ISO-8601 sorts lexicographically, so the first entry at/after now works.
        for (int i = 0; i < data.hourlyTimes.size(); i++) {
            if (data.hourlyTimes.get(i).compareTo(data.currentTime) >= 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Two-column card grid with the extra readings that matter during a flood:
     * humidity, feels-like, pressure, cloud, gusts, visibility and the rain
     * expected over the next six hours. A missing value is shown as
     * "Unavailable" rather than guessed.
     */
    private void renderDetails(WeatherData data) {
        detailAdapter.setData(data);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void openWarningHelp() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.weather_warning_help)
                .setMessage(R.string.weather_warning_help_text)
                .setPositiveButton(R.string.weather_ok, null)
                .show();
    }

    // ------------------------------------------------------------- storage

    private void saveWeatherCache(String rawJson) {
        if (rawJson == null) {
            return;
        }
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(KEY_CACHE, rawJson)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    // -------------------------------------------------------------- parsing

    public WeatherData parseJson(String json, long updatedAt) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            JSONObject root = new JSONObject(json);
            JSONObject current = root.optJSONObject("current");
            if (current == null || !current.has("temperature_2m")) {
                return null;
            }

            WeatherData data = new WeatherData();
            data.latitude = root.optDouble("latitude", 0.0);
            data.longitude = root.optDouble("longitude", 0.0);
            data.temperature = current.optDouble("temperature_2m", 0.0);
            data.weatherCode = current.optInt("weather_code", -1);
            data.currentRain = current.has("rain") ? current.optDouble("rain", 0.0) : -1d;
            data.windSpeed = current.has("wind_speed_10m")
                    ? current.optDouble("wind_speed_10m", 0.0) : -1d;
            data.windDirection = current.has("wind_direction_10m")
                    ? current.optInt("wind_direction_10m", -1) : -1;
            data.currentTime = current.optString("time", "");
            data.humidity = current.has("relativehumidity_2m")
                    ? current.optInt("relativehumidity_2m", -1) : -1;
            data.feelsLike = current.has("apparent_temperature")
                    ? current.optDouble("apparent_temperature", -1d) : -1d;
            data.pressure = current.has("pressure_msl")
                    ? current.optDouble("pressure_msl", -1d) : -1d;
            data.cloudCover = current.has("cloud_cover")
                    ? current.optInt("cloud_cover", -1) : -1;
            data.windGusts = current.has("wind_gusts_10m")
                    ? current.optDouble("wind_gusts_10m", -1d) : -1d;
            data.updatedAt = updatedAt;

            JSONObject hourly = root.optJSONObject("hourly");
            JSONArray times = hourly == null ? null : hourly.optJSONArray("time");
            JSONArray rains = hourly == null ? null : hourly.optJSONArray("rain");
            JSONArray probabilities =
                    hourly == null ? null : hourly.optJSONArray("rain_probability");
            JSONArray visibility =
                    hourly == null ? null : hourly.optJSONArray("visibility");
            if (times != null) {
                for (int i = 0; i < times.length(); i++) {
                    data.hourlyTimes.add(times.optString(i, ""));
                }
            }
            if (rains != null) {
                for (int i = 0; i < rains.length(); i++) {
                    data.hourlyRain.add(rains.optDouble(i, 0.0));
                }
            }

            int now = currentIndex(data);
            data.rainNext24h = 0d;
            data.rainNext6h = -1d;
            data.visibility = -1d;
            data.rainProbability = -1;
            if (now >= 0) {
                int end = Math.min(now + FORECAST_HOURS, data.hourlyRain.size());
                for (int i = now; i < end; i++) {
                    data.rainNext24h += data.hourlyRain.get(i);
                }
                int soon = Math.min(now + 6, data.hourlyRain.size());
                data.rainNext6h = 0d;
                for (int i = now; i < soon; i++) {
                    data.rainNext6h += data.hourlyRain.get(i);
                }
                if (probabilities != null && now < probabilities.length()) {
                    data.rainProbability = probabilities.optInt(now, -1);
                }
                if (visibility != null && now < visibility.length()) {
                    data.visibility = visibility.optDouble(now, -1d);
                }
            } else {
                data.rainNext24h = -1d;
            }

            return data;
        } catch (Exception malformed) {
            return null;
        }
    }

    // --------------------------------------------------------------- models

    public static class WeatherData {
        double latitude;
        double longitude;
        double temperature;
        int weatherCode = -1;
        double currentRain = -1d;
        int rainProbability = -1;
        double rainNext24h = -1d;
        double windSpeed = -1d;
        int windDirection = -1;
        int humidity = -1;
        double feelsLike = -1d;
        double pressure = -1d;
        int cloudCover = -1;
        double windGusts = -1d;
        double visibility = -1d;      // metres, from the hourly array
        double rainNext6h = -1d;
        String currentTime = "";
        long updatedAt;
        final List<String> hourlyTimes = new ArrayList<>();
        final List<Double> hourlyRain = new ArrayList<>();
    }

    /**
     * RecyclerView adapter for the weather details grid.
     *
     * Displays one metric per card in a two-column grid, using themed
     * colors for text, backgrounds, and a subtle icon to make the grid
     * scannable at a glance.
     */
    private class WeatherDetailAdapter extends RecyclerView.Adapter<WeatherDetailAdapter.ViewHolder> {

        private WeatherData data;

        void setData(WeatherData data) {
            this.data = data;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(
                    R.layout.item_weather_detail, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (data == null || data.hourlyTimes.isEmpty()) {
                return;
            }
            String label = getString(R.string.weather_label_humidity);
            String value = getString(R.string.weather_unavailable);

            switch (position) {
                case 0:
                    label = getString(R.string.weather_label_humidity);
                    value = readable(data.humidity, 0);
                    break;
                case 1:
                    label = getString(R.string.weather_label_feels_like);
                    value = readable(data.feelsLike, 1);
                    break;
                case 2:
                    label = getString(R.string.weather_label_pressure);
                    value = readable(data.pressure, 2);
                    break;
                case 3:
                    label = getString(R.string.weather_label_cloud);
                    value = readable(data.cloudCover, 3);
                    break;
                case 4:
                    label = getString(R.string.weather_label_gusts);
                    value = readable(data.windGusts, 4);
                    break;
                case 5:
                    label = getString(R.string.weather_label_visibility);
                    value = readable(data.visibility, 5);
                    break;
                case 6:
                    label = getString(R.string.weather_label_rain_6h);
                    value = readable(data.rainNext6h, 6);
                    break;
            }

            holder.label.setText(label);
            holder.value.setText(value);
        }

        @Override
        public int getItemCount() {
            return 7;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView label;
            TextView value;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                label = itemView.findViewById(R.id.weatherDetailLabel);
                value = itemView.findViewById(R.id.weatherDetailView);
            }
        }

        private String readable(int value, int pos) {
            if (value < 0) {
                return getString(R.string.weather_unavailable);
            }
            switch (pos) {
                case 0:
                    return getString(R.string.weather_value_humidity, value);
                case 3:
                    return getString(R.string.weather_value_cloud, value);
                default:
                    return getString(R.string.weather_unavailable);
            }
        }

        private String readable(double value, int pos) {
            if (value < 0) {
                return getString(R.string.weather_unavailable);
            }
            switch (pos) {
                case 1:
                    return getString(R.string.weather_value_feels_like, value);
                case 2:
                    return getString(R.string.weather_value_pressure, value);
                case 4:
                    return getString(R.string.weather_value_gusts, value);
                case 5:
                    return getString(R.string.weather_value_visibility, value / 1000d);
                case 6:
                    return getString(R.string.weather_value_rain_6h, value);
                default:
                    return getString(R.string.weather_unavailable);
            }
        }
    }

    @Override
    protected void onDestroy() {
        stopWaitingForFix();
        super.onDestroy();
    }
}
