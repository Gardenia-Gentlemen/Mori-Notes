package com.mori.notebook;

/* loaded from: classes.dex */
public class MarkdownRenderer {
    private static final int CODE_BG = 526864548;
    private static final int GRAY = -9539981;
    private static final int LINK = -10006364;
    private static final java.util.regex.Pattern HEADING = java.util.regex.Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final java.util.regex.Pattern BULLET = java.util.regex.Pattern.compile("^[-*+]\\s+(.*)$");
    private static final java.util.regex.Pattern CHECKBOX = java.util.regex.Pattern.compile("^[-*+]\\s+\\[( |x|X)\\]\\s*(.*)$");
    private static final java.util.regex.Pattern NUM = java.util.regex.Pattern.compile("^(\\d+)[.)]\\s+(.*)$");
    private static final java.util.regex.Pattern IMG = java.util.regex.Pattern.compile("^!\\[([^\\]]*)\\]\\((.+)\\)$");
    private static final java.util.regex.Pattern HR = java.util.regex.Pattern.compile("^(-{3,}|\\*{3,}|_{3,})$");

    private static float sizeFor(int i) {
        if (i == 1) {
            return 1.7f;
        }
        if (i == 2) {
            return 1.45f;
        }
        if (i == 3) {
            return 1.25f;
        }
        if (i != 4) {
            return i != 5 ? 1.0f : 1.04f;
        }
        return 1.12f;
    }

    public static java.lang.CharSequence render(java.lang.String str) {
        android.text.SpannableStringBuilder spannableStringBuilder = new android.text.SpannableStringBuilder();
        if (str == null) {
            return spannableStringBuilder;
        }
        java.lang.String[] split = str.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        java.lang.StringBuilder sb = new java.lang.StringBuilder();
        boolean z = false;
        for (java.lang.String str2 : split) {
            java.lang.String trim = str2.trim();
            if (trim.startsWith("```")) {
                if (z) {
                    appendCode(spannableStringBuilder, sb.toString());
                    sb.setLength(0);
                    z = false;
                } else {
                    z = true;
                }
            } else if (z) {
                sb.append(str2).append('\n');
            } else if (trim.isEmpty()) {
                spannableStringBuilder.append((java.lang.CharSequence) "\n");
            } else {
                java.util.regex.Matcher matcher = IMG.matcher(trim);
                if (matcher.matches() && appendImage(spannableStringBuilder, matcher.group(2))) {
                    spannableStringBuilder.append('\n');
                } else if (HR.matcher(trim).matches()) {
                    int length = spannableStringBuilder.length();
                    spannableStringBuilder.append((java.lang.CharSequence) "──────────────\n");
                    spannableStringBuilder.setSpan(new android.text.style.ForegroundColorSpan(GRAY), length, spannableStringBuilder.length(), 33);
                } else {
                    java.util.regex.Matcher matcher2 = HEADING.matcher(trim);
                    if (matcher2.matches()) {
                        int length2 = matcher2.group(1).length();
                        int length3 = spannableStringBuilder.length();
                        spannableStringBuilder.append((java.lang.CharSequence) matcher2.group(2)).append('\n');
                        int length4 = spannableStringBuilder.length() - 1;
                        spannableStringBuilder.setSpan(new android.text.style.StyleSpan(1), length3, length4, 33);
                        spannableStringBuilder.setSpan(new android.text.style.RelativeSizeSpan(sizeFor(length2)), length3, length4, 33);
                    } else if (trim.startsWith(">")) {
                        int length5 = spannableStringBuilder.length();
                        spannableStringBuilder.append((java.lang.CharSequence) trim.substring(1).trim()).append('\n');
                        int length6 = spannableStringBuilder.length() - 1;
                        spannableStringBuilder.setSpan(new android.text.style.StyleSpan(2), length5, length6, 33);
                        spannableStringBuilder.setSpan(new android.text.style.ForegroundColorSpan(GRAY), length5, length6, 33);
                    } else {
                        java.util.regex.Matcher matcher3 = CHECKBOX.matcher(trim);
                        if (matcher3.matches()) {
                            spannableStringBuilder.append((java.lang.CharSequence) (matcher3.group(1).trim().isEmpty() ^ true ? "☑  " : "☐  "));
                            inline(spannableStringBuilder, matcher3.group(2));
                            spannableStringBuilder.append('\n');
                        } else {
                            java.util.regex.Matcher matcher4 = BULLET.matcher(trim);
                            if (matcher4.matches()) {
                                spannableStringBuilder.append((java.lang.CharSequence) "•  ");
                                inline(spannableStringBuilder, matcher4.group(1));
                                spannableStringBuilder.append('\n');
                            } else {
                                java.util.regex.Matcher matcher5 = NUM.matcher(trim);
                                if (matcher5.matches()) {
                                    spannableStringBuilder.append((java.lang.CharSequence) matcher5.group(1)).append((java.lang.CharSequence) ".  ");
                                    inline(spannableStringBuilder, matcher5.group(2));
                                    spannableStringBuilder.append('\n');
                                } else {
                                    inline(spannableStringBuilder, trim);
                                    spannableStringBuilder.append('\n');
                                }
                            }
                        }
                    }
                }
            }
        }
        if (z && sb.length() > 0) {
            appendCode(spannableStringBuilder, sb.toString());
        }
        return spannableStringBuilder;
    }

