package com.mori.notebook;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.text.Layout;
import android.view.View;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

public final class ExportUtils {

    private ExportUtils() {
    }

    public static Intent textIntent(String filename) {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TITLE, filename.endsWith(".txt") ? filename : filename + ".txt");
        return i;
    }

    public static Intent imageIntent(String filename) {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/png");
        i.putExtra(Intent.EXTRA_TITLE, filename.endsWith(".png") ? filename : filename + ".png");
        return i;
    }

    public static void writeText(Context ctx, Uri uri, String text) throws Exception {
        ContentResolver cr = ctx.getContentResolver();
        OutputStream os = cr.openOutputStream(uri);
        if (os == null) throw new Exception("无法写入");
        try {
            os.write((text == null ? "" : text).getBytes("UTF-8"));
            os.flush();
        } finally {
            os.close();
        }
    }

    public static void writeBitmap(Context ctx, Uri uri, Bitmap bmp) throws Exception {
        ContentResolver cr = ctx.getContentResolver();
        OutputStream os = cr.openOutputStream(uri);
        if (os == null) throw new Exception("无法写入");
        try {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.flush();
        } finally {
            os.close();
        }
    }

    public static Bitmap renderMarkdown(Context ctx, String markdown, int widthPx) {
        TextView tv = new TextView(ctx);
        int pad = (int) (20 * ctx.getResources().getDisplayMetrics().density);
        tv.setPadding(pad, pad, pad, pad);
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f);
        tv.setTypeface(android.graphics.Typeface.DEFAULT);
        tv.setTextColor(0xFF1C1B1F);
        tv.setBackgroundColor(0xFFFFFFFF);
        tv.setText(MarkdownRenderer.render(markdown == null ? "" : markdown));
        int wSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY);
        int hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        tv.measure(wSpec, hSpec);
        int w = widthPx;
        int h = Math.max(tv.getMeasuredHeight(), 1);
        tv.layout(0, 0, w, h);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(0xFFFFFFFF);
        tv.draw(canvas);
        return bmp;
    }

    public static Bitmap renderMind(Context ctx, List<MindNode> roots, int widthPx) {
        MindMapView view = new MindMapView(ctx);
        view.setRoots(roots);
        return view.drawToBitmap(widthPx);
    }

    /** 导出目录名：Download/mori_notes */
    public static final String DIR_NAME = "mori_notes";

    public static String saveText(Context ctx, String filename, String text) throws Exception {
        byte[] data = (text == null ? "" : text).getBytes("UTF-8");
        return writeBytes(ctx, withExt(filename, ".txt"), "text/plain", data);
    }

    public static String saveBitmap(Context ctx, String filename, Bitmap bmp) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
        return writeBytes(ctx, withExt(filename, ".png"), "image/png", bos.toByteArray());
    }

    /** 把字节写入用户通过系统文件选择器指定的位置 */
    public static void writeBytes(Context ctx, Uri uri, byte[] data) throws Exception {
        if (uri == null) throw new Exception("无效路径");
        OutputStream os = ctx.getContentResolver().openOutputStream(uri, "wt");
        if (os == null) os = ctx.getContentResolver().openOutputStream(uri);
        if (os == null) throw new Exception("无法写入");
        try {
            os.write(data == null ? new byte[0] : data);
            os.flush();
        } finally {
            os.close();
        }
    }

    /** 保存任意字节内容（.mori 等自定义格式） */
    public static String saveBytes(Context ctx, String filename, String mime, byte[] data)
            throws Exception {
        String n = filename == null ? "" : filename.trim();
        if (n.isEmpty()) n = "Mori-notes.mori";
        return writeBytes(ctx, n, mime == null ? "application/octet-stream" : mime, data);
    }

    private static String withExt(String name, String ext) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) n = "笔记";
        if (!n.toLowerCase().endsWith(ext)) n = n + ext;
        return n;
    }

    private static String writeBytes(Context ctx, String name, String mime, byte[] data)
            throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            cv.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            cv.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/" + DIR_NAME);
            Uri uri = ctx.getContentResolver()
                    .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
            if (uri == null) throw new Exception("无法创建文件");
            OutputStream os = ctx.getContentResolver().openOutputStream(uri);
            if (os == null) throw new Exception("无法写入");
            try {
                os.write(data);
                os.flush();
            } finally {
                os.close();
            }
            return "Download/" + DIR_NAME + "/" + name;
        }
        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), DIR_NAME);
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("无法创建目录");
        File f = new File(dir, name);
        if (f.exists()) {
            String base = name;
            String ext = "";
            int dot = name.lastIndexOf('.');
            if (dot > 0) {
                base = name.substring(0, dot);
                ext = name.substring(dot);
            }
            int i = 1;
            while (f.exists()) {
                f = new File(dir, base + "(" + i + ")" + ext);
                i++;
            }
        }
        FileOutputStream fos = new FileOutputStream(f);
        try {
            fos.write(data);
            fos.flush();
        } finally {
            fos.close();
        }
        return f.getAbsolutePath();
    }
}
