package com.floodcue.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Fired by the system alarm armed while an SOS is queued. It restarts the app
 * process if necessary and resumes the automatic send, so a queued SOS still
 * goes out when the network returns even if Android killed the app meanwhile.
 */
public class SosRetryReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !SosQueue.ACTION_SOS_RETRY.equals(intent.getAction())) {
            return;
        }
        SosQueue.resumeIfPending(context.getApplicationContext());
    }
}
