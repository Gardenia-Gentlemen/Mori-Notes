/*
 * 常见 PANTONE 色号的近似 RGB。
 *
 * 完整 PANTONE 色库为商业授权数据，此处仅收录少量常用色号的近似值，
 * 未收录的色号请改用 HEX（如 #FF5733）或直接使用色盘取色。
 */
package com.mori.notebook;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** 把 #RRGGBB / PANTONE 号解析成颜色，解析失败返回 0。 */
public final class PantoneColors {
    private static final Map<String, Integer> TABLE = new HashMap<>();

    static {
        put("BLACKC", 0xFF2D2926);
        put("WHITEC", 0xFFFFFFFF);
        put("COOLGRAY5C", 0xFFB1B3B3);
        put("COOLGRAY11C", 0xFF53565A);
        put("419C", 0xFF23282B);
        put("424C", 0xFF707372);
        put("431C", 0xFF596365);
        put("7547C", 0xFF2A3244);
        put("871C", 0xFF84754E);
        put("877C", 0xFF888B8D);
        put("012C", 0xFFFFD100);
        put("109C", 0xFFFFD100);
        put("116C", 0xFFFCD757);
        put("021C", 0xFFFE5000);
        put("165C", 0xFFFE5000);
        put("172C", 0xFFFA4616);
        put("179C", 0xFFE03C31);
        put("485C", 0xFFDA291C);
        put("185C", 0xFFE4002B);
        put("186C", 0xFFC8102E);
        put("199C", 0xFFE0004D);
        put("208C", 0xFF861F41);
        put("7427C", 0xFFA6192E);
        put("259C", 0xFF6D2077);
        put("268C", 0xFF753BBD);
        put("2665C", 0xFF7A5DC7);
        put("272C", 0xFF6C7BC4);
        put("293C", 0xFF003DA5);
        put("286C", 0xFF0033A0);
        put("300C", 0xFF005EB8);
        put("348C", 0xFF00843D);
        put("355C", 0xFF009639);
        put("368C", 0xFF78BE20);
        put("375C", 0xFF97D700);
        put("382C", 0xFFC4D600);
    }

    private PantoneColors() {
    }

    private static void put(String key, int color) {
        TABLE.put(key, color);
    }

    /** 解析 #RRGGBB、RRGGBB、PANTONE 185 C、185C 等写法。 */
    public static int parse(String raw) {
        if (raw == null) return 0;
        String s = raw.trim().toUpperCase(Locale.ROOT);
        if (s.isEmpty()) return 0;
        s = s.replace("PANTONE", "").replace("潘通", "").trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.matches("[0-9A-F]{6}")) {
            try {
                return 0xFF000000 | Integer.parseInt(s, 16);
            } catch (Exception ignored) {
                return 0;
            }
        }
        if (s.matches("[0-9A-F]{3}")) {
            try {
                int r = Integer.parseInt(s.substring(0, 1), 16);
                int g = Integer.parseInt(s.substring(1, 2), 16);
                int b = Integer.parseInt(s.substring(2, 3), 16);
                return 0xFF000000 | (r * 17 << 16) | (g * 17 << 8) | (b * 17);
            } catch (Exception ignored) {
                return 0;
            }
        }
        String key = s.replace(" ", "").replace("-", "");
        Integer c = TABLE.get(key);
        if (c == null && !key.endsWith("C") && !key.endsWith("U")) {
            c = TABLE.get(key + "C");
        }
        if (c == null) {
            String head = key.replaceAll("[^0-9]", "");
            if (!head.isEmpty()) c = TABLE.get(head + "C");
        }
        return c == null ? 0 : c;
    }
}
