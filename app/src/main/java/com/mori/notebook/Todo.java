package com.mori.notebook;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 待办条目。
 * 标题 + 备注 + 完成状态 + 截止时间 + 优先级 + 所属清单 + 重复规则 + 子任务 + 排序权重/更新时间。
 */
public class Todo {
    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_NORMAL = 1;
    public static final int PRIORITY_HIGH = 2;

    public static final int REPEAT_NONE = 0;
    public static final int REPEAT_DAILY = 1;
    public static final int REPEAT_WEEKLY = 2;
    public static final int REPEAT_MONTHLY = 3;

    /** 子任务：全部勾完才算完成 */
    public static class SubTask {
        public String title = "";
        public boolean done = false;

        public SubTask() {
        }

        public SubTask(String title, boolean done) {
            this.title = title == null ? "" : title;
            this.done = done;
        }

        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("title", title == null ? "" : title);
                o.put("done", done);
            } catch (Exception ignored) {
            }
            return o;
        }

        public static SubTask fromJson(JSONObject o) {
            SubTask s = new SubTask();
            s.title = o.optString("title", "");
            s.done = o.optBoolean("done", false);
            return s;
        }
    }

    public String id;
    public String title;
    public String note;
    public boolean done;
    public long createdAt;
    /** 完成时间；未完成为 0 */
    public long doneAt;
    /** 截止时间（含时刻）；0 表示没有截止 */
    public long dueAt;
    /** 优先级：0 低 / 1 普通 / 2 高 */
    public int priority = PRIORITY_NORMAL;
    /** 所属清单（分类），空串表示默认清单 */
    public String listId = "";
    /** 重复规则 */
    public int repeatRule = REPEAT_NONE;
    /** 排序权重，越大越靠前 */
    public int sortWeight = 0;
    /** 最后更新时间 */
    public long updatedAt;
    /** 子任务 */
    public List<SubTask> subtasks = new ArrayList<>();

    public Todo() {
        id = UUID.randomUUID().toString();
        title = "";
        note = "";
        done = false;
        createdAt = System.currentTimeMillis();
        doneAt = 0L;
        dueAt = 0L;
        priority = PRIORITY_NORMAL;
        listId = "";
        repeatRule = REPEAT_NONE;
        sortWeight = 0;
        updatedAt = createdAt;
    }

    public boolean hasDue() {
        return dueAt > 0;
    }

    /** 已逾期：有截止、未完成、已过期 */
    public boolean isOverdue() {
        return !done && dueAt > 0 && dueAt < System.currentTimeMillis();
    }

    /** 截止是否在今天之内（含逾期） */
    public boolean isDueToday() {
        if (dueAt <= 0) return false;
        java.util.Calendar a = java.util.Calendar.getInstance();
        java.util.Calendar b = java.util.Calendar.getInstance();
        a.setTimeInMillis(dueAt);
        b.setTimeInMillis(System.currentTimeMillis());
        return a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR)
                && a.get(java.util.Calendar.DAY_OF_YEAR) == b.get(java.util.Calendar.DAY_OF_YEAR);
    }

    /** 是否在 7 天内到期 */
    public boolean isDueThisWeek() {
        if (dueAt <= 0) return false;
        long now = System.currentTimeMillis();
        return dueAt >= now && dueAt <= now + 7L * 24 * 3600 * 1000;
    }

    public int subtaskDone() {
        int n = 0;
        if (subtasks != null) for (SubTask s : subtasks) if (s.done) n++;
        return n;
    }

    public int subtaskCount() {
        return subtasks == null ? 0 : subtasks.size();
    }

    /** 子任务全部完成（且至少有一个） */
    public boolean subtaskAllDone() {
        return subtaskCount() > 0 && subtaskDone() == subtaskCount();
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
            o.put("dueAt", dueAt);
            o.put("priority", priority);
            o.put("listId", listId == null ? "" : listId);
            o.put("repeatRule", repeatRule);
            o.put("sortWeight", sortWeight);
            o.put("updatedAt", updatedAt);
            JSONArray arr = new JSONArray();
            if (subtasks != null) for (SubTask s : subtasks) arr.put(s.toJson());
            o.put("subtasks", arr);
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
        t.dueAt = o.optLong("dueAt", 0L);
        t.priority = o.optInt("priority", PRIORITY_NORMAL);
        t.listId = o.optString("listId", "");
        t.repeatRule = o.optInt("repeatRule", REPEAT_NONE);
        t.sortWeight = o.optInt("sortWeight", 0);
        t.updatedAt = o.optLong("updatedAt", t.createdAt);
        t.subtasks = new ArrayList<>();
        JSONArray arr = o.optJSONArray("subtasks");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject so = arr.optJSONObject(i);
                if (so != null) t.subtasks.add(SubTask.fromJson(so));
            }
        }
        return t;
    }
}
