package com.floodcue.app;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Every FloodCue screen extends this so it opens in the language the user
 * picked: the app language for normal screens, the survival language for the
 * safety-content screens (see {@link #usesSurvivalLanguage()}).
 *
 * The locale is applied in attachBaseContext, before any view is inflated, so
 * layout XML text, getString() calls and programmatically built views all
 * agree. Changing a language in Settings restarts the app, which re-creates
 * every screen with the new locale.
 */
public class FloodCueActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(usesSurvivalLanguage()
                ? LocaleManager.withSurvivalLanguage(newBase)
                : LocaleManager.withAppLanguage(newBase));
    }

    /**
     * True for screens that show survival information (checklist, tips,
     * emergency guide). Those follow the survival language; everything else
     * follows the app language.
     */
    protected boolean usesSurvivalLanguage() {
        return false;
    }
}
