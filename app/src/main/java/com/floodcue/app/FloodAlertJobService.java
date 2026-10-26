package com.floodcue.app;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

/**
 * Keeps checking the flood warnings while FloodCue is not running, so the user
 * is alerted on the lock screen / over other apps instead of only when they
 * open the app.
 *
 * The check re-uses FloodAlertActivity.checkForNewAlerts: same district, same
 * cache, same notification channel, and the same once-per-alert de-duplication
 * the screen uses, so a warning is never announced twice.
 *
 * It runs on Wi-Fi or mobile data only (no location, no charging requirement),
 * roughly every 15 minutes while the device is online. An alarm-manager exact
 * alarm was deliberately avoided: Android 12+ requires a special permission
 * for those, and this app has no need to be listed among alarm apps.
 *
 * The check is best-effort: when there is no connectivity the job simply does
 * nothing, the job ends, and the next one runs on schedule. FloodCue never
 * claims an alert is live unless the official feed actually returned one.
 */
public class FloodAlertJobService extends JobService {

    private static final int JOB_ID = 7401;
    private static final long PERIOD_MS = 15 * 60 * 1000L;

    /** Called from FloodCueApplication so the check survives app restarts. */
    static void schedule(Context context) {
        JobScheduler scheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler == null) {
            return;
        }
        // Check whether this exact job is already pending before adding it.
        for (JobInfo existing : scheduler.getAllPendingJobs()) {
            if (existing.getId() == JOB_ID) {
                return;
            }
        }
        JobInfo job = new JobInfo.Builder(JOB_ID,
                new ComponentName(context, FloodAlertJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD_MS)
                .setPersisted(true) // survives a reboot (needs RECEIVE_BOOT_COMPLETED)
                .build();
        try {
            scheduler.schedule(job);
        } catch (SecurityException unsupported) {
            // Some devices restrict background jobs; the in-app refresh still works.
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        new Thread(() -> {
            try {
                FloodAlertActivity.checkForNewAlerts(getApplicationContext());
            } catch (RuntimeException ignored) {
                // Never crash the job; the next scheduled run will try again.
            }
            jobFinished(params, false);
        }, "flood-alert-check").start();
        // The check is short-lived, so the work is reported as still ongoing.
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        // No partial state to keep: the check either finished and notified, or not.
        return false;
    }
}
