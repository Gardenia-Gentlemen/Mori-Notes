package com.mori.notebook;

import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 待办存储：独立 todos.json，跟随「设置」里选择的存储目录。
 * 与 notes.json 完全分离，互不影响，也不影响 .mori 导入导出。
 */
public class TodoStore {
    private static TodoStore instance;

    private final Context app;
    private final File defaultFile;
    private final List<Todo> todos = new ArrayList<>();

    private TodoStore(Context context) {
        app = context;
        defaultFile = new File(context.getFilesDir(), StoreFile.NAME_TODO);
        load();
    }

    public static synchronized TodoStore get(Context context) {
        if (instance == null) {
            instance = new TodoStore(context.getApplicationContext());
        }
        return instance;
    }

    /** 存储目录随设置实时变化，不缓存 */
    private Uri tree() {
        return UiPrefs.getTreeUri(app);
    }

    /** 重新读取磁盘内容（进入待办页时调用） */
    public void reload() {
        load();
    }

    private void load() {
        todos.clear();
        Uri tree = tree();
        String text = null;
        try {
            if (tree != null) {
                text = StoreFile.read(app, tree, StoreFile.NAME_TODO);
            } else if (defaultFile.exists()) {
                try (InputStream in = new FileInputStream(defaultFile)) {
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
                todos.add(Todo.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception ignored) {
        }
    }

    public void save() {
        JSONArray arr = new JSONArray();
        for (Todo t : todos) arr.put(t.toJson());
        String data = arr.toString();
        try {
            Uri tree = tree();
            if (tree != null) {
                StoreFile.write(app, tree, StoreFile.NAME_TODO, data);
            } else {
                try (FileOutputStream out = new FileOutputStream(defaultFile)) {
                    out.write(data.getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (Exception ignored) {
        }
    }

    /** 未完成：新建时间倒序 */
    public List<Todo> getActive() {
        List<Todo> copy = new ArrayList<>();
        for (Todo t : todos) if (!t.done) copy.add(t);
        Collections.sort(copy, (a, b) -> Long.compare(b.createdAt, a.createdAt));
        return copy;
    }

    /** 已完成：完成时间倒序 */
    public List<Todo> getDone() {
        List<Todo> copy = new ArrayList<>();
        for (Todo t : todos) if (t.done) copy.add(t);
        Collections.sort(copy, (a, b) -> Long.compare(b.doneAt, a.doneAt));
        return copy;
    }

    public int activeCount() {
        int n = 0;
        for (Todo t : todos) if (!t.done) n++;
        return n;
    }

    public Todo find(String id) {
        for (Todo t : todos) if (t.id.equals(id)) return t;
        return null;
    }

    /** 新增或更新 */
    public void put(Todo todo) {
        if (todo == null) return;
        Todo existing = find(todo.id);
        if (existing != null) {
            existing.title = todo.title;
            existing.note = todo.note;
            existing.done = todo.done;
            existing.doneAt = todo.doneAt;
        } else {
            todos.add(todo);
        }
        save();
    }

    public void setDone(String id, boolean done) {
        Todo t = find(id);
        if (t == null) return;
        t.done = done;
        t.doneAt = done ? System.currentTimeMillis() : 0L;
        save();
    }

    public void delete(String id) {
        todos.removeIf(t -> t.id.equals(id));
        save();
    }

    /** 清除所有已完成，返回条数 */
    public int clearDone() {
        int before = todos.size();
        todos.removeIf(t -> t.done);
        int removed = before - todos.size();
        if (removed > 0) save();
        return removed;
    }
}