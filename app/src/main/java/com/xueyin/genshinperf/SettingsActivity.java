package com.xueyin.genshinperf;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private ImageView bgPreview, headerPreview, avatarPreview;
    private SeekBar blurSeek, headerHeightSeek;
    private SwitchCompat darkSwitch, autoScrollSwitch;
    private RadioGroup maxLinesGroup;
    private EditText scriptPath;

    private String tmpBg, tmpHeader, tmpAvatar;
    private String tmpAccent;
    private boolean tmpBgIsGif = false;

    private ActivityResultLauncher<String[]> pickBg, pickHeader, pickAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefs = getSharedPreferences("gpm_prefs", MODE_PRIVATE);

        bgPreview      = findViewById(R.id.bgPreview);
        headerPreview  = findViewById(R.id.headerPreview);
        avatarPreview  = findViewById(R.id.avatarPreview);
        blurSeek       = findViewById(R.id.blurSeek);
        headerHeightSeek = findViewById(R.id.headerHeightSeek);
        darkSwitch     = findViewById(R.id.darkSwitch);
        autoScrollSwitch = findViewById(R.id.autoScrollSwitch);
        maxLinesGroup  = findViewById(R.id.maxLinesGroup);
        scriptPath     = findViewById(R.id.scriptPath);

        // Picker untuk background: support GIF + gambar biasa
        pickBg = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> handlePickUri(uri, 0));

        // Picker untuk header & avatar: hanya gambar biasa
        pickHeader = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> handlePickUri(uri, 1));

        pickAvatar = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> handlePickUri(uri, 2));

        // Background buttons
        findViewById(R.id.btnPickBg).setOnClickListener(v ->
                pickBg.launch(new String[]{"image/*", "image/gif"}));
        findViewById(R.id.btnDefaultBg).setOnClickListener(v -> {
            tmpBg = "";
            tmpBgIsGif = false;
            bgPreview.setImageResource(R.drawable.bg_main);
        });

        // Header buttons
        findViewById(R.id.btnPickHeader).setOnClickListener(v ->
                pickHeader.launch(new String[]{"image/*"}));
        findViewById(R.id.btnDefaultHeader).setOnClickListener(v -> {
            tmpHeader = "";
            headerPreview.setImageResource(R.drawable.header_default);
        });

        // Avatar buttons
        findViewById(R.id.btnPickAvatar).setOnClickListener(v ->
                pickAvatar.launch(new String[]{"image/*"}));
        findViewById(R.id.btnDefaultAvatar).setOnClickListener(v -> {
            tmpAvatar = "";
            avatarPreview.setImageResource(R.drawable.furina);
        });

        // Accent color buttons
        View.OnClickListener accent = v -> {
            tmpAccent = String.valueOf(v.getTag());
            // Highlight selected
            int[] ids = {R.id.accentCyan, R.id.accentGold, R.id.accentPink, R.id.accentPurple};
            for (int id : ids) {
                View av = findViewById(id);
                av.setAlpha(av == v ? 1.0f : 0.5f);
                av.setScaleX(av == v ? 1.15f : 1.0f);
                av.setScaleY(av == v ? 1.15f : 1.0f);
            }
        };
        findViewById(R.id.accentCyan).setTag("#6FD6FF");   findViewById(R.id.accentCyan).setOnClickListener(accent);
        findViewById(R.id.accentGold).setTag("#F2D17E");   findViewById(R.id.accentGold).setOnClickListener(accent);
        findViewById(R.id.accentPink).setTag("#F6B8E2");   findViewById(R.id.accentPink).setOnClickListener(accent);
        findViewById(R.id.accentPurple).setTag("#5B4BA8"); findViewById(R.id.accentPurple).setOnClickListener(accent);

        // Blur preview live
        blurSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                // preview blur di bgPreview
                bgPreview.setAlpha(1.0f - (p / 25f) * 0.4f);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        findViewById(R.id.btnClearAllLogs).setOnClickListener(v -> {
            LogManager.clearAll();
            Toast.makeText(this, "Log dibersihkan", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnSave).setOnClickListener(v -> save());

        load();
    }

    private void handlePickUri(Uri uri, int kind) {
        if (uri == null) return;
        try {
            // Ambil mime type untuk deteksi GIF
            String mime = getContentResolver().getType(uri);
            boolean isGif = "image/gif".equals(mime);

            String ext  = isGif ? ".gif" : ".png";
            String name = kind == 0 ? "custom_bg" + ext
                        : kind == 1 ? "custom_header.png"
                        : "custom_avatar.png";

            File out = new File(getFilesDir(), name);

            try (InputStream is = getContentResolver().openInputStream(uri);
                 FileOutputStream fos = new FileOutputStream(out)) {
                if (is == null) throw new Exception("stream null");
                if (isGif || kind == 0 && isGif) {
                    // Salin langsung (GIF harus disimpan mentah)
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = is.read(buf)) != -1) fos.write(buf, 0, n);
                } else {
                    // Decode sebagai Bitmap lalu simpan PNG
                    android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
                    if (bmp == null) throw new Exception("decode failed");
                    bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos);
                }
            }

            // Preview dengan Glide (support GIF)
            ImageView target = kind == 0 ? bgPreview : kind == 1 ? headerPreview : avatarPreview;
            Glide.with(this).load(out).into(target);

            String path = out.getAbsolutePath();
            if (kind == 0) { tmpBg = path; tmpBgIsGif = isGif; }
            else if (kind == 1) tmpHeader = path;
            else tmpAvatar = path;

        } catch (Exception e) {
            Toast.makeText(this, "Gagal memuat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void load() {
        String bg = prefs.getString("bg_path", null);
        if (bg != null) { tmpBg = bg; tryPreview(bgPreview, bg); }

        String h = prefs.getString("header_path", null);
        if (h != null) { tmpHeader = h; tryPreview(headerPreview, h); }

        String a = prefs.getString("avatar_path", null);
        if (a != null) { tmpAvatar = a; tryPreview(avatarPreview, a); }

        blurSeek.setProgress(prefs.getInt("bg_blur", 6));
        headerHeightSeek.setProgress(prefs.getInt("header_height", 180));
        darkSwitch.setChecked(prefs.getBoolean("dark_mode", false));
        autoScrollSwitch.setChecked(prefs.getBoolean("auto_scroll", true));

        int max = prefs.getInt("max_lines", 500);
        if (max == 100) maxLinesGroup.check(R.id.lines100);
        else if (max == 1000) maxLinesGroup.check(R.id.lines1000);
        else maxLinesGroup.check(R.id.lines500);

        scriptPath.setText(prefs.getString("script_path", "/sdcard/GenshinPerf/main.sh"));
        tmpAccent = prefs.getString("accent", "#6FD6FF");

        // Highlight aksen yang aktif saat ini
        highlightCurrentAccent();
    }

    private void highlightCurrentAccent() {
        int[][] accents = {
            {R.id.accentCyan,   0},
            {R.id.accentGold,   0},
            {R.id.accentPink,   0},
            {R.id.accentPurple, 0}
        };
        String[] tags = {"#6FD6FF", "#F2D17E", "#F6B8E2", "#5B4BA8"};
        int[] ids = {R.id.accentCyan, R.id.accentGold, R.id.accentPink, R.id.accentPurple};
        for (int i = 0; i < ids.length; i++) {
            View v = findViewById(ids[i]);
            boolean sel = tags[i].equalsIgnoreCase(tmpAccent);
            v.setAlpha(sel ? 1.0f : 0.5f);
            v.setScaleX(sel ? 1.15f : 1.0f);
            v.setScaleY(sel ? 1.15f : 1.0f);
        }
    }

    private void tryPreview(ImageView v, String path) {
        try {
            File f = new File(path);
            if (f.exists()) Glide.with(this).load(f).into(v);
        } catch (Exception ignored) { }
    }

    private void save() {
        SharedPreferences.Editor e = prefs.edit();
        if (tmpBg != null) {
            if (tmpBg.isEmpty()) e.remove("bg_path");
            else e.putString("bg_path", tmpBg);
        }
        if (tmpHeader != null) {
            if (tmpHeader.isEmpty()) e.remove("header_path");
            else e.putString("header_path", tmpHeader);
        }
        if (tmpAvatar != null) {
            if (tmpAvatar.isEmpty()) e.remove("avatar_path");
            else e.putString("avatar_path", tmpAvatar);
        }
        e.putInt("bg_blur", blurSeek.getProgress());
        e.putInt("header_height", headerHeightSeek.getProgress());
        e.putBoolean("dark_mode", darkSwitch.isChecked());
        e.putBoolean("auto_scroll", autoScrollSwitch.isChecked());

        int max = 500;
        int id = maxLinesGroup.getCheckedRadioButtonId();
        if (id == R.id.lines100) max = 100;
        else if (id == R.id.lines1000) max = 1000;
        e.putInt("max_lines", max);

        String sp = scriptPath.getText().toString().trim();
        if (sp.isEmpty()) sp = "/sdcard/GenshinPerf/main.sh";
        e.putString("script_path", sp);

        if (tmpAccent != null) e.putString("accent", tmpAccent);
        e.apply();

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
        finish();
    }
}
