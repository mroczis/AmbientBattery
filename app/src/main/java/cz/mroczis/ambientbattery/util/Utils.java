package cz.mroczis.ambientbattery.util;

import android.os.Build;

import cz.mroczis.ambientbattery.service.BatteryService;

/**
 * Utility class
 * Created by Michal Mroček [mroczis@gmail.com] on 29.12.15.
 */
public class Utils {
    private static final String NEXUS_6P = "angler";
    private static final String NEXUS_6 = "shamu";
    private static final String NEXUS_5X = "bullhead";

    /**
     * Fixed sizes of batteries for supported Nexus devices
     * Method {@link BatteryService#getBatteryCapacity()} works on stock ROMs
     * but someone might use this app on custom ROMs...
     *
     * @return stock battery capacity in mAh
     */
    public static double getBatteryCapacityFromCache() {
        switch (Build.BOARD.toLowerCase()) {
            case NEXUS_6P:
                return 3450D;
            case NEXUS_6:
                return 3220D;
            case NEXUS_5X:
                return 2300D;
            default:
                return 0D;
        }
    }
}
