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

    /** 选择字体时接受的 MIME 类型，供文件选择器（含多选）复用。 */
    public static final String[] FONT_MIME_TYPES = {
            "font/ttf", "font/otf", "font/ttc",
            "application/x-font-ttf", "application/x-font-opentype",
            "application/octet-stream"};

    /** 导入结果码。 */
    public static final int FAIL = 0;
    public static final int OK = 1;
    public static final int DUPLICATE = 2;

    /** 字体导入结果：code 为结果码，file 为写入的新文件或命中的已有文件。 */
    public static final class ImportResult {
        public final int code;
        public final File file;

        private ImportResult(int code, File file) {
            this.code = code;
            this.file = file;
        }

        public boolean isOk() {
            return code == OK;
        }

        public String path() {
            return file == null ? null : file.getAbsolutePath();
        }

        static ImportResult ok(File f) {
            return new ImportResult(OK, f);
        }

        static ImportResult duplicate(File f) {
            return new ImportResult(DUPLICATE, f);
        }

        static ImportResult fail() {
            return new ImportResult(FAIL, null);
        }
    }

    private FontImporter() {
    }

    /** 拉起系统文件选择器的 Intent（单选）。 */
    public static Intent pickIntent() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, FONT_MIME_TYPES);
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
        return importTo(ctx, uri, false);
    }

    /** 同 importTo(ctx, uri)；quiet=true 时失败不弹提示，用于批量导入后统一汇报。 */
    public static String importTo(Context ctx, Uri uri, boolean quiet) {
        ImportResult r = importUri(ctx, uri);
        if (r.code == OK) return r.path();
        if (!quiet) {
            if (r.code == DUPLICATE) toast(ctx, "已有此字体，未重复导入");
            else toast(ctx, "不是有效的字体文件");
        }
        return null;
    }

    /** 导入单个字体，返回结果码；DUPLICATE 时 file 为字体库中内容相同的已有文件。 */
    public static ImportResult importUri(Context ctx, Uri uri) {
        if (ctx == null || uri == null) return ImportResult.fail();
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
        if (dir == null) return ImportResult.fail();
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
            return ImportResult.fail();
        }
        File dup = findDuplicate(ctx, tmp);
        if (dup != null) {
            tmp.delete();
            return ImportResult.duplicate(dup);
        }
        File target = unique(dir, base, ext);
        if (!tmp.renameTo(target)) {
            tmp.delete();
            return ImportResult.fail();
        }
        Fonts.clear();
        return ImportResult.ok(target);
    }

    /** 把一个已存在的字体文件复制进字体库        Fonts.clear();
        return target.getAbsolutePath();
    }

    /** 把一个已存在的字体文件复制进字体库（用于压缩包批量导入）。失败返回 null。 */
    public static String importFile(Context ctx, File src) {
        ImportResult r = importFileResult(ctx, src);
        return r.code == OK ? r.path() : null;
    }

    /** 同 importFile，但返回结果码；DUPLICATE 时 file 为字体库中内容相同的已有文件。 */
    public static ImportResult importFileResult(Context ctx, File src) {
        if (ctx == null || src == null || !src.isFile()) return ImportResult.fail();
        if (src.length() <= 0 || src.length() > MAX_SIZE) return ImportResult.fail();
        String name = src.getName();
        String lower = name.toLowerCase(Locale.ROOT);
        String ext;
        if (lower.endsWith(".otf")) ext = ".otf";
        else if (lower.endsWith(".ttc")) ext = ".ttc";
        else if (lower.endsWith(".ttf")) ext = ".ttf";
        else return ImportResult.fail();
        String raw = name.substring(0, name.length() - ext.length());
        raw = raw.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (raw.isEmpty()) raw = "custom";
        File dir = fontDir(ctx);
        if (dir == null) return ImportResult.fail();
        File tmp = new File(dir, "tmp_import" + ext);
        try {
            copyFile(src, tmp);
            if (Typeface.createFromFile(tmp) == null) {
                throw new java.io.IOException("字体无效");
            }
        } catch (Throwable t) {
            tmp.delete();
            return ImportResult.fail();
        }
        File dup = findDuplicate(ctx, tmp);
        if (dup != null) {
            tmp.delete();
            return ImportResult.duplicate(dup);
        }
        File target = unique(dir, raw, ext);
        if (!tmp.renameTo(target)) {
            tmp.delete();
            return ImportResult.fail();
        }
        Fonts.clear();
        return ImportResult.ok(target);
    }

    /**
     * 在字体库里查找与 candidate 内容相同的字体文件。
     * 先按长度快速排除，再比对 SHA-256；跳过临时文件与 candidate 自身。
     */
    public static File findDuplicate(Context ctx, File candidate) {
        if (ctx == null || candidate == null || !candidate.isFile()) return null;
        File dir = fontDir(ctx);
        if (dir == null) return null;
        File[] all = dir.listFiles();
        if (all == null) return null;
        long len = candidate.length();
        String self = candidate.getAbsolutePath();
        String sum = null;
        for (File f : all) {
            if (f == null || !f.isFile()) continue;
            if (f.getAbsolutePath().equals(self)) continue;
            String n = f.getName().toLowerCase(Locale.ROOT);
            if (n.startsWith("tmp_import")) continue;
            if (!(n.endsWith(".ttf") || n.endsWith(".otf") || n.endsWith(".ttc"))) continue;
            if (f.length() != len) continue;
            if (sum == null) sum = digest(candidate);
            if (sum == null) return null;
            if (sum.equals(digest(f))) return f;
        }
        return null;
    }

    /** 计算文件 SHA-256 十六进制摘要，失败返回 null。 */
    private static String digest(File f) {
        InputStream in = null;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            in = new java.io.FileInputStream(f);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            byte[] out = md.digest();
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) {
                int v = b & 0xFF;
                if (v < 16) sb.append('0');
                sb.append(Integer.toHexString(v));
            }
            return sb.toString();
        } catch (Throwable t) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static void copyFile(File src, File dst) throws java.io.IOException {
        InputStream in = new java.io.FileInputStream(src);
        try {
            FileOutputStream os = new FileOutputStream(dst);
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
                os.flush();
            } finally {
                os.close();
            }
        } finally {
            in.close();
        }
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
