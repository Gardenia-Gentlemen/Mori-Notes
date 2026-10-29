package com.mori.notebook;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MindAdapter extends RecyclerView.Adapter<MindAdapter.VH> {

    public interface Listener {
        void onClick(MindNode node);
        void onLongClick(MindNode node);
        void onToggle(MindNode node);
    }

    private final List<MindNode> roots = new ArrayList<>();
    private final Set<String> expanded = new HashSet<>();
    private final List<Row> rows = new ArrayList<>();
    private final Listener listener;

    public MindAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setData(List<MindNode> newRoots) {
        roots.clear();
        roots.addAll(newRoots);
        rebuild();
    }

    public void rebuild() {
        rows.clear();
        for (MindNode n : roots) flatten(n, 0);
        notifyDataSetChanged();
    }

    private void flatten(MindNode n, int depth) {
        rows.add(new Row(n, depth));
        if (expanded.contains(n.id)) {
            for (MindNode c : n.children) flatten(c, depth + 1);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mind, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Row row = rows.get(position);
        MindNode node = row.node;
        int dp = (int) h.itemView.getResources().getDisplayMetrics().density;
        h.row.setPadding(12 * dp + row.depth * 22 * dp, h.row.getPaddingTop(), 12 * dp, h.row.getPaddingBottom());
        h.text.setText(node.text == null || node.text.trim().isEmpty() ? "（空节点）" : node.text);
        if (node.children.isEmpty()) {
            h.toggle.setText("•");
        } else {
            h.toggle.setText(expanded.contains(node.id) ? "▾" : "▸");
        }
        h.count.setText(node.children.isEmpty() ? "" : String.valueOf(node.children.size()));
        h.toggle.setOnClickListener(v -> {
            if (!node.children.isEmpty()) {
                if (expanded.contains(node.id)) expanded.remove(node.id);
                else expanded.add(node.id);
                rebuild();
            }
        });
        h.itemView.setOnClickListener(v -> listener.onClick(node));
        h.itemView.setOnLongClickListener(v -> {
            listener.onLongClick(node);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private static class Row {
        final MindNode node;
        final int depth;

        Row(MindNode node, int depth) {
            this.node = node;
            this.depth = depth;
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView toggle, text, count;
        View row;

        VH(@NonNull View itemView) {
            super(itemView);
            toggle = itemView.findViewById(R.id.tv_toggle);
            text = itemView.findViewById(R.id.tv_text);
            count = itemView.findViewById(R.id.tv_count);
            row = itemView.findViewById(R.id.row);
        }
    }
}