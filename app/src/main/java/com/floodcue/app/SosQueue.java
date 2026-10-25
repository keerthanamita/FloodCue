package com.floodcue.app;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.telephony.SmsManager;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores exactly one SOS message locally and sends it from this device as soon
 * as a network is available.
 *
 * Behaviour:
 * - The queue is only created after the user explicitly confirms sending.
 * - Each recipient gets its own SMS; a number is marked sent only after the
 *   radio confirms it left this device, so retries never resend a number that
 *   already succeeded.
 * - If sending fails (no service, radio off, process restart), the SOS stays
 *   queued and is retried automatically when connectivity changes or when the
 *   app is opened again, at most once per cooldown window.
 * - Nothing is ever uploaded to a server and nothing is written to logs.
 */
public final class SosQueue {

    public enum State {
        EMPTY,
        WAITING,
        SENDING,
        COMPLETE
    }

    public static final String ACTION_SOS_SENT = "com.floodcue.app.SOS_SENT_RESULT";
    /** System alarm that re-runs the queue even after Android kills this process. */
    static final String ACTION_SOS_RETRY = "com.floodcue.app.SOS_RETRY";
    public static final String EXTRA_NUMBER = "floodcue.sos.NUMBER";

    private static final String PREFS_NAME = "FloodCuePrefs";
    private static final String KEY_RECIPIENTS = "sos_queue_recipients";
    private static final String KEY_MESSAGE = "sos_queue_message";
    private static final String KEY_CREATED_AT = "sos_queue_created_at";
    private static final String KEY_SENT_NUMBERS = "sos_queue_sent_numbers";
    private static final String KEY_COMPLETED_AT = "sos_queue_completed_at";
    private static final String KEY_LAST_ATTEMPT_AT = "sos_queue_last_attempt_at";
    private static final String KEY_LAST_ERROR = "sos_queue_last_error";

    /** Minimum gap between automatic send attempts, so retries cannot spam the radio. */
    private static final long RETRY_COOLDOWN_MS = 90_000L;
    /** How long after an attempt the state is reported as "sending". */
    private static final long SENDING_WINDOW_MS = 20_000L;
    /** An in-flight number whose radio never answered is retried after this. */
    private static final long STALE_AWAITING_MS = 120_000L;

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final Object lock = new Object();

    /** number -> parts of that message still awaiting a result from the radio. */
    private static final Map<String, Integer> awaitingResults = new HashMap<>();
    /** number -> time of the attempt that is still awaiting a radio result. */
    private static final Map<String, Long> awaitingSince = new HashMap<>();
    /** Numbers whose last attempt failed; cleared when connectivity changes. */
    private static final Set<String> failedNumbers = new LinkedHashSet<>();

    private static ConnectivityManager.NetworkCallback networkCallback;
    private static ConnectivityManager watchedManager;
    private static Runnable statusListener;
    private static boolean retryScheduled;

    private SosQueue() {
    }

    /** Called by the SOS screen while it is visible; pass null when it pauses. */
    static void setStatusListener(Runnable listener) {
        statusListener = listener;
    }

    // ---------------------------------------------------------------- state

    static State getState(Context context) {
        SharedPreferences prefs = preferences(context);
        String raw = prefs.getString(KEY_RECIPIENTS, "");
        if (raw.isEmpty()) {
            return State.EMPTY;
        }
        Set<String> recipients = split(raw);
        Set<String> sent = split(prefs.getString(KEY_SENT_NUMBERS, ""));
        if (sent.containsAll(recipients)) {
            return State.COMPLETE;
        }
        long lastAttempt = prefs.getLong(KEY_LAST_ATTEMPT_AT, 0L);
        if (System.currentTimeMillis() - lastAttempt < SENDING_WINDOW_MS) {
            return State.SENDING;
        }
        return State.WAITING;
    }

    static boolean hasPending(Context context) {
        State state = getState(context);
        return state == State.WAITING || state == State.SENDING;
    }

    static boolean isComplete(Context context) {
        return getState(context) == State.COMPLETE;
    }

    static int getSentCount(Context context) {
        SharedPreferences prefs = preferences(context);
        Set<String> recipients = split(prefs.getString(KEY_RECIPIENTS, ""));
        Set<String> sent = split(prefs.getString(KEY_SENT_NUMBERS, ""));
        int count = 0;
        for (String number : recipients) {
            if (sent.contains(number)) {
                count++;
            }
        }
        return count;
    }

