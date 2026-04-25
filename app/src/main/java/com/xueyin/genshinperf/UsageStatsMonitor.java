package com.xueyin.genshinperf;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

/**
 * UsageStatsMonitor
 *
 * Detects foreground app changes using UsageStatsManager.queryEvents().
 * Uses a HandlerThread with a minimum interval of 2 seconds — only checks
 * when there has been a MOVE_TO_FOREGROUND event, so CPU impact is minimal.
 *
 * Design: NOT a foreground service. Runs on a HandlerThread that is started
 * only when Smart Profile is enabled, and stopped when disabled.
 */
public class UsageStatsMonitor {

    private static final String TAG = "UsageStatsMonitor";
    private static final long CHECK_INTERVAL_MS = 2000L; // 2 sec, only after events
    private static final long EVENT_LOOKBACK_MS  = 3000L; // look back 3 sec for events

    private static UsageStatsMonitor instance;

    private final Context ctx;
    private HandlerThread thread;
    private Handler handler;
    private SystemEventReceiver sysReceiver;
    private String lastForegroundPkg = "";
    private boolean running = false;

    private final Runnable checkRunnable = new Runnable() {
        @Override public void run() {
            if (!running) return;
            checkForegroundApp();
            handler.postDelayed(this, CHECK_INTERVAL_MS);
        }
    };

    private UsageStatsMonitor(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    public static synchronized UsageStatsMonitor get(Context ctx) {
        if (instance == null) instance = new UsageStatsMonitor(ctx);
        return instance;
    }

    // ── Start / Stop ───────────────────────────────────────────────────────────
    public synchronized void start() {
        if (running) return;
        running = true;

        // Register system broadcast receiver (charging, screen)
        sysReceiver = new SystemEventReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        ctx.registerReceiver(sysReceiver, filter);

        // Seed initial charging state
        Intent battStatus = ctx.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battStatus != null) {
            int status = battStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean charging = (status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL);
            SmartProfileManager.get(ctx).onChargingChanged(charging);
        }

        // Start background thread for UsageStats polling (lightweight)
        thread = new HandlerThread("SmartProfileMonitor");
        thread.start();
        handler = new Handler(thread.getLooper());
        handler.post(checkRunnable);

        Log.d(TAG, "Monitor started");
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;

        if (sysReceiver != null) {
            try { ctx.unregisterReceiver(sysReceiver); } catch (Exception ignored) {}
            sysReceiver = null;
        }

        if (handler != null) handler.removeCallbacks(checkRunnable);
        if (thread  != null) {
            thread.quitSafely();
            thread = null;
        }
        handler = null;
        Log.d(TAG, "Monitor stopped");
    }

    public boolean isRunning() { return running; }

    // ── Foreground detection ───────────────────────────────────────────────────
    private void checkForegroundApp() {
        try {
            UsageStatsManager usm = (UsageStatsManager)
                    ctx.getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm == null) return;

            long now = System.currentTimeMillis();
            UsageEvents events = usm.queryEvents(now - EVENT_LOOKBACK_MS, now);
            if (events == null) return;

            UsageEvents.Event event = new UsageEvents.Event();
            String latestPkg = null;

            // Walk events forward — last MOVE_TO_FOREGROUND wins
            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    latestPkg = event.getPackageName();
                }
            }

            if (latestPkg != null && !latestPkg.equals(lastForegroundPkg)) {
                lastForegroundPkg = latestPkg;
                Log.d(TAG, "Foreground changed: " + latestPkg);
                final String pkg = latestPkg;
                // Notify manager on the same background thread (no main thread needed)
                SmartProfileManager.get(ctx).onForegroundAppChanged(pkg);
            }
        } catch (Exception e) {
            Log.e(TAG, "checkForegroundApp error: " + e.getMessage());
        }
    }
}
