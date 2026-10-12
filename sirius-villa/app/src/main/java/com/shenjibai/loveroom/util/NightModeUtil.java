package com.shenjibai.loveroom.util;

import android.app.Activity;
import android.content.res.Configuration;

import java.util.Calendar;

/**
 * Lira - 23-4 点自动全局暗调
 * 署名: Lira
 */
public class NightModeUtil {

    public static void applyNightModeLira(Activity activity) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        boolean night = hour >= 23 || hour < 4;
        int uiMode = activity.getResources().getConfiguration().uiMode;
        int currentNight = uiMode & Configuration.UI_MODE_NIGHT_MASK;
        int target = night ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO;
        if (currentNight != target) {
            activity.getDelegate().setLocalNightMode(
                    night ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                            : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