    private static boolean appendImage(android.text.SpannableStringBuilder spannableStringBuilder, java.lang.String str) {
        try {
            java.lang.String trim = str.trim();
            if (trim.startsWith("<") && trim.endsWith(">")) {
                trim = trim.substring(1, trim.length() - 1);
            }
            if (trim.startsWith("file://")) {
                trim = android.net.Uri.parse(trim).getPath();
            }
            if (trim.startsWith(java.lang.String.valueOf(kotlin.text.Typography.quote)) && trim.endsWith(java.lang.String.valueOf(kotlin.text.Typography.quote))) {
                trim = trim.substring(1, trim.length() - 1);
            }
            java.io.File file = new java.io.File(trim);
            if (file.exists() && file.isFile()) {
                android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                android.graphics.BitmapFactory.decodeFile(trim, options);
                int i = 1;
                while (options.outWidth / i > 1600) {
                    i *= 2;
                }
                options.inJustDecodeBounds = false;
                options.inSampleSize = i;
                android.graphics.Bitmap decodeFile = android.graphics.BitmapFactory.decodeFile(trim, options);
                if (decodeFile == null) {
                    return false;
                }
                int width = decodeFile.getWidth();
                int height = decodeFile.getHeight();
                if (width > 720) {
                    height = java.lang.Math.max(1, (int) (height * (720 / width)));
                    decodeFile = android.graphics.Bitmap.createScaledBitmap(decodeFile, 720, height, true);
                    width = 720;
                }
                android.graphics.drawable.BitmapDrawable bitmapDrawable = new android.graphics.drawable.BitmapDrawable(android.content.res.Resources.getSystem(), decodeFile);
                bitmapDrawable.setBounds(0, 0, width, height);
                int length = spannableStringBuilder.length();
                spannableStringBuilder.append(" ");
                spannableStringBuilder.setSpan(new android.text.style.ImageSpan(bitmapDrawable, 0), length, length + 1, 33);
                return true;
            }
        } catch (java.lang.Throwable unused) {
        }
        return false;
    }

    private static void appendCode(android.text.SpannableStringBuilder spannableStringBuilder, java.lang.String str) {
        int length = spannableStringBuilder.length();
        spannableStringBuilder.append((java.lang.CharSequence) str);
        int length2 = spannableStringBuilder.length();
        spannableStringBuilder.setSpan(new android.text.style.TypefaceSpan("monospace"), length, length2, 33);
        spannableStringBuilder.setSpan(new android.text.style.BackgroundColorSpan(CODE_BG), length, length2, 33);
        spannableStringBuilder.setSpan(new android.text.style.RelativeSizeSpan(0.92f), length, length2, 33);
        spannableStringBuilder.append('\n');
    }

