package com.mori.notebook;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 导出 / 分享时携带的元数据：标签、收藏、置顶与时间。 */
public final class NoteMeta {

    private NoteMeta() {
    }

    private static final String FMT = "yyyy-MM-dd HH:mm";

    private static String time(long v) {
        if (v <= 0) return "";
        return new SimpleDateFormat(FMT, Locale.getDefault()).format(new Date(v));
    }

    /** 是否存在需要携带的元数据 */
    public static boolean hasMeta(Note n) {
        if (n == null) return false;
        if (n.starred || n.isPinned()) return true;
        return n.tags != null && !n.tags.isEmpty();
    }

    private static String tagLine(Note n) {
        if (n == null || n.tags == null || n.tags.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String t : n.tags) {
            if (t == null || t.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(t.trim());
        }
        return sb.toString();
    }

    /** Markdown frontmatter；无元数据时返回空串 */
    public static String frontmatter(Note n) {
        if (!hasMeta(n)) return "";
        StringBuilder sb = new StringBuilder("---\n");
        String tags = tagLine(n);
        if (!tags.isEmpty()) sb.append("tags: [").append(tags).append("]\n");
        if (n.starred) sb.append("starred: true\n");
        if (n.isPinned()) sb.append("pinned: true\n");
        String c = time(n.createdAt > 0 ? n.createdAt : n.updatedAt);
        if (!c.isEmpty()) sb.append("created: ").append(c).append("\n");
        String u = time(n.updatedAt);
        if (!u.isEmpty()) sb.append("updated: ").append(u).append("\n");
        sb.append("---");
        return sb.toString();
    }

    /** 纯文本单行元信息；无元数据时返回空串 */
    public static String metaLine(Note n) {
        if (!hasMeta(n)) return "";
        StringBuilder sb = new StringBuilder();
        String tags = tagLine(n);
        if (!tags.isEmpty()) {
            sb.append("标签：");
            String[] parts = tags.split(", ");
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) sb.append(' ');
                sb.append('#').append(parts[i]);
            }
        }
        if (n.starred) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append("已收藏");
        }
        if (n.isPinned()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append("已置顶");
        }
        return sb.toString();
    }
}
