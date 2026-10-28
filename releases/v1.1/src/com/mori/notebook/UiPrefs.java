package com.mori.notebook;

/* loaded from: classes.dex */
public class UiPrefs {
    private static final java.lang.String K_BOLD = "home_title_bold";
    private static final java.lang.String K_SCALE = "home_title_scale";
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
}
