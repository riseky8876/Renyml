package com.xueyin.genshinperf;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawer;
    private TextView statusText, deviceInfo, logView;
    private ScrollView logScroll;
    private ProgressBar runProgress;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;

    private View panelMain, panelNetwork;

    // selected preset per card: -1 = none
    private int wifiSel = -1, cellSel = -1;
    private View[] wifiPills, cellPills;
    private TextView wifiSubtitle, cellSubtitle;
    private static final String[] PRESETS = { "gaming", "download", "streaming", "upload" };
    private static final String[] PRESET_LABELS = { "Gaming", "Download", "Streaming", "Upload" };

    private final Runnable deviceTicker = new Runnable() {
        @Override public void run() {
            updateDeviceInfo();
            handler.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("gpm_prefs", MODE_PRIVATE);

        drawer = findViewById(R.id.drawerLayout);
        statusText = findViewById(R.id.statusText);
        deviceInfo = findViewById(R.id.deviceInfo);
        logView = findViewById(R.id.logView);
        logScroll = findViewById(R.id.logScroll);
        runProgress = findViewById(R.id.runProgress);
        panelMain = findViewById(R.id.panelMain);
        panelNetwork = findViewById(R.id.panelNetwork);

        ImageButton avatarBtn = findViewById(R.id.avatarBtn);
        avatarBtn.setOnClickListener(v -> drawer.openDrawer(GravityCompat.START));

        NavigationView nav = findViewById(R.id.navView);
        nav.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else if (id == R.id.nav_about) {
                startActivity(new Intent(this, AboutActivity.class));
            } else if (id == R.id.nav_smart_profile) {
                startActivity(new Intent(this, SmartProfileActivity.class));
            } else if (id == R.id.nav_per_app) {
                startActivity(new Intent(this, PerAppActivity.class));
            }
            drawer.closeDrawer(GravityCompat.START);
            return true;
        });

        // Bottom navigation
        BottomNavigationView bottom = findViewById(R.id.bottomNav);
        bottom.setSelectedItemId(R.id.tab_main);
        bottom.setOnItemSelectedListener(item -> {
            boolean main = item.getItemId() == R.id.tab_main;
            panelMain.setVisibility(main ? View.VISIBLE : View.GONE);
            panelNetwork.setVisibility(main ? View.GONE : View.VISIBLE);
            return true;
        });

        // 2x2 main action grid
        View b1 = findViewById(R.id.btnRunEsp);
        View b2 = findViewById(R.id.btnRunNoEsp);
        View b3 = findViewById(R.id.btnRestore);
        View b4 = findViewById(R.id.btnFreeze);
        attachPressAnim(b1); attachPressAnim(b2); attachPressAnim(b3); attachPressAnim(b4);
        b1.setOnClickListener(v -> runOption(1));
        b2.setOnClickListener(v -> runOption(2));
        b3.setOnClickListener(v -> runOption(3));
        b4.setOnClickListener(v -> runOption(4));

        Button bClear = findViewById(R.id.btnClearLog);
        bClear.setOnClickListener(v -> {
            logView.setText("");
            LogManager.clear();
        });

        setupNetworkPanel();

        logScroll.setOnTouchListener((v, e) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        applyAppearance();
        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyAppearance();
        refreshStatus();
        handler.post(deviceTicker);
        String saved = LogManager.readAll();
        if (saved != null && !saved.isEmpty()) {
            logView.setText("");
            for (String line : saved.split("\n")) appendLog(line, 0);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(deviceTicker);
    }

    private void attachPressAnim(View v) {
        v.setOnTouchListener((view, ev) -> {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_press));
            } else if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
                view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_release));
                if (ev.getAction() == MotionEvent.ACTION_UP) view.performClick();
            }
            return true;
        });
    }

    private void toggleSection(int headerId, int bodyId, int arrowId) {
        View hdr = findViewById(headerId);
        View body = findViewById(bodyId);
        TextView arrow = findViewById(arrowId);
        hdr.setOnClickListener(v -> {
            boolean show = body.getVisibility() != View.VISIBLE;
            body.setVisibility(show ? View.VISIBLE : View.GONE);
            arrow.setText(show ? "▼" : "▶");
        });
    }

    private void setupNetworkPanel() {
        toggleSection(R.id.hdrWifi,  R.id.bodyWifi,  R.id.arrowWifi);
        toggleSection(R.id.hdrCell,  R.id.bodyCell,  R.id.arrowCell);
        toggleSection(R.id.hdrOther, R.id.bodyOther, R.id.arrowOther);

        wifiSubtitle = findViewById(R.id.wifiSubtitle);
        cellSubtitle = findViewById(R.id.cellSubtitle);

        wifiPills = new View[] {
                findViewById(R.id.pillWifiGaming),
                findViewById(R.id.pillWifiDownload),
                findViewById(R.id.pillWifiStreaming),
                findViewById(R.id.pillWifiUpload)
        };
        cellPills = new View[] {
                findViewById(R.id.pillCellGaming),
                findViewById(R.id.pillCellDownload),
                findViewById(R.id.pillCellStreaming),
                findViewById(R.id.pillCellUpload)
        };
        for (int i = 0; i < wifiPills.length; i++) {
            final int idx = i;
            wifiPills[i].setOnClickListener(v -> selectPill(true, idx));
        }
        for (int i = 0; i < cellPills.length; i++) {
            final int idx = i;
            cellPills[i].setOnClickListener(v -> selectPill(false, idx));
        }

        Button wApply = findViewById(R.id.btnWifiApply);
        Button wRev   = findViewById(R.id.btnWifiRevert);
        Button cApply = findViewById(R.id.btnCellApply);
        Button cRev   = findViewById(R.id.btnCellRevert);
        attachPressAnim(wApply); attachPressAnim(wRev);
        attachPressAnim(cApply); attachPressAnim(cRev);

        wApply.setOnClickListener(v -> {
            if (wifiSel < 0) { Toast.makeText(this, R.string.net_pick_preset_first, Toast.LENGTH_SHORT).show(); return; }
            runNetwork("WiFi " + PRESET_LABELS[wifiSel], NetworkOptimizer.wifiMode(PRESETS[wifiSel]));
        });
        wRev.setOnClickListener(v -> runNetwork("WiFi Restore", NetworkOptimizer.restoreScript()));
        cApply.setOnClickListener(v -> {
            if (cellSel < 0) { Toast.makeText(this, R.string.net_pick_preset_first, Toast.LENGTH_SHORT).show(); return; }
            runNetwork("Seluler " + PRESET_LABELS[cellSel], NetworkOptimizer.cellMode(PRESETS[cellSel]));
        });
        cRev.setOnClickListener(v -> runNetwork("Seluler Restore", NetworkOptimizer.restoreScript()));

        Button bStatus = findViewById(R.id.btnNetStatus);
        Button bRestoreAll = findViewById(R.id.btnNetRestore);
        attachPressAnim(bStatus); attachPressAnim(bRestoreAll);
        bStatus.setOnClickListener(v -> runNetwork("Network Status", NetworkOptimizer.statusScript()));
        bRestoreAll.setOnClickListener(v -> runNetwork("Network Restore", NetworkOptimizer.restoreScript()));
    }

    private void selectPill(boolean wifi, int idx) {
        View[] pills = wifi ? wifiPills : cellPills;
        for (int i = 0; i < pills.length; i++) {
            pills[i].setSelected(i == idx);
            propagateSelection(pills[i], i == idx);
        }
        if (wifi) {
            wifiSel = idx;
            wifiSubtitle.setText("Preset: " + PRESET_LABELS[idx]);
        } else {
            cellSel = idx;
            cellSubtitle.setText("Preset: " + PRESET_LABELS[idx]);
        }
    }

    private void propagateSelection(View v, boolean sel) {
        v.setSelected(sel);
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) propagateSelection(g.getChildAt(i), sel);
        }
    }

    private void runNetwork(String label, String script) {
        runProgress.setVisibility(View.VISIBLE);
        appendLog("[ NET ] " + label + "…", ContextCompat.getColor(this, R.color.furina_blue));
        RootExecutor.runShell(script, new RootExecutor.Callback() {
            @Override public void onLine(String line) {
                runOnUiThread(() -> appendLog(line, classifyColor(line)));
            }
            @Override public void onDone(int code) {
                runOnUiThread(() -> {
                    runProgress.setVisibility(View.GONE);
                    int c = code == 0 ? ContextCompat.getColor(MainActivity.this, R.color.status_ok)
                                      : ContextCompat.getColor(MainActivity.this, R.color.status_err);
                    appendLog("[ END ] " + label + " exit=" + code, c);
                });
            }
        });
    }

    private void runOption(int option) {
        runProgress.setVisibility(View.VISIBLE);
        appendLog("[ RUN ] Menjalankan opsi " + option + "…", ContextCompat.getColor(this, R.color.furina_blue));
        String script = prefs.getString("script_path", "/sdcard/GenshinPerf/main.sh");
        RootExecutor.run(script, option, new RootExecutor.Callback() {
            @Override public void onLine(String line) {
                runOnUiThread(() -> appendLog(line, classifyColor(line)));
            }
            @Override public void onDone(int code) {
                runOnUiThread(() -> {
                    runProgress.setVisibility(View.GONE);
                    int c = code == 0 ? ContextCompat.getColor(MainActivity.this, R.color.status_ok)
                                      : ContextCompat.getColor(MainActivity.this, R.color.status_err);
                    appendLog("[ END ] exit=" + code, c);
                });
            }
        });
    }

    private int classifyColor(String line) {
        String l = line.toLowerCase(Locale.ROOT);
        if (l.contains("error") || l.contains("fail") || l.contains("denied")) {
            return ContextCompat.getColor(this, R.color.status_err);
        }
        if (l.contains("warn")) return ContextCompat.getColor(this, R.color.log_warn);
        if (l.contains("ok") || l.contains("success") || l.contains("done") || l.contains("locked")) {
            return ContextCompat.getColor(this, R.color.status_ok);
        }
        return ContextCompat.getColor(this, R.color.text_secondary);
    }

    private void appendLog(String line, int color) {
        if (line == null) return;
        String ts = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        String entry = "[" + ts + "] " + line;
        SpannableString sp = new SpannableString(entry + "\n");
        if (color != 0) sp.setSpan(new ForegroundColorSpan(color), 0, sp.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        logView.append(sp);
        LogManager.append(entry);
        trimLog();
        if (prefs.getBoolean("auto_scroll", true)) {
            logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
        }
    }

    private void trimLog() {
        int max = prefs.getInt("max_lines", 500);
        CharSequence cs = logView.getText();
        String[] lines = cs.toString().split("\n");
        if (lines.length > max) {
            StringBuilder sb = new StringBuilder();
            for (int i = lines.length - max; i < lines.length; i++) sb.append(lines[i]).append('\n');
            logView.setText(sb.toString());
        }
    }

    private void refreshStatus() {
        boolean rooted = RootExecutor.isRootAvailable();
        if (rooted) {
            statusText.setText(R.string.status_ok);
            statusText.setTextColor(ContextCompat.getColor(this, R.color.status_ok));
            statusText.setBackgroundResource(R.drawable.chip_status_ok);
        } else {
            statusText.setText(R.string.status_error);
            statusText.setTextColor(ContextCompat.getColor(this, R.color.status_err));
            statusText.setBackgroundResource(R.drawable.chip_status_err);
        }
    }

    private void updateDeviceInfo() {
        try {
            int cpu = readCpuUsage();
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            long usedMb = (mi.totalMem - mi.availMem) / (1024 * 1024);
            long totalMb = mi.totalMem / (1024 * 1024);
            deviceInfo.setText(getString(R.string.device_info_format,
                    String.valueOf(cpu),
                    usedMb + "/" + totalMb + "MB"));
        } catch (Exception ignored) { }
    }

    private long lastIdle = 0, lastTotal = 0;
    private int readCpuUsage() {
        try (RandomAccessFile r = new RandomAccessFile("/proc/stat", "r")) {
            String line = r.readLine();
            if (line == null) return 0;
            String[] parts = line.trim().split("\\s+");
            long user = Long.parseLong(parts[1]);
            long nice = Long.parseLong(parts[2]);
            long sys  = Long.parseLong(parts[3]);
            long idle = Long.parseLong(parts[4]);
            long iow  = parts.length > 5 ? Long.parseLong(parts[5]) : 0;
            long irq  = parts.length > 6 ? Long.parseLong(parts[6]) : 0;
            long sirq = parts.length > 7 ? Long.parseLong(parts[7]) : 0;
            long total = user + nice + sys + idle + iow + irq + sirq;
            long dt = total - lastTotal;
            long di = idle - lastIdle;
            lastTotal = total; lastIdle = idle;
            if (dt <= 0) return 0;
            return (int) (100 * (dt - di) / dt);
        } catch (Exception e) {
            return 0;
        }
    }

    private void applyAppearance() {
        ImageView header = findViewById(R.id.headerImage);
        loadCustom(header, prefs.getString("header_path", null), R.drawable.header_default);

        int hHeight = prefs.getInt("header_height", 180);
        View hc = findViewById(R.id.headerContainer);
        hc.getLayoutParams().height = (int) (hHeight * getResources().getDisplayMetrics().density);
        hc.requestLayout();

        ImageButton avatar = findViewById(R.id.avatarBtn);
        loadCustom(avatar, prefs.getString("avatar_path", null), R.drawable.furina);
    }

    private void loadCustom(ImageView v, String path, int fallbackRes) {
        if (path != null) {
            try {
                java.io.File f = new java.io.File(path);
                if (f.exists()) {
                    v.setImageBitmap(BitmapFactory.decodeFile(path));
                    return;
                }
            } catch (Exception ignored) { }
        }
        v.setImageResource(fallbackRes);
    }
}
