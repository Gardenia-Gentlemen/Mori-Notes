/*
 * 农历换算（1900–2100）。
 *
 * 参考自 TimeTrace (https://github.com/KIPPUDESU/KIPPU_Trace)
 * Copyright (c) 2026 切符
 * 依据 MIT License 使用，许可全文见项目根目录 LICENSE-KIPPU_Trace。
 */
package com.mori.notebook;

/** 公历日期 → 农历文本；out of range 返回空串。 */
public final class LunarUtil {

    private LunarUtil() {
    }

    /** 1900–2100 每年：低 4 位=闰月，0x10000 位=闰月大小，其余位=各月大小（1=30 天） */
    private static final int[] INFO = {
            0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
            0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
            0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
            0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
            0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
            0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0,
            0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
            0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b5a0, 0x195a6,
            0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
            0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x05ac0, 0x0ab60, 0x096d5, 0x092e0,
            0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
            0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
            0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
            0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
            0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
            0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0,
            0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
            0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
            0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
            0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252,
            0x0d520
    };

    private static final String[] MONTH_NAME = {
            "正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"
    };

    private static final String[] DAY_NAME = {
            "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
            "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
            "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    };

    /** 农历 1900 年正月初一对应的公历日序号 */
    private static final long BASE_DAY = CountdownTime.dayNumber(CountdownTime.millisOf(1900, 1, 31));

    private static int leapMonth(int y) {
        return INFO[y - 1900] & 0xf;
    }

    private static int leapDays(int y) {
        if (leapMonth(y) == 0) return 0;
        return (INFO[y - 1900] & 0x10000) != 0 ? 30 : 29;
    }

    private static int monthDays(int y, int m) {
        return (INFO[y - 1900] & (0x10000 >> m)) != 0 ? 30 : 29;
    }

    private static int yearDays(int y) {
        int sum = 348;
        for (int i = 0x8000; i > 0x8; i >>= 1) {
            if ((INFO[y - 1900] & i) != 0) sum++;
        }
        return sum + leapDays(y);
    }

    /** 公历日序号 → 农历文本，如「农历九月十九」「农历闰六月廿九」 */
    public static String format(long millis) {
        if (millis <= 0) return "";
        long offset = CountdownTime.dayNumber(millis) - BASE_DAY;
        if (offset < 0) return "";
        int year = 1900;
        while (year <= 2100) {
            int d = yearDays(year);
            if (offset < d) break;
            offset -= d;
            year++;
        }
        if (year > 2100) return "";
        int leap = leapMonth(year);
        int month = 1;
        boolean isLeap = false;
        int remain = (int) offset;
        for (int m = 1; m <= 12; m++) {
            int d = monthDays(year, m);
            if (remain < d) {
                month = m;
                isLeap = false;
                break;
            }
            remain -= d;
            if (leap > 0 && m == leap) {
                int ld = leapDays(year);
                if (remain < ld) {
                    month = m;
                    isLeap = true;
                    break;
                }
                remain -= ld;
            }
            month = m;
        }
        int day = remain + 1;
        if (day < 1 || day > 30) return "";
        return "农历" + (isLeap ? "闰" : "") + MONTH_NAME[month - 1] + "月" + DAY_NAME[day - 1];
    }
}
