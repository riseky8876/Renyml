package com.xueyin.genshinperf;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.FileReader;

public class CrashLogActivity extends AppCompatActivity {

    private TextView content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_log);
        content = findViewById(R.id.crashContent);
        load();

        findViewById(R.id.btnShare).setOnClickListener(v -> share());
        findViewById(R.id.btnDelete).setOnClickListener(v -> {
            if (LogManager.crashLogFile() != null) LogManager.crashLogFile().delete();
            Toast.makeText(this, R.string.crash_deleted, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void load() {
        StringBuilder sb = new StringBuilder();
        if (LogManager.crashLogFile() != null && LogManager.crashLogFile().exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(LogManager.crashLogFile()))) {
                String l;
                while ((l = br.readLine()) != null) sb.append(l).append('\n');
            } catch (Exception ignored) { }
        }
        content.setText(sb.length() > 0 ? sb.toString() : "(no crash log)");
    }

    private void share() {
        if (LogManager.crashLogFile() == null || !LogManager.crashLogFile().exists()) return;
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", LogManager.crashLogFile());
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_STREAM, uri);
        i.putExtra(Intent.EXTRA_SUBJECT, "Genshin Performance Manager - Crash Log");
        i.putExtra(Intent.EXTRA_TEXT, content.getText().toString());
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(i, "Share crash log"));
    }
}
