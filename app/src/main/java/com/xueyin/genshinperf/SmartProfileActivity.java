package com.xueyin.genshinperf;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class SmartProfileActivity extends AppCompatActivity {

    private SmartProfileManager mgr;

    // Views
    private Switch switchEnable;
    private TextView tvRootStatus, tvUsageStatus;

    // Gaming
    private Spinner spinGamingGov;
    private EditText etGamingGpuMin;
    private LinearLayout llGamingApps;

    // Charging
    private Spinner spinChargingGov;
    private CheckBox cbRelaxThermal;

    // Idle
    private Spinner spinIdleGov;
    private EditText etIdleGpuMax;
    private CheckBox cbIdleScreenOff;

    // Manual
    private Spinner spinManualGov;
    private EditText etManualGpuMin, etManualGpuMax;

    private static final String[] GOVERNORS =
        { "performance", "schedutil", "interactive", "powersave", "ondemand", "conservative" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_profile);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Smart Profile");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mgr = SmartProfileManager.get(this);
        bindViews();
        checkRoot();
        checkUsagePermission();
        loadAll();
    }

    private void bindViews() {
        switchEnable    = findViewById(R.id.switchSmartEnable);
        tvRootStatus    = findViewById(R.id.tvRootStatus);
        tvUsageStatus   = findViewById(R.id.tvUsageStatus);

        // Gaming
        spinGamingGov   = findViewById(R.id.spinGamingGov);
        etGamingGpuMin  = findViewById(R.id.etGamingGpuMin);
        llGamingApps    = findViewById(R.id.llGamingApps);
        Button btnAddGame = findViewById(R.id.btnAddGameApp);
        btnAddGame.setOnClickListener(v -> pickGameApp());

        // Charging
        spinChargingGov = findViewById(R.id.spinChargingGov);
        cbRelaxThermal  = findViewById(R.id.cbRelaxThermal);

        // Idle
        spinIdleGov     = findViewById(R.id.spinIdleGov);
        etIdleGpuMax    = findViewById(R.id.etIdleGpuMax);
        cbIdleScreenOff = findViewById(R.id.cbIdleScreenOff);

        // Manual
        spinManualGov   = findViewById(R.id.spinManualGov);
        etManualGpuMin  = findViewById(R.id.etManualGpuMin);
        etManualGpuMax  = findViewById(R.id.etManualGpuMax);

        // Wire up spinners
        ArrayAdapter<String> govAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, GOVERNORS);
        govAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinGamingGov.setAdapter(govAdapter);
        spinChargingGov.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, GOVERNORS));
        ((ArrayAdapter) spinChargingGov.getAdapter())
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinIdleGov.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, GOVERNORS));
        ((ArrayAdapter) spinIdleGov.getAdapter())
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinManualGov.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, GOVERNORS));
        ((ArrayAdapter) spinManualGov.getAdapter())
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Enable switch
        switchEnable.setOnCheckedChangeListener((btn, checked) -> {
            mgr.setEnabled(checked);
            if (checked) {
                if (!hasUsagePermission()) {
                    requestUsagePermission();
                    switchEnable.setChecked(false);
                    return;
                }
                UsageStatsMonitor.get(this).start();
                Toast.makeText(this, "Smart Profile aktif", Toast.LENGTH_SHORT).show();
            } else {
                UsageStatsMonitor.get(this).stop();
                Toast.makeText(this, "Smart Profile dinonaktifkan", Toast.LENGTH_SHORT).show();
            }
        });

        // Save button
        findViewById(R.id.btnSaveSmartProfile).setOnClickListener(v -> saveAll());
    }

    // ── Load saved values ──────────────────────────────────────────────────────
    private void loadAll() {
        switchEnable.setChecked(mgr.isEnabled());

        ProfileConfig.GamingProfile gp = mgr.loadGamingProfile();
        spinGamingGov.setSelection(govIndex(gp.cpuGovernor));
        etGamingGpuMin.setText(gp.gpuFreqMin);
        refreshGameApps(gp.triggerPackages);

        ProfileConfig.ChargingProfile cp = mgr.loadChargingProfile();
        spinChargingGov.setSelection(govIndex(cp.cpuGovernor));
        cbRelaxThermal.setChecked(cp.relaxThermal);

        ProfileConfig.IdleProfile ip = mgr.loadIdleProfile();
        spinIdleGov.setSelection(govIndex(ip.cpuGovernor));
        etIdleGpuMax.setText(ip.gpuFreqMax);
        cbIdleScreenOff.setChecked(ip.screenOffOnly);

        ProfileConfig.ManualProfile mp = mgr.loadManualProfile();
        spinManualGov.setSelection(govIndex(mp.cpuGovernor));
        etManualGpuMin.setText(mp.gpuFreqMin);
        etManualGpuMax.setText(mp.gpuFreqMax);
    }

    // ── Save ───────────────────────────────────────────────────────────────────
    private void saveAll() {
        ProfileConfig.GamingProfile gp = mgr.loadGamingProfile();
        gp.cpuGovernor = GOVERNORS[spinGamingGov.getSelectedItemPosition()];
        gp.gpuFreqMin  = etGamingGpuMin.getText().toString().trim();
        mgr.saveGamingProfile(gp);

        ProfileConfig.ChargingProfile cp = new ProfileConfig.ChargingProfile();
        cp.cpuGovernor  = GOVERNORS[spinChargingGov.getSelectedItemPosition()];
        cp.relaxThermal = cbRelaxThermal.isChecked();
        mgr.saveChargingProfile(cp);

        ProfileConfig.IdleProfile ip = new ProfileConfig.IdleProfile();
        ip.cpuGovernor  = GOVERNORS[spinIdleGov.getSelectedItemPosition()];
        ip.gpuFreqMax   = etIdleGpuMax.getText().toString().trim();
        ip.screenOffOnly = cbIdleScreenOff.isChecked();
        mgr.saveIdleProfile(ip);

        ProfileConfig.ManualProfile mp = new ProfileConfig.ManualProfile();
        mp.cpuGovernor = GOVERNORS[spinManualGov.getSelectedItemPosition()];
        mp.gpuFreqMin  = etManualGpuMin.getText().toString().trim();
        mp.gpuFreqMax  = etManualGpuMax.getText().toString().trim();
        mgr.saveManualProfile(mp);

        Toast.makeText(this, "Smart Profile disimpan", Toast.LENGTH_SHORT).show();
    }

    // ── Game app list ──────────────────────────────────────────────────────────
    private static final int REQ_PICK_GAME = 1001;

    private void pickGameApp() {
        startActivityForResult(new Intent(this, AppPickerActivity.class), REQ_PICK_GAME);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_PICK_GAME && res == RESULT_OK && data != null) {
            String pkg   = data.getStringExtra(AppPickerActivity.EXTRA_PACKAGE);
            String label = data.getStringExtra(AppPickerActivity.EXTRA_LABEL);
            ProfileConfig.GamingProfile gp = mgr.loadGamingProfile();
            if (!gp.triggerPackages.contains(pkg)) {
                gp.triggerPackages.add(pkg);
                mgr.saveGamingProfile(gp);
                refreshGameApps(gp.triggerPackages);
            }
        }
    }

    private void refreshGameApps(List<String> packages) {
        llGamingApps.removeAllViews();
        for (String pkg : packages) {
            View chip = LayoutInflater.from(this)
                    .inflate(R.layout.item_app_chip, llGamingApps, false);
            TextView tv = chip.findViewById(R.id.chipLabel);
            tv.setText(pkg);
            chip.findViewById(R.id.chipRemove).setOnClickListener(v -> {
                ProfileConfig.GamingProfile gp = mgr.loadGamingProfile();
                gp.triggerPackages.remove(pkg);
                mgr.saveGamingProfile(gp);
                refreshGameApps(gp.triggerPackages);
            });
            llGamingApps.addView(chip);
        }
    }

    // ── Root / Permission checks ───────────────────────────────────────────────
    private void checkRoot() {
        boolean rooted = RootExecutor.isRootAvailable();
        tvRootStatus.setText(rooted ? "✓ Root tersedia" : "⚠ Root tidak tersedia — profile akan dicatat tapi tidak diterapkan");
        tvRootStatus.setTextColor(getColor(rooted ? R.color.status_ok : R.color.status_warn));
    }

    private void checkUsagePermission() {
        boolean has = hasUsagePermission();
        tvUsageStatus.setText(has ? "✓ Izin Usage Stats OK" : "⚠ Izin Usage Stats diperlukan");
        tvUsageStatus.setTextColor(getColor(has ? R.color.status_ok : R.color.status_warn));
        if (!has) {
            tvUsageStatus.setOnClickListener(v -> requestUsagePermission());
        }
    }

    private boolean hasUsagePermission() {
        AppOpsManager aom = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = aom.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void requestUsagePermission() {
        new AlertDialog.Builder(this)
                .setTitle("Izin Diperlukan")
                .setMessage("Smart Profile membutuhkan izin 'Usage Access' untuk mendeteksi " +
                        "aplikasi yang sedang berjalan. Buka Pengaturan → Aplikasi Khusus → Akses Penggunaan?")
                .setPositiveButton("Buka Pengaturan", (d, w) ->
                        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)))
                .setNegativeButton("Batal", null)
                .show();
    }

    private int govIndex(String gov) {
        for (int i = 0; i < GOVERNORS.length; i++) {
            if (GOVERNORS[i].equals(gov)) return i;
        }
        return 0;
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
