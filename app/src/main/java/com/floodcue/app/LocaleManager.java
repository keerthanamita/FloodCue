package com.floodcue.app;

import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;

import java.util.Locale;

/**
 * Two independent languages, chosen by the user at first launch and changeable
 * later from Settings:
 *
 *  - app language      -> everything that is part of FloodCue itself: buttons,
 *                         menus, settings, SOS and weather screens.
 *  - survival language -> the safety content the user reads under pressure:
 *                         flood checklist, safety tips and the emergency guide.
 *
 * Android only has one locale per process, so each screen asks for the locale
 * it needs in attachBaseContext (see FloodCueActivity) and gets its own
 * configuration context. Screens that show survival content pass
 * {@link #usesSurvivalLanguage()} = true and resolve their strings in the
 * survival language, while every other screen uses the app language.
 *
 * Untranslated items fall back to English, so a language can be added to
 * res/values-XX/ at any time without touching code.
 */
public final class LocaleManager {

    public static final String PREFS_NAME = "FloodCuePrefs";
    public static final String KEY_APP_LANGUAGE = "app_language";
    public static final String KEY_SURVIVAL_LANGUAGE = "survival_language";
    public static final String DEFAULT_LANGUAGE = "en";

    private LocaleManager() {
    }

    /** The language chosen for FloodCue's own interface. */
    public static String appLanguage(Context context) {
        return language(context, KEY_APP_LANGUAGE);
    }

    /** The language chosen for survival information. */
    public static String survivalLanguage(Context context) {
        return language(context, KEY_SURVIVAL_LANGUAGE);
    }

    private static String language(Context context, String key) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(key, DEFAULT_LANGUAGE);
    }

    /** Wraps a context so its resources resolve in the given language. */
    public static Context wrap(Context base, String languageCode) {
        Locale locale = localeFor(languageCode);
        LocaleList locales = LocaleList.forLanguageTags(locale.toLanguageTag());

        Configuration configuration =
                new Configuration(base.getResources().getConfiguration());
        configuration.setLocale(locale);
        configuration.setLocales(locales);
        configuration.setLayoutDirection(locale);

        return base.createConfigurationContext(configuration);
    }

    /** Wraps a context with FloodCue's app language (application context use). */
    public static Context withAppLanguage(Context base) {
        return wrap(base, appLanguage(base));
    }

    /** Wraps a context with the survival language (content screens). */
    public static Context withSurvivalLanguage(Context base) {
        return wrap(base, survivalLanguage(base));
    }

    private static Locale localeFor(String languageCode) {
        if (languageCode == null || languageCode.trim().isEmpty()) {
            languageCode = DEFAULT_LANGUAGE;
        }
        Locale locale = Locale.forLanguageTag(languageCode);
        if (locale.getLanguage().isEmpty()) {
            locale = Locale.forLanguageTag(DEFAULT_LANGUAGE);
        }
        return locale;
    }
}
