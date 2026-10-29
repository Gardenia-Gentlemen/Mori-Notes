package com.mori.notebook;

import org.json.JSONObject;

import java.util.UUID;

/** 待办条目：标题 + 备注 + 完成状态 */
public class Todo {
    public String id;
    public String title;
    public String note;
    public boolean done;
    public long createdAt;
    /** 完成时间；未完成为 0 */
    public long doneAt;

    public Todo() {
        id = UUID.randomUUID().toString();
        title = "";
        note = "";
        done = false;
        createdAt = System.currentTimeMillis();
        doneAt = 0L;
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("title", title == null ? "" : title);
            o.put("note", note == null ? "" : note);
            o.put("done", done);
            o.put("createdAt", createdAt);
            o.put("doneAt", doneAt);
        } catch (Exception ignored) {
        }
        return o;
    }

    public static Todo fromJson(JSONObject o) {
        Todo t = new Todo();
        t.id = o.optString("id", UUID.randomUUID().toString());
        t.title = o.optString("title", "");
        t.note = o.optString("note", "");
        t.done = o.optBoolean("done", false);
        t.createdAt = o.optLong("createdAt", System.currentTimeMillis());
        t.doneAt = o.optLong("doneAt", 0L);
        return t;
    }
}
