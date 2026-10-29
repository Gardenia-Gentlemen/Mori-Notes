package com.mori.notebook;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import com.google.android.material.color.MaterialColors;

/** 全局背景：默认 / 蓝紫渐变 / 日落渐变。深色模式自动使用深色变体。 */
public class BackgroundPrefs {

    public static final int STYLE_DEFAULT = 0;
    public static final int STYLE_BLUE_PURPLE = 1;
    public static final int STYLE_SUNSET = 2;

    public static final String[] NAMES = {"默认", "蓝紫渐变", "日落渐变"};

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("bg", Context.MODE_PRIVATE);
    }

    public static int getStyle(Context c) {
        int s = prefs(c).getInt("style", STYLE_DEFAULT);
        if (s < 0 || s >= NAMES.length) {
            prefs(c).edit().putInt("style", STYLE_DEFAULT).apply();
            return STYLE_DEFAULT;
        }
        return s;
    }

    public static void setStyle(Context c, int style) {
        if (style < 0 || style >= NAMES.length) style = STYLE_DEFAULT;
        prefs(c).edit().putInt("style", style).apply();
    }

    public static boolean isNight(Context c) {
        return (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    /** 全局背景 Drawable 的 ConstantState 缓存：同一套样式不重复构建、不重复读配置。 */
    private static int bgCacheStyle = Integer.MIN_VALUE;
    private static boolean bgCacheNight;
    private static android.graphics.drawable.Drawable.ConstantState bgCacheState;

    /** 按全局设置构建背景 Drawable，供根布局与倒数日全屏页共用。 */
    public static Drawable drawable(Context context) {
        int style = getStyle(context);
        boolean night = isNight(context);
        if (bgCacheState != null && bgCacheStyle == style && bgCacheNight == night) {
            Drawable cached = bgCacheState.newDrawable(context.getResources());
            if (cached != null) {
                return cached;
            }
        }
        Drawable d = buildDrawable(context, style, night);
        android.graphics.drawable.Drawable.ConstantState cs = d.getConstantState();
        if (cs != null) {
            bgCacheStyle = style;
            bgCacheNight = night;
            bgCacheState = cs;
        }
        return d;
    }

    private static Drawable buildDrawable(Context context, int style, boolean night) {
        if (style == STYLE_BLUE_PURPLE) {
            return new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    night ? new int[]{0xFF1A1B2E, 0xFF221B2E, 0xFF2A1B24}
                          : new int[]{0xFFE8EAFF, 0xFFF3E7FF, 0xFFFFE9F2});
        }
        if (style == STYLE_SUNSET) {
            return new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    night ? new int[]{0xFF2B211A, 0xFF2B1B20, 0xFF241B2B}
                          : new int[]{0xFFFFF1E1, 0xFFFFE3E9, 0xFFF6E7FF});
        }
        return new ColorDrawable(surfaceColor(context));
    }

    /** 全局背景是否偏浅（浅底用深色文字）。 */
    public static boolean isLightStyle(Context c) {
        return !isNight(c);
    }

    private static int surfaceColor(Context c) {
        return MaterialColors.getColor(c, com.google.android.material.R.attr.colorSurface, 0xFFFEF7FF);
    }

    public static void apply(Context context, View root) {
        if (root == null) return;
        root.setBackground(drawable(context));
    }
}
