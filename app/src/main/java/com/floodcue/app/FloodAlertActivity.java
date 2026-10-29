package com.floodcue.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;


import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * Flood and heavy-rain warnings for one district.
 *
 * Sources, in order of reliability:
 * 1. NDMA SACHET public CAP feed (https://sachet.ndma.gov.in) - open, no key,
 *    fetched straight from the device, filtered to the chosen district.
 *    Severity comes from each alert's own CAP record, never from guesswork.
 * 2. CWC river/flood forecasts have no verified machine-readable endpoint,
 *    so the official portal is linked rather than scraped.
 *
 * (IMD district warnings need an API key bound to one server address, so they
 * are only readable through a user-hosted proxy - server/imd_proxy.php - which
 * this screen does not wire up yet.)
 *
 * Everything is cached in SharedPreferences with its retrieval time and is
 * clearly labelled when shown from cache. Notifications fire only for alerts
 * that match the district and only once per alert id.
 */
public class FloodAlertActivity extends FloodCueActivity {

    private static final String PREFS_NAME = "FloodCuePrefs";
    private static final String KEY_DISTRICT = "flood_alert_district";
    private static final String KEY_RSS_CACHE = "flood_alert_rss_cache";
    private static final String KEY_RSS_CACHE_AT = "flood_alert_rss_cache_at";
    private static final String KEY_SEVERITY_CACHE = "flood_alert_severity_cache";
    private static final String KEY_NOTIFIED = "flood_alert_notified";
    private static final String KEY_NOTIF_ASKED = "flood_alert_notif_asked";

    private static final String SACHET_RSS_URL =
            "https://sachet.ndma.gov.in/cap_public_website/rss/rss_india.xml";
    private static final String SACHET_PORTAL_URL = "https://sachet.ndma.gov.in/";
    private static final String CWC_FLOOD_URL = "https://ffs.india-water.gov.in/";

    /**
     * The first channel was silent and Android never lets an existing channel
     * change its sound afterwards, so alerts moved to a fresh channel.
     */
    private static final String CHANNEL_ID = "flood_alerts_v2";
    private static final String LEGACY_CHANNEL_ID = "flood_alerts";
    /** Strong repeating buzz, so the alert is felt as well as heard. */
    private static final long[] ALERT_VIBRATION = {0, 400, 250, 400, 250, 400, 250, 400};
    private static final int LOCATION_PERMISSION_REQUEST = 4201;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 4202;
    private static final int FETCH_TIMEOUT_MS = 9_000;
    private static final int CAP_TIMEOUT_MS = 6_000;
    /** How many CAP records are read per refresh: enough for severity, cheap on data. */
    private static final int MAX_ENRICH = 8;
    private static final int MAX_SHOWN = 25;
    private static final int NOTIFY_PER_REFRESH = 3;
    private static final int MAX_NOTIFIED_IDS = 300;

    private static final int SEVERITY_UNKNOWN = 0;
    private static final int SEVERITY_GREEN = 1;
    private static final int SEVERITY_YELLOW = 2;
    private static final int SEVERITY_ORANGE = 3;
    private static final int SEVERITY_RED = 4;

    private TextView statusText;
    private TextView cachedText;
    private LinearLayout sachetList;
    private EditText districtInput;
    private Button refreshButton;
    private Button locationButton;

    private String district = "";
    private boolean loading;
    private boolean waitingForFix;

    private LocationManager locationManager;
    private LocationListener activeLocationListener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable locationTimeout = this::onLocationTimeout;

    // ------------------------------------------------------------------ model

    private static final class Alert {
        String id = "";
        String title = "";
        String link = "";
        String source = "";
        String area = "";
        String event = "";
        String effective = "";
        String expiry = "";
        long publishedAt;
        int severity = SEVERITY_UNKNOWN;
    }

    // ------------------------------------------------------------------ setup

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flood_alert);

        statusText = findViewById(R.id.floodAlertStatus);
        cachedText = findViewById(R.id.floodAlertCached);
        sachetList = findViewById(R.id.floodSachetList);
        districtInput = findViewById(R.id.floodDistrictInput);
        refreshButton = findViewById(R.id.buttonFloodRefresh);
        locationButton = findViewById(R.id.buttonFloodUseLocation);

        findViewById(R.id.buttonBackFromFloodAlert).setOnClickListener(v -> finish());
        findViewById(R.id.buttonFloodDistrictSet).setOnClickListener(v -> applyTypedDistrict());
        findViewById(R.id.buttonFloodRefresh).setOnClickListener(v -> refresh());

        findViewById(R.id.buttonFloodSachet).setOnClickListener(v ->
                openLink(SACHET_PORTAL_URL));
        findViewById(R.id.buttonFloodCwc).setOnClickListener(v -> openLink(CWC_FLOOD_URL));
        locationButton.setOnClickListener(v -> useMyLocation());

        createNotificationChannel();
        requestNotificationPermissionOnce();

        district = preferences().getString(KEY_DISTRICT, "");
        districtInput.setText(district);

        if (district.isEmpty()) {
            showStatus(R.string.flood_alert_district_empty);
            renderSachetFromCache();
        } else {
            renderSachetFromCache();
            refresh();
        }
    }

    private SharedPreferences preferences() {
        return preferences(this);
    }

    /** The same preferences, readable from the background check without an Activity. */
    static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private void createNotificationChannel() {
        ensureChannel(this);
    }

    /**
     * The alert channel plays the bundled siren on the alarm stream, vibrates,
     * and is important enough for Android to show it as a heads-up over other
     * apps while FloodCue is closed. The legacy channel was silent, and channel
     * settings cannot change after creation, so it is deleted and replaced.
     */
    static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.flood_alert_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.flood_alert_channel_desc));
        channel.setSound(alarmSound(context), new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        channel.enableVibration(true);
        channel.setVibrationPattern(ALERT_VIBRATION.clone());
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(channel);
    }

    /**
     * The app's own siren, played as an alarm so it rings even when the phone
     * is on vibrate or silent.
     */
    private static Uri alarmSound(Context context) {
        return Uri.parse("android.resource://" + context.getPackageName()
                + "/" + R.raw.siren);
    }

    private void requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < 33) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        SharedPreferences prefs = preferences();
        if (prefs.getBoolean(KEY_NOTIF_ASKED, false)) {
            return;
        }
        prefs.edit().putBoolean(KEY_NOTIF_ASKED, true).apply();
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                NOTIFICATION_PERMISSION_REQUEST);
    }

    // --------------------------------------------------------------- district

    private void applyTypedDistrict() {
        String typed = districtInput.getText().toString().trim();
        if (typed.isEmpty()) {
            showStatus(R.string.flood_alert_district_empty);
            return;
        }
        saveDistrict(typed);
        refresh();
    }

    private void saveDistrict(String value) {
        district = value;
        districtInput.setText(value);
        preferences().edit().putString(KEY_DISTRICT, value).apply();
    }

    private void useMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }
        showStatus(R.string.flood_alert_location_reading);

        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        }
        if (locationManager == null) {
            showStatus(R.string.flood_alert_district_location_failed);
            return;
        }

        Location known = bestLastKnownLocation();
        if (known != null) {
            resolveDistrict(known.getLatitude(), known.getLongitude());
            return;
        }

        String provider = enabledProvider();
        if (provider == null) {
            showStatus(R.string.flood_alert_district_location_failed);
            return;
        }

        waitingForFix = true;
        activeLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                waitingForFix = false;
                mainHandler.removeCallbacks(locationTimeout);
                removeActiveListener();
                resolveDistrict(location.getLatitude(), location.getLongitude());
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
            mainHandler.postDelayed(locationTimeout, 10_000L);
        } catch (SecurityException | IllegalArgumentException failed) {
            waitingForFix = false;
            activeLocationListener = null;
            showStatus(R.string.flood_alert_district_location_failed);
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
                // Provider unavailable: try the next one.
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

    private void onLocationTimeout() {
        waitingForFix = false;
        removeActiveListener();
        showStatus(R.string.flood_alert_district_location_failed);
    }

    private void removeActiveListener() {
        if (activeLocationListener != null && locationManager != null) {
            try {
                locationManager.removeUpdates(activeLocationListener);
            } catch (SecurityException ignored) {
            }
            activeLocationListener = null;
        }
    }

    /** District name comes from the geocoder; the lookup needs data, so run it off the UI thread. */
    private void resolveDistrict(double latitude, double longitude) {
        new Thread(() -> {
            String found = null;
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                @SuppressWarnings("deprecation")
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    found = firstNonEmpty(address.getSubAdminArea(),
                            address.getLocality(), address.getAdminArea());
                }
            } catch (IOException | RuntimeException geocoderFailed) {
                found = null;
            }
            final String districtName = found;
            runOnUiThread(() -> {
                if (districtName == null || districtName.trim().isEmpty()) {
                    showStatus(R.string.flood_alert_district_location_failed);
                    return;
                }
                saveDistrict(districtName.trim());
                refresh();
            });
        }).start();
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0
                || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            if (requestCode == LOCATION_PERMISSION_REQUEST) {
                showStatus(R.string.flood_alert_district_location_permission);
            }
            return;
        }
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            useMyLocation();
        }
    }

    // ---------------------------------------------------------------- fetch

    private void refresh() {
        String selected = districtInput.getText().toString().trim();
        if (selected.isEmpty()) {
            showStatus(R.string.flood_alert_district_empty);
            return;
        }
        saveDistrict(selected);
        if (loading) {
            return;
        }
        loading = true;
        refreshButton.setEnabled(false);
        locationButton.setEnabled(false);
        showStatus(R.string.flood_alert_loading);

        final String wantedDistrict = district;

        new Thread(() -> {
            try {
                String rss = null;
                boolean offline = false;
                try {
                    rss = httpGet(SACHET_RSS_URL, FETCH_TIMEOUT_MS);
                } catch (IOException networkDown) {
                    offline = true;
                }
                // A body that small is an error page, not the feed.
                if (rss != null && rss.length() < 200) {
                    rss = null;
                }

                List<Alert> matched = new ArrayList<>();
                boolean parsed = false;
                if (rss != null) {
                    List<Alert> all = parseRss(rss);
                    parsed = !all.isEmpty();
                    if (parsed) {
                        // Known severity/area from an earlier read of these ids
                        // helps matching and the chips without extra fetches.
                        restoreSeverity(all);
                        matched = filterByDistrict(all, wantedDistrict);
                        enrich(matched);
                        // The cache only changes after a feed really arrived and
                        // parsed, so a failed refresh can never wipe saved alerts.
                        preferences().edit()
                                .putString(KEY_RSS_CACHE, rss)
                                .putLong(KEY_RSS_CACHE_AT, System.currentTimeMillis())
                                .apply();
                    }
                }

                final String freshRss = parsed ? rss : null;
                final List<Alert> freshMatched = matched;
                final boolean wasOffline = offline;

                runOnUiThread(() -> {
                    loading = false;
                    refreshButton.setEnabled(true);
                    locationButton.setEnabled(true);

                    if (freshRss == null) {
                        renderSachetFromCache();
                        showStatus(wasOffline
                                ? R.string.flood_alert_offline
                                : R.string.flood_alert_error);
                        return;
                    }

                    renderSachet(freshMatched, false, System.currentTimeMillis());
                    if (freshMatched.isEmpty()) {
                        showStatus(getString(R.string.flood_alert_no_matching, district));
                    } else {
                        showStatus(R.string.flood_alert_feed_updated);
                    }
                    notifyNew(this, freshMatched, district);
                    if (!freshMatched.isEmpty() && notificationsBlocked()) {
                        // The warnings are on screen but cannot announce themselves.
                        showStatus(R.string.flood_alert_notifications_blocked);
                    }
                });
            } catch (RuntimeException feedBroken) {
                // A parsing or prefs failure must not leave the buttons dead.
                runOnUiThread(() -> {
                    loading = false;
                    refreshButton.setEnabled(true);
                    locationButton.setEnabled(true);
                    renderSachetFromCache();
                    showStatus(R.string.flood_alert_error);
                });
            }
        }).start();
    }

    /** Android 13+ needs POST_NOTIFICATIONS for an alert to reach the shade. */
    private boolean notificationsBlocked() {
        return Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this,
                        Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED;
    }

    /** CAP uses a "cap:" prefix and RSS may use "atom:", so match on the local name. */
    private static String localName(String name) {
        if (name == null) {
            return "";
        }
        int colon = name.lastIndexOf(':');
        return colon >= 0 ? name.substring(colon + 1) : name;
    }


    private static String httpGet(String address, int timeoutMs) throws IOException {
        HttpURLConnection connection =
                (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        connection.setRequestProperty("User-Agent", "FloodCue/1.0");
        try {
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                return null;
            }
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"));
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

    // ----------------------------------------------------------------- parse

    private static List<Alert> parseRss(String xml) {
        List<Alert> alerts = new ArrayList<>();
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(new java.io.StringReader(xml));
            SimpleDateFormat pubFormat =
                    new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
            pubFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

            Alert current = null;
            String tag = "";
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    tag = localName(parser.getName());
                    if ("item".equals(tag)) {
                        current = new Alert();
                    }
                } else if (event == XmlPullParser.TEXT && current != null) {
                    String text = parser.getText();
                    if (text == null) {
                        continue;
                    }
                    text = text.trim();
                    if (text.isEmpty()) {
                        continue;
                    }
                    switch (tag) {
                        case "title":
                            current.title = text;
                            break;
                        case "link":
                            current.link = text;
                            break;
                        case "guid":
                            current.id = text;
                            break;
                        case "author":
                            current.source = text;
                            break;
                        case "pubDate":
                            try {
                                current.publishedAt = pubFormat.parse(text).getTime();
                            } catch (java.text.ParseException ignored) {
                                current.publishedAt = 0L;
                            }
                            break;
                        default:
                            break;
                    }
                } else if (event == XmlPullParser.END_TAG
                        && "item".equals(localName(parser.getName()))) {
                    if (current != null && !current.title.isEmpty()) {
                        if (current.id.isEmpty()) {
                            current.id = current.link;
                        }
                        alerts.add(current);
                    }
                    current = null;
                }
                event = parser.next();
            }
        } catch (Exception malformed) {
            return alerts;
        }
        return alerts;
    }

    private static List<Alert> filterByDistrict(List<Alert> alerts, String wantedDistrict) {
        List<Alert> matched = new ArrayList<>();
        if (wantedDistrict == null || wantedDistrict.trim().isEmpty()) {
            return matched;
        }
        String needle = wantedDistrict.trim();
// Match district name as a whole word (case‑insensitive), allowing for
// common prefixes/suffixes and punctuation.  Use \b‑style look‑behind/
// look‑ahead that works even when the name appears inside other text.
        Pattern loose = Pattern.compile(
                "(?i)\b" + Pattern.quote(needle) + "\b");
        Pattern exact = Pattern.compile(
                "(?i)(?<![a-z])" + Pattern.quote(needle) + "(?![a-z])");
        for (Alert alert : alerts) {
            boolean hit = false;
            if (alert.title != null) {
                hit = loose.matcher(alert.title).find();
            }
            // After enrich(), alert.area is populated from CAP areaDesc
            if (!hit && alert.area != null) {
                hit = loose.matcher(alert.area).find();
            }
            if (!hit) {
                // Also try exact‑word match on both fields as a fallback
                if (alert.title != null) {
                    hit = exact.matcher(alert.title).find();
                }
                if (!hit && alert.area != null) {
                    hit = exact.matcher(alert.area).find();
                }
            }
            if (hit) {
                matched.add(alert);
            }
        }
        return matched;
    }

    /** Reads severity/validity from each alert's own CAP record. */
    private void enrich(List<Alert> alerts) {
        int read = 0;
        for (Alert alert : alerts) {
            if (read >= MAX_ENRICH || alert.link.isEmpty()) {
                continue;
            }
            try {
                String cap = httpGet(alert.link, CAP_TIMEOUT_MS);
                if (cap != null) {
                    parseCap(alert, cap);
                    read++;
                }
            } catch (IOException unreachable) {
                // Severity stays unknown rather than being guessed.
            }
        }
        saveSeverityCache(alerts);
    }

    private void parseCap(Alert alert, String cap) {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(new java.io.StringReader(cap));
            StringBuilder area = new StringBuilder();
            String tag = "";
            String severity = "";
            String event = "";
            String sender = "";
            String effective = "";
            String expiry = "";

            int state = parser.getEventType();
            while (state != XmlPullParser.END_DOCUMENT) {
                if (state == XmlPullParser.START_TAG) {
                    tag = localName(parser.getName());
                } else if (state == XmlPullParser.TEXT) {
                    String text = parser.getText() == null ? "" : parser.getText().trim();
                    if (text.isEmpty()) {
                        continue;
                    }
                    switch (tag) {
                        case "severity":
                            severity = text;
                            break;
                        case "event":
                            event = text;
                            break;
                        case "sender":
                        case "senderName":
                            sender = text;
                            break;
                        case "effective":
                            effective = text;
                            break;
                        case "expiry":
                            expiry = text;
                            break;
                        case "areaDesc":
                            if (area.length() > 0) {
                                area.append(", ");
                            }
                            area.append(text);
                            break;
                        default:
                            break;
                    }
                } else if (state == XmlPullParser.END_TAG) {
                    tag = "";
                }
                state = parser.next();
            }

            alert.severity = severityFromCap(severity);
            alert.event = event.isEmpty() ? alert.event : event;
            alert.source = sender.isEmpty() ? alert.source : sender;
            alert.effective = effective;
            alert.expiry = expiry;
            alert.area = area.length() > 0 ? area.toString() : alert.area;
        } catch (Exception malformed) {
            // Leave the alert without severity instead of inventing one.
        }
    }

    private static int severityFromCap(String severity) {
        if (severity == null) {
            return SEVERITY_UNKNOWN;
        }
        switch (severity.trim().toLowerCase(Locale.US)) {
            case "extreme":
                return SEVERITY_RED;
            case "severe":
                return SEVERITY_ORANGE;
            case "moderate":
                return SEVERITY_YELLOW;
            case "minor":
                return SEVERITY_GREEN;
            default:
                return SEVERITY_UNKNOWN;
        }
    }


    private void renderSachetFromCache() {
        String cached = preferences().getString(KEY_RSS_CACHE, null);
        if (cached == null || district.isEmpty()) {
            return;
        }
        List<Alert> all = parseRss(cached);
        // Severity and area come back from the cache before matching, so cached
        // area names also count for "is this alert for my district".
        restoreSeverity(all);
        List<Alert> matched = filterByDistrict(all, district);
        renderSachet(matched, true, preferences().getLong(KEY_RSS_CACHE_AT, 0L));
    }

    /**
     * Signature of the currently drawn alert panel. Re-rendering the same
     * content removes and re-adds every card, which makes the screen visually
     * jump; this lets us skip identical repaints entirely.
     */
    private String lastSachetSignature = null;

    private void renderSachet(List<Alert> alerts, boolean fromCache, long updatedAt) {
        StringBuilder signature = new StringBuilder(String.valueOf(alerts.size()));
        for (Alert alert : alerts) {
            signature.append('|')
                    .append(alert.severity).append('|')
                    .append(alert.title).append('|')
                    .append(alert.expiry).append('|')
                    .append(alert.source);
        }
        String signatureString = signature.toString();
        if (signatureString.equals(lastSachetSignature)) {
            // Same alert cards already drawn — only refresh the cached note.
            if (fromCache) {
                showCacheNote(updatedAt);
            } else {
                cachedText.setText("");
            }
            return;
        }
        lastSachetSignature = signatureString;
        sachetList.removeAllViews();
        if (alerts.isEmpty()) {
            if (!district.isEmpty()) {
                showStatus(getString(R.string.flood_alert_none, district));
            }
        } else {
            statusText.setText("");
        }
        for (int i = 0; i < Math.min(alerts.size(), MAX_SHOWN); i++) {
            sachetList.addView(buildAlertCard(alerts.get(i)));
        }
        if (fromCache) {
            showCacheNote(updatedAt);
        } else {
            cachedText.setText("");
        }
    }

    private View buildAlertCard(Alert alert) {
        int margin = dp(10);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackground(cardBackground());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = margin;
        card.setLayoutParams(params);

        TextView chip = new TextView(this);
        chip.setText(severityName(alert.severity));
        chip.setTextSize(12f);
        chip.setTextColor(severityTextColour(alert.severity));
        chip.setPadding(dp(10), dp(3), dp(10), dp(3));
        chip.setBackground(chipBackground(alert.severity));
        card.addView(chip);

        TextView title = new TextView(this);
        title.setText(alert.title);
        title.setTextSize(14f);
        title.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(6);
        title.setLayoutParams(titleParams);
        card.addView(title);

        List<String> facts = new ArrayList<>();
        if (alert.publishedAt > 0L) {
            facts.add(getString(R.string.flood_alert_issued, formatDateTime(alert.publishedAt)));
        }
        if (!alert.expiry.isEmpty()) {
            facts.add(getString(R.string.flood_alert_valid_until, alert.expiry));
        }
        if (!alert.source.isEmpty()) {
            facts.add(getString(R.string.flood_alert_source, alert.source));
        }
        if (alert.severity == SEVERITY_UNKNOWN) {
            facts.add(getString(R.string.flood_alert_severity_unknown));
        }
        if (!facts.isEmpty()) {
            TextView meta = new TextView(this);
            String joined = join(facts);
            meta.setText(joined);
            meta.setTextSize(12f);
            meta.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            metaParams.topMargin = dp(4);
            meta.setLayoutParams(metaParams);
            card.addView(meta);
        }

        return card;
    }

    private void showCacheNote(long updatedAt) {
        String freshness = isStale(updatedAt)
                ? getString(R.string.flood_alert_cache_old)
                : getString(R.string.flood_alert_cache_fresh);
        cachedText.setText(withUpdatedSuffix(freshness, updatedAt));
        cachedText.setVisibility(View.VISIBLE);
    }

    private String withUpdatedSuffix(String prefix, long updatedAt) {
        String when = updatedAt > 0L
                ? formatDateTime(updatedAt)
                : getString(R.string.flood_alert_severity_unknown);
        return getString(R.string.flood_alert_cached_note, prefix, when);
    }

    private static boolean isStale(long updatedAt) {
        return updatedAt <= 0L
                || System.currentTimeMillis() - updatedAt > 15 * 60 * 1000L;
    }

    private void showStatus(int messageRes) {
        statusText.setText(messageRes);
        statusText.setVisibility(View.VISIBLE);
    }

    private void showStatus(String message) {
        statusText.setText(message);
        statusText.setVisibility(View.VISIBLE);
    }

    // ----------------------------------------------------------- notifications

    /**
     * Announces alerts that have not been shown before. Shared with
     * FloodAlertJobService, so a warning is announced exactly once whether it
     * is found while FloodCue is open or by the background check.
     */
    private static void notifyNew(Context context,
                                  List<Alert> alerts,
                                  String districtName) {
        if (alerts.isEmpty()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(context,
                        Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission denied: do not fire notifications, but still update cache
            // so the screen can show what was found when permission is later granted.
            return;
        }
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        SharedPreferences prefs = preferences(context);
        LinkedHashSet<String> notified = new LinkedHashSet<>();
        // Older versions joined ids with " . "; split both ways so alerts
        // announced before that fix are still de-duplicated.
        for (String chunk : prefs.getString(KEY_NOTIFIED, "").split(",")) {
            for (String id : chunk.split("\u00b7")) {
                if (!id.trim().isEmpty()) {
                    notified.add(id.trim());
                }
            }
        }

        int sent = 0;
        for (Alert alert : alerts) {
            if (sent >= NOTIFY_PER_REFRESH) {
                break;
            }
            if (alert.id.isEmpty() || notified.contains(alert.id)) {
                continue;
            }
            notified.add(alert.id);
            sent++;
            try {
                manager.notify(alert.id.hashCode(),
                        buildNotification(context, alert, districtName));
            } catch (SecurityException noPermission) {
                // Permission revoked after starting; stop sending further notifications.
                return;
            }
        }
        if (sent == 0) {
            return;
        }
        while (notified.size() > MAX_NOTIFIED_IDS) {
            Iterator<String> iterator = notified.iterator();
            if (!iterator.hasNext()) {
                break;
            }
            iterator.next();
            iterator.remove();
        }
        StringBuilder stored = new StringBuilder();
        for (String id : notified) {
            if (stored.length() > 0) {
                stored.append(',');
            }
            stored.append(id);
        }
        prefs.edit().putString(KEY_NOTIFIED, stored.toString()).apply();
    }

    private static Notification buildNotification(Context context,
                                                  Alert alert,
                                                  String districtName) {
        Intent open = new Intent(context, FloodAlertActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        int pendingFlags = android.app.PendingIntent.FLAG_UPDATE_CURRENT
                | android.app.PendingIntent.FLAG_IMMUTABLE;
        android.app.PendingIntent contentIntent = android.app.PendingIntent.getActivity(
                context, 0, open, pendingFlags);
        android.app.PendingIntent fullScreenIntent = android.app.PendingIntent.getActivity(
                context, 1, open, pendingFlags);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(
                        R.string.flood_alert_notification_title, districtName))
                .setContentText(alert.title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(alert.title))
                .setTicker(alert.title)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSound(alarmSound(context))
                .setVibrate(ALERT_VIBRATION)
                .setFullScreenIntent(fullScreenIntent, true)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);
        if (alert.publishedAt > 0L) {
            builder.setWhen(alert.publishedAt);
        }
        return builder.build();
    }

    /**
     * Background check used by FloodAlertJobService while FloodCue is closed:
     * it reads the district chosen on this screen, refreshes the same cache and
     * announces through the same de-duplicated path, so the notification and
     * what the screen later shows always agree.
     */
    static void checkForNewAlerts(Context context) {
        // Background notifications must use FloodCue's app language, not the
        // phone's system language.
        context = LocaleManager.withAppLanguage(context);
        SharedPreferences prefs = preferences(context);
        String wanted = prefs.getString(KEY_DISTRICT, "").trim();
        if (wanted.isEmpty()) {
            return;
        }

        String rss;
        try {
            rss = httpGet(SACHET_RSS_URL, FETCH_TIMEOUT_MS);
        } catch (IOException noNetwork) {
            return;
        }
        if (rss == null) {
            return;
        }

        prefs.edit()
                .putString(KEY_RSS_CACHE, rss)
                .putLong(KEY_RSS_CACHE_AT, System.currentTimeMillis())
                .apply();
        ensureChannel(context);
        notifyNew(context, filterByDistrict(parseRss(rss), wanted), wanted);
    }

    // ----------------------------------------------------------------- helpers

    private void openLink(String address) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(address)));
        } catch (RuntimeException noBrowser) {
            Toast.makeText(this, R.string.flood_alert_open_failed, Toast.LENGTH_LONG).show();
        }
    }

    private GradientDrawable cardBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(ContextCompat.getColor(this, R.color.surface_elevated_color));
        background.setCornerRadius(dp(14));
        background.setStroke(dp(1), ContextCompat.getColor(this, R.color.border_color));
        return background;
    }

    private GradientDrawable chipBackground(int severity) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(severityColour(severity));
        background.setCornerRadius(dp(8));
        return background;
    }

    private int severityColour(int severity) {
        switch (severity) {
            case SEVERITY_RED:
                return ContextCompat.getColor(this, R.color.alert_red);
            case SEVERITY_ORANGE:
                return ContextCompat.getColor(this, R.color.alert_orange);
            case SEVERITY_YELLOW:
                return ContextCompat.getColor(this, R.color.alert_yellow);
            case SEVERITY_GREEN:
                return ContextCompat.getColor(this, R.color.alert_green);
            default:
                return ContextCompat.getColor(this, R.color.alert_unknown);
        }
    }

    private int severityTextColour(int severity) {
        switch (severity) {
            case SEVERITY_RED:
                return ContextCompat.getColor(this, R.color.alert_red_text);
            case SEVERITY_ORANGE:
                return ContextCompat.getColor(this, R.color.alert_orange_text);
            case SEVERITY_YELLOW:
                return ContextCompat.getColor(this, R.color.alert_yellow_text);
            case SEVERITY_GREEN:
                return ContextCompat.getColor(this, R.color.alert_green_text);
            default:
                return ContextCompat.getColor(this, R.color.alert_unknown_text);
        }
    }

    private String severityName(int severity) {
        switch (severity) {
            case SEVERITY_RED:
                // Fall back to an English literal: a locale may not define this.
                return getStringOrDefault(R.string.flood_alert_severity_red,
                        getString(R.string.flood_alert_severity_red));
            case SEVERITY_ORANGE:
                return getStringOrDefault(R.string.flood_alert_severity_orange,
                        getString(R.string.flood_alert_severity_orange));
            case SEVERITY_YELLOW:
                return getStringOrDefault(R.string.flood_alert_severity_yellow,
                        getString(R.string.flood_alert_severity_yellow));
            case SEVERITY_GREEN:
                return getStringOrDefault(R.string.flood_alert_severity_green,
                        getString(R.string.flood_alert_severity_green));
            default:
                return getStringOrDefault(R.string.flood_alert_severity_unknown,
                        getString(R.string.flood_alert_severity_unknown));
        }
    }

    /** Returns the string, or a literal fallback when the locale is missing it. */
    private String getStringOrDefault(int resId, String fallback) {
        try {
            return getString(resId);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private String formatDateTime(long timestamp) {
        return new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                .format(new Date(timestamp));
    }

    private static String join(List<String> parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" · ");
            }
            builder.append(part);
        }
        return builder.toString();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // ---------------------------------------------- severity cache for offline

    private void saveSeverityCache(List<Alert> alerts) {
        try {
            JSONObject cache = new JSONObject(preferences().getString(KEY_SEVERITY_CACHE, "{}"));
            for (Alert alert : alerts) {
                if (alert.id.isEmpty()) {
                    continue;
                }
                JSONObject entry = new JSONObject();
                entry.put("s", alert.severity);
                entry.put("a", alert.area);
                entry.put("e", alert.effective);
                entry.put("x", alert.expiry);
                entry.put("u", alert.source);
                cache.put(alert.id, entry);
            }
            while (cache.length() > MAX_NOTIFIED_IDS) {
                Iterator<String> keys = cache.keys();
                if (!keys.hasNext()) {
                    break;
                }
                cache.remove(keys.next());
            }
            preferences().edit().putString(KEY_SEVERITY_CACHE, cache.toString()).apply();
        } catch (Exception ignored) {
            // A cache problem must never break the screen.
        }
    }

    private void restoreSeverity(List<Alert> alerts) {
        try {
            JSONObject cache = new JSONObject(preferences().getString(KEY_SEVERITY_CACHE, "{}"));
            for (Alert alert : alerts) {
                JSONObject entry = cache.optJSONObject(alert.id);
                if (entry == null) {
                    continue;
                }
                alert.severity = entry.optInt("s", SEVERITY_UNKNOWN);
                alert.area = entry.optString("a", alert.area);
                alert.effective = entry.optString("e", alert.effective);
                alert.expiry = entry.optString("x", alert.expiry);
                alert.source = entry.optString("u", alert.source);
            }
        } catch (Exception ignored) {
            // Show the alerts without cached severity.
        }
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(locationTimeout);
        removeActiveListener();
        super.onDestroy();
    }
}
