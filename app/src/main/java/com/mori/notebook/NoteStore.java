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
import java.util.List;

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
    }
    public List<Note> getNotes() {
        List<Note> copy = new ArrayList<>();
        for (Note n : notes) {
            if (!n.inTrash() && !n.isPrivate) copy.add(n);
        }
        Collections.sort(copy, (a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        return copy;
    }

    /** 隐私笔记：按更新时间倒序 */
    public List<Note> getPrivateNotes() {
        List<Note> copy = new ArrayList<>();
        for (Note n : notes) {
            if (!n.inTrash() && n.isPrivate) copy.add(n);
        }
        Collections.sort(copy, (a, b) -> Long.compare(b.updatedAt, a.updatedAt));
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

    public static final long TRASH_KEEP_MS = 10L * 24 * 60 * 60 * 1000;

    /** 放入回收站 */
    public void moveToTrash(String id) {
        Note n = find(id);
        if (n == null) return;
        n.deletedAt = System.currentTimeMillis();
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
        boolean changed = notes.removeIf(n -> n.inTrash() && now - n.deletedAt >= TRASH_KEEP_MS);
        if (changed) save();
    }

    /** 清空回收站，返回彻底删除的条数 */
    public int clearTrash() {
        int before = notes.size();
        notes.removeIf(n -> n.inTrash());
        int removed = before - notes.size();
        if (removed > 0) save();
        return removed;
    }

    public void deleteForever(String id) {
        notes.removeIf(n -> n.id.equals(id));
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
        if (changed > 0) save();
        return changed;
    }

    /** 批量导入（.mori）；id 冲突时换新 id，保留 isPrivate 归属，不进入回收站 */
    public int importNotes(List<Note> incoming) {
        if (incoming == null || incoming.isEmpty()) return 0;
        int added = 0;
        for (Note n : incoming) {
            if (n == null) continue;
            if (n.id == null || n.id.trim().isEmpty() || find(n.id) != null) {
                n.id = java.util.UUID.randomUUID().toString();
            }
            n.deletedAt = 0L;
            notes.add(n);
            added++;
        }
        if (added > 0) save();
        return added;
    }
}
