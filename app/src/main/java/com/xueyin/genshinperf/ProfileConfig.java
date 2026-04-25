package com.xueyin.genshinperf;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ProfileConfig {

    public enum ProfileMode { GAMING, CHARGING, IDLE, MANUAL }

    // Priority: GAMING > CHARGING > IDLE > MANUAL
    public static int getPriority(ProfileMode mode) {
        switch (mode) {
            case GAMING:   return 4;
            case CHARGING: return 3;
            case IDLE:     return 2;
            case MANUAL:   return 1;
            default:       return 0;
        }
    }

    // ── Gaming profile ──────────────────────────────────────────────────────
    public static class GamingProfile {
        public String cpuGovernor = "performance";
        public String gpuFreqMin  = "400000000";  // Hz
        public String netPriority = "gaming";
        public List<String> triggerPackages = new ArrayList<>();

        public JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("cpuGovernor", cpuGovernor);
            o.put("gpuFreqMin", gpuFreqMin);
            o.put("netPriority", netPriority);
            JSONArray arr = new JSONArray();
            for (String pkg : triggerPackages) arr.put(pkg);
            o.put("triggerPackages", arr);
            return o;
        }

        public static GamingProfile fromJson(JSONObject o) throws JSONException {
            GamingProfile p = new GamingProfile();
            if (o.has("cpuGovernor")) p.cpuGovernor = o.getString("cpuGovernor");
            if (o.has("gpuFreqMin"))  p.gpuFreqMin  = o.getString("gpuFreqMin");
            if (o.has("netPriority")) p.netPriority = o.getString("netPriority");
            if (o.has("triggerPackages")) {
                JSONArray arr = o.getJSONArray("triggerPackages");
                for (int i = 0; i < arr.length(); i++) p.triggerPackages.add(arr.getString(i));
            }
            return p;
        }
    }

    // ── Charging profile ─────────────────────────────────────────────────────
    public static class ChargingProfile {
        public String cpuGovernor   = "schedutil";
        public boolean relaxThermal = true;   // loosen thermal limit while charging
        public String thermalProfile = "balance"; // balance / performance / powersave

        public JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("cpuGovernor", cpuGovernor);
            o.put("relaxThermal", relaxThermal);
            o.put("thermalProfile", thermalProfile);
            return o;
        }

        public static ChargingProfile fromJson(JSONObject o) throws JSONException {
            ChargingProfile p = new ChargingProfile();
            if (o.has("cpuGovernor"))   p.cpuGovernor   = o.getString("cpuGovernor");
            if (o.has("relaxThermal"))  p.relaxThermal  = o.getBoolean("relaxThermal");
            if (o.has("thermalProfile")) p.thermalProfile = o.getString("thermalProfile");
            return p;
        }
    }

    // ── Idle profile ──────────────────────────────────────────────────────────
    public static class IdleProfile {
        public String cpuGovernor  = "powersave";
        public String gpuFreqMax   = "200000000"; // cap GPU low
        public boolean screenOffOnly = true;       // only apply when screen off

        public JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("cpuGovernor", cpuGovernor);
            o.put("gpuFreqMax", gpuFreqMax);
            o.put("screenOffOnly", screenOffOnly);
            return o;
        }

        public static IdleProfile fromJson(JSONObject o) throws JSONException {
            IdleProfile p = new IdleProfile();
            if (o.has("cpuGovernor"))   p.cpuGovernor   = o.getString("cpuGovernor");
            if (o.has("gpuFreqMax"))    p.gpuFreqMax    = o.getString("gpuFreqMax");
            if (o.has("screenOffOnly")) p.screenOffOnly = o.getBoolean("screenOffOnly");
            return p;
        }
    }

    // ── Manual profile ────────────────────────────────────────────────────────
    public static class ManualProfile {
        public String cpuGovernor = "schedutil";
        public String gpuFreqMin  = "";
        public String gpuFreqMax  = "";
        public String netPriority = "default";

        public JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("cpuGovernor", cpuGovernor);
            o.put("gpuFreqMin", gpuFreqMin);
            o.put("gpuFreqMax", gpuFreqMax);
            o.put("netPriority", netPriority);
            return o;
        }

        public static ManualProfile fromJson(JSONObject o) throws JSONException {
            ManualProfile p = new ManualProfile();
            if (o.has("cpuGovernor")) p.cpuGovernor = o.getString("cpuGovernor");
            if (o.has("gpuFreqMin"))  p.gpuFreqMin  = o.getString("gpuFreqMin");
            if (o.has("gpuFreqMax"))  p.gpuFreqMax  = o.getString("gpuFreqMax");
            if (o.has("netPriority")) p.netPriority = o.getString("netPriority");
            return p;
        }
    }
}
