package com.mori.notebook;

import org.json.JSONObject;

/** 时间轴手记：文字、地点、记录时间与记录时区。 */
public class TimelineNote {
    public String id = "";
    /** 所属倒数日 id。 */
    public String countdownId = "";
    public String text = "";
    public String place = "";
    /** 记录时间，本机当前毫秒时间戳（强制，不可手改）。 */
    public long timeMs = 0L;
    /** 记录时的时区 id，例如 Asia/Shanghai。 */
    public String timeZone = "";

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("countdownId", countdownId);
            o.put("text", text);
            o.put("place", place);
            o.put("timeMs", timeMs);
            o.put("timeZone", timeZone);
        } catch (Exception ignored) {
        }
        return o;
    }

    public static TimelineNote fromJson(JSONObject o) {
        TimelineNote n = new TimelineNote();
        n.id = o.optString("id", "");
        n.countdownId = o.optString("countdownId", "");
        n.text = o.optString("text", "");
        n.place = o.optString("place", "");
        n.timeMs = o.optLong("timeMs", 0L);
        n.timeZone = o.optString("timeZone", "");
        return n;
    }
}
