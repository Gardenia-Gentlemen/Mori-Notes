package com.mori.notebook;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/** 笔记分享（仅图片分享） */
public final class ShareUtils {

    private ShareUtils() {
    }

    /** 笔记标题去掉非法文件名字符 */
    private static String safeName(Note note) {
        String t = note.title == null ? "" : note.title.trim();
        if (t.isEmpty()) t = "笔记";
        t = t.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_");
        if (t.length() > 24) t = t.substring(0, 24);
        return t;
    }

    public static Bitmap renderNote(Context ctx, Note note) {
        int width = Math.min(ctx.getResources().getDisplayMetrics().widthPixels - dp(ctx, 32), dp(ctx, 720));
        width = Math.max(width, dp(ctx, 320));
        if (note.type == 1) {
            List<MindNode> roots = MindNode.parse(note.content);
            return ExportUtils.renderMind(ctx, roots, width);
        }
        return ExportUtils.renderMarkdown(ctx, note.content, width);
    }

    /** 分享为图片；失败会 toast */
    public static void shareAsImage(Context ctx, Note note) {
        try {
            Bitmap bmp = renderNote(ctx, note);
            File dir = new File(ctx.getCacheDir(), "share");
            if (!dir.exists()) dir.mkdirs();
            File out = new File(dir, safeName(note) + "_" + System.currentTimeMillis() + ".png");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
            }
            Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", out);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("image/png");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.putExtra(Intent.EXTRA_SUBJECT, note.title == null ? "" : note.title);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser = Intent.createChooser(send, "分享笔记图片");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(ctx, "分享失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static int dp(Context ctx, int value) {
        return (int) (value * ctx.getResources().getDisplayMetrics().density);
    }
}