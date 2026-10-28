/*
 * 图片分享：单条笔记、多条笔记、倒数日卡片。
 *
 * 参考自 TimeTrace (https://github.com/KIPPUDESU/KIPPU_Trace)
 * Copyright (c) 2026 切符
 * 依据 MIT License 使用，许可全文见项目根目录 LICENSE-KIPPU_Trace。
 */
package com.mori.notebook;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/** 笔记与倒数日的图片分享 */
public final class ShareUtils {
    private ShareUtils() {
    }

    private static int dp(Context ctx, int value) {
        return (int) (value * ctx.getResources().getDisplayMetrics().density);
    }

    private static String safeName(Note note) {
        String t = note.title == null ? "" : note.title.trim();
        if (t.isEmpty()) t = "笔记";
        t = t.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_");
        if (t.length() > 24) t = t.substring(0, 24);
        return t;
    }

    private static String safeName(String title, String fallback) {
        String t = title == null ? "" : title.trim();
        if (t.isEmpty()) t = fallback;
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

    /** 单条笔记分享为图片 */
    public static void shareAsImage(Context ctx, Note note) {
        try {
            Bitmap bmp = renderNote(ctx, note);
            shareBitmap(ctx, bmp, safeName(note), note.title == null ? "" : note.title, "分享笔记图片");
        } catch (Exception e) {
            Toast.makeText(ctx, "分享失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /** 多条笔记竖向拼接后分享为一张图片 */
    public static void shareNotesAsImage(Context ctx, List<Note> notes) {
        if (notes == null || notes.isEmpty()) {
            Toast.makeText(ctx, "没有可分享的内容", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (notes.size() == 1) {
                shareAsImage(ctx, notes.get(0));
                return;
            }
            int gap = dp(ctx, 16);
            int width = 0;
            Bitmap[] parts = new Bitmap[notes.size()];
            for (int i = 0; i < notes.size(); i++) {
                parts[i] = renderNote(ctx, notes.get(i));
                width = Math.max(width, parts[i].getWidth());
            }
            int height = gap;
            for (Bitmap b : parts) height += b.getHeight() + gap;
            Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(out);
            canvas.drawColor(0xFFEFEFF3);
            int y = gap;
            for (Bitmap b : parts) {
                canvas.drawBitmap(b, (width - b.getWidth()) / 2f, y, null);
                y += b.getHeight() + gap;
            }
            shareBitmap(ctx, out, "笔记合集_" + notes.size() + "条", "笔记合集", "分享笔记图片");
        } catch (Exception e) {
            Toast.makeText(ctx, "分享失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /** 倒数日分享为图片；多条竖向拼接 */
    public static void shareCountdownsAsImage(Context ctx, List<Countdown> list) {
        if (list == null || list.isEmpty()) {
            Toast.makeText(ctx, "没有可分享的倒数日", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            int gap = dp(ctx, 14);
            Bitmap[] parts = new Bitmap[list.size()];
            int width = 0;
            for (int i = 0; i < list.size(); i++) {
                parts[i] = renderCountdown(ctx, list.get(i));
                width = Math.max(width, parts[i].getWidth());
            }
            int height = gap;
            for (Bitmap b : parts) height += b.getHeight() + gap;
            Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(out);
            canvas.drawColor(0xFFF6F5F8);
            int y = gap;
            for (Bitmap b : parts) {
                canvas.drawBitmap(b, 0f, y, null);
                y += b.getHeight() + gap;
            }
            shareBitmap(ctx, out, "倒数日" + (list.size() > 1 ? "合集" : ""),
                    "倒数日", "分享倒数日图片");
        } catch (Exception e) {
            Toast.makeText(ctx, "分享失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /** 单条倒数日分享为图片 */
    public static void shareCountdownAsImage(Context ctx, Countdown c) {
        if (c == null) {
            Toast.makeText(ctx, "没有可分享的倒数日", Toast.LENGTH_SHORT).show();
            return;
        }
        shareCountdownsAsImage(ctx, java.util.Collections.singletonList(c));
    }

    /** 单张倒数日卡片位图 */
    public static Bitmap renderCountdown(Context ctx, Countdown c) {
        int width = Math.min(ctx.getResources().getDisplayMetrics().widthPixels - dp(ctx, 32), dp(ctx, 720));
        width = Math.max(width, dp(ctx, 320));
        long today = CountdownTime.today();
        long diff = CountdownTime.dayDiff(today, c.date);
        long abs = Math.abs(diff);
        String lunar = LunarUtil.format(c.date);
        String anniv = CountdownTime.anniversaryText(c, today);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(Color.WHITE);
        int pad = dp(ctx, 20);
        row.setPadding(pad, pad, pad, pad);

        LinearLayout left = new LinearLayout(ctx);
        left.setOrientation(LinearLayout.VERTICAL);
        row.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(ctx);
        title.setText(c.title == null || c.title.isEmpty() ? "无标题" : c.title);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
        title.setTextColor(0xFF1C1B1F);
        left.addView(title);

        TextView date = new TextView(ctx);
        date.setText(CountdownTime.formatDate(c.date) + (lunar.isEmpty() ? "" : " · " + lunar));
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        date.setTextColor(0xFF6B6B70);
        date.setPadding(0, dp(ctx, 4), 0, 0);
        left.addView(date);

        if (anniv != null && !anniv.isEmpty()) {
            TextView a = new TextView(ctx);
            a.setText(anniv);
            a.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            a.setTextColor(0xFF5B5BD6);
            a.setPadding(0, dp(ctx, 4), 0, 0);
            left.addView(a);
        }

        TextView num = new TextView(ctx);
        num.setText(String.valueOf(abs));
        num.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f);
        num.setTextColor(0xFF1C1B1F);
        row.addView(num);

        TextView unit = new TextView(ctx);
        unit.setText(diff == 0 ? " 就是今天" : (diff > 0 ? " 天后" : " 天前"));
        unit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        unit.setTextColor(0xFF6B6B70);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(ctx, 2);
        lp.gravity = android.view.Gravity.BOTTOM;
        lp.bottomMargin = dp(ctx, 6);
        row.addView(unit, lp);

        int wSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        row.measure(wSpec, hSpec);
        int h = Math.max(row.getMeasuredHeight(), 1);
        row.layout(0, 0, width, h);
        Bitmap bmp = Bitmap.createBitmap(width, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);
        row.draw(canvas);
        Paint border = new Paint();
        border.setColor(0xFFE6E6EA);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(dp(ctx, 1));
        canvas.drawRect(0.5f, 0.5f, width - 0.5f, h - 0.5f, border);
        return bmp;
    }

    /** 存到 cache 并拉起系统分享 */
    public static void shareBitmap(Context ctx, Bitmap bmp, String name, String subject, String chooserTitle) {
        try {
            File dir = new File(ctx.getCacheDir(), "share");
            if (!dir.exists()) dir.mkdirs();
            File out = new File(dir, name + "_" + System.currentTimeMillis() + ".png");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
            }
            Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", out);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("image/png");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.putExtra(Intent.EXTRA_SUBJECT, subject == null ? "" : subject);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser = Intent.createChooser(send, chooserTitle);
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(ctx, "分享失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
