/*
 * 字体库列表适配器：显示字体名（用该字体本身预览）与文件信息。
 * 单击预览、长按进入多选；多选态左侧显示勾选框并隐藏删除按钮。
 */
package com.mori.notebook;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class FontAdapter extends RecyclerView.Adapter<FontAdapter.Holder> {

    public interface Listener {
        void onDelete(File font);

        void onPreview(File font);

        void onSelectionChanged(int count);
    }

    private final List<File> data = new ArrayList<>();
    private final LinkedHashSet<String> selected = new LinkedHashSet<>();
    private final Listener listener;
    private boolean selectMode = false;

    public FontAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<File> fonts) {
        data.clear();
        if (fonts != null) data.addAll(fonts);
        LinkedHashSet<String> alive = new LinkedHashSet<>();
        for (File f : data) alive.add(f.getAbsolutePath());
        selected.retainAll(alive);
        if (data.isEmpty() && selectMode) {
            selectMode = false;
            notifySelection();
        }
        notifyDataSetChanged();
    }

    public boolean isSelectMode() {
        return selectMode;
    }

    public int selectedCount() {
        return selected.size();
    }

    public List<File> selectedFiles() {
        List<File> out = new ArrayList<>();
        for (File f : data) {
            if (selected.contains(f.getAbsolutePath())) out.add(f);
        }
        return out;
    }

    public void setSelectMode(boolean on) {
        if (selectMode == on) return;
        selectMode = on;
        if (!on) selected.clear();
        notifyDataSetChanged();
        notifySelection();
    }

    public void toggleSelectAll() {
        if (data.isEmpty()) return;
        if (selected.size() == data.size()) {
            selected.clear();
        } else {
            selected.clear();
            for (File f : data) selected.add(f.getAbsolutePath());
        }
        notifyDataSetChanged();
        notifySelection();
    }

    public void toggle(File f) {
        String key = f.getAbsolutePath();
        if (!selected.remove(key)) selected.add(key);
        notifyDataSetChanged();
        notifySelection();
    }

    private void notifySelection() {
        if (listener != null) listener.onSelectionChanged(selected.size());
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_font, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        final File f = data.get(position);

        h.name.setText(FontImporter.displayName(f));
        // 用字体本身渲染名称，方便一眼看出效果
        h.name.setTypeface(Fonts.regular(h.itemView.getContext(), f.getAbsolutePath()));
        h.meta.setText(meta(f));

        final boolean checked = selected.contains(f.getAbsolutePath());
        h.check.setVisibility(selectMode ? View.VISIBLE : View.GONE);
        h.check.setChecked(checked);
        h.del.setVisibility(selectMode ? View.GONE : View.VISIBLE);

        h.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectMode) {
                    toggle(f);
                } else if (listener != null) {
                    listener.onPreview(f);
                }
            }
        });
        h.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (selectMode) {
                    toggle(f);
                } else {
                    selectMode = true;
                    selected.add(f.getAbsolutePath());
                    notifyDataSetChanged();
                    notifySelection();
                }
                return true;
            }
        });

        h.del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onDelete(f);
            }
        });
    }

    private String meta(File f) {
        String ext = f.getName().toLowerCase(Locale.ROOT);
        int dot = ext.lastIndexOf('.');
        if (dot > 0) ext = ext.substring(dot + 1);
        long kb = Math.max(1, f.length() / 1024);
        return ext.toUpperCase(Locale.ROOT) + " · " + kb + " KB";
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView meta;
        final ImageButton del;
        final CheckBox check;

        Holder(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.tv_font_name);
            meta = v.findViewById(R.id.tv_font_meta);
            del = v.findViewById(R.id.btn_font_delete);
            check = v.findViewById(R.id.cb_font_select);
        }
    }
}
