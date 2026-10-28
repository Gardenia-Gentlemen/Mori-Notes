package com.mori.notebook;

import android.content.Context;
import android.content.SharedPreferences;

/** 界面偏好设置 */
public class UiPrefs {
    private static final String P = "ui_prefs";
    private static final String K_BOLD = "home_title_bold";
    private static final String K_SCALE = "home_title_scale";

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(P, Context.MODE_PRIVATE);
    }

    public static boolean isHomeTitleBold(Context c) {
        return sp(c).getBoolean(K_BOLD, true);
    }

    public static void setHomeTitleBold(Context c, boolean v) {
        sp(c).edit().putBoolean(K_BOLD, v).apply();
    }

    /** 标题字号倍率，范围 0.9 ~ 1.3 */
    public static float getHomeTitleScale(Context c) {
        return sp(c).getFloat(K_SCALE, 1.0f);
    }

    public static void setHomeTitleScale(Context c, float v) {
        sp(c).edit().putFloat(K_SCALE, v).apply();
    }

    private static final String K_TREE = "store_tree_uri";

    public static android.net.Uri getTreeUri(Context c) {
        String v = sp(c).getString(K_TREE, null);
        return v == null ? null : android.net.Uri.parse(v);
    }

    public static void setTreeUri(Context c, android.net.Uri uri) {
        if (uri == null) {
            sp(c).edit().remove(K_TREE).apply();
        } else {
            sp(c).edit().putString(K_TREE, uri.toString()).apply();
        }
    }

    private static final String K_THEME = "theme_mode";
    public static int getThemeMode(Context c) {
        return sp(c).getInt(K_THEME, 0);
    }
    public static void setThemeMode(Context c, int v) {
        sp(c).edit().putInt(K_THEME, v).apply();
    }
    private static final String K_GRID = "grid_mode";
    public static boolean isGridMode(Context c) {
        return sp(c).getBoolean(K_GRID, false);
    }
    public static void setGridMode(Context c, boolean v) {
        sp(c).edit().putBoolean(K_GRID, v).apply();
    }
    private static final String K_FONT_SP = "editor_font_sp";
    private static final String K_FONT_FAMILY = "editor_font_family";
    public static float getEditorFontSp(Context c) {
        return sp(c).getFloat(K_FONT_SP, 16f);
    }
    public static void setEditorFontSp(Context c, float v) {
        sp(c).edit().putFloat(K_FONT_SP, v).apply();
    }
    public static String getEditorFontFamily(Context c) {
        return sp(c).getString(K_FONT_FAMILY, "sans");
    }
    public static void setEditorFontFamily(Context c, String v) {
        sp(c).edit().putString(K_FONT_FAMILY, v).apply();
    }
}
