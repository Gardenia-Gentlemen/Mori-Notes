package com.mori.notebook;

import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.color.MaterialColors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 待办列表：分组头 + 条目（优先级色点 / 截止时间 / 清单 / 子任务进度） */
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
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.CHINA);

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
        final Todo todo = row.todo;
        ItemHolder h = (ItemHolder) holder;
        String title = TextUtils.isEmpty(todo.title) ? "无标题" : todo.title;
        h.title.setText(title);
        h.title.setPaintFlags(todo.done
                ? h.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                : h.title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        h.title.setAlpha(todo.done ? 0.55f : 1f);

        // 优先级色点
        if (todo.done) {
            h.dot.setVisibility(View.GONE);
        } else {
            h.dot.setVisibility(View.VISIBLE);
            int color;
            if (todo.priority == Todo.PRIORITY_HIGH) {
                color = MaterialColors.getColor(h.itemView, androidx.appcompat.R.attr.colorError);
            } else if (todo.priority == Todo.PRIORITY_LOW) {
                color = MaterialColors.getColor(h.itemView, com.google.android.material.R.attr.colorOutline);
            } else {
                color = MaterialColors.getColor(h.itemView, androidx.appcompat.R.attr.colorPrimary);
            }
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(color);
            h.dot.setBackground(g);
        }

        if (TextUtils.isEmpty(todo.note)) {
            h.note.setVisibility(View.GONE);
        } else {
            h.note.setVisibility(View.VISIBLE);
            h.note.setText(todo.note.replace('\n', ' '));
            h.note.setAlpha(todo.done ? 0.5f : 1f);
        }

        // 截止时间；逾期标红
        if (todo.hasDue()) {
            h.due.setVisibility(View.VISIBLE);
            Date d = new Date(todo.dueAt);
            if (todo.isOverdue()) {
                h.due.setText("已逾期 " + dayFmt.format(d));
                h.due.setTextColor(MaterialColors.getColor(h.itemView,
                        androidx.appcompat.R.attr.colorError));
            } else if (todo.isDueToday()) {
                h.due.setText("今天 " + timeFmt.format(d));
                h.due.setTextColor(MaterialColors.getColor(h.itemView,
                        androidx.appcompat.R.attr.colorPrimary));
            } else {
                h.due.setText(dayFmt.format(d));
                h.due.setTextColor(MaterialColors.getColor(h.itemView,
                        com.google.android.material.R.attr.colorOnSurfaceVariant));
            }
        } else {
            h.due.setVisibility(View.GONE);
        }

        // 子任务进度 / 完成时间
        if (todo.done && todo.doneAt > 0) {
            h.meta.setVisibility(View.VISIBLE);
            h.meta.setText("完成于 " + dayFmt.format(new Date(todo.doneAt)));
        } else if (todo.subtaskCount() > 0) {
            h.meta.setVisibility(View.VISIBLE);
            h.meta.setText("子任务 " + todo.subtaskDone() + "/" + todo.subtaskCount());
        } else {
            h.meta.setVisibility(View.GONE);
        }

        // 所属清单
        if (!TextUtils.isEmpty(todo.listId)) {
            h.list.setVisibility(View.VISIBLE);
            h.list.setText(todo.listId);
        } else {
            h.list.setVisibility(View.GONE);
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
        final TextView due;
        final TextView list;
        final View dot;

        ItemHolder(View v) {
            super(v);
            check = v.findViewById(R.id.cb_done);
            title = v.findViewById(R.id.tv_title);
            note = v.findViewById(R.id.tv_note);
            meta = v.findViewById(R.id.tv_meta);
            due = v.findViewById(R.id.tv_due);
            list = v.findViewById(R.id.tv_list);
            dot = v.findViewById(R.id.dot_priority);
        }
    }
}
