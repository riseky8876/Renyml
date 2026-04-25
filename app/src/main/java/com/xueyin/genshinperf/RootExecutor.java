package com.xueyin.genshinperf;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class RootExecutor {

    public interface Callback {
        void onLine(String line);
        void onDone(int code);
    }

    public static boolean isRootAvailable() {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("id\n");
            os.writeBytes("exit\n");
            os.flush();
            int code = p.waitFor();
            return code == 0;
        } catch (Exception e) {
            return false;
        } finally {
            if (p != null) p.destroy();
        }
    }

    public static void runShell(String script, Callback cb) {
        Thread t = new Thread(() -> {
            int exitCode = -1;
            try {
                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(process.getOutputStream());
                os.writeBytes(script);
                os.writeBytes("\nexit\n");
                os.flush();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    cb.onLine(line);
                }
                exitCode = process.waitFor();
            } catch (Exception e) {
                cb.onLine("ERROR: " + e.getMessage());
            }
            cb.onDone(exitCode);
        });
        t.setDaemon(true);
        t.start();
    }

    public static void run(String scriptPath, int option, Callback cb) {
        Thread t = new Thread(() -> {
            int exitCode = -1;
            try {
                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(process.getOutputStream());
                os.writeBytes("sh " + scriptPath + " " + option + " 2>&1\n");
                os.writeBytes("exit\n");
                os.flush();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    cb.onLine(line);
                }
                exitCode = process.waitFor();
            } catch (Exception e) {
                cb.onLine("ERROR: " + e.getMessage());
            }
            cb.onDone(exitCode);
        });
        t.setDaemon(true);
        t.start();
    }
}
