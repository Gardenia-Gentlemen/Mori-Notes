package com.mori.notebook;

import android.app.Application;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

/** 全局 Application：启动时套用深色模式偏好。 */
public class NotebookApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        applyTheme(this);
        CountdownTime.setDayStartHour(CountdownPrefs.getDayStartHour(this));
        // 首次启动：时区默认取手机系统时区；之后沿用用户选择
        CountdownTime.setTimeZone(CountdownPrefs.resolveTimeZoneId(this));
        if (CountdownPrefs.getSeenSystemTimeZone(this).isEmpty()) {
            CountdownPrefs.setSeenSystemTimeZone(this, java.util.TimeZone.getDefault().getID());
        }
        // 内置字体：首次启动/升级后首次启动把 assets/fonts 的字体解到字体库
        BuiltinFonts.ensure(this);
    }

    public static void applyTheme(Context c) {
        int mode = UiPrefs.getThemeMode(c);
        int night;
        if (mode == 1) night = AppCompatDelegate.MODE_NIGHT_NO;
        else if (mode == 2) night = AppCompatDelegate.MODE_NIGHT_YES;
        else night = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        AppCompatDelegate.setDefaultNightMode(night);
    }
}
