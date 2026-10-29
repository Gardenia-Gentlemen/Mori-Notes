/*
 * 自定义字体导入：字体统一存在应用私有目录 files/fonts/，
 * 既可设为全局默认字体，也可作为单条倒数日的专属字体。
 *
 * 参考自 TimeTrace (https://github.com/KIPPUDESU/KIPPU_Trace)
 * Copyright (c) 2026 切符
 * 依据 MIT License 使用，许可全文见项目根目录 LICENSE-KIPPU_Trace。
 */
package com.mori.notebook;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** 选择并导入本地字体文件（ttf / otf / ttc）。 */
public final class FontImporter {
    private static final long MAX_SIZE = 20L * 1024 * 1024;

    private FontImporter() {
    }

    /** 拉起系统文件选择器的 Intent。 */
    public static Intent pickIntent() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "font/ttf", "font/otf", "font/ttc",
                "application/x-font-ttf", "application/x-font-opentype",
                "application/octet-stream"});
        return i;
    }

    /** 导入到字体库（不改变默认字体）。失败时已提示。 */
    public static boolean importFrom(Context ctx, Uri uri) {
        String path = importTo(ctx, uri);
        if (path == null) return false;
        toast(ctx, "已加入字体库，可在全屏页选择字体");
        return true;
    }

    /** 复制到字体库并校验可加载，返回保存后的绝对路径；失败返回 null 并已提示。 */
    public static String importTo(Context ctx, Uri uri) {
        if (ctx == null || uri == null) return null;
        String ext = ".ttf";
        String base = "custom";
        String name = fileName(ctx, uri);
        if (name != null) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".otf")) ext = ".otf";
            else if (lower.endsWith(".ttc")) ext = ".ttc";
            int dot = name.lastIndexOf('.');
            String raw = dot > 0 ? name.substring(0, dot) : name;
            raw = raw.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
            if (!raw.isEmpty()) base = raw;
        }
        File dir = fontDir(ctx);
        if (dir == null) {
            toast(ctx, "无法创建字体目录");
            return null;
        }
        File tmp = new File(dir, "tmp_import" + ext);
        try {
            InputStream in = ctx.getContentResolver().openInputStream(uri);
            if (in == null) throw new java.io.IOException("空文件");
            try {
                FileOutputStream os = new FileOutputStream(tmp);
                try {
                    byte[] buf = new byte[8192];
                    int n;
                    long total = 0;
                    while ((n = in.read(buf)) > 0) {
                        total += n;
                        if (total > MAX_SIZE) throw new java.io.IOException("文件过大");
                        os.write(buf, 0, n);
                    }
                    os.flush();
                } finally {
                    os.close();
                }
            } finally {
                in.close();
            }
            if (Typeface.createFromFile(tmp) == null) {
                throw new java.io.IOException("字体无效");
            }
        } catch (Throwable t) {
            tmp.delete();
            toast(ctx, "不是有效的字体文件");
            return null;
        }
        File target = unique(dir, base, ext);
        if (!tmp.renameTo(target)) {
            tmp.delete();
            toast(ctx, "字体保存失败");
            return null;
        }
        Fonts.clear();
        return target.getAbsolutePath();
    }

    /** 字体库目录。 */
    public static File fontDir(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "fonts");
        if (!dir.exists() && !dir.mkdirs()) return null;
        return dir;
    }

    /** 字体库里已导入的字体，按文件名排序。 */
    public static List<File> list(Context ctx) {
        List<File> out = new ArrayList<>();
        File dir = fontDir(ctx);
        if (dir == null) return out;
        File[] all = dir.listFiles();
        if (all == null) return out;
        Arrays.sort(all);
        for (File f : all) {
            String n = f.getName().toLowerCase(Locale.ROOT);
            if (f.isFile() && (n.endsWith(".ttf") || n.endsWith(".otf") || n.endsWith(".ttc"))) {
                out.add(f);
            }
        }
        return out;
    }

    /** 字体显示名（去扩展名）。 */
    public static String displayName(File f) {
        String n = f.getName();
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }

    private static File unique(File dir, String base, String ext) {
        File f = new File(dir, base + ext);
        int i = 2;
        while (f.exists()) {
            f = new File(dir, base + "-" + i + ext);
            i++;
        }
        return f;
    }

    private static String fileName(Context ctx, Uri uri) {
        android.database.Cursor c = null;
        try {
            c = ctx.getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return c.getString(idx);
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    private static void toast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }
}
