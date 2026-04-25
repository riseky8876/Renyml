package com.xueyin.genshinperf;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PerAppActivity extends AppCompatActivity {

    private List<PerAppConfig> configs;
    private PerAppAdapter adapter;

    private static final int REQ_PICK_APP = 2001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_per_app);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Per-App Optimization");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        configs = PerAppConfig.loadAll(this);

        RecyclerView recycler = findViewById(R.id.perAppRecycler);
        adapter = new PerAppAdapter(configs,
                this::showEditDialog,
                pkg -> {
                    PerAppConfig.remove(this, pkg);
                    configs.removeIf(c -> c.packageName.equals(pkg));
                    adapter.notifyDataSetChanged();
                });
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        findViewById(R.id.btnAddPerApp).setOnClickListener(v ->
                startActivityForResult(new Intent(this, AppPickerActivity.class), REQ_PICK_APP));
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_PICK_APP && res == RESULT_OK && data != null) {
            String pkg   = data.getStringExtra(AppPickerActivity.EXTRA_PACKAGE);
            String label = data.getStringExtra(AppPickerActivity.EXTRA_LABEL);
            // Don't add duplicate
            for (PerAppConfig c : configs) {
                if (c.packageName.equals(pkg)) {
                    showEditDialog(c);
                    return;
                }
            }
            PerAppConfig newCfg = new PerAppConfig();
            newCfg.packageName = pkg;
            newCfg.appLabel    = label;
            showEditDialog(newCfg);
        }
    }

    private void showEditDialog(PerAppConfig cfg) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_per_app_edit, null);

        TextView tvTitle = view.findViewById(R.id.dialogAppTitle);
        tvTitle.setText(cfg.appLabel.isEmpty() ? cfg.packageName : cfg.appLabel);

        // CPU Governor
        Spinner spinGov = view.findViewById(R.id.spinPerAppGov);
        String[] govs = { "(sistem default)", "performance", "schedutil",
                          "interactive", "powersave", "ondemand", "conservative" };
        ArrayAdapter<String> ga = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, govs);
        ga.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinGov.setAdapter(ga);
        int govIdx = 0;
        for (int i = 0; i < govs.length; i++) {
            if (govs[i].equals(cfg.cpuGovernor)) { govIdx = i; break; }
        }
        spinGov.setSelection(govIdx);

        // GPU Min / Max
        TextView etGpuMin = view.findViewById(R.id.etPerAppGpuMin);
        TextView etGpuMax = view.findViewById(R.id.etPerAppGpuMax);
        etGpuMin.setText(cfg.gpuFreqMin);
        etGpuMax.setText(cfg.gpuFreqMax);

        // Network priority
        Spinner spinNet = view.findViewById(R.id.spinPerAppNet);
        String[] nets = { "default", "gaming", "download", "upload" };
        ArrayAdapter<String> na = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, nets);
        na.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinNet.setAdapter(na);
        int netIdx = 0;
        for (int i = 0; i < nets.length; i++) {
            if (nets[i].equals(cfg.netPriority)) { netIdx = i; break; }
        }
        spinNet.setSelection(netIdx);

        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("Simpan", (d, w) -> {
                    int gIdx = spinGov.getSelectedItemPosition();
                    cfg.cpuGovernor = gIdx == 0 ? "" : govs[gIdx];
                    cfg.gpuFreqMin  = etGpuMin.getText().toString().trim();
                    cfg.gpuFreqMax  = etGpuMax.getText().toString().trim();
                    cfg.netPriority = nets[spinNet.getSelectedItemPosition()];
                    PerAppConfig.upsert(this, cfg);
                    // Refresh list
                    configs.clear();
                    configs.addAll(PerAppConfig.loadAll(this));
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Disimpan: " + cfg.appLabel, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    // ── Adapter ────────────────────────────────────────────────────────────────
    static class PerAppAdapter extends RecyclerView.Adapter<PerAppAdapter.VH> {
        interface OnEdit   { void edit(PerAppConfig cfg); }
        interface OnRemove { void remove(String pkg);    }

        private final List<PerAppConfig> data;
        private final OnEdit   onEdit;
        private final OnRemove onRemove;

        PerAppAdapter(List<PerAppConfig> data, OnEdit onEdit, OnRemove onRemove) {
            this.data     = data;
            this.onEdit   = onEdit;
            this.onRemove = onRemove;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_per_app, parent, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            PerAppConfig c = data.get(pos);

            // Try to load icon
            try {
                Drawable icon = h.itemView.getContext().getPackageManager()
                        .getApplicationIcon(c.packageName);
                h.icon.setImageDrawable(icon);
            } catch (Exception ignored) {
                h.icon.setImageResource(android.R.drawable.sym_def_app_icon);
            }

            h.label.setText(c.appLabel.isEmpty() ? c.packageName : c.appLabel);
            h.pkg.setText(c.packageName);

            StringBuilder summary = new StringBuilder();
            if (!c.cpuGovernor.isEmpty()) summary.append("CPU: ").append(c.cpuGovernor).append("  ");
            if (!c.gpuFreqMin.isEmpty())  summary.append("GPU min: ").append(c.gpuFreqMin).append("  ");
            if (!c.gpuFreqMax.isEmpty())  summary.append("GPU max: ").append(c.gpuFreqMax).append("  ");
            if (!"default".equals(c.netPriority)) summary.append("Net: ").append(c.netPriority);
            h.summary.setText(summary.length() > 0 ? summary.toString().trim() : "Tidak ada override");

            h.btnEdit.setOnClickListener(v -> onEdit.edit(c));
            h.btnRemove.setOnClickListener(v -> onRemove.remove(c.packageName));
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView label, pkg, summary;
            View btnEdit, btnRemove;
            VH(View v) {
                super(v);
                icon      = v.findViewById(R.id.perAppIcon);
                label     = v.findViewById(R.id.perAppLabel);
                pkg       = v.findViewById(R.id.perAppPackage);
                summary   = v.findViewById(R.id.perAppSummary);
                btnEdit   = v.findViewById(R.id.perAppEdit);
                btnRemove = v.findViewById(R.id.perAppRemove);
            }
        }
    }
}
