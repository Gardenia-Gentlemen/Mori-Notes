/*
 * 倒数日卡片的背景图片与蒙版。
 *
 * 参考自 TimeTrace (https://github.com/KIPPUDESU/KIPPU_Trace)
 * Copyright (c) 2026 切符
 * 依据 MIT License 使用，许可全文见项目根目录 LICENSE-KIPPU_Trace。
 */
package com.mori.notebook;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;

/** 把背景图与蒙版铺到卡片上，未设置时清理干净。 */
public final class CardBg {
    /** 多张背景图共用的位图缓存，避免滑动列表时反复解码。 */
    private static final android.util.LruCache<String, Bitmap> CACHE;
    static {
        int maxKb = (int) (Runtime.getRuntime().maxMemory() / 1024 / 8);
        CACHE = new android.util.LruCache<String, Bitmap>(Math.max(4 * 1024, maxKb)) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return Math.max(1, value.getByteCount() / 1024);
            }
        };
    }
    private CardBg() {
    }
    /** bgColor 取此值表示“跟随应用背景”（BackgroundPrefs）。 */
    public static final int BG_APP = -1;
    public static void clear() {
        CACHE.evictAll();
    }
    private static Bitmap load(Context ctx, Uri uri) {
        String key = uri.toString();
        Bitmap hit = CACHE.get(key);
        if (hit != null) {
            return hit;
        }
        Bitmap bmp = null;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, o);
            }
            int sample = 1;
            while (o.outWidth / (sample * 2) >= 720) {
                sample *= 2;
            }
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sample;
            try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
                bmp = BitmapFactory.decodeStream(in, null, o2);
            }
        } catch (Exception ignored) {
        }
        if (bmp != null) {
            CACHE.put(key, bmp);
        }
        return bmp;
    }

    /** 应用背景；无图时恢复主题默认色。 */
    public static void apply(Context ctx, ImageView bg, View scrim,
                            TextView title, TextView date, TextView anniv,
                            TextView days, TextView unit,
                            int titleColor, int subColor, int heroColor) {
        apply(ctx, null, 0, bg, scrim, title, date, anniv, days, unit,
                titleColor, subColor, heroColor);
    }

    /** 颜色是否偏浅（浅色上应使用深色文字）。 */
    public static boolean isLight(int color) {
        double lum = (0.299 * Color.red(color) + 0.587 * Color.green(color)
                + 0.114 * Color.blue(color)) / 255d;
        return lum > 0.62d;
    }

    /** 纯色背景铺底：不盖蒙版，文字颜色按底色深浅自动切换。 */
    private static void applySolid(int color, ImageView bg, View scrim,
                                TextView title, TextView date, TextView anniv,
                                TextView days, TextView unit,
                                int titleColor, int subColor, int heroColor) {
        bg.setVisibility(View.VISIBLE);
        bg.setImageDrawable(null);
        bg.setBackgroundColor(color);
        scrim.setVisibility(View.GONE);
        if (isLight(color)) {
            title.setTextColor(titleColor);
            date.setTextColor(subColor);
            anniv.setTextColor(subColor);
            days.setTextColor(heroColor);
            unit.setTextColor(subColor);
        } else {
            title.setTextColor(0xFFFFFFFF);
            date.setTextColor(0xF2FFFFFF);
            anniv.setTextColor(0xF2FFFFFF);
            days.setTextColor(0xFFFFFFFF);
            unit.setTextColor(0xF2FFFFFF);
        }
    }

    /** 取图片位图给编辑页预览用，失败返回 null。 */
    public static Bitmap preview(Context ctx, String uriStr) {
        if (uriStr == null || uriStr.isEmpty()) {
            return null;
        }
        try {
            return load(ctx, Uri.parse(uriStr));
        } catch (Exception e) {
            return null;
        }
    }

    /** 应用背景；itemUri 非空时优先使用单条卡片的图，否则用全局设置。 */
    public static void apply(Context ctx, String itemUri, ImageView bg, View scrim,
                            TextView title, TextView date, TextView anniv,
                            TextView days, TextView unit,
                            int titleColor, int subColor, int heroColor) {
        apply(ctx, itemUri, 0, bg, scrim, title, date, anniv, days, unit,
                titleColor, subColor, heroColor);
    }

    /** 铺应用全局背景（跟随设置页的背景样式）。 */
    private static void applyAppBg(Context ctx, ImageView bg, View scrim,
                                TextView title, TextView date, TextView anniv,
                                TextView days, TextView unit,
                                int titleColor, int subColor, int heroColor) {
        bg.setVisibility(View.VISIBLE);
        bg.setImageDrawable(null);
        bg.setBackground(BackgroundPrefs.drawable(ctx));
        scrim.setVisibility(View.GONE);
        if (BackgroundPrefs.isLightStyle(ctx)) {
            title.setTextColor(titleColor);
            date.setTextColor(subColor);
            anniv.setTextColor(subColor);
            days.setTextColor(heroColor);
            unit.setTextColor(subColor);
        } else {
            title.setTextColor(0xFFFFFFFF);
            date.setTextColor(0xF2FFFFFF);
            anniv.setTextColor(0xF2FFFFFF);
            days.setTextColor(0xFFFFFFFF);
            unit.setTextColor(0xF2FFFFFF);
        }
    }

    /** 应用背景；itemColor 非 0 时铺纯色，其次单条卡片图，最后全局设置。 */
    public static void apply(Context ctx, String itemUri, int itemColor, ImageView bg, View scrim,
                            TextView title, TextView date, TextView anniv,
                            TextView days, TextView unit,
                            int titleColor, int subColor, int heroColor) {
        apply(ctx, itemUri, itemColor, bg, scrim, title, date, anniv, days, unit,
                -1, titleColor, subColor, heroColor);
    }
    /** 带专属蒙版强度的重载；maskPct < 0 时取全局设置。 */
    public static void apply(Context ctx, String itemUri, int itemColor, ImageView bg, View scrim,
                            TextView title, TextView date, TextView anniv,
                            TextView days, TextView unit, int maskPct,
                            int titleColor, int subColor, int heroColor) {
        if (itemColor == BG_APP) {
            applyAppBg(ctx, bg, scrim, title, date, anniv, days, unit,
                    titleColor, subColor, heroColor);
            return;
        }
        if (itemColor != 0) {
            applySolid(itemColor, bg, scrim, title, date, anniv, days, unit,
                    titleColor, subColor, heroColor);
            return;
        }
        bg.setBackgroundColor(Color.TRANSPARENT);
        Uri uri = null;
        if (itemUri != null && !itemUri.isEmpty()) {
            try {
                uri = Uri.parse(itemUri);
            } catch (Exception ignored) {
            }
        }
        if (uri == null) uri = CountdownPrefs.getCardBg(ctx);
        Bitmap bmp = uri == null ? null : load(ctx, uri);
        if (bmp == null) {
            bg.setVisibility(View.GONE);
            bg.setImageDrawable(null);
            scrim.setVisibility(View.GONE);
            title.setTextColor(titleColor);
            date.setTextColor(subColor);
            anniv.setTextColor(subColor);
            days.setTextColor(heroColor);
            unit.setTextColor(subColor);
            return;
        }
        bg.setVisibility(View.VISIBLE);
        bg.setImageBitmap(bmp);
        scrim.setVisibility(View.VISIBLE);
        int mp = maskPct < 0 ? CountdownPrefs.getCardMask(ctx) : Math.max(0, Math.min(100, maskPct));
        int alpha = Math.round(255f * mp / 100f);
        scrim.setBackgroundColor(0xFF000000);
        scrim.setAlpha(alpha / 255f);
        title.setTextColor(0xFFFFFFFF);
        date.setTextColor(0xF2FFFFFF);
        anniv.setTextColor(0xF2FFFFFF);
        days.setTextColor(0xFFFFFFFF);
        unit.setTextColor(0xF2FFFFFF);
    }
}
