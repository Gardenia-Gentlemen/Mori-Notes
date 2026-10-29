/*
 * 倒数日数据模型。
 *
 * 参考自 TimeTrace (https://github.com/KIPPUDESU/KIPPU_Trace)
 * Copyright (c) 2026 切符
 * 依据 MIT License 使用，许可全文见项目根目录 LICENSE-KIPPU_Trace。
 */
package com.mori.notebook;

import org.json.JSONObject;

import java.util.UUID;

public class Countdown {

    public static final String MODE_DOWN = "down";
    public static final String MODE_UP = "up";

    public static final int ANNIV_NONE = 0;
    public static final int ANNIV_CUSTOM_DAYS = 1;
    public static final int ANNIV_CALENDAR = 2;

    public static final int REPEAT_NONE = 0;
    public static final int REPEAT_YEARLY = 1;
    public static final int REPEAT_MONTHLY = 2;
    public static final int REPEAT_WEEKLY = 3;

    public String id = "";
    public String title = "";
    public String note = "";
    /** 目标日期，UTC 午夜毫秒（DatePicker 语义）。 */
    public long date = 0L;
    /** 显示模式，仅作记录，实际以日期与今天比较为准。 */
    public String mode = MODE_DOWN;
    public boolean pinned = false;

    public int annivType = ANNIV_NONE;
    public int customDays = 0;
    public String annivMessage = "";
    public boolean showYear = false;
    public boolean showMonth = false;
    public boolean showWeek = false;

    public int repeatType = REPEAT_NONE;
    public long repeatAnchorDate = 0L;

    public long createdAt = 0L;
    /** 该卡片自用背景图 uri，空则用全局设置。 */
    public String bgUri = "";
    /** 该卡片纯色背景（ARGB），0 表示未设置；设置后优先于背景图。 */
    public int bgColor = 0;
    /** 该卡片专属字体文件绝对路径，空则用全局默认字体。 */
    public String fontPath = "";
    /** 手动排序序号，越小越靠前；-1 表示未排序。 */
    public int sortIndex = -1;

    /** 移入回收站的时间戳，0 表示未删除。 */
    public long deletedAt = 0L;

    public boolean inTrash() {
        return deletedAt > 0L;
    }

    /** 累计模式创建后可补录旧回忆的时间窗口：24 小时。 */
    public static final long BACKFILL_WINDOW_MS = 24L * 3600L * 1000L;

    /** 是否仍处于「可设置/修改回忆时间」的窗口期（累计模式创建后 24 小时内）。 */
    public boolean canBackfillNoteTime() {
        if (!MODE_UP.equals(mode)) return false;
        if (createdAt <= 0L) return false;
        long used = System.currentTimeMillis() - createdAt;
        return used >= 0L && used < BACKFILL_WINDOW_MS;
    }

    public Countdown() {
    }

    public static Countdown create(String title, long date) {
        Countdown c = new Countdown();
        c.id = UUID.randomUUID().toString();
        c.title = title == null ? "" : title;
        c.date = date;
        c.createdAt = System.currentTimeMillis();
        return c;
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("title", title);
            o.put("note", note);
            o.put("date", date);
            o.put("mode", mode);
            o.put("pinned", pinned);
            o.put("annivType", annivType);
            o.put("customDays", customDays);
            o.put("annivMessage", annivMessage);
            o.put("showYear", showYear);
            o.put("showMonth", showMonth);
            o.put("showWeek", showWeek);
            o.put("repeatType", repeatType);
            o.put("repeatAnchorDate", repeatAnchorDate);
            o.put("createdAt", createdAt);
            o.put("bgUri", bgUri == null ? "" : bgUri);
            o.put("bgColor", bgColor);
            o.put("fontPath", fontPath == null ? "" : fontPath);
            o.put("sortIndex", sortIndex);
            o.put("deletedAt", deletedAt);
        } catch (Exception ignored) {
        }
        return o;
    }

    public static Countdown fromJson(JSONObject o) {
        Countdown c = new Countdown();
        c.id = o.optString("id", "");
        c.title = o.optString("title", "");
        c.note = o.optString("note", "");
        c.date = o.optLong("date", 0L);
        c.mode = MODE_UP.equals(o.optString("mode", MODE_DOWN)) ? MODE_UP : MODE_DOWN;
        c.pinned = o.optBoolean("pinned", false);
        c.annivType = o.optInt("annivType", ANNIV_NONE);
        c.customDays = o.optInt("customDays", 0);
        c.annivMessage = o.optString("annivMessage", "");
        c.showYear = o.optBoolean("showYear", false);
        c.showMonth = o.optBoolean("showMonth", false);
        c.showWeek = o.optBoolean("showWeek", false);
        c.repeatType = o.optInt("repeatType", REPEAT_NONE);
        c.repeatAnchorDate = o.optLong("repeatAnchorDate", 0L);
        c.createdAt = o.optLong("createdAt", 0L);
        c.bgUri = o.optString("bgUri", "");
        c.bgColor = o.optInt("bgColor", 0);
        c.fontPath = o.optString("fontPath", "");
        c.sortIndex = o.optInt("sortIndex", -1);
        c.deletedAt = o.optLong("deletedAt", 0L);
        if (c.id.isEmpty()) {
            c.id = UUID.randomUUID().toString();
        }
        return c;
    }
}
