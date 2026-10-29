package com.floodcue.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Shared pieces of the one-tap SOS: who receives the message, what it says and
 * how the current position is read.
 *
 * Used by the red home SOS button and by {@link SosEmergencyActivity} so both
 * paths message exactly the same people:
 *
 * <ul>
 *  <li>the user's personal emergency contacts saved during profile setup, and</li>
 *  <li>the numbers the user chose on the SOS Emergency Message screen.</li>
 * </ul>
 *
 * Nothing here contacts a server; the message leaves the device as SMS only.
 */
final class SosQuickSend {

    /** Must stay in sync with {@code SosActivity.DRAFT_NUMBERS_KEY}. */
    static final String DRAFT_NUMBERS_KEY = "sos_draft_numbers";

    private static final String PREFS_NAME = "FloodCuePrefs";

    private static final String[] PERSONAL_CONTACT_KEYS = {
            "primary_contact_phone",
            "primary_contact_alt_phone",
            "second_contact_phone",
            "second_contact_alt_phone",
            "third_contact_phone",
            "third_contact_alt_phone"
    };

    private SosQuickSend() {
    }

    /** Personal emergency contacts plus the numbers picked for SOS messages. */
    static Set<String> recipients(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        LinkedHashSet<String> numbers = new LinkedHashSet<>();
        for (String key : PERSONAL_CONTACT_KEYS) {
            addNumber(numbers, prefs.getString(key, null));
        }
        for (String raw : prefs.getString(DRAFT_NUMBERS_KEY, "").split(",")) {
            addNumber(numbers, raw);
        }
        return numbers;
    }

    private static void addNumber(Set<String> numbers, String raw) {
        if (raw == null) {
            return;
        }
        String value = raw.trim();
        if (!value.isEmpty()) {
            numbers.add(value);
        }
    }

    /**
     * The message body. When a location is available it carries the coordinate
     * and the time it was recorded; otherwise it says so plainly instead of
     * inventing a position.
     */
    static String buildMessage(Context context, Location location) {
        Context survivalContext = LocaleManager.withSurvivalLanguage(context);
        StringBuilder body = new StringBuilder(
                survivalContext.getString(R.string.sos_emergency_sms_body));
        if (location == null) {
            body.append("\n\n")
                    .append(survivalContext.getString(R.string.sos_no_gps_coordinates));
            return body.toString();
        }
        body.append("\n\n")
                .append(survivalContext.getString(R.string.sos_my_location))
                .append(": ")
                .append(String.format(Locale.US, "%.6f", location.getLatitude()))
                .append(", ")
                .append(String.format(Locale.US, "%.6f", location.getLongitude()))
                .append(" (")
                .append(new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                        .format(new Date(location.getTime())))
                .append(")")
                .append("\nhttps://maps.google.com/maps?q=")
                .append(location.getLatitude())
                .append(",")
                .append(location.getLongitude());
        return body.toString();
    }

    /** Newest fix the device already knows about, or null when none exists. */
    static Location bestLastKnown(Context context, boolean permissionGranted) {
        if (!permissionGranted) {
            return null;
        }
        LocationManager manager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (manager == null) {
            return null;
        }
        Location best = null;
        for (String provider : new String[]{
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER}) {
            try {
                if (!manager.isProviderEnabled(provider)) {
                    continue;
                }
                Location candidate = manager.getLastKnownLocation(provider);
                if (candidate != null && candidate.getTime() > 0
                        && (best == null || candidate.getTime() > best.getTime())) {
                    best = candidate;
                }
            } catch (SecurityException | IllegalArgumentException ignored) {
                // Provider unavailable or permission revoked: try the next one.
            }
        }
        return best;
    }

    /** A provider that can still produce a fresh fix, or null if none is on. */
    static String enabledProvider(Context context) {
        LocationManager manager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (manager == null) {
            return null;
        }
        for (String provider : new String[]{
                LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
            try {
                if (manager.isProviderEnabled(provider)) {
                    return provider;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }
}
