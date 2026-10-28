package com.mori.notebook;

/* loaded from: classes.dex */
public class MindAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<com.mori.notebook.MindAdapter.VH> {
    private final com.mori.notebook.MindAdapter.Listener listener;
    private final java.util.List<com.mori.notebook.MindNode> roots = new java.util.ArrayList();
    private final java.util.Set<java.lang.String> expanded = new java.util.HashSet();
    private final java.util.List<com.mori.notebook.MindAdapter.Row> rows = new java.util.ArrayList();

    public interface Listener {
        void onClick(com.mori.notebook.MindNode mindNode);

        void onLongClick(com.mori.notebook.MindNode mindNode);

        void onToggle(com.mori.notebook.MindNode mindNode);
    }

    public MindAdapter(com.mori.notebook.MindAdapter.Listener listener) {
        this.listener = listener;
    }

    public void setData(java.util.List<com.mori.notebook.MindNode> list) {
        this.roots.clear();
        this.roots.addAll(list);
        rebuild();
    }

    public void rebuild() {
        this.rows.clear();
        java.util.Iterator<com.mori.notebook.MindNode> it = this.roots.iterator();
        while (it.hasNext()) {
            flatten(it.next(), 0);
        }
        notifyDataSetChanged();
    }

    private void flatten(com.mori.notebook.MindNode mindNode, int i) {
        this.rows.add(new com.mori.notebook.MindAdapter.Row(mindNode, i));
        if (this.expanded.contains(mindNode.id)) {
            java.util.Iterator<com.mori.notebook.MindNode> it = mindNode.children.iterator();
            while (it.hasNext()) {
                flatten(it.next(), i + 1);
            }
        }
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public com.mori.notebook.MindAdapter.VH onCreateViewHolder(android.view.ViewGroup viewGroup, int i) {
        return new com.mori.notebook.MindAdapter.VH(android.view.LayoutInflater.from(viewGroup.getContext()).inflate(com.mori.notebook.R.layout.item_mind, viewGroup, false));
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onBindViewHolder(com.mori.notebook.MindAdapter.VH vh, int i) {
        com.mori.notebook.MindAdapter.Row row = this.rows.get(i);
        final com.mori.notebook.MindNode mindNode = row.node;
        int i2 = (int) vh.itemView.getResources().getDisplayMetrics().density;
        int i3 = i2 * 12;
        vh.row.setPadding((row.depth * 22 * i2) + i3, vh.row.getPaddingTop(), i3, vh.row.getPaddingBottom());
        vh.text.setText((mindNode.text == null || mindNode.text.trim().isEmpty()) ? "（空节点）" : mindNode.text);
        if (mindNode.children.isEmpty()) {
            vh.toggle.setText("•");
        } else {
            vh.toggle.setText(this.expanded.contains(mindNode.id) ? "▾" : "▸");
        }
        vh.count.setText(mindNode.children.isEmpty() ? "" : java.lang.String.valueOf(mindNode.children.size()));
        vh.toggle.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.mori.notebook.MindAdapter$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.mori.notebook.MindAdapter.this.lambda$onBindViewHolder$0(mindNode, view);
            }
        });
        vh.itemView.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.mori.notebook.MindAdapter$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.mori.notebook.MindAdapter.this.lambda$onBindViewHolder$1(mindNode, view);
            }
        });
        vh.itemView.setOnLongClickListener(new android.view.View.OnLongClickListener() { // from class: com.mori.notebook.MindAdapter$$ExternalSyntheticLambda2
            @Override // android.view.View.OnLongClickListener
            public final boolean onLongClick(android.view.View view) {
                boolean lambda$onBindViewHolder$2;
                lambda$onBindViewHolder$2 = com.mori.notebook.MindAdapter.this.lambda$onBindViewHolder$2(mindNode, view);
                return lambda$onBindViewHolder$2;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onBindViewHolder$0(com.mori.notebook.MindNode mindNode, android.view.View view) {
        if (mindNode.children.isEmpty()) {
            return;
        }
        if (this.expanded.contains(mindNode.id)) {
            this.expanded.remove(mindNode.id);
        } else {
            this.expanded.add(mindNode.id);
        }
        rebuild();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onBindViewHolder$1(com.mori.notebook.MindNode mindNode, android.view.View view) {
        this.listener.onClick(mindNode);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$onBindViewHolder$2(com.mori.notebook.MindNode mindNode, android.view.View view) {
        this.listener.onLongClick(mindNode);
        return true;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.rows.size();
    }

    private static class Row {
        final int depth;
        final com.mori.notebook.MindNode node;

        Row(com.mori.notebook.MindNode mindNode, int i) {
            this.node = mindNode;
            this.depth = i;
        }
    }

    static class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        android.widget.TextView count;
        android.view.View row;
        android.widget.TextView text;
        android.widget.TextView toggle;

        VH(android.view.View view) {
            super(view);
            this.toggle = (android.widget.TextView) view.findViewById(com.mori.notebook.R.id.tv_toggle);
            this.text = (android.widget.TextView) view.findViewById(com.mori.notebook.R.id.tv_text);
            this.count = (android.widget.TextView) view.findViewById(com.mori.notebook.R.id.tv_count);
            this.row = view.findViewById(com.mori.notebook.R.id.row);
        }
    }
}
