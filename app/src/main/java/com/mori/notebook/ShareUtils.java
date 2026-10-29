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

    /** 分享为 Markdown 文本（保留原始语法） */
    public static void shareAsMarkdown(Context ctx, Note note) {
        if (note == null) return;
        shareText(ctx, safeName(note), textOf(note, true), "分享 Markdown 文本");
    }

    /** 分享为纯文本（去掉 Markdown 语法） */
    public static void shareAsText(Context ctx, Note note) {
        if (note == null) return;
        shareText(ctx, safeName(note), textOf(note, false), "分享文本");
    }

    /** 组装分享文本：标题 + 正文；思维导图笔记转缩进大纲 */
    private static String textOf(Note note, boolean markdown) {
        String title = note.title == null ? "" : note.title.trim();
        StringBuilder sb = new StringBuilder();
        if (!title.isEmpty()) {
            sb.append(markdown ? "# " + title : title).append("\n\n");
        }
        String body;
        if (note.type == 1) {
            body = mindOutline(MindNode.parse(note.content), 0);
        } else if (markdown) {
            body = note.content == null ? "" : note.content;
        } else {
            body = MarkdownRenderer.toPlainText(note.content);
        }
        sb.append(body == null ? "" : body.trim());
        return sb.toString().trim();
    }

    /** 思维导图节点转缩进大纲文本 */
    private static String mindOutline(List<MindNode> nodes, int depth) {
        if (nodes == null || nodes.isEmpty()) return "";
        StringBuilder pad = new StringBuilder();
        for (int i = 0; i < depth; i++) pad.append("  ");
        StringBuilder sb = new StringBuilder();
        for (MindNode n : nodes) {
            String t = n.text == null ? "" : n.text.trim();
            if (!t.isEmpty()) sb.append(pad).append("- ").append(t).append("\n");
            sb.append(mindOutline(n.children, depth + 1));
        }
        return sb.toString();
    }

    /** 拉起系统分享面板分享一段文字 */
    private static void shareText(Context ctx, String subject, String text, String chooserTitle) {
        if (text == null || text.trim().isEmpty()) {
            Toast.makeText(ctx, "没有可分享的内容", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_SUBJECT, subject == null ? "" : subject);
            send.putExtra(Intent.EXTRA_TEXT, text);
            Intent chooser = Intent.createChooser(send, chooserTitle);
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(ctx, "分享失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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

    /** 多条倒数日竖向拼接成一张卡片式长图（分享与保存共用） */
    public static Bitmap renderCountdownsList(Context ctx, List<Countdown> list) {
        int gap = dp(ctx, 14);
        Bitmap[] parts = new Bitmap[list.size()];
        int width = 0;
        for (int i = 0; i < list.size(); i++) {
            parts[i] = renderCountdown(ctx, list.get(i));
            width = Math.max(width, parts[i].getWidth());
        }
        int height = gap;
        for (Bitmap b : parts) height += b.getHeight() + gap;
        int total = Math.max(height, 1);
        Bitmap out = Bitmap.createBitmap(width, total, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        // 整图背景取第一条倒数日的全屏背景图，无图时回退浅灰底色
        Bitmap outer = backgroundOf(ctx, list.get(0));
        if (outer != null) {
            drawBgCover(canvas, outer, width, total);
            drawScrim(ctx, canvas, width, total);
        } else {
            canvas.drawColor(0xFFF6F5F8);
        }
        int y = gap;
        for (Bitmap b : parts) {
            canvas.drawBitmap(b, 0f, y, null);
            y += b.getHeight() + gap;
        }
        return out;
    }

    /** 倒数日分享图：4:3 横版画布 + 模糊背景，卡片 16:9 圆角，按列排布并整体居中（必要时等比缩小）。 */
    public static Bitmap renderCountdownsLandscape(Context ctx, List<Countdown> list) {
        final int canvasW = dp(ctx, 720);
        final int canvasH = Math.round(canvasW * 3f / 4f);
        final int gap = dp(ctx, 12);
        final int pad = dp(ctx, 10);
        final int n = list.size();
        int bestCols = 1;
        Bitmap[] bestParts = null;
        int bestW = 0;
        int bestMaxH = 0;
        int fbCols = 1;
        Bitmap[] fbParts = null;
        int fbW = 0;
        int fbMaxH = 0;
        int fbH = Integer.MAX_VALUE;
        for (int t = 1; t <= n; t++) {
            int cardW = Math.round((canvasW - gap * (t + 1)) / (float) t * 0.9f);
            if (cardW < dp(ctx, 140)) break;
            Bitmap[] parts = new Bitmap[n];
            int mh = 0;
            for (int i = 0; i < n; i++) {
                parts[i] = renderCountdown(ctx, list.get(i), cardW,
                        Math.round(cardW * 9f / 16f), dp(ctx, 16));
                mh = Math.max(mh, parts[i].getHeight());
            }
            int rows = (n + t - 1) / t;
            int bh = rows * mh + (rows - 1) * gap;
            if (bh < fbH) {
                fbH = bh;
                fbCols = t;
                fbParts = parts;
                fbW = cardW;
                fbMaxH = mh;
            }
            if (bh <= canvasH - pad * 2) {
                bestCols = t;
                bestParts = parts;
                bestW = cardW;
                bestMaxH = mh;
                break;
            }
        }
        if (bestParts == null) {
            bestCols = fbCols;
            bestParts = fbParts;
            bestW = fbW;
            bestMaxH = fbMaxH;
        }
        int rows = (n + bestCols - 1) / bestCols;
        int blockH = rows * bestMaxH + (rows - 1) * gap;
        int firstRow = Math.min(bestCols, n);
        int blockW = firstRow * bestW + (firstRow - 1) * gap;
        Bitmap out = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Bitmap outer = backgroundOf(ctx, list.get(0));
        if (outer != null) {
            drawBgCover(canvas, blurBg(outer), canvasW, canvasH, 1.1f);
            drawScrim(ctx, canvas, canvasW, canvasH);
        } else {
            canvas.drawColor(0xFFF6F5F8);
        }
        float scale = Math.min(1f, Math.min((canvasW - pad * 2) / (float) blockW,
                (canvasH - pad * 2) / (float) blockH));
        float baseX = (canvasW - blockW * scale) / 2f;
        float baseY = (canvasH - blockH * scale) / 2f;
        for (int i = 0; i < n; i++) {
            int r = i / bestCols;
            int c = i % bestCols;
            int rowCount = Math.min(bestCols, n - r * bestCols);
            float rowW = rowCount * bestW + (rowCount - 1) * gap;
            float x = baseX + ((blockW - rowW) / 2f + c * (bestW + gap)) * scale;
            float y = baseY + r * (bestMaxH + gap) * scale;
            Bitmap b = bestParts[i];
            android.graphics.RectF dst = new android.graphics.RectF(
                    x, y, x + b.getWidth() * scale, y + b.getHeight() * scale);
            drawCardShadow(ctx, canvas, dst, dp(ctx, 16) * scale);
            canvas.drawBitmap(b, null, dst, null);
        }
        return out;
    }

    /** 倒数日分享为图片；4:3 横版 + 模糊背景 */
    public static void shareCountdownsAsImage(Context ctx, List<Countdown> list) {
        if (list == null || list.isEmpty()) {
            Toast.makeText(ctx, "没有可分享的倒数日", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Bitmap out = renderCountdownsLandscape(ctx, list);
            shareBitmap(ctx, out, "倒数日" + (list.size() > 1 ? "合集" : ""),
                    "倒数日", "分享倒数日图片");
        } catch (Exception e) {
            Toast.makeText(ctx, "分享失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /** 保存倒数日卡片图到 Download/mori_notes/；多条时每条单独存一张，不合并长图 */
    public static void saveCountdownsImage(Context ctx, List<Countdown> list) {
        if (list == null || list.isEmpty()) {
            Toast.makeText(ctx, "没有可保存的倒数日", Toast.LENGTH_SHORT).show();
            return;
        }
        String stamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                .format(new java.util.Date());
        boolean many = list.size() > 1;
        int ok = 0;
        String last = "";
        String err = null;
        for (int i = 0; i < list.size(); i++) {
            Countdown c = list.get(i);
            try {
                Bitmap out = renderCountdown(ctx, c);
                String name;
                if (many) {
                    String idx = list.size() > 99 ? String.valueOf(i + 1)
                            : String.format(java.util.Locale.US, "%02d", i + 1);
                    name = "倒数日_" + idx + "_" + safeName(c.title) + "_" + stamp;
                } else {
                    name = "倒数日_" + stamp;
                }
                last = ExportUtils.saveBitmap(ctx, name, out);
                ok++;
            } catch (Exception e) {
                err = e.getMessage();
            }
        }
        if (ok == 0) {
            Toast.makeText(ctx, "保存失败：" + (err == null ? "未知错误" : err),
                    Toast.LENGTH_SHORT).show();
        } else if (many) {
            Toast.makeText(ctx, "已分开保存 " + ok + " 张到 Download/" + ExportUtils.DIR_NAME + "/",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(ctx, "已保存到 " + last, Toast.LENGTH_LONG).show();
        }
    }

    /** 文件名清洗：去掉系统不允许的字符，过长截断 */
    private static String safeName(String title) {
        String n = title == null ? "" : title.trim();
        n = n.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "_");
        n = n.replaceAll("\\s+", " ").trim();
        if (n.isEmpty()) n = "无标题";
        if (n.length() > 24) n = n.substring(0, 24);
        return n;
    }

    /** 单条倒数日分享为图片 */
    public static void shareCountdownAsImage(Context ctx, Countdown c) {
        if (c == null) {
            Toast.makeText(ctx, "没有可分享的倒数日", Toast.LENGTH_SHORT).show();
            return;
        }
        shareCountdownsAsImage(ctx, java.util.Collections.singletonList(c));
    }

    /** 单张倒数日卡片位图；有全屏背景图时以背景铺底并加蒙版。 */
    public static Bitmap renderCountdown(Context ctx, Countdown c) {
        int width = Math.min(ctx.getResources().getDisplayMetrics().widthPixels - dp(ctx, 32), dp(ctx, 720));
        return renderCountdown(ctx, c, Math.max(width, dp(ctx, 320)));
    }

    /** 指定卡片宽度的单张倒数日卡片位图。 */
    public static Bitmap renderCountdown(Context ctx, Countdown c, int width) {
        return renderCountdown(ctx, c, width, 0, 0);
    }

    /** 指定宽度/高度/圆角的卡片位图；fixedH>0 时固定高度且内容垂直居中，cornerPx>0 时圆角裁剪。 */
    public static Bitmap renderCountdown(Context ctx, Countdown c, int width, int fixedH, int cornerPx) {
        width = Math.max(width, dp(ctx, 140));
        final float fs = fixedH > 0 ? 1.1f : 1f;
        long today = CountdownTime.today();
        long diff = CountdownTime.dayDiff(today, c.date);
        long abs = Math.abs(diff);
        String lunar = LunarUtil.format(c.date);
        String anniv = CountdownTime.anniversaryText(c, today);

        Bitmap bg = backgroundOf(ctx, c);
        boolean hasBg = bg != null;

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(hasBg ? Color.TRANSPARENT : Color.WHITE);
        int pad = dp(ctx, 20);
        row.setPadding(pad, pad, pad, pad);

        LinearLayout left = new LinearLayout(ctx);
        left.setOrientation(LinearLayout.VERTICAL);
        row.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        android.graphics.Typeface tfR = Fonts.regular(ctx, c.fontPath);
        android.graphics.Typeface tfB = Fonts.bold(ctx, c.fontPath);
        TextView title = new TextView(ctx);
        if (tfR != null) title.setTypeface(tfR);
        title.setText(c.title == null || c.title.isEmpty() ? "无标题" : c.title);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f * fs);
        title.setTextColor(hasBg ? 0xFFFFFFFF : 0xFF1C1B1F);
        left.addView(title);

        TextView date = new TextView(ctx);
        if (tfR != null) date.setTypeface(tfR);
        date.setText(CountdownTime.formatDate(c.date) + (lunar.isEmpty() ? "" : " · " + lunar));
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f * fs);
        date.setTextColor(hasBg ? 0xF2FFFFFF : 0xFF6B6B70);
        date.setPadding(0, dp(ctx, 4), 0, 0);
        left.addView(date);

        if (anniv != null && !anniv.isEmpty()) {
            TextView a = new TextView(ctx);
            if (tfR != null) a.setTypeface(tfR);
            a.setText(anniv);
            a.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f * fs);
            a.setTextColor(hasBg ? 0xF2FFFFFF : 0xFF5B5BD6);
            a.setPadding(0, dp(ctx, 4), 0, 0);
            left.addView(a);
        }

        TextView num = new TextView(ctx);
        if (tfB != null) num.setTypeface(tfB);
        num.setText(String.valueOf(abs));
        num.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f * fs);
        num.setTextColor(hasBg ? 0xFFFFFFFF : 0xFF1C1B1F);
        row.addView(num);

        TextView unit = new TextView(ctx);
        if (tfR != null) unit.setTypeface(tfR);
        unit.setText(diff == 0 ? " 就是今天" : (diff > 0 ? " 天后" : " 天前"));
        unit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f * fs);
        unit.setTextColor(hasBg ? 0xF2FFFFFF : 0xFF6B6B70);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(ctx, 2);
        lp.gravity = android.view.Gravity.BOTTOM;
        lp.bottomMargin = dp(ctx, 6);
        row.addView(unit, lp);

        int wSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        row.measure(wSpec, hSpec);
        int contentH = Math.max(row.getMeasuredHeight(), 1);
        int h = fixedH > 0 ? Math.max(fixedH, contentH) : contentH;
        row.layout(0, (h - contentH) / 2, width, (h - contentH) / 2 + contentH);
        Bitmap bmp = Bitmap.createBitmap(width, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        if (cornerPx > 0) {
            android.graphics.Path clip = new android.graphics.Path();
            clip.addRoundRect(new android.graphics.RectF(0, 0, width, h), cornerPx, cornerPx,
                    android.graphics.Path.Direction.CW);
            canvas.clipPath(clip);
        }
        if (hasBg) {
            drawBgCover(canvas, bg, width, h);
            drawScrim(ctx, canvas, width, h);
            if (cornerPx > 0) drawFrost(ctx, canvas, width, h, cornerPx);
            row.draw(canvas);
        } else {
            canvas.drawColor(Color.WHITE);
            if (cornerPx > 0) drawFrost(ctx, canvas, width, h, cornerPx);
            row.draw(canvas);
            Paint border = new Paint();
            border.setColor(0xFFE6E6EA);
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(dp(ctx, 1));
            canvas.drawRect(0.5f, 0.5f, width - 0.5f, h - 0.5f, border);
        }
        return bmp;
    }

    /** 卡片磨砂效果：半透明白色面 + 细亮边。 */
    private static void drawFrost(Context ctx, Canvas canvas, int w, int h, int r) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0x1AFFFFFF);
        canvas.drawRoundRect(new android.graphics.RectF(0, 0, w, h), r, r, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(ctx, 1));
        p.setColor(0x33FFFFFF);
        canvas.drawRoundRect(new android.graphics.RectF(0.5f, 0.5f, w - 0.5f, h - 0.5f), r, r, p);
    }

    /** 卡片投影：沿圆角矩形向外柔和投射。 */
    private static void drawCardShadow(Context ctx, Canvas canvas, android.graphics.RectF r, float radius) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0x00000000);
        p.setShadowLayer(dp(ctx, 10), 0, dp(ctx, 4), 0x33000000);
        canvas.drawRoundRect(r, radius, radius, p);
    }

    /** 该条倒数日的背景图：优先单条设置，其次全局卡片背景；无图返回 null。 */
    private static Bitmap backgroundOf(Context ctx, Countdown c) {
        try {
            if (c != null && c.bgUri != null && !c.bgUri.isEmpty()) {
                Bitmap b = CardBg.preview(ctx, c.bgUri);
                if (b != null) return b;
            }
            Uri g = CountdownPrefs.getCardBg(ctx);
            if (g != null) return CardBg.preview(ctx, g.toString());
        } catch (Exception ignored) {
        }
        return null;
    }

    /** 背景模糊：先大幅降采样再多次盒式模糊，得到柔和的虚化底图。 */
    private static Bitmap blurBg(Bitmap src) {
        if (src == null) return null;
        try {
            int w = Math.max(24, src.getWidth() / 8);
            int h = Math.max(24, src.getHeight() / 8);
            Bitmap small = Bitmap.createScaledBitmap(src, w, h, true);
            int[] px = new int[w * h];
            small.getPixels(px, 0, w, 0, 0, w, h);
            int[] tmp = new int[px.length];
            for (int pass = 0; pass < 2; pass++) {
                boxH(px, tmp, w, h, 1);
                boxV(tmp, px, w, h, 1);
            }
            small.setPixels(px, 0, w, 0, 0, w, h);
            return small;
        } catch (Exception e) {
            return src;
        }
    }

    private static void boxH(int[] src, int[] dst, int w, int h, int r) {
        for (int y = 0; y < h; y++) {
            int row = y * w;
            for (int x = 0; x < w; x++) {
                int a = 0, rr = 0, gg = 0, bb = 0, c = 0;
                for (int k = -r; k <= r; k++) {
                    int xx = Math.min(w - 1, Math.max(0, x + k));
                    int p = src[row + xx];
                    a += (p >>> 24) & 0xFF;
                    rr += (p >> 16) & 0xFF;
                    gg += (p >> 8) & 0xFF;
                    bb += p & 0xFF;
                    c++;
                }
                dst[row + x] = ((a / c) << 24) | ((rr / c) << 16) | ((gg / c) << 8) | (bb / c);
            }
        }
    }

    private static void boxV(int[] src, int[] dst, int w, int h, int r) {
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int a = 0, rr = 0, gg = 0, bb = 0, c = 0;
                for (int k = -r; k <= r; k++) {
                    int yy = Math.min(h - 1, Math.max(0, y + k));
                    int p = src[yy * w + x];
                    a += (p >>> 24) & 0xFF;
                    rr += (p >> 16) & 0xFF;
                    gg += (p >> 8) & 0xFF;
                    bb += p & 0xFF;
                    c++;
                }
                dst[y * w + x] = ((a / c) << 24) | ((rr / c) << 16) | ((gg / c) << 8) | (bb / c);
            }
        }
    }

    /** 等比铺满裁剪绘制（cover）。 */
    private static void drawBgCover(Canvas canvas, Bitmap bg, int w, int h) {
        drawBgCover(canvas, bg, w, h, 1f);
    }

    /** 同 drawBgCover，zoom 为额外放大倍数（>1 表示画面放大、边缘裁切）。 */
    private static void drawBgCover(Canvas canvas, Bitmap bg, int w, int h, float zoom) {
        if (bg == null || w <= 0 || h <= 0) return;
        float scale = Math.max(w / (float) bg.getWidth(), h / (float) bg.getHeight()) * zoom;
        float dw = bg.getWidth() * scale;
        float dh = bg.getHeight() * scale;
        float dx = (w - dw) / 2f;
        float dy = (h - dh) / 2f;
        Paint filter = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        canvas.save();
        canvas.clipRect(0, 0, w, h);
        canvas.drawBitmap(bg, null, new android.graphics.RectF(dx, dy, dx + dw, dy + dh), filter);
        canvas.restore();
    }

    /** 背景图上的深色蒙版，强度取卡片蒙版设置。 */
    private static void drawScrim(Context ctx, Canvas canvas, int w, int h) {
        int alpha = Math.round(255f * CountdownPrefs.getCardMask(ctx) / 100f);
        if (alpha <= 0) return;
        Paint p = new Paint();
        p.setColor(Color.BLACK);
        p.setAlpha(alpha);
        canvas.drawRect(0, 0, w, h, p);
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
