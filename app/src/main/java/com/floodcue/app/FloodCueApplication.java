package com.floodcue.app;

import android.app.Application;

/**
 * Re-arms the queued SOS on every process start, so an emergency message that
 * was stored while offline is still sent automatically after the app process
 * is restarted by the system or by the user.
 */
public class FloodCueApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SosQueue.resumeIfPending(this);
        // Keep checking official flood warnings while FloodCue is closed, so
        // an alert is announced on the screen even if the app is not open.
        FloodAlertJobService.schedule(this);
    }
}
