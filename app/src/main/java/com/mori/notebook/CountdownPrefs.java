/*
 * 倒数日偏好：日切时间、卡片背景图片与蒙版强度。
 *
 * 参考自 TimeTrace (https://github.com/KIPPUDESU/KIPPU_Trace)
 * Copyright (c) 2026 切符
 * 依据 MIT License 使用，许可全文见项目根目录 LICENSE-KIPPU_Trace。
 */
package com.mori.notebook;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.util.TimeZone;

public final class CountdownPrefs {
    private static final String P = "countdown_prefs";
    private static final String K_HOUR = "day_start_hour";
    private static final String K_BG = "card_bg_uri";
    private static final String K_MASK = "card_mask";
    private static final String K_FONT = "fullscreen_font_path";
    private static final String K_TZ = "timezone_id";
    private static final String K_TZ_SYS = "sys_timezone_seen";

    private CountdownPrefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(P, Context.MODE_PRIVATE);
    }

    /** 日切时间：0~6 点，默认 0。 */
    public static int getDayStartHour(Context c) {
        return sp(c).getInt(K_HOUR, 0);
    }

    public static void setDayStartHour(Context c, int h) {
        int v = Math.max(0, Math.min(6, h));
        sp(c).edit().putInt(K_HOUR, v).apply();
        CountdownTime.setDayStartHour(v);
    }

    public static Uri getCardBg(Context c) {
        String s = sp(c).getString(K_BG, null);
        return s == null ? null : Uri.parse(s);
    }

    public static void setCardBg(Context c, Uri uri) {
        if (uri == null) {
            sp(c).edit().remove(K_BG).apply();
        } else {
            sp(c).edit().putString(K_BG, uri.toString()).apply();
        }
    }

    /** 蒙版强度 0~80（百分比）。 */
    public static int getCardMask(Context c) {
        return sp(c).getInt(K_MASK, 40);
    }

    public static void setCardMask(Context c, int v) {
        sp(c).edit().putInt(K_MASK, Math.max(0, Math.min(80, v))).apply();
    }

    /** 全屏页自定义字体的本地文件绝对路径，空表示用系统字体。 */
    public static String getFontPath(Context c) {
        return sp(c).getString(K_FONT, null);
    }

    public static void setFontPath(Context c, String path) {
        if (path == null || path.trim().isEmpty()) {
            sp(c).edit().remove(K_FONT).apply();
        } else {
            sp(c).edit().putString(K_FONT, path).apply();
        }
    }
    /** 应用时区 ID：空字符串表示跟随系统。 */
    public static String getTimeZoneId(Context c) {
        return sp(c).getString(K_TZ, "");
    }

    /** 实际生效的时区 ID（跟随系统时解析为系统当前时区）。 */
    public static String resolveTimeZoneId(Context c) {
        String id = getTimeZoneId(c).trim();
        return id.isEmpty() ? TimeZone.getDefault().getID() : id;
    }

    public static boolean isFollowSystem(Context c) {
        return getTimeZoneId(c).trim().isEmpty();
    }

    /** 设置应用时区；null 或空表示跟随系统。写入后立即同步到运行时。 */
    public static void setTimeZoneId(Context c, String id) {
        if (id == null || id.trim().isEmpty()) {
            sp(c).edit().remove(K_TZ).apply();
        } else {
            sp(c).edit().putString(K_TZ, id.trim()).apply();
        }
        CountdownTime.setTimeZone(resolveTimeZoneId(c));
    }

    /** 上一次已确认过的系统时区，用于检测手机时区变化。 */
    public static String getSeenSystemTimeZone(Context c) {
        return sp(c).getString(K_TZ_SYS, "");
    }

    public static void setSeenSystemTimeZone(Context c, String id) {
        if (id == null || id.trim().isEmpty()) {
            sp(c).edit().remove(K_TZ_SYS).apply();
        } else {
            sp(c).edit().putString(K_TZ_SYS, id.trim()).apply();
        }
    }
}
