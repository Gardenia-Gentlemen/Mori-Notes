package com.mori.notebook;

/* loaded from: classes.dex */
public class UiPrefs {
    private static final java.lang.String K_BOLD = "home_title_bold";
    private static final java.lang.String K_FONT_FAMILY = "editor_font_family";
    private static final java.lang.String K_FONT_SP = "editor_font_sp";
    private static final java.lang.String K_GRID = "grid_mode";
    private static final java.lang.String K_SCALE = "home_title_scale";
    private static final java.lang.String K_THEME = "theme_mode";
    private static final java.lang.String K_TREE = "store_tree_uri";
    private static final java.lang.String P = "ui_prefs";

    private static android.content.SharedPreferences sp(android.content.Context context) {
        return context.getSharedPreferences(P, 0);
    }

    public static boolean isHomeTitleBold(android.content.Context context) {
        return sp(context).getBoolean(K_BOLD, true);
    }

    public static void setHomeTitleBold(android.content.Context context, boolean z) {
        sp(context).edit().putBoolean(K_BOLD, z).apply();
    }

    public static float getHomeTitleScale(android.content.Context context) {
        return sp(context).getFloat(K_SCALE, 1.0f);
    }

    public static void setHomeTitleScale(android.content.Context context, float f) {
        sp(context).edit().putFloat(K_SCALE, f).apply();
    }

    public static android.net.Uri getTreeUri(android.content.Context context) {
        java.lang.String string = sp(context).getString(K_TREE, null);
        if (string == null) {
            return null;
        }
        return android.net.Uri.parse(string);
    }

    public static void setTreeUri(android.content.Context context, android.net.Uri uri) {
        if (uri == null) {
            sp(context).edit().remove(K_TREE).apply();
        } else {
            sp(context).edit().putString(K_TREE, uri.toString()).apply();
        }
    }

    public static int getThemeMode(android.content.Context context) {
        return sp(context).getInt(K_THEME, 0);
    }

    public static void setThemeMode(android.content.Context context, int i) {
        sp(context).edit().putInt(K_THEME, i).apply();
    }

    public static boolean isGridMode(android.content.Context context) {
        return sp(context).getBoolean(K_GRID, false);
    }

    public static void setGridMode(android.content.Context context, boolean z) {
        sp(context).edit().putBoolean(K_GRID, z).apply();
    }

    public static float getEditorFontSp(android.content.Context context) {
        return sp(context).getFloat(K_FONT_SP, 16.0f);
    }

    public static void setEditorFontSp(android.content.Context context, float f) {
        sp(context).edit().putFloat(K_FONT_SP, f).apply();
    }

    public static java.lang.String getEditorFontFamily(android.content.Context context) {
        return sp(context).getString(K_FONT_FAMILY, "sans");
    }

    public static void setEditorFontFamily(android.content.Context context, java.lang.String str) {
        sp(context).edit().putString(K_FONT_FAMILY, str).apply();
    }
}
