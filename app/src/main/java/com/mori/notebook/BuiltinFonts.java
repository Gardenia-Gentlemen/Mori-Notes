/*
 * 内置字体：随 APK 打包在 assets/fonts 下的字体文件。
 * 安装后首次启动（或版本升级后首次启动）自动解出到字体库 files/fonts，
 * 之后与手动导入的字体完全同权：可直接选用、可设为默认、可用于水印。
 */
package com.mori.notebook;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

public final class BuiltinFonts {
    private static final String ASSET_DIR = "fonts";
    private static final String PREF = "builtin_fonts";
    private static final String KEY_VERSION = "seeded_version";

    private BuiltinFonts() {
    }

    /** 确保内置字体已解出到字体库；已解出过则直接返回。 */
    public static void ensure(Context ctx) {
        if (ctx == null) return;
        try {
            int code = ctx.getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0).versionCode;
            SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            if (sp.getInt(KEY_VERSION, 0) == code) return;
            File dir = FontImporter.fontDir(ctx);
            if (dir == null) return;
            String[] names = ctx.getAssets().list(ASSET_DIR);
            if (names == null || names.length == 0) {
                sp.edit().putInt(KEY_VERSION, code).apply();
                return;
            }
            boolean changed = false;
            for (String name : names) {
                if (name == null) continue;
                String lower = name.toLowerCase(Locale.ROOT);
                if (!(lower.endsWith(".ttf") || lower.endsWith(".otf") || lower.endsWith(".ttc"))) {
                    continue;
                }
                File out = new File(dir, name);
                if (out.isFile() && out.length() > 0) continue;
                if (copyAsset(ctx, ASSET_DIR + "/" + name, out)) changed = true;
            }
            if (changed) {
                Fonts.clear();
                FontChooser.clearCache();
            }
            sp.edit().putInt(KEY_VERSION, code).apply();
        } catch (Throwable ignored) {
        }
    }

    private static boolean copyAsset(Context ctx, String assetName, File out) {
        File tmp = new File(out.getParentFile(), out.getName() + ".part");
        InputStream in = null;
        FileOutputStream os = null;
        try {
            in = ctx.getAssets().open(assetName);
            os = new FileOutputStream(tmp);
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                os.write(buf, 0, n);
            }
            os.flush();
            os.close();
            os = null;
            in.close();
            in = null;
            if (tmp.length() <= 0) {
                tmp.delete();
                return false;
            }
            if (out.exists()) out.delete();
            return tmp.renameTo(out);
        } catch (Throwable t) {
            tmp.delete();
            return false;
        } finally {
            if (os != null) {
                try { os.close(); } catch (Throwable ignored) { }
            }
            if (in != null) {
                try { in.close(); } catch (Throwable ignored) { }
            }
        }
    }
}
