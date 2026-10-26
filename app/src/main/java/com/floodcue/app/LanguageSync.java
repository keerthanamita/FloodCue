package com.floodcue.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;


import com.google.firebase.auth.FirebaseAuth;

/**
 * Keeps the chosen app language and survival language on the server as well as
 * on this phone, so the same account gets the same languages on another device.
 *
 * Saving is best-effort: the local copy is always written first, the server is
 * told afterwards, and a failed attempt is remembered so the next app start
 * tries again. Nothing here blocks the user or repeats a save on its own.
 */
public final class LanguageSync {

    private static final String KEY_PENDING = "language_push_pending";

    private LanguageSync() {
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(LocaleManager.PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Sends both languages to the server. Call after the user changes them.
     * Safe to call when signed out: the profile setup saves the languages too.
     */
    public static void pushNow(Context context) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // No account yet; SetupActivity stores the languages with the profile.
            return;
        }
        preferences(context).edit().putBoolean(KEY_PENDING, true).apply();
        push(context, preferences(context));
    }

    /** Called on every app start: retries only if an earlier push failed. */
    public static void retryIfPending(Context context) {
        SharedPreferences prefs = preferences(context);
        if (!prefs.getBoolean(KEY_PENDING, false)) {
            return;
        }
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }
        push(context, prefs);
    }

    private static void push(Context context, SharedPreferences prefs) {
        String appLanguage = prefs.getString(LocaleManager.KEY_APP_LANGUAGE,
                LocaleManager.DEFAULT_LANGUAGE);
        String survivalLanguage = prefs.getString(LocaleManager.KEY_SURVIVAL_LANGUAGE,
                LocaleManager.DEFAULT_LANGUAGE);

        ProfileApi.fetchProfile(new ProfileApi.FetchCallback() {
            @Override
            public void onResult(boolean success, boolean exists,
                                  UserProfile profile, String error) {
                if (!success || profile == null) {
                    return; // offline or not signed in yet; the retry flag stays set
                }
                if (!exists) {
                    // No server profile yet: profile setup stores the languages.
                    prefs.edit().putBoolean(KEY_PENDING, false).apply();
                    return;
                }

                profile.app_language = appLanguage;
                profile.survival_language = survivalLanguage;

                ProfileApi.saveProfile(profile, new ProfileApi.SaveCallback() {
                    @Override
                    public void onResult(boolean success2,
                                         UserProfile savedProfile, String error2) {
                        if (success2) {
                            prefs.edit().putBoolean(KEY_PENDING, false).apply();
                            return;
                        }
                        if ("setup_not_completed".equals(error2)) {
                            // Profile setup will store the languages itself.
                            prefs.edit().putBoolean(KEY_PENDING, false).apply();
                            return;
                        }
                        // Kept pending; retried on the next app start.
                    }
                });
            }
        });
    }

    /** Short confirmation shown after a language change. */
    public static void showSavedMessage(Context context) {
        Toast.makeText(context, R.string.language_updated, Toast.LENGTH_SHORT).show();
    }
}
