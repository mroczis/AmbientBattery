package cz.mroczis.ambientbattery.model;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;

import cz.mroczis.ambientbattery.R;

/**
 * Defines days, hours and minutes of capacity remaining (discharging) or time to 100% charge (charging)
 * Created by Michal Mroček [mroczis@gmail.com] on 27.12.15.
 */
public class Capacity {

    int mDays;
    int mHours;
    int mMinutes;

    public Capacity(double hours) {
        hours = Math.abs(hours);

        mDays = (int) hours / 24;
        mHours = (int) hours - mDays * 24;
        mMinutes = (int) (((hours - mDays * 24) - mHours) * 60);
    }


    public int getDays() {
        return mDays;
    }

    public int getHours() {
        return mHours;
    }

    public int getMinutes() {
        return mMinutes;
    }

    /**
     * Converts days, hours and minutes to human readable string
     *
     * @param context context
     * @return example: 2 days 3hours 48 minutes
     */
    @Nullable
    public String toHumanString(Context context) {
        Resources resources = context.getResources();
        String result = "";

        if (mDays > 365) {
            // which phone would last more than one year?
            return null;
        } else if (mDays > 6) {
            return resources.getString(R.string.capacity_week) + " ";
        }

        if (mDays > 0) {
            result += resources.getQuantityString(R.plurals.capacity_day, mDays, mDays) + " ";
        }

        if (mHours > 0) {
            result += resources.getQuantityString(R.plurals.capacity_hour, mHours, mHours) + " ";
        }

        if (mMinutes > 0) {
            result += resources.getQuantityString(R.plurals.capacity_minute, mMinutes, mMinutes) + " ";
        }

        return result;
    }
}
