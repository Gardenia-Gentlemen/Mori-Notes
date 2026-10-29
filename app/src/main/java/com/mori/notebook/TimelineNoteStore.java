package com.mori.notebook;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** 时间轴手记存储：filesDir/timeline_notes.json。 */
public class TimelineNoteStore {

    private static final String FILE_NAME = "timeline_notes.json";

    private final File file;
    private final List<TimelineNote> items = new ArrayList<>();

    private TimelineNoteStore(Context context) {
        file = new File(context.getApplicationContext().getFilesDir(), FILE_NAME);
        load();
    }

    public static TimelineNoteStore get(Context context) {
        return new TimelineNoteStore(context);
    }

    private void load() {
        items.clear();
        if (!file.exists()) return;
        try (FileInputStream in = new FileInputStream(file)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            String text = out.toString(StandardCharsets.UTF_8.name());
            JSONArray arr = new JSONArray(text);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) items.add(TimelineNote.fromJson(o));
            }
        } catch (Exception ignored) {
        }
    }

    private void save() {
        try {
            JSONArray arr = new JSONArray();
            for (TimelineNote n : items) arr.put(n.toJson());
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(arr.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
        }
    }

    /** 取某条倒数日下的全部手记，按时间升序。 */
    public List<TimelineNote> list(String countdownId) {
        List<TimelineNote> res = new ArrayList<>();
        for (TimelineNote n : items) {
            if (countdownId != null && countdownId.equals(n.countdownId)) res.add(n);
        }
        res.sort((a, b) -> Long.compare(a.timeMs, b.timeMs));
        return res;
    }

    public void add(TimelineNote n) {
        items.add(n);
        save();
    }
}
