/*
 * 倒数日的日期计算工具（全部按 UTC 日历日语义运算，与系统时区无关）。
 *
 * 参考自 TimeTrace (https://github.com/KIPPUDESU/KIPPU_Trace)
 * Copyright (c) 2026 切符
 * 依据 MIT License 使用，许可全文见项目根目录 LICENSE-KIPPU_Trace。
 *
 * 说明：本项目 minSdk 24，不能直接使用 java.time，因此用 Calendar(UTC) 复刻原实现的语义。
 * DatePicker 返回的 UTC 午夜毫秒在这里一律解释为一个“日历日”。
 */
package com.mori.notebook;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class CountdownTime {

    private static final long DAY_MS = 86400000L;
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private CountdownTime() {
    }

    private static Calendar utcCal() {
        Calendar c = Calendar.getInstance(UTC, Locale.CHINA);
        return c;
    }

    /** 由年月日构造 UTC 午夜毫秒。 */
    public static long millisOf(int y, int m, int d) {
        Calendar c = utcCal();
        c.clear();
        c.set(y, m - 1, d, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** 取毫秒对应的 UTC 日序号。 */
    public static long dayNumber(long millis) {
        return Math.floorDiv(millis, DAY_MS);
    }

    /** 两个毫秒值的日历日差（正负号保留）。 */
    public static long dayDiff(long from, long to) {
        return dayNumber(to) - dayNumber(from);
    }

    public static int[] ymd(long millis) {
        Calendar c = utcCal();
        c.setTimeInMillis(millis);
        return new int[]{c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)};
    }

    /** 日切时间（0~6 点）：凌晨未到该点时仍算前一天。 */
    private static int dayStartHour = 0;

    public static void setDayStartHour(int h) {
        dayStartHour = Math.max(0, Math.min(6, h));
    }

    public static int dayStartHour() {
        return dayStartHour;
    }

    /** 本地时区下的“今天”，折算成 UTC 午夜毫秒，方便与其他日期做日差。 */
    public static long today() {
        Calendar local = Calendar.getInstance(appTz, Locale.CHINA);
        if (local.get(Calendar.HOUR_OF_DAY) < dayStartHour) {
            local.add(Calendar.DAY_OF_MONTH, -1);
        }
        return millisOf(local.get(Calendar.YEAR), local.get(Calendar.MONTH) + 1,
                local.get(Calendar.DAY_OF_MONTH));
    }

    public static String todayYmd() {
        return formatDateShort(today());
    }

    public static int daysInMonth(int y, int m) {
        Calendar c = utcCal();
        c.clear();
        c.set(y, m - 1, 1, 0, 0, 0);
        return c.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    public static long addDays(long millis, int n) {
        return millis + (long) n * DAY_MS;
    }

    public static long addMonths(long millis, int n) {
        int[] a = ymd(millis);
        int total = (a[0] * 12 + (a[1] - 1)) + n;
        int y = Math.floorDiv(total, 12);
        int m = Math.floorMod(total, 12) + 1;
        int d = Math.min(a[2], daysInMonth(y, m));
        return millisOf(y, m, d);
    }

    public static long addYears(long millis, int n) {
        int[] a = ymd(millis);
        int y = a[0] + n;
        int d = Math.min(a[2], daysInMonth(y, a[1]));
        return millisOf(y, a[1], d);
    }

    public static int dayOfWeek(long millis) {
        Calendar c = utcCal();
        c.setTimeInMillis(millis);
        return c.get(Calendar.DAY_OF_WEEK);
    }

    /* ==================== 相对时间拆分 ==================== */

    public static class Rel {
        public final int years;
        public final int months;
        public final int weeks;
        public final int days;

        Rel(int years, int months, int weeks, int days) {
            this.years = years;
            this.months = months;
            this.weeks = weeks;
            this.days = days;
        }

        public boolean isZero() {
            return years == 0 && months == 0 && weeks == 0 && days == 0;
        }
    }

    /** 以 start 为起点、end 为终点，按 年/月/周/日 拆分（等价于原实现的 Period.between）。 */
    public static Rel relative(long start, long end) {
        if (end <= start) {
            return new Rel(0, 0, 0, 0);
        }
        int years = 0;
        while (years < 500 && addYears(start, years + 1) <= end) {
            years++;
        }
        long cursor = addYears(start, years);
        int months = 0;
        while (months < 600 && addMonths(cursor, months + 1) <= end) {
            months++;
        }
        cursor = addMonths(cursor, months);
        long left = dayDiff(cursor, end);
        int weeks = (int) (left / 7);
        int days = (int) (left % 7);
        return new Rel(years, months, weeks, days);
    }

    /** 拼接成“1年2月3周4天”，全零时返回 fallback。 */
    public static String formatRelative(Rel r, String fallback) {
        if (r == null || r.isZero()) {
            return fallback;
        }
        StringBuilder sb = new StringBuilder();
        if (r.years > 0) {
            sb.append(r.years).append("年");
        }
        if (r.months > 0) {
            sb.append(r.months).append("月");
        }
        if (r.weeks > 0) {
            sb.append(r.weeks).append("周");
        }
        if (r.days > 0) {
            sb.append(r.days).append("天");
        }
        return sb.toString();
    }

    /* ==================== 时区 ==================== */
    private static TimeZone appTz = TimeZone.getDefault();

    /** 设置应用时区；空值回退到系统默认时区。 */
    public static void setTimeZone(String id) {
        if (id == null || id.trim().isEmpty()) {
            appTz = TimeZone.getDefault();
            return;
        }
        appTz = TimeZone.getTimeZone(id.trim());
    }

    public static TimeZone timeZone() {
        return appTz;
    }

    /** 按应用时区格式化「yyyy-MM-dd HH:mm」。 */
    public static String formatDateTime(long millis) {
        java.text.SimpleDateFormat f =
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
        f.setTimeZone(appTz);
        return f.format(new java.util.Date(millis));
    }

    /* ==================== 文本 ==================== */

    public static String formatDate(long millis) {
        int[] a = ymd(millis);
        return String.format(Locale.CHINA, "%d年%d月%d日", a[0], a[1], a[2]);
    }

    public static String formatDateShort(long millis) {
        int[] a = ymd(millis);
        return String.format(Locale.CHINA, "%04d-%02d-%02d", a[0], a[1], a[2]);
    }

    /** 视觉宽度：ASCII 记 0.5，其余记 1.0，用于判断标题与天数是否碰撞。 */
    public static double visualWidth(String s) {
        if (s == null) {
            return 0.0;
        }
        double w = 0.0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            w += (ch >= 0 && ch <= 127) ? 0.5 : 1.0;
        }
        return w;
    }

    /** 折叠连续空白并去首尾。 */
    public static String pretty(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("[\\s\\u00A0]+", " ").trim();
    }

    /* ==================== 纪念日 ==================== */

    /** 每 N 天计一次，整除时返回次数，否则返回 0。 */
    public static int customCount(long days, int interval) {
        if (interval <= 0 || days <= 0) {
            return 0;
        }
        if (days % interval != 0) {
            return 0;
        }
        return (int) (days / interval);
    }

    public static class Cal {
        public final String label;
        public final int value;

        Cal(String label, int value) {
            this.label = label;
            this.value = value;
        }
    }

    /**
     * 按 年/月/周 精确命中生成纪念日计数列表。
     * 年、月要求起点加上 n 个单位后正好等于今天；周按同星期几取整周。
     */
    public static List<Cal> calendar(long start, long todayMillis,
                                    boolean showYear, boolean showMonth, boolean showWeek) {
        List<Cal> list = new ArrayList<>();
        if (todayMillis <= start) {
            return list;
        }
        if (showYear) {
            int n = 0;
            while (n < 500 && addYears(start, n) < todayMillis) {
                n++;
            }
            if (addYears(start, n) == todayMillis) {
                list.add(new Cal("年", n));
            }
        }
        if (showMonth) {
            int n = 0;
            while (n < 1200 && addMonths(start, n) < todayMillis) {
                n++;
            }
            if (addMonths(start, n) == todayMillis) {
                list.add(new Cal("月", n));
            }
        }
        if (showWeek) {
            long days = dayDiff(start, todayMillis);
            if (days > 0 && days % 7 == 0 && dayOfWeek(start) == dayOfWeek(todayMillis)) {
                list.add(new Cal("周", (int) (days / 7)));
            }
        }
        return list;
    }

    /** 今天是否正好命中设定的纪念日（每 N 天 / 第 N 年·月·周）。 */
    public static boolean isAnnivHit(Countdown c, long todayMillis) {
        if (c == null || c.annivType == Countdown.ANNIV_NONE) {
            return false;
        }
        long days = dayDiff(c.date, todayMillis);
        if (days <= 0) {
            return false;
        }
        if (c.annivType == Countdown.ANNIV_CUSTOM_DAYS) {
            return customCount(days, c.customDays) > 0;
        }
        if (c.annivType == Countdown.ANNIV_CALENDAR) {
            return !calendar(c.date, todayMillis, c.showYear, c.showMonth, c.showWeek).isEmpty();
        }
        return false;
    }

    /** 组装纪念日文案；非累计态或未启用时返回 null。 */
    public static String anniversaryText(Countdown c, long todayMillis) {
        if (c == null || c.annivType == Countdown.ANNIV_NONE) {
            return null;
        }
        long days = dayDiff(c.date, todayMillis);
        if (days <= 0) {
            return null;
        }
        // 自定义文案只在真正命中纪念日那天出现，其余日子不该占着卡片位置
        if (c.annivMessage != null && !c.annivMessage.trim().isEmpty()
                && isAnnivHit(c, todayMillis)) {
            return c.annivMessage.trim();
        }
        if (c.annivType == Countdown.ANNIV_CUSTOM_DAYS) {
            int n = customCount(days, c.customDays);
            return n > 0 ? ("第 " + n + " 次") : null;
        }
        if (c.annivType == Countdown.ANNIV_CALENDAR) {
            List<Cal> list = calendar(c.date, todayMillis, c.showYear, c.showMonth, c.showWeek);
            if (list.isEmpty()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (Cal item : list) {
                if (sb.length() > 0) {
                    sb.append(" · ");
                }
                sb.append("第 ").append(item.value).append(" ").append(item.label);
            }
            return sb.toString();
        }
        return null;
    }

    /* ==================== 重复推进 ==================== */

    /** 目标日已过去时按重复规则推进到下一个未到期的日期。返回是否被修改。 */
    public static boolean advance(Countdown c, long todayMillis) {
        if (c == null || c.date == 0L) {
            return false;
        }
        if (dayDiff(c.date, todayMillis) >= 0) {
            return false;
        }
        if (c.repeatType == Countdown.REPEAT_NONE) {
            return false;
        }
        long anchor = c.repeatAnchorDate != 0L ? c.repeatAnchorDate : c.date;
        long next = anchor;
        int guard = 0;
        while (dayDiff(next, todayMillis) > 0 && guard++ < 5000) {
            if (c.repeatType == Countdown.REPEAT_YEARLY) {
                next = addYears(next, 1);
            } else if (c.repeatType == Countdown.REPEAT_MONTHLY) {
                next = addMonths(next, 1);
            } else if (c.repeatType == Countdown.REPEAT_WEEKLY) {
                next = addDays(next, 7);
            } else {
                break;
            }
        }
        if (next == anchor) {
            return false;
        }
        c.repeatAnchorDate = anchor;
        c.date = next;
        return true;
    }
}
