package cz.mroczis.ambientbattery.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cz.mroczis.ambientbattery.service.BatteryService;
import cz.mroczis.ambientbattery.util.Preferences;

/**
 * Receiver which starts core service after phone boot is completed
 * Created by Michal Mroček [mroczis@gmail.com] on 27.12.15.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Preferences.isEnabled()) {
            context.startService(new Intent(context, BatteryService.class));
        }
    }
}