    private static void inline(android.text.SpannableStringBuilder spannableStringBuilder, java.lang.String str) {
        int i;
        int indexOf;
        int i2;
        int i3;
        int indexOf2;
        int i4;
        int i5;
        int length = str.length();
        int i6 = 0;
        while (i6 < length) {
            char charAt = str.charAt(i6);
            if (charAt == '\\' && (i5 = i6 + 1) < length) {
                spannableStringBuilder.append(str.charAt(i5));
                i6 += 2;
            } else {
                if (str.startsWith("**", i6) && (indexOf2 = str.indexOf("**", (i4 = i6 + 2))) > 0) {
                    int length2 = spannableStringBuilder.length();
                    spannableStringBuilder.append((java.lang.CharSequence) str, i4, indexOf2);
                    spannableStringBuilder.setSpan(new android.text.style.StyleSpan(1), length2, spannableStringBuilder.length(), 33);
                } else if (!str.startsWith("~~", i6) || (indexOf2 = str.indexOf("~~", (i3 = i6 + 2))) <= 0) {
                    if (charAt == '`' && (indexOf = str.indexOf(96, (i2 = i6 + 1))) > 0) {
                        int length3 = spannableStringBuilder.length();
                        spannableStringBuilder.append((java.lang.CharSequence) str, i2, indexOf);
                        spannableStringBuilder.setSpan(new android.text.style.TypefaceSpan("monospace"), length3, spannableStringBuilder.length(), 33);
                        spannableStringBuilder.setSpan(new android.text.style.BackgroundColorSpan(CODE_BG), length3, spannableStringBuilder.length(), 33);
                    } else if (charAt == '*' && (indexOf = str.indexOf(42, (i = i6 + 1))) > i) {
                        int length4 = spannableStringBuilder.length();
                        spannableStringBuilder.append((java.lang.CharSequence) str, i, indexOf);
                        spannableStringBuilder.setSpan(new android.text.style.StyleSpan(2), length4, spannableStringBuilder.length(), 33);
                    } else {
                        if (str.startsWith("![", i6)) {
                            int i7 = i6 + 2;
                            int indexOf3 = str.indexOf("](", i7);
                            int indexOf4 = indexOf3 > 0 ? str.indexOf(41, indexOf3 + 2) : -1;
                            if (indexOf3 > 0 && indexOf4 > 0) {
                                int length5 = spannableStringBuilder.length();
                                spannableStringBuilder.append("🖼 ");
                                spannableStringBuilder.append((java.lang.CharSequence) str, i7, indexOf3);
                                spannableStringBuilder.setSpan(new android.text.style.StyleSpan(2), length5, spannableStringBuilder.length(), 33);
                                i6 = indexOf4 + 1;
                            }
                        }
                        if (charAt == '[') {
                            int i8 = i6 + 1;
                            int indexOf5 = str.indexOf("](", i8);
                            int indexOf6 = indexOf5 > 0 ? str.indexOf(41, indexOf5 + 2) : -1;
                            if (indexOf5 > 0 && indexOf6 > 0) {
                                int length6 = spannableStringBuilder.length();
                                spannableStringBuilder.append((java.lang.CharSequence) str, i8, indexOf5);
                                int length7 = spannableStringBuilder.length();
                                spannableStringBuilder.setSpan(new android.text.style.ForegroundColorSpan(LINK), length6, length7, 33);
                                spannableStringBuilder.setSpan(new android.text.style.StyleSpan(1), length6, length7, 33);
                                spannableStringBuilder.setSpan(new android.text.style.UnderlineSpan(), length6, length7, 33);
                                i6 = indexOf6 + 1;
                            }
                        }
                        spannableStringBuilder.append(charAt);
                        i6++;
                    }
                    i6 = indexOf + 1;
                } else {
                    int length8 = spannableStringBuilder.length();
                    spannableStringBuilder.append((java.lang.CharSequence) str, i3, indexOf2);
                    spannableStringBuilder.setSpan(new android.text.style.StrikethroughSpan(), length8, spannableStringBuilder.length(), 33);
                }
                i6 = indexOf2 + 2;
            }
        }
    }

    public static java.lang.String toPlainText(java.lang.String str) {
        if (str == null) {
            return "";
        }
        java.lang.String[] split = str.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        java.lang.StringBuilder sb = new java.lang.StringBuilder();
        boolean z = false;
        for (java.lang.String str2 : split) {
            java.lang.String trim = str2.trim();
            if (trim.startsWith("```")) {
                z = !z;
            } else if (z) {
                sb.append(str2).append('\n');
            } else if (!HR.matcher(trim).matches()) {
                java.util.regex.Matcher matcher = IMG.matcher(trim);
                if (matcher.matches()) {
                    java.lang.String trim2 = matcher.group(1).trim();
                    sb.append(trim2.isEmpty() ? "[图片]" : "[" + trim2 + "]").append('\n');
                } else {
                    java.util.regex.Matcher matcher2 = HEADING.matcher(trim);
                    if (matcher2.matches()) {
                        trim = matcher2.group(2);
                    } else {
                        java.util.regex.Matcher matcher3 = CHECKBOX.matcher(trim);
                        if (matcher3.matches()) {
                            trim = ("x".equalsIgnoreCase(matcher3.group(1)) ? "[x] " : "[ ] ") + matcher3.group(2);
                        } else {
                            java.util.regex.Matcher matcher4 = BULLET.matcher(trim);
                            if (matcher4.matches()) {
                                trim = "· " + matcher4.group(1);
                            } else {
                                java.util.regex.Matcher matcher5 = NUM.matcher(trim);
                                if (matcher5.matches()) {
                                    trim = matcher5.group(1) + ". " + matcher5.group(2);
                                } else if (trim.startsWith(">")) {
                                    trim = trim.replaceFirst("^>\\s*", "");
                                }
                            }
                        }
                    }
                    sb.append(trim.replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)", "[图片]").replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1").replaceAll("`([^`]*)`", "$1").replaceAll("\\*\\*([^*]+)\\*\\*", "$1").replaceAll("__([^_]+)__", "$1").replaceAll("~~([^~]+)~~", "$1").replaceAll("\\*([^*]+)\\*", "$1").replaceAll("<[^>]+>", "")).append('\n');
                }
            }
        }
        java.lang.String sb2 = sb.toString();
        while (sb2.endsWith("\n\n")) {
            sb2 = sb2.substring(0, sb2.length() - 1);
        }
        return sb2;
    }
}
