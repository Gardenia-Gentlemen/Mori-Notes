package com.mori.notebook;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Note {
    public String id;
    public String title;
    public String content;
    public int type; // 0 = markdown, 1 = mind
    public long updatedAt;
    /** 创建时间：老数据缺失时回退为 updatedAt */
    public long createdAt;
    /** 0 = 正常；>0 = 放入回收站的时间戳 */
    public long deletedAt;
    /** 隐私笔记：不在主列表显示，只能从主页下拉进入 */
    public boolean isPrivate;
    /** 0 = 未置顶；>0 = 置顶时间戳 */
    public long pinnedAt;
    /** 收藏 */
    public boolean starred;
    /** 标签（去重、去空白） */
    public List<String> tags = new ArrayList<>();

    public Note() {
        id = UUID.randomUUID().toString();
        title = "";
        content = "";
        type = 0;
        updatedAt = System.currentTimeMillis();
        createdAt = updatedAt;
        deletedAt = 0L;
    }

    public Note(String id, String title, String content, int type, long updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.type = type;
        this.updatedAt = updatedAt;
        this.createdAt = updatedAt;
        this.deletedAt = 0L;
    }

    public boolean inTrash() {
        return deletedAt > 0L;
    }

    public boolean isPinned() {
        return pinnedAt > 0L;
    }

    /** 标签串：用于列表角标展示，最多 3 个 */
    public String tagLabel(int max) {
        if (tags == null || tags.isEmpty() || max <= 0) return "";
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (String t : tags) {
            if (t == null || t.trim().isEmpty()) continue;
            if (n > 0) sb.append(' ');
            sb.append('#').append(t.trim());
            n++;
            if (n >= max) break;
        }
        return sb.toString();
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("title", title == null ? "" : title);
        o.put("content", content == null ? "" : content);
        o.put("type", type);
        o.put("updatedAt", updatedAt);
        o.put("createdAt", createdAt > 0 ? createdAt : updatedAt);
        o.put("deletedAt", deletedAt);
        o.put("isPrivate", isPrivate);
        o.put("pinnedAt", pinnedAt);
        o.put("starred", starred);
        JSONArray arr = new JSONArray();
        if (tags != null) {
            for (String t : tags) {
                if (t != null && !t.trim().isEmpty()) arr.put(t.trim());
            }
        }
        o.put("tags", arr);
        return o;
    }

    public static Note fromJson(JSONObject o) {
        Note n = new Note(
                o.optString("id", UUID.randomUUID().toString()),
                o.optString("title", ""),
                o.optString("content", ""),
                o.optInt("type", 0),
                o.optLong("updatedAt", System.currentTimeMillis()));
        n.createdAt = o.optLong("createdAt", n.updatedAt);
        n.deletedAt = o.optLong("deletedAt", 0L);
        n.isPrivate = o.optBoolean("isPrivate", false);
        n.pinnedAt = o.optLong("pinnedAt", 0L);
        n.starred = o.optBoolean("starred", false);
        JSONArray arr = o.optJSONArray("tags");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                String t = arr.optString(i, "").trim();
                if (!t.isEmpty() && !n.tags.contains(t)) n.tags.add(t);
            }
        }
        return n;
    }
}
