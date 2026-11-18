package com.mori.notebook;

import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class NoteStore {
    private static NoteStore instance;

    private final Context app;
    private final File defaultFile;
    private Uri tree; // null = 应用内部存储
    private final List<Note> notes = new ArrayList<>();

    private NoteStore(Context context) {
        app = context;
        defaultFile = new File(context.getFilesDir(), StoreFile.NAME);
        tree = UiPrefs.getTreeUri(context);
        load();
    }

    public static synchronized NoteStore get(Context context) {
        if (instance == null) {
            instance = new NoteStore(context.getApplicationContext());
        }
        return instance;
    }

    public Uri getTree() {
        return tree;
    }

    public String locationLabel() {
        return StoreFile.describe(tree);
    }

    /** 重读当前存储位置 */
    public void reload() {
        load();
    }

    /** 切换存储目录；返回 true 表示新目录里已有笔记并已加载 */
    public boolean switchLocation(Uri newTree) {
        tree = newTree;
        UiPrefs.setTreeUri(app, newTree);
        String existing = null;
        if (tree != null) {
            try {
                existing = StoreFile.read(app, tree);
            } catch (Exception ignored) {
            }
        }
        if (existing != null && !existing.trim().isEmpty()) {
            load();
            return true;
        }
        save();
        return false;
    }

    public void useInternal() {
        tree = null;
        UiPrefs.setTreeUri(app, null);
        try {
            if (defaultFile.exists()) {
                load();
            } else {
                save();
            }
        } catch (Exception ignored) {
            save();
        }
    }

    /** 把内存中的笔记写入指定目录（用于先删后选的迁移） */
    public void exportTo(Uri target) {
        try {
            StoreFile.write(app, target, serialize());
        } catch (Exception ignored) {
        }
    }

    private void load() {
        notes.clear();
        String text = null;
        try {
            if (tree != null) {
                text = StoreFile.read(app, tree);
            } else if (defaultFile.exists()) {
                try (InputStream in = new java.io.FileInputStream(defaultFile)) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) > 0) bos.write(buf, 0, len);
                    text = new String(bos.toByteArray(), StandardCharsets.UTF_8);
                }
            }
        } catch (Exception ignored) {
        }
        if (text == null || text.trim().isEmpty()) return;
        try {
            JSONArray arr = new JSONArray(text);
            for (int i = 0; i < arr.length(); i++) {
                notes.add(Note.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception ignored) {
        }
        purgeExpired();
        pruneDrafts();
    }

    /** 清理已不存在笔记的草稿（删除笔记后不残留） */
    private void pruneDrafts() {
        Set<String> alive = new java.util.HashSet<>();
        for (Note n : notes) alive.add(n.id);
        DraftStore.prune(app, alive);
    }

    private String serialize() {
        JSONArray arr = new JSONArray();
        for (Note n : notes) {
            try {
                arr.put(n.toJson());
            } catch (Exception ignored) {
            }
        }
        return arr.toString();
    }

    public void save() {
        String data = serialize();
        try {
            if (tree != null) {
                StoreFile.write(app, tree, data);
            } else {
                try (FileOutputStream out = new FileOutputStream(defaultFile)) {
                    out.write(data.getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (Exception ignored) {
        }
        Widgets.updateAll(app);
    }

    // ================= 排序 =================

    private static String titleKey(Note n) {
        String t = n.title == null ? "" : n.title.trim();
        return t.isEmpty() ? "无标题" : t;
    }

    /**
     * 统一排序：置顶优先（组内按置顶时间倒序），其后按偏好排序。
     * mode 0=更新时间 1=创建时间 2=标题
     */
    public static Comparator<Note> buildComparator(final int mode) {
        return (a, b) -> {
            boolean pa = a.isPinned();
            boolean pb = b.isPinned();
            if (pa != pb) return pa ? -1 : 1;
            if (pa && pb) {
                int c = Long.compare(b.pinnedAt, a.pinnedAt);
                if (c != 0) return c;
            }
            switch (mode) {
                case 1:
                    return Long.compare(
                            b.createdAt > 0 ? b.createdAt : b.updatedAt,
                            a.createdAt > 0 ? a.createdAt : a.updatedAt);
                case 2:
                    return titleKey(a).compareToIgnoreCase(titleKey(b));
                default:
                    return Long.compare(b.updatedAt, a.updatedAt);
            }
        };
    }

    /** 按当前偏好排序（置顶优先） */
    public void sortList(List<Note> list) {
        if (list == null) return;
        Collections.sort(list, buildComparator(UiPrefs.getSortMode(app)));
    }

    /**
     * 全文搜索：标题、正文或标签包含关键词（忽略大小写），排除回收站与隐私笔记。
     * 关键词以 # 开头时只匹配标签。
     */
    public List<Note> search(String q) {
        List<Note> out = new ArrayList<>();
        String raw = q == null ? "" : q.trim();
        boolean tagOnly = raw.startsWith("#");
        String key = (tagOnly ? raw.substring(1) : raw).trim().toLowerCase();
        if (key.isEmpty()) return getNotes();
        for (Note n : notes) {
            if (n.inTrash() || n.isPrivate) continue;
            if (matchesTag(n, key)) {
                out.add(n);
                continue;
            }
            if (tagOnly) continue;
            String title = n.title == null ? "" : n.title.toLowerCase();
            String body = n.content == null ? "" : n.content.toLowerCase();
            if (title.contains(key) || body.contains(key)) out.add(n);
        }
        sortList(out);
        return out;
    }

    /** 标签是否包含关键词（子串匹配，忽略大小写） */
    private static boolean matchesTag(Note n, String key) {
        if (n.tags == null) return false;
        for (String t : n.tags) {
            if (t != null && t.toLowerCase().contains(key)) return true;
        }
        return false;
    }

    public List<Note> getNotes() {
        List<Note> copy = new ArrayList<>();
        for (Note n : notes) {
            if (!n.inTrash() && !n.isPrivate) copy.add(n);
        }
        sortList(copy);
        return copy;
    }

    /** 隐私笔记：按当前偏好排序 */
    public List<Note> getPrivateNotes() {
        List<Note> copy = new ArrayList<>();
        for (Note n : notes) {
            if (!n.inTrash() && n.isPrivate) copy.add(n);
        }
        sortList(copy);
        return copy;
    }

    /** 设为 / 移出隐私笔记 */
    public void setPrivate(String id, boolean value) {
        Note n = find(id);
        if (n == null) return;
        n.isPrivate = value;
        n.updatedAt = System.currentTimeMillis();
        save();
    }

    /** 回收站：按删除时间倒序 */
    public List<Note> getTrash() {
        List<Note> copy = new ArrayList<>();
        for (Note n : notes) {
            if (n.inTrash()) copy.add(n);
        }
        Collections.sort(copy, (a, b) -> Long.compare(b.deletedAt, a.deletedAt));
        return copy;
    }

    // ================= 置顶 / 收藏 / 标签 =================

    public void setPinned(String id, boolean pinned) {
        Note n = find(id);
        if (n == null) return;
        n.pinnedAt = pinned ? System.currentTimeMillis() : 0L;
        save();
    }

    public void setStarred(String id, boolean starred) {
        Note n = find(id);
        if (n == null) return;
        n.starred = starred;
        save();
    }

    /** 规范化单个标签：去 #、去空白 */
    public static String normalizeTag(String raw) {
        if (raw == null) return "";
        String v = raw.trim().replace("#", "").trim();
        return v;
    }

    public void setTags(String id, List<String> tags) {
        Note n = find(id);
        if (n == null) return;
        n.tags.clear();
        if (tags != null) {
            for (String t : tags) {
                String v = normalizeTag(t);
                if (!v.isEmpty() && !n.tags.contains(v)) n.tags.add(v);
            }
        }
        save();
    }

    /** 全部标签（不含回收站），按名称排序 */
    public List<String> allTags() {
        Set<String> set = new LinkedHashSet<>();
        for (Note n : notes) {
            if (n.inTrash()) continue;
            if (n.tags == null) continue;
            for (String t : n.tags) {
                String v = normalizeTag(t);
                if (!v.isEmpty()) set.add(v);
            }
        }
        List<String> out = new ArrayList<>(set);
        Collections.sort(out, String::compareToIgnoreCase);
        return out;
    }

    /** 标签 -> 条数（不含回收站） */
    public java.util.Map<String, Integer> tagCounts() {
        java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
        for (String t : allTags()) map.put(t, 0);
        for (Note n : notes) {
            if (n.inTrash() || n.tags == null) continue;
            for (String t : n.tags) {
                String v = normalizeTag(t);
                if (v.isEmpty() || !map.containsKey(v)) continue;
                map.put(v, map.get(v) + 1);
            }
        }
        return map;
    }

    /** 重命名标签：全库替换（同名已存在时自动去重），返回受影响条数 */
    public int renameTag(String from, String to) {
        String a = normalizeTag(from);
        String b = normalizeTag(to);
        if (a.isEmpty() || b.isEmpty() || a.equals(b)) return 0;
        int changed = 0;
        for (Note n : notes) {
            if (n.tags == null || !n.tags.contains(a)) continue;
            List<String> out = new ArrayList<>();
            for (String t : n.tags) {
                String v = a.equals(t) ? b : t;
                if (!out.contains(v)) out.add(v);
            }
            n.tags.clear();
            n.tags.addAll(out);
            changed++;
        }
        if (changed > 0) save();
        return changed;
    }

    /** 删除标签：从所有笔记中移除，返回受影响条数 */
    public int removeTag(String name) {
        String a = normalizeTag(name);
        if (a.isEmpty()) return 0;
        int changed = 0;
        for (Note n : notes) {
            if (n.tags == null || !n.tags.remove(a)) continue;
            changed++;
        }
        if (changed > 0) save();
        return changed;
    }

    public static final long TRASH_KEEP_MS = 10L * 24 * 60 * 60 * 1000;

    /** 放入回收站 */
    public void moveToTrash(String id) {
        Note n = find(id);
        if (n == null) return;
        n.deletedAt = System.currentTimeMillis();
        DraftStore.clear(app, id);
        save();
    }

    /** 从回收站恢复 */
    public void restore(String id) {
        Note n = find(id);
        if (n == null) return;
        n.deletedAt = 0L;
        n.updatedAt = System.currentTimeMillis();
        save();
    }

    /** 回收站剩余保留毫秒数 */
    public long trashRemainMs(Note n) {
        long left = n.deletedAt + TRASH_KEEP_MS - System.currentTimeMillis();
        return Math.max(left, 0L);
    }

    /** 清空超过 10 天的回收站内容 */
    public void purgeExpired() {
        long now = System.currentTimeMillis();
        List<String> gone = new ArrayList<>();
        for (Note n : notes) {
            if (n.inTrash() && now - n.deletedAt >= TRASH_KEEP_MS) gone.add(n.id);
        }
        boolean changed = notes.removeIf(n -> n.inTrash() && now - n.deletedAt >= TRASH_KEEP_MS);
        if (changed) {
            for (String id : gone) DraftStore.clear(app, id);
            save();
        }
    }

    /** 清空回收站，返回彻底删除的条数 */
    public int clearTrash() {
        List<String> gone = new ArrayList<>();
        for (Note n : notes) {
            if (n.inTrash()) gone.add(n.id);
        }
        int before = notes.size();
        notes.removeIf(n -> n.inTrash());
        int removed = before - notes.size();
        if (removed > 0) {
            for (String id : gone) DraftStore.clear(app, id);
            save();
        }
        return removed;
    }

    public void deleteForever(String id) {
        notes.removeIf(n -> n.id.equals(id));
        DraftStore.clear(app, id);
        save();
    }


    public Note find(String id) {
        for (Note n : notes) {
            if (n.id.equals(id)) return n;
        }
        return null;
    }

    public void put(Note note) {
        Note existing = find(note.id);
        if (existing != null) {
            existing.title = note.title;
            existing.content = note.content;
            existing.type = note.type;
            existing.updatedAt = note.updatedAt;
            existing.isPrivate = note.isPrivate;
            if (note.createdAt > 0) existing.createdAt = note.createdAt;
            existing.pinnedAt = note.pinnedAt;
            existing.starred = note.starred;
            existing.tags = new ArrayList<>(note.tags == null ? new ArrayList<>() : note.tags);
        } else {
            notes.add(note);
        }
        save();
    }

    public void delete(String id) {
        notes.removeIf(n -> n.id.equals(id));
        save();
    }

    /** 按导出范围取笔记（不含回收站） */
    public List<Note> notesForScope(String scope) {
        List<Note> copy = new ArrayList<>();
        for (Note n : notes) {
            if (n.inTrash()) continue;
            if (MoriFile.SCOPE_PRIVATE.equals(scope)) {
                if (n.isPrivate) copy.add(n);
            } else if (MoriFile.SCOPE_NORMAL.equals(scope)) {
                if (!n.isPrivate) copy.add(n);
            } else {
                copy.add(n);
            }
        }
        Collections.sort(copy, (a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        return copy;
    }

    /** 批量设为 / 移出隐私笔记 */
    public int setPrivateBatch(List<String> ids, boolean value) {
        if (ids == null || ids.isEmpty()) return 0;
        long now = System.currentTimeMillis();
        int changed = 0;
        for (String id : ids) {
            Note n = find(id);
            if (n == null) continue;
            n.isPrivate = value;
            n.updatedAt = now;
            changed++;
        }
        if (changed > 0) save();
        return changed;
    }

    /** 批量移入回收站 */
    public int moveToTrashBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        long now = System.currentTimeMillis();
        int changed = 0;
        for (String id : ids) {
            Note n = find(id);
            if (n == null || n.inTrash()) continue;
            n.deletedAt = now;
            changed++;
        }
        if (changed > 0) {
            for (String id : ids) DraftStore.clear(app, id);
            save();
        }
        return changed;
    }

    /** 批量导入（.mori）；id 冲突时换新 id，保留 isPrivate 归属，不进入回收站 */
    public int importNotes(List<Note> incoming) {
        return importNotes(incoming, false);
    }

    /**
     * 导入笔记。
     * merge=false（追加）：全部作为新条目加入，id 冲突时换新 id，本机原内容不受影响。
     * merge=true（合并）：同 id 且不在回收站时以文件内容覆盖；其余按新增处理。
     */
    public int importNotes(List<Note> incoming, boolean merge) {
        if (incoming == null || incoming.isEmpty()) return 0;
        int count = 0;
        for (Note n : incoming) {
            if (n == null) continue;
            boolean hasId = n.id != null && !n.id.trim().isEmpty();
            Note existing = hasId ? find(n.id) : null;
            if (merge && existing != null && !existing.inTrash()) {
                existing.title = n.title;
                existing.content = n.content;
                existing.type = n.type;
                existing.updatedAt = n.updatedAt;
                existing.isPrivate = n.isPrivate;
                existing.deletedAt = 0L;
                if (n.createdAt > 0) existing.createdAt = n.createdAt;
                existing.pinnedAt = n.pinnedAt;
                existing.starred = n.starred;
                existing.tags = new ArrayList<>(n.tags == null ? new ArrayList<>() : n.tags);
            } else {
                if (!hasId || existing != null) n.id = java.util.UUID.randomUUID().toString();
                n.deletedAt = 0L;
                notes.add(n);
            }
            count++;
        }
        if (count > 0) save();
        return count;
    }
}
