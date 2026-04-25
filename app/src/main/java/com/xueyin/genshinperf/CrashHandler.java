package com.xueyin.genshinperf;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String PREFS = "gpm_prefs";
    private static final String KEY = "has_crash";

    private final Context ctx;
    private final Thread.UncaughtExceptionHandler def;

    private CrashHandler(Context c) {
        this.ctx = c.getApplicationContext();
        this.def = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static void install(Context ctx) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(ctx));
    }

    public static boolean hasCrash(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY, false) && LogManager.crashLogFile() != null
                && LogManager.crashLogFile().exists();
    }

    public static void markSeen(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY, false).apply();
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        try {
            LogManager.init(ctx);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            pw.println("=== CRASH " + ts + " ===");
            pw.println("Device: " + Build.MANUFACTURER + " " + Build.MODEL);
            pw.println("Android: " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")");
            pw.println("ROM: " + Build.DISPLAY);
            pw.println("Root: " + (RootExecutor.isRootAvailable() ? "available" : "not available"));
            pw.println();
            pw.println("--- Stack Trace ---");
            e.printStackTrace(pw);
            pw.println();
            pw.println("--- Last 50 log lines ---");
            pw.println(LogManager.readLastLines(50));
            pw.flush();

            try (FileWriter fw = new FileWriter(LogManager.crashLogFile(), false)) {
                fw.write(sw.toString());
            }

            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY, true).apply();
        } catch (Exception ignored) { }

        if (def != null) def.uncaughtException(t, e);
        else {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }
}
