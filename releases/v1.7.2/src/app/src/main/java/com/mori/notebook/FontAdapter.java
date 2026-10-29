/* 字体库列表适配器：显示字体名（用该字体本身预览）与文件信息，右侧删除。 */
package com.mori.notebook;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FontAdapter extends RecyclerView.Adapter<FontAdapter.Holder> {

    public interface Listener {
        void onDelete(File font);
    }

    private final List<File> data = new ArrayList<>();
    private final Listener listener;

    public FontAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<File> fonts) {
        data.clear();
        if (fonts != null) data.addAll(fonts);
        notifyDataSetChanged();
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
        h.del.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(f);
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

        Holder(View v) {
            super(v);
            name = v.findViewById(R.id.tv_font_name);
            meta = v.findViewById(R.id.tv_font_meta);
            del = v.findViewById(R.id.btn_font_delete);
        }
    }
}
