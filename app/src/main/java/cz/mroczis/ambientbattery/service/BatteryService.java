package cz.mroczis.ambientbattery.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import cz.mroczis.ambientbattery.R;
import cz.mroczis.ambientbattery.model.BatteryInfo;
import cz.mroczis.ambientbattery.model.Capacity;
import cz.mroczis.ambientbattery.util.Preferences;
import cz.mroczis.ambientbattery.util.Utils;

/**
 * Battery service which handles everything
 * Created by Michal Mroček [mroczis@gmail.com] on 27.12.15.
 */
public class BatteryService extends CoreService {

    private static final int NOTIFICATION_ID = 1;
    private static final int CACHE_SIZE = 10;
    private static final int REFRESH_INTERVAL = 6000;
    private static final int MINUTES_2 = 120000;
    private static final String TAG = "BatteryService";

    private RemoteViews mRemoteViews;
    private Handler mUiHandler;
    private BatteryInfo mBatteryInfo = null;
    private List<Integer> mConsumptionHistory = new ArrayList<>();

    private boolean mScreenOff = false;
    private boolean mDeviceUnlocked = true;
    private double mBatteryCapacity = Integer.MAX_VALUE;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        getNotificationManager().cancel(NOTIFICATION_ID);

        mBatteryCapacity = getBatteryCapacity();

        if (mUiHandler == null) {
            mUiHandler = new Handler(Looper.getMainLooper());
        }

