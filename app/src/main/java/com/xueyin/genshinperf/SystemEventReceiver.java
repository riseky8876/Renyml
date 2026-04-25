package com.xueyin.genshinperf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * SystemEventReceiver
 *
 * Listens for system broadcasts:
 *  - ACTION_POWER_CONNECTED / DISCONNECTED  → charging state
 *  - ACTION_SCREEN_ON / OFF                 → idle state
 *
 * Registered dynamically in UsageStatsMonitor (not in Manifest)
 * so it only lives while the monitor service is active.
 */
public class SystemEventReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        SmartProfileManager mgr = SmartProfileManager.get(context);

        switch (intent.getAction()) {
            case Intent.ACTION_POWER_CONNECTED:
                mgr.onChargingChanged(true);
                break;

            case Intent.ACTION_POWER_DISCONNECTED:
                mgr.onChargingChanged(false);
                break;

            case Intent.ACTION_SCREEN_OFF:
                mgr.onScreenChanged(false);
                break;

            case Intent.ACTION_SCREEN_ON:
                mgr.onScreenChanged(true);
                break;
        }
    }
}
