/*
 * 字体解析器：单条倒数日可绑定专属字体，未绑定则使用手机系统默认字体。
 * 按字体文件路径缓存 Typeface，避免重复解析。
 */
package com.mori.notebook;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class Fonts {

    /** 按字体文件路径缓存的 Typeface，{常规, 加粗}。 */
    private static final Map<String, Typeface[]> CACHE = new HashMap<>();

    private Fonts() {
    }

    /** 清除缓存，字体变更后调用。 */
    public static synchronized void clear() {
        CACHE.clear();
    }

    private static Typeface[] load(String path) {
        if (TextUtils.isEmpty(path)) return null;
        File f = new File(path);
        if (!f.exists()) return null;
        try {
            Typeface t = Typeface.createFromFile(f);
            if (t == null) return null;
            return new Typeface[]{t, Typeface.create(t, Typeface.BOLD)};
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 手机系统默认字体（常规）。 */
    public static Typeface regular(Context ctx) {
        return Typeface.DEFAULT;
    }

    /** 手机系统默认字体（加粗）。 */
    public static Typeface bold(Context ctx) {
        return Typeface.DEFAULT_BOLD;
    }

    /** 指定路径的常规字重；路径为空或失效时回退手机系统默认字体。 */
    public static Typeface regular(Context ctx, String path) {
        if (TextUtils.isEmpty(path)) return regular(ctx);
        Typeface[] t = byPath(path);
        return t == null ? regular(ctx) : t[0];
    }

    /** 指定路径的加粗；路径为空或失效时回退手机系统默认字体。 */
    public static Typeface bold(Context ctx, String path) {
        if (TextUtils.isEmpty(path)) return bold(ctx);
        Typeface[] t = byPath(path);
        return t == null ? bold(ctx) : t[1];
    }

    /** 水印用字体：优先字体库中的霞鹜文楷，找不到则回退系统衬线体。 */
    public static Typeface watermark(Context ctx) {
        try {
            File dir = new File(ctx.getFilesDir(), "fonts");
            File f = new File(dir, "楷书-霞鹜文楷.ttf");
            if (f.isFile()) {
                Typeface t = regular(ctx, f.getAbsolutePath());
                if (t != null) return t;
            }
            File[] fs = dir.listFiles();
            if (fs != null) {
                for (File x : fs) {
                    if (x.isFile() && x.getName().contains("霞鹜")) {
                        Typeface t = regular(ctx, x.getAbsolutePath());
                        if (t != null) return t;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return Typeface.SERIF;
    }

    private static synchronized Typeface[] byPath(String path) {
        Typeface[] cached = CACHE.get(path);
        if (cached != null) return cached;
        Typeface[] t = load(path);
        if (t != null) CACHE.put(path, t);
        return t;
    }
}
