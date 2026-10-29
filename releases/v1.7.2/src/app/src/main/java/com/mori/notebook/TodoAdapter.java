package com.mori.notebook;

import android.graphics.Paint;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 待办列表：分组头 + 条目 */
public class TodoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public interface Listener {
        void onToggle(Todo todo, boolean done);

        void onClick(Todo todo);
    }

    private static class Row {
        String header;
        Todo todo;

        Row(String header) {
            this.header = header;
        }

        Row(Todo todo) {
            this.todo = todo;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private final Listener listener;
    private final SimpleDateFormat dayFmt = new SimpleDateFormat("M月d日 HH:mm", Locale.CHINA);

    public TodoAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setData(List<Todo> active, List<Todo> done) {
        rows.clear();
        if (!active.isEmpty()) {
            rows.add(new Row("进行中 · " + active.size()));
            for (Todo t : active) rows.add(new Row(t));
        }
        if (!done.isEmpty()) {
            rows.add(new Row("已完成 · " + done.size()));
            for (Todo t : done) rows.add(new Row(t));
        }
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).todo == null ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inf.inflate(R.layout.item_todo_header, parent, false));
        }
        return new ItemHolder(inf.inflate(R.layout.item_todo, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).text.setText(row.header);
            return;
        }
        Todo todo = row.todo;
        ItemHolder h = (ItemHolder) holder;
        String title = TextUtils.isEmpty(todo.title) ? "无标题" : todo.title;
        h.title.setText(title);
        h.title.setPaintFlags(todo.done
                ? h.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                : h.title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        h.title.setAlpha(todo.done ? 0.55f : 1f);
        if (TextUtils.isEmpty(todo.note)) {
            h.note.setVisibility(View.GONE);
        } else {
            h.note.setVisibility(View.VISIBLE);
            h.note.setText(todo.note.replace('\n', ' '));
            h.note.setAlpha(todo.done ? 0.5f : 1f);
        }
        if (todo.done && todo.doneAt > 0) {
            h.meta.setVisibility(View.VISIBLE);
            h.meta.setText("完成于 " + dayFmt.format(new Date(todo.doneAt)));
        } else {
            h.meta.setVisibility(View.GONE);
        }
        h.check.setOnCheckedChangeListener(null);
        h.check.setChecked(todo.done);
        h.check.setOnCheckedChangeListener((b, checked) -> listener.onToggle(todo, checked));
        h.itemView.setOnClickListener(v -> listener.onClick(todo));
        h.itemView.setAlpha(todo.done ? 0.75f : 1f);
    }

    public Todo todoAt(int position) {
        if (position < 0 || position >= rows.size()) return null;
        return rows.get(position).todo;
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView text;

        HeaderHolder(View v) {
            super(v);
            text = v.findViewById(R.id.tv_header);
        }
    }

    static class ItemHolder extends RecyclerView.ViewHolder {
        final MaterialCheckBox check;
        final TextView title;
        final TextView note;
        final TextView meta;

        ItemHolder(View v) {
            super(v);
            check = v.findViewById(R.id.cb_done);
            title = v.findViewById(R.id.tv_title);
            note = v.findViewById(R.id.tv_note);
            meta = v.findViewById(R.id.tv_meta);
        }
    }
}