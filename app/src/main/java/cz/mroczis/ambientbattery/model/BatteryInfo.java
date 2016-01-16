package cz.mroczis.ambientbattery.model;

/**
 * Battery info
 * Created by Michal Mroček [mroczis@gmail.com] on 27.12.15.
 */
public class BatteryInfo {

    private int mLevel;
    private int mMaxLevel;

    private boolean mCharging;
    private int mChargingType;

    public BatteryInfo(boolean charging, int chargingType, int level, int maxLevel) {
        this.mCharging = charging;
        this.mChargingType = chargingType;
        this.mLevel = level;
        this.mMaxLevel = maxLevel;
    }

    public boolean isCharging() {
        return mCharging;
    }

    public void setCharging(boolean mCharging) {
        this.mCharging = mCharging;
    }

    public int getChargingType() {
        return mChargingType;
    }

    public void setChargingType(int mChargingType) {
        this.mChargingType = mChargingType;
    }

    public int getLevel() {
        return mLevel;
    }

    public void setLevel(int mLevel) {
        this.mLevel = mLevel;
    }

    public int getMaxLevel() {
        return mMaxLevel;
    }
}
