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
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;

/** 把背景图与蒙版铺到卡片上，未设置时清理干净。 */
public final class CardBg {
    private static Bitmap cache;
    private static String cacheKey;

    private CardBg() {
    }

    public static void clear() {
        cache = null;
        cacheKey = null;
    }

    private static Bitmap load(Context ctx, Uri uri) {
        String key = uri.toString();
        if (cache != null && key.equals(cacheKey)) {
            return cache;
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
            cache = bmp;
            cacheKey = key;
        }
        return bmp;
    }

    /** 应用背景；无图时恢复主题默认色。 */
    public static void apply(Context ctx, ImageView bg, View scrim,
                            TextView title, TextView date, TextView anniv,
                            TextView days, TextView unit,
                            int titleColor, int subColor, int heroColor) {
        apply(ctx, null, bg, scrim, title, date, anniv, days, unit,
                titleColor, subColor, heroColor);
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
        int alpha = Math.round(255f * CountdownPrefs.getCardMask(ctx) / 100f);
        scrim.setBackgroundColor(0xFF000000);
        scrim.setAlpha(alpha / 255f);
        title.setTextColor(0xFFFFFFFF);
        date.setTextColor(0xF2FFFFFF);
        anniv.setTextColor(0xF2FFFFFF);
        days.setTextColor(0xFFFFFFFF);
        unit.setTextColor(0xF2FFFFFF);
    }
}
