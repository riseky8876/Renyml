package com.xueyin.genshinperf;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class LogManager {

    private static File logDir;
    private static File appLog;
    private static File crashLog;

    public static void init(Context ctx) {
        File base = new File(Environment.getExternalStorageDirectory(), "GenshinPerf/logs");
        if (!base.exists()) base.mkdirs();
        logDir = base;
        appLog = new File(base, "app_log.txt");
        crashLog = new File(base, "crash_log.txt");
    }

    public static File appLogFile() { return appLog; }
    public static File crashLogFile() { return crashLog; }
    public static File logDir() { return logDir; }

    public static synchronized void append(String line) {
        if (appLog == null) return;
        try (FileWriter fw = new FileWriter(appLog, true)) {
            fw.write(line);
            fw.write('\n');
        } catch (Exception ignored) { }
    }

    public static synchronized String readAll() {
        if (appLog == null || !appLog.exists()) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(appLog))) {
            String l;
            while ((l = br.readLine()) != null) sb.append(l).append('\n');
        } catch (Exception ignored) { }
        return sb.toString();
    }

    public static synchronized String readLastLines(int n) {
        String all = readAll();
        if (all.isEmpty()) return "";
        String[] lines = all.split("\n");
        int start = Math.max(0, lines.length - n);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < lines.length; i++) sb.append(lines[i]).append('\n');
        return sb.toString();
    }

    public static synchronized void clear() {
        if (appLog != null && appLog.exists()) appLog.delete();
    }

    public static synchronized void clearAll() {
        clear();
        if (crashLog != null && crashLog.exists()) crashLog.delete();
    }
}
