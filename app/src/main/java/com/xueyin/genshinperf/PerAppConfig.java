package com.xueyin.genshinperf;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class PerAppConfig {

    public String packageName   = "";
    public String appLabel      = "";
    public String cpuGovernor   = ""; // empty = use system default
    public String gpuFreqMin    = ""; // empty = don't touch
    public String gpuFreqMax    = ""; // empty = don't touch
    public String netPriority   = "default"; // default / gaming / download / upload

    private static final String PREF_KEY = "per_app_configs";

    // ── Serialization ──────────────────────────────────────────────────────────
    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("packageName", packageName);
        o.put("appLabel", appLabel);
        o.put("cpuGovernor", cpuGovernor);
        o.put("gpuFreqMin", gpuFreqMin);
        o.put("gpuFreqMax", gpuFreqMax);
        o.put("netPriority", netPriority);
        return o;
    }

    public static PerAppConfig fromJson(JSONObject o) throws JSONException {
        PerAppConfig c = new PerAppConfig();
        c.packageName = o.optString("packageName", "");
        c.appLabel    = o.optString("appLabel", "");
        c.cpuGovernor = o.optString("cpuGovernor", "");
        c.gpuFreqMin  = o.optString("gpuFreqMin", "");
        c.gpuFreqMax  = o.optString("gpuFreqMax", "");
        c.netPriority = o.optString("netPriority", "default");
        return c;
    }

    // ── Persistence ────────────────────────────────────────────────────────────
    public static List<PerAppConfig> loadAll(Context ctx) {
        List<PerAppConfig> list = new ArrayList<>();
        SharedPreferences prefs = ctx.getSharedPreferences("gpm_prefs", Context.MODE_PRIVATE);
        String json = prefs.getString(PREF_KEY, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                list.add(fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException ignored) {}
        return list;
    }

    public static void saveAll(Context ctx, List<PerAppConfig> list) {
        JSONArray arr = new JSONArray();
        for (PerAppConfig c : list) {
            try { arr.put(c.toJson()); } catch (JSONException ignored) {}
        }
        ctx.getSharedPreferences("gpm_prefs", Context.MODE_PRIVATE)
           .edit().putString(PREF_KEY, arr.toString()).apply();
    }

    /** Find config for a given package, or null if not configured */
    public static PerAppConfig findFor(List<PerAppConfig> list, String pkg) {
        for (PerAppConfig c : list) {
            if (c.packageName.equals(pkg)) return c;
        }
        return null;
    }

    public static void upsert(Context ctx, PerAppConfig updated) {
        List<PerAppConfig> list = loadAll(ctx);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).packageName.equals(updated.packageName)) {
                list.set(i, updated);
                saveAll(ctx, list);
                return;
            }
        }
        list.add(updated);
        saveAll(ctx, list);
    }

    public static void remove(Context ctx, String packageName) {
        List<PerAppConfig> list = loadAll(ctx);
        list.removeIf(c -> c.packageName.equals(packageName));
        saveAll(ctx, list);
    }
}
