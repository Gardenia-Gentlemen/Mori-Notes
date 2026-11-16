package com.mori.notebook;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 轻量 Markdown 渲染：标题、粗斜体、删除线、行内/块代码、列表、待办、引用、分割线、链接、图片占位。 */
public class MarkdownRenderer {

    private static final int GRAY = 0xFF6E6E73;
    private static final int LINK = 0xFF6750A4;
    private static final int CODE_BG = 0x1F6750A4;

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final Pattern BULLET = Pattern.compile("^[-*+]\\s+(.*)$");
    private static final Pattern CHECKBOX = Pattern.compile("^[-*+]\\s+\\[( |x|X)\\]\\s*(.*)$");
    private static final Pattern NUM = Pattern.compile("^(\\d+)[.)]\\s+(.*)$");
    private static final Pattern IMG = Pattern.compile("^!\\[([^\\]]*)\\]\\((.+)\\)$");
    private static final Pattern HR = Pattern.compile("^(-{3,}|\\*{3,}|_{3,})$");

    public static CharSequence render(String md) {
        SpannableStringBuilder out = new SpannableStringBuilder();
        if (md == null) return out;
        String[] lines = md.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        boolean inCode = false;
        StringBuilder code = new StringBuilder();

        for (String line : lines) {
            String t = line.trim();
            if (t.startsWith("```")) {
                if (inCode) {
                    appendCode(out, code.toString());
                    code.setLength(0);
                    inCode = false;
                } else {
                    inCode = true;
                }
                continue;
            }
            if (inCode) {
                code.append(line).append('\n');
                continue;
            }
            if (t.isEmpty()) {
                out.append("\n");
                continue;
            }
            Matcher im = IMG.matcher(t);
            if (im.matches() && appendImage(out, im.group(2))) {
                out.append('\n');
                continue;
            }
            if (HR.matcher(t).matches()) {
                int s = out.length();
                out.append("──────────────\n");
                out.setSpan(new ForegroundColorSpan(GRAY), s, out.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                continue;
            }
            Matcher h = HEADING.matcher(t);
            if (h.matches()) {
                int level = h.group(1).length();
                int s = out.length();
                out.append(h.group(2)).append('\n');
                int e = out.length() - 1;
                out.setSpan(new StyleSpan(Typeface.BOLD), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new RelativeSizeSpan(sizeFor(level)), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                continue;
            }
            if (t.startsWith(">")) {
                int s = out.length();
                out.append(t.substring(1).trim()).append('\n');
                int e = out.length() - 1;
                out.setSpan(new StyleSpan(Typeface.ITALIC), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new ForegroundColorSpan(GRAY), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                continue;
            }
            Matcher cb = CHECKBOX.matcher(t);
            if (cb.matches()) {
                boolean done = !cb.group(1).trim().isEmpty();
                out.append(done ? "☑  " : "☐  ");
                inline(out, cb.group(2));
                out.append('\n');
                continue;
            }
            Matcher b = BULLET.matcher(t);
            if (b.matches()) {
                out.append("•  ");
                inline(out, b.group(1));
                out.append('\n');
                continue;
            }
            Matcher n = NUM.matcher(t);
            if (n.matches()) {
                out.append(n.group(1)).append(".  ");
                inline(out, n.group(2));
                out.append('\n');
                continue;
            }
            inline(out, t);
            out.append('\n');
        }
        if (inCode && code.length() > 0) appendCode(out, code.toString());
        return out;
    }

    private static float sizeFor(int level) {
        switch (level) {
            case 1: return 1.7f;
            case 2: return 1.45f;
            case 3: return 1.25f;
            case 4: return 1.12f;
            case 5: return 1.04f;
            default: return 1.0f;
        }
    }

    /** 将本地图片插入为 ImageSpan，成功返回 true。 */
    private static boolean appendImage(SpannableStringBuilder out, String target) {
        try {
            String path = target.trim();
            if (path.startsWith("<") && path.endsWith(">")) path = path.substring(1, path.length() - 1);
            if (path.startsWith("file://")) path = android.net.Uri.parse(path).getPath();
            if (path.startsWith(String.valueOf((char)34)) && path.endsWith(String.valueOf((char)34))) path = path.substring(1, path.length() - 1);
            java.io.File f = new java.io.File(path);
            if (!f.exists() || !f.isFile()) return false;
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, o);
            int sample = 1;
            while (o.outWidth / sample > 1600) sample *= 2;
            o.inJustDecodeBounds = false;
            o.inSampleSize = sample;
            Bitmap bmp = BitmapFactory.decodeFile(path, o);
            if (bmp == null) return false;
            int w = bmp.getWidth();
            int h = bmp.getHeight();
            int maxW = 720;
            if (w > maxW) {
                int nh = Math.max(1, (int) (h * (maxW / (float) w)));
                bmp = Bitmap.createScaledBitmap(bmp, maxW, nh, true);
                w = maxW;
                h = nh;
            }
            BitmapDrawable d = new BitmapDrawable(android.content.res.Resources.getSystem(), bmp);
            d.setBounds(0, 0, w, h);
            int s0 = out.length();
            out.append(" ");
            out.setSpan(new ImageSpan(d, ImageSpan.ALIGN_BOTTOM), s0, s0 + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private static void appendCode(SpannableStringBuilder out, String code) {
        int s = out.length();
        out.append(code);
        int e = out.length();
        out.setSpan(new TypefaceSpan("monospace"), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.setSpan(new BackgroundColorSpan(CODE_BG), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.setSpan(new RelativeSizeSpan(0.92f), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.append('\n');
    }

    private static void inline(SpannableStringBuilder out, String text) {
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < n) {
                out.append(text.charAt(i + 1));
                i += 2;
                continue;
            }
            if (text.startsWith("**", i)) {
                int end = text.indexOf("**", i + 2);
                if (end > 0) {
                    int s = out.length();
                    out.append(text, i + 2, end);
                    out.setSpan(new StyleSpan(Typeface.BOLD), s, out.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i = end + 2;
                    continue;
                }
            }
            if (text.startsWith("~~", i)) {
                int end = text.indexOf("~~", i + 2);
                if (end > 0) {
                    int s = out.length();
                    out.append(text, i + 2, end);
                    out.setSpan(new StrikethroughSpan(), s, out.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i = end + 2;
                    continue;
                }
            }
            if (c == '`') {
                int end = text.indexOf('`', i + 1);
                if (end > 0) {
                    int s = out.length();
                    out.append(text, i + 1, end);
                    out.setSpan(new TypefaceSpan("monospace"), s, out.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    out.setSpan(new BackgroundColorSpan(CODE_BG), s, out.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i = end + 1;
                    continue;
                }
            }
            if (c == '*') {
                int end = text.indexOf('*', i + 1);
                if (end > i + 1) {
                    int s = out.length();
                    out.append(text, i + 1, end);
                    out.setSpan(new StyleSpan(Typeface.ITALIC), s, out.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i = end + 1;
                    continue;
                }
            }
            if (text.startsWith("![", i)) {
                int close = text.indexOf("](", i + 2);
                int paren = close > 0 ? text.indexOf(')', close + 2) : -1;
                if (close > 0 && paren > 0) {
                    int s = out.length();
                    out.append("🖼 ");
                    out.append(text, i + 2, close);
                    out.setSpan(new StyleSpan(Typeface.ITALIC), s, out.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i = paren + 1;
                    continue;
                }
            }
            if (c == '[') {
                int close = text.indexOf("](", i + 1);
                int paren = close > 0 ? text.indexOf(')', close + 2) : -1;
                if (close > 0 && paren > 0) {
                    int s = out.length();
                    out.append(text, i + 1, close);
                    int e = out.length();
                    out.setSpan(new ForegroundColorSpan(LINK), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    out.setSpan(new StyleSpan(Typeface.BOLD), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    out.setSpan(new UnderlineSpan(), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i = paren + 1;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
    }

    /** 去除 Markdown 语法标记，得到纯文本。 */
    public static String toPlainText(String md) {
        if (md == null) return "";
        String[] lines = md.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        StringBuilder out = new StringBuilder();
        boolean inCode = false;
        for (String line : lines) {
            String t = line.trim();
            if (t.startsWith("```")) {
                inCode = !inCode;
                continue;
            }
            if (inCode) {
                out.append(line).append('\n');
                continue;
            }
            if (HR.matcher(t).matches()) continue;
            Matcher im = IMG.matcher(t);
            if (im.matches()) {
                String alt = im.group(1).trim();
                out.append(alt.isEmpty() ? "[图片]" : "[" + alt + "]").append('\n');
                continue;
            }
            String l = t;
            Matcher m;
            if ((m = HEADING.matcher(l)).matches()) {
                l = m.group(2);
            } else if ((m = CHECKBOX.matcher(l)).matches()) {
                l = ("x".equalsIgnoreCase(m.group(1)) ? "[x] " : "[ ] ") + m.group(2);
            } else if ((m = BULLET.matcher(l)).matches()) {
                l = "· " + m.group(1);
            } else if ((m = NUM.matcher(l)).matches()) {
                l = m.group(1) + ". " + m.group(2);
            } else if (l.startsWith(">")) {
                l = l.replaceFirst("^>\\s*", "");
            }
            l = l.replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)", "[图片]");
            l = l.replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1");
            l = l.replaceAll("`([^`]*)`", "$1");
            l = l.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
            l = l.replaceAll("__([^_]+)__", "$1");
            l = l.replaceAll("~~([^~]+)~~", "$1");
            l = l.replaceAll("\\*([^*]+)\\*", "$1");
            l = l.replaceAll("<[^>]+>", "");
            out.append(l).append('\n');
        }
        String r = out.toString();
        while (r.endsWith("\n\n")) r = r.substring(0, r.length() - 1);
        return r;
    }
}
