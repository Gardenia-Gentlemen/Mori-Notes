/*
 * 倒数日存储。
 *
 * 参考自 TimeTrace (https://github.com/KIPPUDESU/KIPPU_Trace)
 * Copyright (c) 2026 切符
 * 依据 MIT License 使用，许可全文见项目根目录 LICENSE-KIPPU_Trace。
 */
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
 * 倒数日存储：独立 countdowns.json，跟随「设置」里选择的存储目录。
 */
public class CountdownStore {

    private static CountdownStore instance;

    private final Context app;
    private final File defaultFile;
    private final List<Countdown> items = new ArrayList<>();

    private CountdownStore(Context context) {
        app = context;
        defaultFile = new File(context.getFilesDir(), StoreFile.NAME_COUNTDOWN);
        load();
    }

    public static synchronized CountdownStore get(Context context) {
        if (instance == null) {
            instance = new CountdownStore(context.getApplicationContext());
        }
        return instance;
    }

    private Uri tree() {
        return UiPrefs.getTreeUri(app);
    }

    public void reload() {
        load();
    }

    private void load() {
        items.clear();
        Uri tree = tree();
        String text = null;
        try {
            if (tree != null) {
                text = StoreFile.read(app, tree, StoreFile.NAME_COUNTDOWN);
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
        boolean changed = false;
        long today = CountdownTime.today();
        try {
            JSONArray arr = new JSONArray(text);
            for (int i = 0; i < arr.length(); i++) {
                Countdown c = Countdown.fromJson(arr.getJSONObject(i));
                if (CountdownTime.advance(c, today)) {
                    changed = true;
                }
                items.add(c);
            }
        } catch (Exception ignored) {
        }
        boolean dirty = changed;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).sortIndex < 0) {
                items.get(i).sortIndex = i;
                dirty = true;
            }
        }
        if (dirty) save();
        purgeExpired();
    }
    public void save() {
        JSONArray arr = new JSONArray();
        for (Countdown c : items) arr.put(c.toJson());
        String data = arr.toString();
        try {
            Uri tree = tree();
            if (tree != null) {
                StoreFile.write(app, tree, StoreFile.NAME_COUNTDOWN, data);
            } else {
                try (FileOutputStream out = new FileOutputStream(defaultFile)) {
                    out.write(data.getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (Exception ignored) {
        }
    }

    /** 排序：置顶优先；其后按手动序号；序号相同再按日期规则。 */
    public List<Countdown> getAll() {
        long today = CountdownTime.today();
        List<Countdown> copy = new ArrayList<>();
        for (Countdown c : items) {
            if (!c.inTrash()) copy.add(c);
        }
        Collections.sort(copy, (a, b) -> {
            if (a.pinned != b.pinned) return a.pinned ? -1 : 1;
            if (a.sortIndex != b.sortIndex) return Integer.compare(a.sortIndex, b.sortIndex);
            boolean pa = CountdownTime.dayDiff(a.date, today) < 0;
            boolean pb = CountdownTime.dayDiff(b.date, today) < 0;
            if (pa != pb) return pa ? 1 : -1;
            if (pa) return Long.compare(b.date, a.date);
            if (a.date != b.date) return Long.compare(a.date, b.date);
            return Long.compare(a.createdAt, b.createdAt);
        });
        return copy;
    }

    public int size() {
        return items.size();
    }

    public Countdown find(String id) {
        for (Countdown c : items) if (c.id.equals(id)) return c;
        return null;
    }

    public void put(Countdown c) {
        if (c == null) return;
        Countdown existing = find(c.id);
        if (existing != null) {
            existing.title = c.title;
            existing.note = c.note;
            existing.date = c.date;
            existing.mode = c.mode;
            existing.pinned = c.pinned;
            existing.annivType = c.annivType;
            existing.customDays = c.customDays;
            existing.annivMessage = c.annivMessage;
            existing.showYear = c.showYear;
            existing.showMonth = c.showMonth;
            existing.showWeek = c.showWeek;
            existing.repeatType = c.repeatType;
            existing.repeatAnchorDate = c.repeatAnchorDate;
            existing.bgUri = c.bgUri;
            existing.sortIndex = c.sortIndex;
        } else {
            if (c.sortIndex < 0) c.sortIndex = maxIndex() + 1;
            items.add(c);
        }
        save();
    }
    private int maxIndex() {
        int max = -1;
        for (Countdown c : items) if (c.sortIndex > max) max = c.sortIndex;
        return max;
    }

    /** 按给定顺序写回手动序号并保存。 */
    public void saveOrder(List<Countdown> ordered) {
        if (ordered == null) return;
        int i = 0;
        for (Countdown c : ordered) {
            if (c == null) continue;
            Countdown e = find(c.id);
            if (e != null) e.sortIndex = i;
            i++;
        }
        save();
    }

    public void setPinned(String id, boolean pinned) {
        Countdown c = find(id);
        if (c == null) return;
        c.pinned = pinned;
        save();
    }

    /** 软删除：移入回收站。 */
    public void delete(String id) {
        Countdown c = find(id);
        if (c != null && !c.inTrash()) c.deletedAt = System.currentTimeMillis();
        save();
    }

    /** 批量软删除：移入回收站。 */
    public int deleteBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        long now = System.currentTimeMillis();
        int n = 0;
        for (Countdown c : items) {
            if (c.inTrash()) continue;
            if (ids.contains(c.id)) {
                c.deletedAt = now;
                n++;
            }
        }
        save();
        return n;
    }

    // ================= 回收站（与笔记回收站完全独立） =================

    /** 回收站保留天数。 */
    public static final long TRASH_KEEP_MS = 10L * 24 * 60 * 60 * 1000;

    public Countdown findIncludingTrash(String id) {
        return find(id);
    }

    /** 回收站列表，按删除时间倒序。 */
    public List<Countdown> getTrash() {
        List<Countdown> copy = new ArrayList<>();
        for (Countdown c : items) {
            if (c.inTrash()) copy.add(c);
        }
        Collections.sort(copy, (a, b) -> Long.compare(b.deletedAt, a.deletedAt));
        return copy;
    }

    public long trashRemainMs(Countdown c) {
        if (c == null || c.deletedAt <= 0L) return 0L;
        long left = c.deletedAt + TRASH_KEEP_MS - System.currentTimeMillis();
        return Math.max(0L, left);
    }

    /** 清理过期的回收站条目。 */
    public void purgeExpired() {
        long now = System.currentTimeMillis();
        if (items.removeIf(c -> c.inTrash() && now - c.deletedAt >= TRASH_KEEP_MS)) save();
    }

    public void restore(String id) {
        Countdown c = find(id);
        if (c == null || !c.inTrash()) return;
        c.deletedAt = 0L;
        save();
    }

    public void deleteForever(String id) {
        if (items.removeIf(c -> c.id.equals(id))) save();
    }

    public int clearTrash() {
        int before = items.size();
        items.removeIf(Countdown::inTrash);
        int removed = before - items.size();
        if (removed > 0) save();
        return removed;
    }
}