    static int getTotalCount(Context context) {
        return split(preferences(context).getString(KEY_RECIPIENTS, "")).size();
    }

    static long getCompletedAt(Context context) {
        return preferences(context).getLong(KEY_COMPLETED_AT, 0L);
    }

    /**
     * SmsManager error code of the most recent failed attempt (0 = none).
     * Used to tell the user exactly why nothing was sent yet.
     */
    static int getLastError(Context context) {
        return preferences(context).getInt(KEY_LAST_ERROR, 0);
    }

    // ---------------------------------------------------------------- writes

    /** Stores the SOS locally and starts the automatic send right away. */
    static void enqueue(Context context, List<String> recipients, String message) {
        Context app = context.getApplicationContext();
        preferences(app).edit()
                .putString(KEY_RECIPIENTS, String.join(",", recipients))
                .putString(KEY_MESSAGE, message)
                .putLong(KEY_CREATED_AT, System.currentTimeMillis())
                .putString(KEY_SENT_NUMBERS, "")
                .putLong(KEY_COMPLETED_AT, 0L)
                .putLong(KEY_LAST_ATTEMPT_AT, 0L)
                .putInt(KEY_LAST_ERROR, 0)
                .apply();
        synchronized (lock) {
            awaitingResults.clear();
            awaitingSince.clear();
            failedNumbers.clear();
        }
        notifyStatus();
        attemptSend(app);
    }

    /** Removes the queued SOS. Nothing already handed to the radio can be recalled. */
    static void clear(Context context) {
        Context app = context.getApplicationContext();
        synchronized (lock) {
            awaitingResults.clear();
            awaitingSince.clear();
            failedNumbers.clear();
        }
        preferences(app).edit()
                .remove(KEY_RECIPIENTS)
                .remove(KEY_MESSAGE)
                .remove(KEY_CREATED_AT)
                .remove(KEY_SENT_NUMBERS)
                .remove(KEY_COMPLETED_AT)
                .remove(KEY_LAST_ATTEMPT_AT)
                .remove(KEY_LAST_ERROR)
                .apply();
        stopWatching();
        updateAlarm(app);
        notifyStatus();
    }

    /** Called when the app process starts with a queued SOS still pending. */
    static void resumeIfPending(Context context) {
        Context app = context.getApplicationContext();
        if (!hasPending(app)) {
            return;
        }
        synchronized (lock) {
            failedNumbers.clear();
        }
        attemptSend(app);
    }

    /** Called when the user reopens the SOS screen: allow one more automatic attempt. */
    static void resumeRetry(Context context) {
        Context app = context.getApplicationContext();
        if (getState(app) == State.EMPTY) {
            return;
        }
        synchronized (lock) {
            failedNumbers.clear();
        }
        attemptSend(app);
    }

    // ---------------------------------------------------------------- sending

    /**
     * Tries to send every number of the queued SOS that has not been confirmed
     * yet. Safe to call from anywhere on the main thread; the cooldown and the
     * in-flight map make duplicate sends impossible.
     */
    private static void attemptSend(Context app) {
        SharedPreferences prefs = preferences(app);
        String rawRecipients = prefs.getString(KEY_RECIPIENTS, "");
        String message = prefs.getString(KEY_MESSAGE, "");
        Set<String> recipients = split(rawRecipients);
        if (recipients.isEmpty() || message.trim().isEmpty()) {
            stopWatching();
            updateAlarm(app);
            return;
        }

        if (!hasSmsPermission(app)) {
            // The user denied automatic sending; the SMS-app fallback is used instead.
            stopWatching();
            updateAlarm(app);
            notifyStatus();
            return;
        }

        Set<String> sent = split(prefs.getString(KEY_SENT_NUMBERS, ""));
        if (sent.containsAll(recipients)) {
            onAllSent(prefs, recipients);
            stopWatching();
            updateAlarm(app);
            return;
        }

        // An in-flight number whose radio never answered must not block forever.
        long staleNow = System.currentTimeMillis();
        synchronized (lock) {
            Iterator<Map.Entry<String, Integer>> iterator = awaitingResults.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Integer> entry = iterator.next();
                Long since = awaitingSince.get(entry.getKey());
                if (since == null || staleNow - since > STALE_AWAITING_MS) {
                    iterator.remove();
                    awaitingSince.remove(entry.getKey());
                }
            }
        }

