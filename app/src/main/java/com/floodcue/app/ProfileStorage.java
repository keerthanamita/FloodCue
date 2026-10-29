
package com.floodcue.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

public final class ProfileStorage {

    private static final String PREFS_NAME = "FloodCuePrefs";
    private static final String PROFILE_PREFIX = "user_profile_";
    private static final String SETUP_PREFIX = "setup_completed_";

    private ProfileStorage() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );
    }

    private static String profileKey(String uid) {
        return PROFILE_PREFIX + uid;
    }

    private static String setupKey(String uid) {
        return SETUP_PREFIX + uid;
    }

    public static void saveProfile(
            Context context,
            String uid,
            UserProfile profile
    ) throws JSONException {
        if (uid == null || uid.trim().isEmpty() || profile == null) {
            throw new IllegalArgumentException("UID and profile are required");
        }

        prefs(context).edit()
                .putString(profileKey(uid), profile.toJson().toString())
                .putBoolean(setupKey(uid), profile.setup_completed)
                .apply();
    }

    public static UserProfile getProfile(Context context, String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return null;
        }

        String stored = prefs(context).getString(profileKey(uid), null);

        if (stored == null || stored.trim().isEmpty()) {
            return null;
        }

        try {
            return UserProfile.fromJson(new JSONObject(stored));
        } catch (JSONException e) {
            return null;
        }
    }

    public static boolean isSetupCompleted(Context context, String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return false;
        }

        return prefs(context).getBoolean(setupKey(uid), false);
    }

    public static void clearProfile(Context context, String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return;
        }

        prefs(context).edit()
                .remove(profileKey(uid))
                .remove(setupKey(uid))
                .apply();
    }
}
