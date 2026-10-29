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
import java.util.Calendar;
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
    /**
     * 未完成：先按是否逾期/有截止排（有截止的在前，截止近的在前），
     * 再按优先级高→低，再手动权重，最后新建时间倒序。
     */
    public List<Todo> getActive() {
        List<Todo> copy = new ArrayList<>();
        for (Todo t : todos) if (!t.done) copy.add(t);
        Collections.sort(copy, (a, b) -> {
            boolean da = a.dueAt > 0;
            boolean db = b.dueAt > 0;
            if (da != db) return da ? -1 : 1;
            if (da) {
                int c = Long.compare(a.dueAt, b.dueAt);
                if (c != 0) return c;
            }
            if (a.priority != b.priority) return b.priority - a.priority;
            if (a.sortWeight != b.sortWeight) return b.sortWeight - a.sortWeight;
            return Long.compare(b.createdAt, a.createdAt);
        });
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
    /** 已有的清单名（去重、非空） */
    public List<String> listNames() {
        List<String> out = new ArrayList<>();
        for (Todo t : todos) {
            String s = t.listId == null ? "" : t.listId.trim();
            if (!s.isEmpty() && !out.contains(s)) out.add(s);
        }
        return out;
    }
    public Todo find(String id) {
        for (Todo t : todos) if (t.id.equals(id)) return t;
        return null;
    }
    /** 新增或更新（更新时同步全部字段） */
    public void put(Todo todo) {
        if (todo == null) return;
        Todo existing = find(todo.id);
        if (existing != null) {
            existing.title = todo.title;
            existing.note = todo.note;
            existing.done = todo.done;
            existing.doneAt = todo.doneAt;
            existing.createdAt = todo.createdAt;
            existing.dueAt = todo.dueAt;
            existing.priority = todo.priority;
            existing.listId = todo.listId;
            existing.repeatRule = todo.repeatRule;
            existing.sortWeight = todo.sortWeight;
            existing.updatedAt = todo.updatedAt;
            existing.subtasks = new ArrayList<>();
            if (todo.subtasks != null) {
                for (Todo.SubTask s : todo.subtasks) {
                    existing.subtasks.add(new Todo.SubTask(s.title, s.done));
                }
            }
        } else {
            todos.add(todo);
        }
        save();
    }
    public void setDone(String id, boolean done) {
        Todo t = find(id);
        if (t == null) return;
        boolean was = t.done;
        t.done = done;
        t.doneAt = done ? System.currentTimeMillis() : 0L;
        t.updatedAt = System.currentTimeMillis();
        if (done && !was && t.repeatRule != Todo.REPEAT_NONE) spawnNext(t);
        save();
    }
    /** 重复待办：完成时生成下一条（截止时间顺延） */
    private void spawnNext(Todo src) {
        Todo n = new Todo();
        n.title = src.title;
        n.note = src.note;
        n.priority = src.priority;
        n.listId = src.listId;
        n.repeatRule = src.repeatRule;
        n.sortWeight = src.sortWeight;
        if (src.subtasks != null) {
            for (Todo.SubTask s : src.subtasks) n.subtasks.add(new Todo.SubTask(s.title, false));
        }
        long base = src.dueAt > 0 ? src.dueAt : System.currentTimeMillis();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(base);
        if (src.repeatRule == Todo.REPEAT_DAILY) {
            c.add(Calendar.DAY_OF_YEAR, 1);
        } else if (src.repeatRule == Todo.REPEAT_WEEKLY) {
            c.add(Calendar.DAY_OF_YEAR, 7);
        } else if (src.repeatRule == Todo.REPEAT_MONTHLY) {
            c.add(Calendar.MONTH, 1);
        }
        n.dueAt = c.getTimeInMillis();
        todos.add(n);
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