        List<String> pendingNumbers = new ArrayList<>();
        synchronized (lock) {
            for (String number : recipients) {
                if (!sent.contains(number)
                        && !awaitingResults.containsKey(number)
                        && !failedNumbers.contains(number)) {
                    pendingNumbers.add(number);
                }
            }
        }

        if (pendingNumbers.isEmpty()) {
            // Everything is either in flight or waiting for the next connectivity event.
            ensureWatching(app);
            updateAlarm(app);
            return;
        }

        long now = System.currentTimeMillis();
        long lastAttempt = prefs.getLong(KEY_LAST_ATTEMPT_AT, 0L);
        long sinceAttempt = now - lastAttempt;
        if (lastAttempt > 0 && sinceAttempt < RETRY_COOLDOWN_MS) {
            scheduleRetry(app, RETRY_COOLDOWN_MS - sinceAttempt);
            ensureWatching(app);
            updateAlarm(app);
            return;
        }

        prefs.edit().putLong(KEY_LAST_ATTEMPT_AT, now).apply();
        notifyStatus();
        ensureWatching(app);

        SmsManager sms = SmsManager.getDefault();
        for (String number : pendingNumbers) {
            try {
                List<String> divided = sms.divideMessage(message);
                if (divided.isEmpty()) {
                    continue;
                }
                ArrayList<String> parts = new ArrayList<>(divided);
                PendingIntent sentIntent = sentIntentFor(app, number);
                synchronized (lock) {
                    awaitingResults.put(number, parts.size());
                    awaitingSince.put(number, System.currentTimeMillis());
                    failedNumbers.remove(number);
                }
                if (parts.size() > 1) {
                    ArrayList<PendingIntent> sentIntents =
                            new ArrayList<>(Collections.nCopies(parts.size(), sentIntent));
                    sms.sendMultipartTextMessage(number, null, parts, sentIntents, null);
                } else {
                    sms.sendTextMessage(number, null, message, sentIntent, null);
                }
            } catch (Exception securityOrRadioProblem) {
                // Missing permission or radio unavailable: keep the SOS queued.
                synchronized (lock) {
                    awaitingResults.remove(number);
                    awaitingSince.remove(number);
                    failedNumbers.add(number);
                }
            }
        }
        updateAlarm(app);
    }

    /** Called by {@link SosSendReceiver} for every per-number send result. */
    static void recordResult(Context context, String number, int resultCode) {
        Context app = context.getApplicationContext();
        boolean confirmed = false;
        synchronized (lock) {
            if (resultCode == android.app.Activity.RESULT_OK) {
                Integer remaining = awaitingResults.get(number);
                if (remaining == null || remaining <= 1) {
                    awaitingResults.remove(number);
                    awaitingSince.remove(number);
                    confirmed = true;
                } else {
                    awaitingResults.put(number, remaining - 1);
                }
                failedNumbers.remove(number);
            } else {
                // No service / radio off / generic failure: keep it queued for retry.
                awaitingResults.remove(number);
                awaitingSince.remove(number);
                failedNumbers.add(number);
                preferences(app).edit().putInt(KEY_LAST_ERROR, resultCode).apply();
            }
        }

        if (confirmed) {
            markSent(app, number);
        } else {
            // Failed attempt: come back after the cooldown even if no
            // connectivity callback ever fires (e.g. radio was off).
            scheduleRetry(app, RETRY_COOLDOWN_MS);
        }
        updateAlarm(app);
        notifyStatus();
    }

    private static void markSent(Context app, String number) {
        SharedPreferences prefs = preferences(app);
        Set<String> recipients = split(prefs.getString(KEY_RECIPIENTS, ""));
        if (recipients.isEmpty() || !recipients.contains(number)) {
            return;
        }
        Set<String> sent = split(prefs.getString(KEY_SENT_NUMBERS, ""));
        if (sent.add(number)) {
            SharedPreferences.Editor editor = prefs.edit().putString(
                    KEY_SENT_NUMBERS, String.join(",", sent));
            if (sent.containsAll(recipients) && prefs.getLong(KEY_COMPLETED_AT, 0L) == 0L) {
                editor.putLong(KEY_COMPLETED_AT, System.currentTimeMillis());
                editor.putInt(KEY_LAST_ERROR, 0);
            }
            editor.apply();
        }
        synchronized (lock) {
            awaitingResults.remove(number);
            awaitingSince.remove(number);
            failedNumbers.remove(number);
        }
    }

    private static void onAllSent(SharedPreferences prefs, Set<String> recipients) {
        if (prefs.getLong(KEY_COMPLETED_AT, 0L) == 0L) {
            prefs.edit().putLong(KEY_COMPLETED_AT, System.currentTimeMillis()).apply();
        }
        synchronized (lock) {
            awaitingResults.clear();
            awaitingSince.clear();
            failedNumbers.clear();
        }
        notifyStatus();
    }

    // --------------------------------------------------------- connectivity

    private static void ensureWatching(Context app) {
        if (networkCallback != null) {
            return;
        }
        ConnectivityManager manager =
                (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return;
        }
        try {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    // Connectivity changed: failed numbers may succeed now.
                    mainHandler.post(() -> {
                        synchronized (lock) {
                            failedNumbers.clear();
                        }
                        attemptSend(app);
                    });
                }
            };
            manager.registerDefaultNetworkCallback(networkCallback);
            watchedManager = manager;
        } catch (RuntimeException notAvailable) {
            networkCallback = null;
            watchedManager = null;
        }
    }

    private static void stopWatching() {
        if (networkCallback == null) {
            return;
        }
        if (watchedManager != null) {
            try {
                watchedManager.unregisterNetworkCallback(networkCallback);
            } catch (RuntimeException ignored) {
            }
        }
        networkCallback = null;
        watchedManager = null;
    }

    /**
     * Arms a system alarm for the next retry. Unlike the in-process handler and
     * the network callback, an AlarmManager alarm survives Android killing this
     * process, so a queued SOS still goes out when the network comes back —
     * even if the user never reopens the app.
     */
    private static void updateAlarm(Context app) {
        AlarmManager manager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            return;
        }
        PendingIntent retryIntent = retryPendingIntent(app);
        try {
            manager.cancel(retryIntent);
            if (hasPending(app)) {
                manager.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME,
                        SystemClock.elapsedRealtime() + RETRY_COOLDOWN_MS,
                        retryIntent
                );
            }
        } catch (RuntimeException ignored) {
            // Some OEMs restrict alarms; the in-process paths still apply.
        }
    }

    private static PendingIntent retryPendingIntent(Context app) {
        Intent intent = new Intent(ACTION_SOS_RETRY).setClass(app, SosRetryReceiver.class);
        return PendingIntent.getBroadcast(
                app,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static void scheduleRetry(Context app, long delayMs) {
        if (retryScheduled) {
            return;
        }
        retryScheduled = true;
        mainHandler.postDelayed(() -> {
            retryScheduled = false;
            synchronized (lock) {
                // The cooldown has passed, so previous failures may be retried.
                failedNumbers.clear();
            }
            attemptSend(app);
        }, Math.max(delayMs, 1_000L));
    }

    private static PendingIntent sentIntentFor(Context app, String number) {
        Intent intent = new Intent(ACTION_SOS_SENT)
                .setClass(app, SosSendReceiver.class)
                .putExtra(EXTRA_NUMBER, number);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Mutable so the telephony stack can attach its result extras.
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        return PendingIntent.getBroadcast(app, number.hashCode(), intent, flags);
    }

    // -------------------------------------------------------------- helpers

    static boolean hasSmsPermission(Context context) {
        return ContextCompat.checkSelfPermission(
                context, Manifest.permission.SEND_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static Set<String> split(String raw) {
        Set<String> values = new LinkedHashSet<>();
        if (raw == null || raw.trim().isEmpty()) {
            return values;
        }
        for (String part : raw.split(",")) {
            if (!part.trim().isEmpty()) {
                values.add(part.trim());
            }
        }
        return values;
    }

    private static void notifyStatus() {
        Runnable listener = statusListener;
        if (listener != null) {
            mainHandler.post(listener);
        }
    }
}
