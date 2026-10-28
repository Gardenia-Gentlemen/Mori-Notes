package com.mori.notebook;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.VH> {
    public interface Listener {
        void onClick(Note note);
        void onLongClick(Note note);
    }

    private final List<Note> data = new ArrayList<>();
    private final Listener listener;
    private final SimpleDateFormat fmt = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
    private boolean grid;

    public NoteAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setData(List<Note> notes) {
        data.clear();
        data.addAll(notes);
        notifyDataSetChanged();
    }

    public void setGrid(boolean grid) {
        if (this.grid == grid) return;
        this.grid = grid;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = grid ? R.layout.item_note_grid : R.layout.item_note;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Note n = data.get(position);
        String title = n.title == null ? "" : n.title.trim();
        h.title.setText(title.isEmpty() ? "无标题" : title);
        float sc = UiPrefs.getHomeTitleScale(h.itemView.getContext());
        h.title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, (grid ? 15f : 16f) * sc);
        h.title.setTypeface(null, UiPrefs.isHomeTitleBold(h.itemView.getContext())
                ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        if (n.type == 1) {
            h.type.setText("思维");
            h.preview.setText(mindPreview(n.content));
        } else {
            h.type.setText("笔记");
            h.preview.setText(markdownPreview(n.content));
        }
        h.meta.setText(fmt.format(new Date(n.updatedAt)));
        h.itemView.setOnClickListener(v -> listener.onClick(n));
        h.itemView.setOnLongClickListener(v -> {
            listener.onLongClick(n);
            return true;
        });
    }

    private String plainPreview(String text) {
        if (text == null) return "";
        String s = text.replaceAll("\\s+", " ").trim();
        return s.isEmpty() ? "空白笔记" : s;
    }

    private String markdownPreview(String md) {
        if (md == null) return "";
        String s = md.replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)", "[图片]")
                .replaceAll("(?m)^#{1,6}\\s*", "")
                .replaceAll("[`*_~>#]", "")
                .replaceAll("(?m)^-\\s+\\[.\\]\\s*", "☐ ")
                .replaceAll("\\s+", " ")
                .trim();
        return s.isEmpty() ? "空白笔记" : s;
    }

    private String mindPreview(String json) {
        try {
            org.json.JSONArray arr = new org.json.JSONArray(json == null || json.isEmpty() ? "[]" : json);
            List<String> texts = new ArrayList<>();
            collect(arr, texts);
            if (texts.isEmpty()) return "空思维笔记";
            StringBuilder sb = new StringBuilder();
            for (String t : texts) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(t);
                if (sb.length() > 60) break;
            }
            return sb.toString();
        } catch (Exception e) {
            return "空思维笔记";
        }
    }

    private void collect(org.json.JSONArray arr, List<String> out) {
        for (int i = 0; i < arr.length(); i++) {
            org.json.JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            out.add(o.optString("text", ""));
            collect(o.optJSONArray("children") == null ? new org.json.JSONArray() : o.optJSONArray("children"), out);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, preview, meta, type;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_title);
            preview = itemView.findViewById(R.id.tv_preview);
            meta = itemView.findViewById(R.id.tv_meta);
            type = itemView.findViewById(R.id.tv_type);
        }
    }
}
