package cz.mroczis.ambientbattery.util;

import cz.mroczis.ambientbattery.App;

/**
 * Preferences helper
 * Created by Michal Mroček [mroczis@gmail.com] on 27.12.15.
 */
public class Preferences {

    private static final String PREF_ENABLED = "enabled";
    private static final String PREF_PERCENT_SPEED = "level_time";
    private static final String PREF_PERCENT_CHANGE = "percent_change";
    private static final String PREF_LOCKSCREEN_ENABLED = "lockscreen_enabled";
    private static final String PREF_CALCULATE_ENABLED = "calculate_enabled";
    private static final String PREF_STRIP_ENABLED = "strip_enabled";

    public static final long SPEED_INVALID = 0L;
    private static final long YEAR_MILLISECONDS = 365 * 24 * 60 * 60 * 1000L;

    public static boolean isEnabled () {
        return App.getPreferences().getBoolean(PREF_ENABLED, true);
    }

    public static void setEnabled (boolean enabled) {
        App.getPreferences().edit().putBoolean(PREF_ENABLED, enabled).apply();
    }

    public static boolean isLockscreenEnabled () {
        return App.getPreferences().getBoolean(PREF_LOCKSCREEN_ENABLED, true);
    }

    public static void setLockscreenEnabled (boolean enabled) {
        App.getPreferences().edit().putBoolean(PREF_LOCKSCREEN_ENABLED, enabled).apply();
    }

    public static boolean isCalculatingEnabled () {
        return App.getPreferences().getBoolean(PREF_CALCULATE_ENABLED, true);
    }

    public static void setCalculatingEnabled (boolean enabled) {
        App.getPreferences().edit().putBoolean(PREF_CALCULATE_ENABLED, enabled).apply();
    }

    public static void setPercentChangeTime (long time) {
        App.getPreferences().edit().putLong(PREF_PERCENT_CHANGE, time).apply();
    }

    public static long getPercentChangeTime() {
        return App.getPreferences().getLong(PREF_PERCENT_CHANGE, SPEED_INVALID);
    }

    public static void setStripEnabled(boolean enabled) {
        App.getPreferences().edit().putBoolean(PREF_STRIP_ENABLED, enabled).apply();
    }

    public static boolean isStripEnabled () {
        return App.getPreferences().getBoolean(PREF_STRIP_ENABLED, true);
    }

    /**
     *
     * @return how long it took to consume 1% of battery
     */
    public static long getPercentSpeed() {
        return App.getPreferences().getLong(PREF_PERCENT_SPEED, SPEED_INVALID);
    }

    public static void setPercentSpeed(long time) {
        App.getPreferences().edit().putLong(PREF_PERCENT_SPEED, time).apply();
    }

    public static boolean isPercentSpeedValid () {
        long speed = App.getPreferences().getLong(PREF_PERCENT_SPEED, SPEED_INVALID);
        return speed != 0 && speed < YEAR_MILLISECONDS;
    }

}
