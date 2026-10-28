package com.mori.notebook;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class Note {
    public String id;
    public String title;
    public String content;
    public int type; // 0 = markdown, 1 = mind
    public long updatedAt;
    /** 0 = 正常；>0 = 放入回收站的时间戳 */
    public long deletedAt;
    /** 隐私笔记：不在主列表显示，只能从主页下拉进入 */
    public boolean isPrivate;

    public Note() {
        id = UUID.randomUUID().toString();
        title = "";
        content = "";
        type = 0;
        updatedAt = System.currentTimeMillis();
        deletedAt = 0L;
    }

    public Note(String id, String title, String content, int type, long updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.type = type;
        this.updatedAt = updatedAt;
        this.deletedAt = 0L;
    }

    public boolean inTrash() {
        return deletedAt > 0L;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("title", title == null ? "" : title);
        o.put("content", content == null ? "" : content);
        o.put("type", type);
        o.put("updatedAt", updatedAt);
        o.put("deletedAt", deletedAt);
        o.put("isPrivate", isPrivate);
        return o;
    }

    public static Note fromJson(JSONObject o) {
        Note n = new Note(
                o.optString("id", UUID.randomUUID().toString()),
                o.optString("title", ""),
                o.optString("content", ""),
                o.optInt("type", 0),
                o.optLong("updatedAt", System.currentTimeMillis()));
        n.deletedAt = o.optLong("deletedAt", 0L);
        n.isPrivate = o.optBoolean("isPrivate", false);
        return n;
    }
}