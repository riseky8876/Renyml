package com.xueyin.genshinperf;

import android.app.Application;

public class GpmApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.install(getApplicationContext());
        LogManager.init(getApplicationContext());
    }
}
