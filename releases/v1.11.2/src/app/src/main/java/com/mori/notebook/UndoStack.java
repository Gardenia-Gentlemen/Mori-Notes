package com.mori.notebook;

import android.os.SystemClock;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 简单的文本撤销 / 重做栈。
 * 连续输入（间隔 ≤ 500ms）自动合并为一次快照，避免每敲一个字都生成一步。
 */
public class UndoStack {
    private static final long MERGE_MS = 500L;

    private final int limit;
    private final Deque<String> undo = new ArrayDeque<>();
    private final Deque<String> redo = new ArrayDeque<>();
    private long lastRecordAt = 0L;

    public UndoStack() {
        this(50);
    }

    public UndoStack(int limit) {
        this.limit = limit <= 0 ? 50 : limit;
    }

    /** 记录一次变更前的文本 */
    public void record(String previous) {
        if (previous == null) return;
        long now = SystemClock.uptimeMillis();
        if (lastRecordAt > 0L && now - lastRecordAt <= MERGE_MS) {
            lastRecordAt = now;
            return;
        }
        lastRecordAt = now;
        undo.push(previous);
        while (undo.size() > limit) undo.removeLast();
        redo.clear();
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }

    /** 返回撤销后的文本，无可用记录返回 null */
    public String undo(String current) {
        if (undo.isEmpty()) return null;
        redo.push(current == null ? "" : current);
        while (redo.size() > limit) redo.removeLast();
        lastRecordAt = 0L;
        return undo.pop();
    }

    /** 返回重做后的文本，无可用记录返回 null */
    public String redo(String current) {
        if (redo.isEmpty()) return null;
        undo.push(current == null ? "" : current);
        while (undo.size() > limit) undo.removeLast();
        lastRecordAt = 0L;
        return redo.pop();
    }

    public void clear() {
        undo.clear();
        redo.clear();
        lastRecordAt = 0L;
    }
}