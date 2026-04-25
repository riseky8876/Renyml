package com.xueyin.genshinperf;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;

/**
 * SmartProfileManager
 *
 * Decides which profile is active based on current conditions and applies it
 * via root shell. Called by event sources (BroadcastReceiver / UsageStatsMonitor)
 * so there is NO polling — it only runs when something actually changes.
 *
 * Priority: GAMING > CHARGING > IDLE > MANUAL
 */
public class SmartProfileManager {

    private static final String TAG = "SmartProfile";
    private static final String PREF_FILE = "gpm_prefs";
    private static final String KEY_GAMING  = "profile_gaming";
    private static final String KEY_CHARGING = "profile_charging";
    private static final String KEY_IDLE    = "profile_idle";
    private static final String KEY_MANUAL  = "profile_manual";
    private static final String KEY_SMART_ENABLED = "smart_profile_enabled";

    private static SmartProfileManager instance;

    private final Context ctx;
    private final SharedPreferences prefs;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Current condition flags (set by event receivers)
    private boolean isGaming   = false;
    private boolean isCharging = false;
    private boolean isIdle     = false;   // screen off

    // Currently active mode (to avoid redundant re-apply)
    private ProfileConfig.ProfileMode activeMode = null;

    private SmartProfileManager(Context ctx) {
        this.ctx   = ctx.getApplicationContext();
        this.prefs = this.ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    public static synchronized SmartProfileManager get(Context ctx) {
        if (instance == null) instance = new SmartProfileManager(ctx);
        return instance;
    }

    // ── Enabled flag ───────────────────────────────────────────────────────────
    public boolean isEnabled() {
        return prefs.getBoolean(KEY_SMART_ENABLED, false);
    }

    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SMART_ENABLED, enabled).apply();
        if (!enabled) activeMode = null;
    }

    // ── Event callbacks (called by receivers) ──────────────────────────────────
    public void onForegroundAppChanged(String packageName) {
        if (!isEnabled()) return;
        List<PerAppConfig> perAppList = PerAppConfig.loadAll(ctx);
        PerAppConfig perApp = PerAppConfig.findFor(perAppList, packageName);

        if (perApp != null) {
            // Per-app override takes precedence over Smart Profile
            applyPerAppConfig(perApp);
            return;
        }

        ProfileConfig.GamingProfile gp = loadGamingProfile();
        isGaming = gp.triggerPackages.contains(packageName);
        evaluate();
    }

    public void onChargingChanged(boolean charging) {
        if (!isEnabled()) return;
        isCharging = charging;
        evaluate();
    }

    public void onScreenChanged(boolean screenOn) {
        if (!isEnabled()) return;
        isIdle = !screenOn;
        if (screenOn && activeMode == ProfileConfig.ProfileMode.IDLE) {
            // Screen came back on, re-evaluate
            isIdle = false;
        }
        evaluate();
    }

    // ── Core decision logic ────────────────────────────────────────────────────
    private void evaluate() {
        ProfileConfig.ProfileMode desired;
        if (isGaming)        desired = ProfileConfig.ProfileMode.GAMING;
        else if (isCharging) desired = ProfileConfig.ProfileMode.CHARGING;
        else if (isIdle)     desired = ProfileConfig.ProfileMode.IDLE;
        else                 desired = ProfileConfig.ProfileMode.MANUAL;

        if (desired == activeMode) return; // nothing changed, skip
        activeMode = desired;
        Log.d(TAG, "Switching to profile: " + desired.name());
        applyProfile(desired);
    }

    // ── Apply helpers ──────────────────────────────────────────────────────────
    private void applyProfile(ProfileConfig.ProfileMode mode) {
        switch (mode) {
            case GAMING:   applyGaming();   break;
            case CHARGING: applyCharging(); break;
            case IDLE:     applyIdle();     break;
            case MANUAL:   applyManual();   break;
        }
    }

    private void applyGaming() {
        ProfileConfig.GamingProfile p = loadGamingProfile();
        StringBuilder sb = new StringBuilder();
        // CPU governor for all cores
        sb.append(setCpuGovernorScript(p.cpuGovernor));
        // GPU min frequency
        if (!p.gpuFreqMin.isEmpty()) {
            sb.append(setGpuMinFreqScript(p.gpuFreqMin));
        }
        LogManager.append("[SMART] Gaming profile aktif (gov=" + p.cpuGovernor + ")");
        runRoot(sb.toString());
    }

    private void applyCharging() {
        ProfileConfig.ChargingProfile p = loadChargingProfile();
        StringBuilder sb = new StringBuilder();
        sb.append(setCpuGovernorScript(p.cpuGovernor));
        if (p.relaxThermal) {
            // Common thermal control node paths (varies by device)
            sb.append("for f in /sys/class/thermal/thermal_zone*/mode; do echo disabled > $f 2>/dev/null; done\n");
        }
        LogManager.append("[SMART] Charging profile aktif (gov=" + p.cpuGovernor + ", relaxThermal=" + p.relaxThermal + ")");
        runRoot(sb.toString());
    }

    private void applyIdle() {
        ProfileConfig.IdleProfile p = loadIdleProfile();
        StringBuilder sb = new StringBuilder();
        sb.append(setCpuGovernorScript(p.cpuGovernor));
        if (!p.gpuFreqMax.isEmpty()) {
            sb.append(setGpuMaxFreqScript(p.gpuFreqMax));
        }
        LogManager.append("[SMART] Idle profile aktif (gov=" + p.cpuGovernor + ")");
        runRoot(sb.toString());
    }

    private void applyManual() {
        ProfileConfig.ManualProfile p = loadManualProfile();
        StringBuilder sb = new StringBuilder();
        sb.append(setCpuGovernorScript(p.cpuGovernor));
        if (!p.gpuFreqMin.isEmpty()) sb.append(setGpuMinFreqScript(p.gpuFreqMin));
        if (!p.gpuFreqMax.isEmpty()) sb.append(setGpuMaxFreqScript(p.gpuFreqMax));
        LogManager.append("[SMART] Manual profile aktif (gov=" + p.cpuGovernor + ")");
        runRoot(sb.toString());
    }

    private void applyPerAppConfig(PerAppConfig c) {
        StringBuilder sb = new StringBuilder();
        if (!c.cpuGovernor.isEmpty()) sb.append(setCpuGovernorScript(c.cpuGovernor));
        if (!c.gpuFreqMin.isEmpty())  sb.append(setGpuMinFreqScript(c.gpuFreqMin));
        if (!c.gpuFreqMax.isEmpty())  sb.append(setGpuMaxFreqScript(c.gpuFreqMax));
        LogManager.append("[PER-APP] " + c.appLabel + " → gov=" + c.cpuGovernor);
        runRoot(sb.toString());
    }

    // ── Shell script builders ──────────────────────────────────────────────────
    /** Writes governor to all online CPU policy dirs */
    private String setCpuGovernorScript(String gov) {
        return "for f in /sys/devices/system/cpu/cpufreq/policy*/scaling_governor " +
               "/sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do " +
               "[ -f \"$f\" ] && echo " + gov + " > \"$f\" 2>/dev/null; done\n";
    }

    private String setGpuMinFreqScript(String freq) {
        // Common paths: Adreno, Mali
        return "for f in /sys/class/kgsl/kgsl-3d0/devfreq/min_freq " +
               "/sys/kernel/gpu/gpu_min_clock; do " +
               "[ -f \"$f\" ] && echo " + freq + " > \"$f\" 2>/dev/null; done\n";
    }

    private String setGpuMaxFreqScript(String freq) {
        return "for f in /sys/class/kgsl/kgsl-3d0/devfreq/max_freq " +
               "/sys/kernel/gpu/gpu_max_clock; do " +
               "[ -f \"$f\" ] && echo " + freq + " > \"$f\" 2>/dev/null; done\n";
    }

    private void runRoot(String script) {
        if (!RootExecutor.isRootAvailable()) {
            mainHandler.post(() ->
                LogManager.append("[SMART] Warning: Root tidak tersedia, profile tidak dapat diterapkan"));
            return;
        }
        RootExecutor.runShell(script, new RootExecutor.Callback() {
            @Override public void onLine(String line) { Log.d(TAG, line); }
            @Override public void onDone(int code) { Log.d(TAG, "Profile applied, exit=" + code); }
        });
    }

    // ── Profile persistence ────────────────────────────────────────────────────
    public ProfileConfig.GamingProfile loadGamingProfile() {
        try {
            String s = prefs.getString(KEY_GAMING, null);
            if (s != null) return ProfileConfig.GamingProfile.fromJson(new JSONObject(s));
        } catch (JSONException ignored) {}
        return new ProfileConfig.GamingProfile();
    }

    public void saveGamingProfile(ProfileConfig.GamingProfile p) {
        try { prefs.edit().putString(KEY_GAMING, p.toJson().toString()).apply(); }
        catch (JSONException ignored) {}
    }

    public ProfileConfig.ChargingProfile loadChargingProfile() {
        try {
            String s = prefs.getString(KEY_CHARGING, null);
            if (s != null) return ProfileConfig.ChargingProfile.fromJson(new JSONObject(s));
        } catch (JSONException ignored) {}
        return new ProfileConfig.ChargingProfile();
    }

    public void saveChargingProfile(ProfileConfig.ChargingProfile p) {
        try { prefs.edit().putString(KEY_CHARGING, p.toJson().toString()).apply(); }
        catch (JSONException ignored) {}
    }

    public ProfileConfig.IdleProfile loadIdleProfile() {
        try {
            String s = prefs.getString(KEY_IDLE, null);
            if (s != null) return ProfileConfig.IdleProfile.fromJson(new JSONObject(s));
        } catch (JSONException ignored) {}
        return new ProfileConfig.IdleProfile();
    }

    public void saveIdleProfile(ProfileConfig.IdleProfile p) {
        try { prefs.edit().putString(KEY_IDLE, p.toJson().toString()).apply(); }
        catch (JSONException ignored) {}
    }

    public ProfileConfig.ManualProfile loadManualProfile() {
        try {
            String s = prefs.getString(KEY_MANUAL, null);
            if (s != null) return ProfileConfig.ManualProfile.fromJson(new JSONObject(s));
        } catch (JSONException ignored) {}
        return new ProfileConfig.ManualProfile();
    }

    public void saveManualProfile(ProfileConfig.ManualProfile p) {
        try { prefs.edit().putString(KEY_MANUAL, p.toJson().toString()).apply(); }
        catch (JSONException ignored) {}
    }
}
