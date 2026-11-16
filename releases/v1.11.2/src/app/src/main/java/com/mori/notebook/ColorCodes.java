/*
 * 颜色编码解析：支持 HEX 与十进制 RGB。
 */
package com.mori.notebook;

import java.util.Locale;

/** 把 #RRGGBB / RRGGBB / 255,87,51 解析成颜色，失败返回 0。 */
public final class ColorCodes {
    private ColorCodes() {
    }

    /** 解析 HEX（#RRGGBB、RRGGBB、#RGB）或 RGB（255,87,51 / 255 87 51）。 */
    public static int parse(String raw) {
        if (raw == null) return 0;
        String s = raw.trim();
        if (s.isEmpty()) return 0;
        // RGB 写法
        if (s.contains(",") || s.matches("[0-9\s]+\s[0-9\s]+")) {
            String[] parts = s.split("[,，;\s]+");
            java.util.ArrayList<String> nums = new java.util.ArrayList<>();
            for (String p : parts) {
                if (!p.isEmpty()) nums.add(p);
            }
            if (nums.size() == 3) {
                try {
                    int r = Integer.parseInt(nums.get(0));
                    int g = Integer.parseInt(nums.get(1));
                    int b = Integer.parseInt(nums.get(2));
                    if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) return 0;
                    return 0xFF000000 | (r << 16) | (g << 8) | b;
                } catch (Exception ignored) {
                    return 0;
                }
            }
            return 0;
        }
        // HEX 写法
        String h = s.toUpperCase(Locale.ROOT);
        if (h.startsWith("#")) h = h.substring(1);
        if (h.startsWith("0X")) h = h.substring(2);
        try {
            if (h.matches("[0-9A-F]{6}")) {
                return 0xFF000000 | Integer.parseInt(h, 16);
            }
            if (h.matches("[0-9A-F]{3}")) {
                int r = Integer.parseInt(h.substring(0, 1), 16);
                int g = Integer.parseInt(h.substring(1, 2), 16);
                int b = Integer.parseInt(h.substring(2, 3), 16);
                return 0xFF000000 | (r * 17 << 16) | (g * 17 << 8) | (b * 17);
            }
        } catch (Exception ignored) {
            return 0;
        }
        return 0;
    }

    /** 颜色 → #RRGGBB 文本。 */
    public static String hex(int color) {
        return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }
}
