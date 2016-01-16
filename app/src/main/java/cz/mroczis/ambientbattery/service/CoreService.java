package cz.mroczis.ambientbattery.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import cz.mroczis.ambientbattery.model.BatteryInfo;

/**
 * Core for {@link BatteryService} - just receiver and abstract methods
 * Created by Michal Mroček [mroczis@gmail.com] on 27.12.15.
 */
public abstract class CoreService extends Service {

    private BroadcastReceiver mCoreReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent != null && !TextUtils.isEmpty(intent.getAction())) {
                switch (intent.getAction()) {
                    case Intent.ACTION_SCREEN_ON:
                        onScreenOn ();
                        break;
                    case Intent.ACTION_SCREEN_OFF:
                        onScreenOff();
                        break;
                    case Intent.ACTION_BATTERY_CHANGED:
                        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                        int max = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                status == BatteryManager.BATTERY_STATUS_FULL;
                        int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

                        onBatteryChanged (new BatteryInfo(isCharging, chargePlug, level, max));
                        break;
                    case Intent.ACTION_USER_PRESENT:
                        onDeviceUnlocked();
                        break;
                }
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        registerReceiver(mCoreReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
        registerReceiver(mCoreReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        registerReceiver(mCoreReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        registerReceiver(mCoreReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mCoreReceiver);

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected abstract void onScreenOn ();
    protected abstract void onScreenOff ();
    protected abstract void onBatteryChanged (BatteryInfo info);
    protected abstract void onDeviceUnlocked ();
}
