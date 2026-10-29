package com.floodcue.app;

import android.app.Application;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Re-arms the queued SOS on every process start, so an emergency message that
 * was stored while offline is still sent automatically after the app process
 * is restarted by the system or by the user.
 */
public class FloodCueApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        // The application context (notifications, queues, services) uses the
        // app language too; screens apply their own locale in FloodCueActivity.
        super.attachBaseContext(LocaleManager.withAppLanguage(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        );
        SosQueue.resumeIfPending(this);
        // Keep checking official flood warnings while FloodCue is closed, so
        // an alert is announced on the screen even if the app is not open.
        FloodAlertJobService.schedule(this);
        // A language change that could not reach the server is sent now.
        LanguageSync.retryIfPending(this);
    }
}
