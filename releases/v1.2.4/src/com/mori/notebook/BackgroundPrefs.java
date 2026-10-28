package com.mori.notebook;

/* loaded from: classes.dex */
public class BackgroundPrefs {
    public static final java.lang.String[] NAMES = {"默认", "暖白", "淡绿", "淡蓝", "淡紫", "蓝紫渐变", "日落渐变", "自定义图片"};
    private static final int[] SOLIDS = {0, -1806, -1378578, -1510662, -856838};
    private static final int[] SOLIDS_DARK = {0, -14475494, -15326950, -15328223, -14804444};

    private static android.content.SharedPreferences prefs(android.content.Context context) {
        return context.getSharedPreferences("bg", 0);
    }

    public static int getStyle(android.content.Context context) {
        return prefs(context).getInt("style", 0);
    }

    public static void setStyle(android.content.Context context, int i) {
        prefs(context).edit().putInt("style", i).apply();
    }

    public static java.lang.String getImageUri(android.content.Context context) {
        return prefs(context).getString("uri", null);
    }

    public static void setImageUri(android.content.Context context, android.net.Uri uri) {
        prefs(context).edit().putString("uri", uri.toString()).apply();
    }

    public static boolean isNight(android.content.Context context) {
        return (context.getResources().getConfiguration().uiMode & 48) == 32;
    }

    public static void apply(android.content.Context context, android.view.View view) {
        android.graphics.drawable.Drawable colorDrawable;
        int[] iArr;
        int[] iArr2;
        if (view == null) {
            return;
        }
        int style = getStyle(context);
        boolean isNight = isNight(context);
        if (style == 7) {
            android.graphics.drawable.Drawable imageDrawable = imageDrawable(context);
            if (imageDrawable == null) {
                colorDrawable = new android.graphics.drawable.ColorDrawable(com.google.android.material.color.MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurface));
            } else {
                android.graphics.drawable.Drawable[] drawableArr = new android.graphics.drawable.Drawable[2];
                drawableArr[0] = new android.graphics.drawable.ColorDrawable(isNight ? 1711276032 : 1308622847);
                drawableArr[1] = imageDrawable;
                colorDrawable = new android.graphics.drawable.LayerDrawable(drawableArr);
            }
        } else if (style == 5) {
            android.graphics.drawable.GradientDrawable.Orientation orientation = android.graphics.drawable.GradientDrawable.Orientation.TL_BR;
            if (isNight) {
                iArr2 = new int[]{-15066322, -14542034, -14017756};
            } else {
                iArr2 = new int[]{-1512705, -792577, -5646};
            }
            colorDrawable = new android.graphics.drawable.GradientDrawable(orientation, iArr2);
        } else if (style == 6) {
            android.graphics.drawable.GradientDrawable.Orientation orientation2 = android.graphics.drawable.GradientDrawable.Orientation.TL_BR;
            if (isNight) {
                iArr = new int[]{-13950694, -13952224, -14410965};
            } else {
                iArr = new int[]{-3615, -7191, -595969};
            }
            colorDrawable = new android.graphics.drawable.GradientDrawable(orientation2, iArr);
        } else if (style >= 1 && style <= 4) {
            colorDrawable = new android.graphics.drawable.ColorDrawable(isNight ? SOLIDS_DARK[style] : SOLIDS[style]);
        } else {
            colorDrawable = new android.graphics.drawable.ColorDrawable(com.google.android.material.color.MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurface));
        }
        view.setBackground(colorDrawable);
    }

    private static android.graphics.drawable.Drawable imageDrawable(android.content.Context context) {
        java.lang.String imageUri = getImageUri(context);
        if (imageUri == null) {
            return null;
        }
        try {
            android.net.Uri parse = android.net.Uri.parse(imageUri);
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            int i = 1;
            options.inJustDecodeBounds = true;
            java.io.InputStream openInputStream = context.getContentResolver().openInputStream(parse);
            try {
                android.graphics.BitmapFactory.decodeStream(openInputStream, null, options);
                if (openInputStream != null) {
                    openInputStream.close();
                }
                while (true) {
                    int i2 = i * 2;
                    if (options.outWidth / i2 < 1080) {
                        break;
                    }
                    i = i2;
                }
                android.graphics.BitmapFactory.Options options2 = new android.graphics.BitmapFactory.Options();
                options2.inSampleSize = i;
                openInputStream = context.getContentResolver().openInputStream(parse);
                try {
                    android.graphics.Bitmap decodeStream = android.graphics.BitmapFactory.decodeStream(openInputStream, null, options2);
                    if (openInputStream != null) {
                        openInputStream.close();
                    }
                    if (decodeStream == null) {
                        return null;
                    }
                    android.graphics.drawable.BitmapDrawable bitmapDrawable = new android.graphics.drawable.BitmapDrawable(context.getResources(), decodeStream);
                    bitmapDrawable.setGravity(119);
                    return bitmapDrawable;
                } finally {
                }
            } finally {
            }
        } catch (java.lang.Exception unused) {
            return null;
        }
    }
}
