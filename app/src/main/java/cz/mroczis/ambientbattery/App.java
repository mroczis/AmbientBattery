package cz.mroczis.ambientbattery;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Core app class which just holds instance to shared preferences
 * Created by Michal Mroček [mroczis@gmail.com] on 27.12.15.
 */
public class App extends Application {

    private static App sInstance;
    private static SharedPreferences sPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    public static SharedPreferences getPreferences () {
        if (sPreferences == null) {
            sPreferences = PreferenceManager.getDefaultSharedPreferences(sInstance);
        }

        return sPreferences;
    }

    public static App getInstance() {
        return sInstance;
    }
}
