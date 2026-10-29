/*
 * 从相册选图：把选中的图片复制到应用私有目录，避免权限失效。
 */
package com.mori.notebook;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/** 调用系统相册选图，并复制到私有目录长期保存。 */
public final class Gallery {
    private Gallery() {
    }

    /** 相册选图 Intent。 */
    public static Intent intent() {
        Intent i = new Intent(Intent.ACTION_PICK);
        i.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return i;
    }

    /** 复制到 files/bg/，返回 file uri 字符串；失败返回 null。 */
    public static String save(Context ctx, Uri src) {
        if (src == null) return null;
        File dir = new File(ctx.getFilesDir(), "bg");
        if (!dir.exists() && !dir.mkdirs()) return null;
        File out = new File(dir, "bg-" + System.currentTimeMillis() + ".img");
        InputStream in = null;
        OutputStream os = null;
        try {
            in = ctx.getContentResolver().openInputStream(src);
            if (in == null) return null;
            os = new FileOutputStream(out);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                os.write(buf, 0, n);
            }
            os.flush();
        } catch (Exception e) {
            out.delete();
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) { }
            try { if (os != null) os.close(); } catch (Exception ignored) { }
        }
        if (out.length() <= 0) {
            out.delete();
            return null;
        }
        return Uri.fromFile(out).toString();
    }

    /** 替换背景时删掉上一个私有副本，避免文件堆积。 */
    public static void deleteCopy(Context ctx, String uriStr) {
        if (uriStr == null || !uriStr.startsWith("file://")) return;
        try {
            File f = new File(Uri.parse(uriStr).getPath());
            File dir = new File(ctx.getFilesDir(), "bg");
            if (f.getParentFile() != null && f.getParentFile().equals(dir)) {
                f.delete();
            }
        } catch (Exception ignored) {
        }
    }
}
