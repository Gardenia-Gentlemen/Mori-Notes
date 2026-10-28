package com.mori.notebook;

/* loaded from: classes.dex */
public class NotebookApp extends android.app.Application {
    @Override // android.app.Application
    public void onCreate() {
        super.onCreate();
        applyTheme(this);
    }

    public static void applyTheme(android.content.Context context) {
        int themeMode = com.mori.notebook.UiPrefs.getThemeMode(context);
        int i = 1;
        if (themeMode != 1) {
            i = 2;
            if (themeMode != 2) {
                i = -1;
            }
        }
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(i);
    }
}
