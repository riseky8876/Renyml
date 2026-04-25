package com.xueyin.genshinperf;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AppPickerActivity extends AppCompatActivity {

    public static final String EXTRA_PACKAGE = "picked_package";
    public static final String EXTRA_LABEL   = "picked_label";

    private RecyclerView recycler;
    private ProgressBar progress;
    private AppAdapter adapter;
    private List<AppInfo> allApps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_picker);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Pilih Aplikasi");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        progress  = findViewById(R.id.pickerProgress);
        recycler  = findViewById(R.id.pickerRecycler);
        EditText search = findViewById(R.id.pickerSearch);

        adapter = new AppAdapter(new ArrayList<>(), this::onAppPicked);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadApps();
    }

    private void loadApps() {
        progress.setVisibility(View.VISIBLE);
        new AsyncTask<Void, Void, List<AppInfo>>() {
            @Override protected List<AppInfo> doInBackground(Void... v) {
                PackageManager pm = getPackageManager();
                List<ApplicationInfo> infos = pm.getInstalledApplications(PackageManager.GET_META_DATA);
                List<AppInfo> list = new ArrayList<>();
                for (ApplicationInfo info : infos) {
                    // Only show user-installed and launchable apps
                    if (pm.getLaunchIntentForPackage(info.packageName) == null) continue;
                    AppInfo a = new AppInfo();
                    a.packageName = info.packageName;
                    a.label       = pm.getApplicationLabel(info).toString();
                    a.icon        = pm.getApplicationIcon(info);
                    list.add(a);
                }
                list.sort(Comparator.comparing(x -> x.label.toLowerCase()));
                return list;
            }

            @Override protected void onPostExecute(List<AppInfo> list) {
                allApps = list;
                progress.setVisibility(View.GONE);
                adapter.setData(list);
            }
        }.execute();
    }

    private void filter(String query) {
        List<AppInfo> filtered = new ArrayList<>();
        String q = query.toLowerCase().trim();
        for (AppInfo a : allApps) {
            if (a.label.toLowerCase().contains(q) || a.packageName.toLowerCase().contains(q)) {
                filtered.add(a);
            }
        }
        adapter.setData(filtered);
    }

    private void onAppPicked(AppInfo app) {
        Intent result = new Intent();
        result.putExtra(EXTRA_PACKAGE, app.packageName);
        result.putExtra(EXTRA_LABEL, app.label);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    // ── Data model ─────────────────────────────────────────────────────────────
    static class AppInfo {
        String packageName;
        String label;
        Drawable icon;
    }

    // ── Adapter ────────────────────────────────────────────────────────────────
    static class AppAdapter extends RecyclerView.Adapter<AppAdapter.VH> {
        interface OnPick { void pick(AppInfo app); }
        private List<AppInfo> data;
        private final OnPick listener;

        AppAdapter(List<AppInfo> data, OnPick listener) {
            this.data = data;
            this.listener = listener;
        }

        void setData(List<AppInfo> d) { this.data = d; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_app_picker, parent, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            AppInfo a = data.get(pos);
            h.icon.setImageDrawable(a.icon);
            h.label.setText(a.label);
            h.pkg.setText(a.packageName);
            h.itemView.setOnClickListener(v -> listener.pick(a));
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView label, pkg;
            VH(View v) {
                super(v);
                icon  = v.findViewById(R.id.appIcon);
                label = v.findViewById(R.id.appLabel);
                pkg   = v.findViewById(R.id.appPackage);
            }
        }
    }
}
