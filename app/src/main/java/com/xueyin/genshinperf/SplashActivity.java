package com.xueyin.genshinperf;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen - hilangkan status bar hitam
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat ctrl = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        ctrl.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
        ctrl.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setContentView(R.layout.activity_splash);

        TextView title = findViewById(R.id.splashTitle);
        TextView subtitle = findViewById(R.id.splashSubtitle);
        View avatar = findViewById(R.id.splashAvatar);
        ProgressBar progress = findViewById(R.id.splashProgress);
        TextView loadingText = findViewById(R.id.splashLoading);

        title.startAnimation(AnimationUtils.loadAnimation(this, R.anim.glow_pulse));
        avatar.startAnimation(AnimationUtils.loadAnimation(this, R.anim.glow_pulse));

        ValueAnimator anim = ValueAnimator.ofInt(0, 100);
        anim.setDuration(2400);
        anim.addUpdateListener(a -> progress.setProgress((int) a.getAnimatedValue()));
        anim.start();

        progress.postDelayed(() -> {
            if (CrashHandler.hasCrash(this)) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.crash_log_title)
                        .setMessage(R.string.crash_dialog_msg)
                        .setPositiveButton(R.string.yes, (d, w) -> {
                            CrashHandler.markSeen(this);
                            startActivity(new Intent(this, CrashLogActivity.class));
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        })
                        .setNegativeButton(R.string.no, (d, w) -> {
                            CrashHandler.markSeen(this);
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        })
                        .setCancelable(false)
                        .show();
            } else {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        }, 2600);
    }
}
