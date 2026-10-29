package com.floodcue.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Resumes a queued SOS after the phone reboots: without this the queue would
 * wait until the user happened to open the app again.
 */
public class SosBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        // BOOT_COMPLETED arrives after first unlock, when our prefs are readable.
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SosQueue.resumeIfPending(context.getApplicationContext());
        }
    }
}
