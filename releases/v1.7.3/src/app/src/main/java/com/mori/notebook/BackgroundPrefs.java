package com.mori.notebook;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.view.View;

import com.google.android.material.color.MaterialColors;

/** 全局背景：预设纯色/渐变 + 相册图片。深色模式自动使用深色变体。 */
public class BackgroundPrefs {

    public static final String[] NAMES = {
            "默认", "暖白", "淡绿", "淡蓝", "淡紫", "蓝紫渐变", "日落渐变", "自定义图片"
    };

    private static final int[] SOLIDS = {
            Color.TRANSPARENT,
            0xFFFFF8F2,
            0xFFEAF6EE,
            0xFFE8F2FA,
            0xFFF2ECFA
    };

    private static final int[] SOLIDS_DARK = {
            Color.TRANSPARENT,
            0xFF231F1A,
            0xFF16211A,
            0xFF161C21,
            0xFF1E1A24
    };

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("bg", Context.MODE_PRIVATE);
    }

    public static int getStyle(Context c) {
        return prefs(c).getInt("style", 0);
    }

    public static void setStyle(Context c, int style) {
        prefs(c).edit().putInt("style", style).apply();
    }

    public static String getImageUri(Context c) {
        return prefs(c).getString("uri", null);
    }

    public static void setImageUri(Context c, Uri uri) {
        prefs(c).edit().putString("uri", uri.toString()).apply();
    }

    public static boolean isNight(Context c) {
        return (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    public static void apply(Context context, View root) {
        if (root == null) return;
        int style = getStyle(context);
        boolean night = isNight(context);
        Drawable d;
        if (style == 7) {
            d = imageDrawable(context);
            if (d == null) {
                d = new ColorDrawable(MaterialColors.getColor(root, com.google.android.material.R.attr.colorSurface));
            } else {
                d = new LayerDrawable(new Drawable[]{new ColorDrawable(night ? 0x66000000 : 0x4DFFFFFF), d});
            }
        } else if (style == 5) {
            d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    night ? new int[]{0xFF1A1B2E, 0xFF221B2E, 0xFF2A1B24}
                            : new int[]{0xFFE8EAFF, 0xFFF3E7FF, 0xFFFFE9F2});
        } else if (style == 6) {
            d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    night ? new int[]{0xFF2B211A, 0xFF2B1B20, 0xFF241B2B}
                            : new int[]{0xFFFFF1E1, 0xFFFFE3E9, 0xFFF6E7FF});
        } else if (style >= 1 && style <= 4) {
            d = new ColorDrawable(night ? SOLIDS_DARK[style] : SOLIDS[style]);
        } else {
            d = new ColorDrawable(MaterialColors.getColor(root, com.google.android.material.R.attr.colorSurface));
        }
        root.setBackground(d);
    }

    private static Drawable imageDrawable(Context context) {
        String s = getImageUri(context);
        if (s == null) return null;
        try {
            Uri uri = Uri.parse(s);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (java.io.InputStream in = context.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, o);
            }
            int sample = 1;
            while (o.outWidth / (sample * 2) >= 1080) sample *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sample;
            Bitmap bmp;
            try (java.io.InputStream in = context.getContentResolver().openInputStream(uri)) {
                bmp = BitmapFactory.decodeStream(in, null, o2);
            }
            if (bmp == null) return null;
            BitmapDrawable bd = new BitmapDrawable(context.getResources(), bmp);
            bd.setGravity(android.view.Gravity.FILL);
            return bd;
        } catch (Exception e) {
            return null;
        }
    }
}
