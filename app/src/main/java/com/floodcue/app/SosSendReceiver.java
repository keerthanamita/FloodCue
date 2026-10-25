package com.floodcue.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receives the per-number send results from the telephony stack and feeds them
 * back into {@link SosQueue}, which marks confirmed numbers as sent and keeps
 * failed ones queued for an automatic retry. It never sends anything itself.
 */
public class SosSendReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !SosQueue.ACTION_SOS_SENT.equals(intent.getAction())) {
            return;
        }
        String number = intent.getStringExtra(SosQueue.EXTRA_NUMBER);
        if (number == null || number.isEmpty()) {
            return;
        }
        SosQueue.recordResult(context.getApplicationContext(), number, getResultCode());
    }
}
