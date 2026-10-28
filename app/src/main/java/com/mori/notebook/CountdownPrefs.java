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

public final class CountdownPrefs {
    private static final String P = "countdown_prefs";
    private static final String K_HOUR = "day_start_hour";
    private static final String K_BG = "card_bg_uri";
    private static final String K_MASK = "card_mask";

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
}
