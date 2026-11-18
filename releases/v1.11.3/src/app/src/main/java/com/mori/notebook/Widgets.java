package com.mori.notebook;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;

/** 桌面小组件统一刷新入口；投递到主线程执行，避免与数据加载重入。 */
public final class Widgets {

    private Widgets() {
    }

    public static void updateAll(Context context) {
        if (context == null) return;
        final Context app = context.getApplicationContext();
        try {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        TodoWidgetProvider.refreshAll(app);
                    } catch (Throwable ignored) {
                    }
                    try {
                        NoteWidgetProvider.refreshAll(app);
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable ignored) {
        }
    }

    /** 当前是否深色（跟随主题设置） */
    static boolean isDark(Context c) {
        int mode = UiPrefs.getThemeMode(c);
        if (mode == 2) return true;
        if (mode == 1) return false;
        return (c.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }
}