        if (Preferences.isCalculatingEnabled()) {
            mRemoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);
        } else {
            mRemoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout_center);
        }

        checkMaConsumption();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        getNotificationManager().cancel(NOTIFICATION_ID);
        Preferences.setPercentSpeed(Preferences.SPEED_INVALID);
        mUiHandler.removeCallbacks(null);
        mUiHandler = null;
        super.onDestroy();
    }

    /**
     * Reads mA consumption and caches it
     */
    private void checkMaConsumption() {
        int rate = getCurrentMaRate() / 1000; // uA --> mA

        if (rate != Integer.MAX_VALUE) {
            if (mConsumptionHistory.size() == CACHE_SIZE) {
                // remember just 15 freshest values
                mConsumptionHistory.remove(0);
            }

            if (mConsumptionHistory.size() == 0 || mConsumptionHistory.get(mConsumptionHistory.size() - 1) != rate) {
                mConsumptionHistory.add(rate);
                Log.d(TAG, "Adding new entry '" + rate + "' mA");
            }
        }

        if (mBatteryInfo != null && mBatteryInfo.isCharging()) {
            refreshNotificationData();
        }

        if (mUiHandler != null) {
            mUiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Preferences.isEnabled() && Preferences.isCalculatingEnabled()) {
                        checkMaConsumption();
                    } else if (!Preferences.isEnabled()) {
                        stopSelf();
                    }
                }
            }, REFRESH_INTERVAL);
        }
    }

    /**
     * Invoked when screen is turned on
     */
    @Override
    protected void onScreenOn() {
        mScreenOff = false;

        if (!mDeviceUnlocked && Preferences.isLockscreenEnabled() && mBatteryInfo != null && Preferences.isCalculatingEnabled()) {
            mRemoteViews.setTextColor(R.id.battery_level, getClr(R.color.white));
            postNotification();
        } else if (!Preferences.isLockscreenEnabled()) {
            getNotificationManager().cancel(NOTIFICATION_ID);
        }
    }

    /**
     * Invoked when screen is turned off
     */
    @Override
    protected void onScreenOff() {
        mScreenOff = true;
        mDeviceUnlocked = false;
        if (mBatteryInfo != null && mUiHandler != null) {
            mUiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // post delayed to avoid small animation glitch when user turns off display and want to turn it on immediately
                    refreshNotificationData();
                }
            }, 200);
        }
    }

    @Override
    protected void onDeviceUnlocked() {
        mDeviceUnlocked = true;
        Log.d(TAG, "Device was unlocked");
        getNotificationManager().cancel(NOTIFICATION_ID);
    }

    /**
     * Called when batter level or voltage has changed
     *
     * @param info new info about battery
     */
    @Override
    protected void onBatteryChanged(BatteryInfo info) {
        if (mBatteryInfo != null && mBatteryInfo.isCharging() != info.isCharging()) {
            // was charging and now is not (vice versa) - clear stats
            mConsumptionHistory.clear();
            Preferences.setPercentSpeed(Preferences.SPEED_INVALID);
            Preferences.setPercentChangeTime(System.currentTimeMillis());
        } else if (mBatteryInfo != null && !mBatteryInfo.isCharging()) {

            if (mBatteryInfo.getLevel() != info.getLevel()) {
                // Standard battery change
                Log.d(TAG, "Discharge speed - 1% per" + (System.currentTimeMillis() - Preferences.getPercentChangeTime()) + " ms");
                Preferences.setPercentSpeed(System.currentTimeMillis() - Preferences.getPercentChangeTime());
                Preferences.setPercentChangeTime(System.currentTimeMillis());
            } else if (!Preferences.isPercentSpeedValid() && System.currentTimeMillis() - Preferences.getPercentChangeTime() > MINUTES_2) {
                // Level is same for a while - lets just extend estimated battery life
                Log.d(TAG, "Estimated discharge speed - 1% per" + (System.currentTimeMillis() - Preferences.getPercentChangeTime()) + " ms");
                Preferences.setPercentSpeed(System.currentTimeMillis() - Preferences.getPercentChangeTime());
            }
        }

        mBatteryInfo = info;

        if (mScreenOff || (!mDeviceUnlocked && Preferences.isLockscreenEnabled())) {
            refreshNotificationData();
        }
    }


    /**
     * Updates all data about battery in notification
     */
    private void refreshNotificationData() {
        if (mBatteryInfo != null && (mScreenOff || (!mDeviceUnlocked && Preferences.isLockscreenEnabled()))) {

            if (Preferences.isCalculatingEnabled()) {
                Capacity capacity = getEstimatedTime();

                if (mBatteryInfo.isCharging()) {
                    switch (mBatteryInfo.getChargingType()) {
                        case BatteryManager.BATTERY_PLUGGED_AC:
                            mRemoteViews.setTextViewText(R.id.charging_state, getString(R.string.charging_ac));
                            break;
                        case BatteryManager.BATTERY_PLUGGED_USB:
                            mRemoteViews.setTextViewText(R.id.charging_state, getString(R.string.charging_usb));
                            break;
                        case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                            mRemoteViews.setTextViewText(R.id.charging_state, getString(R.string.charging_wireless));
                            break;
                    }

                    if (mBatteryInfo.getLevel() == mBatteryInfo.getMaxLevel()) {
                        mRemoteViews.setTextViewText(R.id.time_remaining, getString(R.string.charging_full));
                    } else if (capacity != null && capacity.toHumanString(getApplicationContext()) != null && mBatteryCapacity != Integer.MAX_VALUE) {
                        mRemoteViews.setTextViewText(R.id.time_remaining, capacity.toHumanString(getApplicationContext()) + getApplicationContext().getResources().getString(R.string.capacity_charge_postfix));
                    } else if (mBatteryCapacity == Integer.MAX_VALUE) {
                        mRemoteViews.setTextViewText(R.id.time_remaining, getString(R.string.capacity_unsupported));
                    } else {
                        mRemoteViews.setTextViewText(R.id.time_remaining, getString(R.string.capacity_calculating));
                    }

                } else {
                    mRemoteViews.setTextViewText(R.id.charging_state, getString(R.string.charging_not));

                    if (capacity != null && capacity.toHumanString(getApplicationContext()) != null && mBatteryCapacity != Integer.MAX_VALUE) {
                        mRemoteViews.setTextViewText(R.id.time_remaining, capacity.toHumanString(getApplicationContext()) + getApplicationContext().getResources().getString(R.string.capacity_discharge_postfix));
                    } else if (mBatteryCapacity == Integer.MAX_VALUE) {
                        mRemoteViews.setTextViewText(R.id.time_remaining, getString(R.string.capacity_unsupported));
                    } else if (capacity != null && capacity.toHumanString(getApplicationContext()) == null) {
                        mRemoteViews.setTextViewText(R.id.time_remaining, getString(R.string.charging_infinite));
                    } else {
                        mRemoteViews.setTextViewText(R.id.time_remaining, getString(R.string.capacity_calculating));
                    }
                }
            }

            int notificationWidth = getNotificationWidth();

            mRemoteViews.setViewVisibility(R.id.parent, Preferences.isStripEnabled() ? View.VISIBLE : View.GONE);
            mRemoteViews.setTextViewText(R.id.battery_level, String.valueOf(mBatteryInfo.getLevel()));
            mRemoteViews.setViewPadding(R.id.parent, 0, 0, (int) (notificationWidth - (notificationWidth * mBatteryInfo.getLevel() / (float) mBatteryInfo.getMaxLevel())), 0);

            if (Preferences.isCalculatingEnabled()) {
                mRemoteViews.setTextColor(R.id.battery_level, getClr(mScreenOff ? R.color.black : R.color.white));
            }

            postNotification();
        }
    }

    /**
     * Makes our notification :)
     */
    private void postNotification() {
        if (!Preferences.isEnabled()) {
            getNotificationManager().cancel(NOTIFICATION_ID);
            stopSelf();
        }

        PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(Intent.ACTION_POWER_USAGE_SUMMARY), PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setContent(mRemoteViews)
                .setSmallIcon(R.drawable.ic_notification)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(intent)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MAX);

        getNotificationManager().notify(NOTIFICATION_ID, builder.build());
        Log.d(TAG, "Updating battery notification");

    }

    /**
     * Estimated time to full battery (if charging) or to empty battery (if discharging)
     *
     * @return null if failed or estimated time
     */
    @Nullable
    private Capacity getEstimatedTime() {
        if (mBatteryInfo == null) {
            // we have any piece of information about battery
            return null;
        } else {
            if (mBatteryInfo.isCharging() && mConsumptionHistory.size() >= CACHE_SIZE / 3) {
                // use system values - they are accurate when charging
                int average = getMaAverage();
                double batteryCapacity = mBatteryCapacity;

                if (batteryCapacity != Integer.MAX_VALUE) {
                    double remaining;

                    // charging - estimated time to 100% from current state
                    remaining = batteryCapacity * (mBatteryInfo.getMaxLevel() - mBatteryInfo.getLevel()) / (float) mBatteryInfo.getMaxLevel();

                    double ratio = remaining / average;

                    Log.d(TAG, "Estimated speed (dis)charge time is " + ratio + " hours");

                    if (ratio < 0) {
                        // charging and consumption is bigger
                        mConsumptionHistory.clear();
                        return null;
                    } else {
                        return new Capacity(remaining / average);
                    }
                } else {
                    Log.e(TAG, "Battery capacity is unknown - cannot compute time to dis(charge)");
                    return null;
                }
            } else if (!mBatteryInfo.isCharging() && Preferences.isPercentSpeedValid()) {
                // use 1% difference cause system reports kinda random values...
                long percentSpeed = Preferences.getPercentSpeed();
                long timeDifference = System.currentTimeMillis() - Preferences.getPercentChangeTime();

                if (timeDifference > percentSpeed) {
                    // At least 1% should be gone by this time but it is not --> extend estimated battery life by magic number 1.2
                    percentSpeed *= 1.2F;
                    Preferences.setPercentSpeed(percentSpeed);
                }

                double millisToEmptyBattery = (percentSpeed * mBatteryInfo.getLevel() - timeDifference);
                double hoursToEmptyBattery = millisToEmptyBattery / (1000 * 60 * 60D);

                Log.d(TAG, "Discharge estimated - " + hoursToEmptyBattery + " hours");
                return new Capacity(hoursToEmptyBattery); // milliseconds --> hours
            } else {
                // not enough info
                return null;
            }
        }
    }

    /**
     * Calculates average mA consumption from cached values
     *
     * @return average consumption
     */
    private int getMaAverage() {
        int totalSize = 0;

        for (Integer integer : mConsumptionHistory) {
            totalSize += integer;
        }

        Log.d(TAG, "Average power consumption " + totalSize / mConsumptionHistory.size());
        return totalSize / mConsumptionHistory.size();
    }

    /**
     * Current mA consumption (negative if discharging, positive if charging)
     *
     * @return mA consumption of Integer.MAX_VALUE if is this values unreachable
     */
    private int getCurrentMaRate() {
        try {
            File fl = new File("/sys/class/power_supply/battery/current_now");
            FileInputStream fin = new FileInputStream(fl);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            reader.close();
            fin.close();

            try {
                return Integer.valueOf(sb.toString().replaceAll("[^0-9-]", ""));
            } catch (NumberFormatException e) {
                return Integer.MAX_VALUE;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Reads battery capacity from PowerProfile
     *
     * @return battery capacity or Integer.MAX_VALUE if failed
     */
    private double getBatteryCapacity() {
        Object mPowerProfile_ = null;

        final String POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile";

        try {
            mPowerProfile_ = Class.forName(POWER_PROFILE_CLASS)
                    .getConstructor(Context.class).newInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            return (Double) Class
                    .forName(POWER_PROFILE_CLASS)
                    .getMethod("getAveragePower", java.lang.String.class)
                    .invoke(mPowerProfile_, "battery.capacity");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Utils.getBatteryCapacityFromCache();
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
    }

    private int getNotificationWidth() {
        return getResources().getDisplayMetrics().widthPixels - 2 * getResources().getDimensionPixelSize(R.dimen.notification_margin);
    }

    /**
     * Get the color from resource
     *
     * @param id color resource
     * @return color in dec
     */
    @SuppressWarnings("deprecation")
    private int getClr(@ColorRes int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getColor(id);
        } else {
            return getResources().getColor(id);
        }
    }
}
