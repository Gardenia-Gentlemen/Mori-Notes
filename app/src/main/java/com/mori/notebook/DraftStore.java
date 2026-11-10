package com.mori.notebook;

import android.content.Context;
import android.content.SharedPreferences;

/** 编辑器草稿：独立 SP 文件，与正式数据完全分离 */
public class DraftStore {
    private static final String P = "editor_drafts";
    private static final String SEP = "\u0002";

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(P, Context.MODE_PRIVATE);
    }

    private static String key(String id) {
        return "draft_" + (id == null ? "new" : id);
    }

    /** 保存草稿（标题 + 正文） */
    public static void save(Context c, String id, String title, String content) {
        String t = title == null ? "" : title;
        String b = content == null ? "" : content;
        if (t.trim().isEmpty() && b.trim().isEmpty()) {
            clear(c, id);
            return;
        }
        sp(c).edit().putString(key(id), t + SEP + b).apply();
    }

    public static boolean has(Context c, String id) {
        return sp(c).contains(key(id));
    }

    /** 返回 [title, content]；无草稿返回 null */
    public static String[] get(Context c, String id) {
        String raw = sp(c).getString(key(id), null);
        if (raw == null) return null;
        int i = raw.indexOf(SEP);
        if (i < 0) return new String[]{"", raw};
        return new String[]{raw.substring(0, i), raw.substring(i + 1)};
    }

    public static void clear(Context c, String id) {
        sp(c).edit().remove(key(id)).apply();
    }
}