package com.mori.notebook;

/* loaded from: classes.dex */
public class NoteAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<com.mori.notebook.NoteAdapter.VH> {
    private final java.util.List<com.mori.notebook.Note> data = new java.util.ArrayList();
    private final java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault());
    private boolean grid;
    private final com.mori.notebook.NoteAdapter.Listener listener;

    public interface Listener {
        void onClick(com.mori.notebook.Note note);

        void onLongClick(com.mori.notebook.Note note);
    }

    public NoteAdapter(com.mori.notebook.NoteAdapter.Listener listener) {
        this.listener = listener;
    }

    public void setData(java.util.List<com.mori.notebook.Note> list) {
        this.data.clear();
        this.data.addAll(list);
        notifyDataSetChanged();
    }

    public void setGrid(boolean z) {
        if (this.grid == z) {
            return;
        }
        this.grid = z;
        notifyDataSetChanged();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public com.mori.notebook.NoteAdapter.VH onCreateViewHolder(android.view.ViewGroup viewGroup, int i) {
        return new com.mori.notebook.NoteAdapter.VH(android.view.LayoutInflater.from(viewGroup.getContext()).inflate(this.grid ? com.mori.notebook.R.layout.item_note_grid : com.mori.notebook.R.layout.item_note, viewGroup, false));
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onBindViewHolder(com.mori.notebook.NoteAdapter.VH vh, int i) {
        final com.mori.notebook.Note note = this.data.get(i);
        java.lang.String trim = note.title == null ? "" : note.title.trim();
        android.widget.TextView textView = vh.title;
        if (trim.isEmpty()) {
            trim = "无标题";
        }
        textView.setText(trim);
        vh.title.setTextSize(2, (this.grid ? 15.0f : 16.0f) * com.mori.notebook.UiPrefs.getHomeTitleScale(vh.itemView.getContext()));
        vh.title.setTypeface(null, com.mori.notebook.UiPrefs.isHomeTitleBold(vh.itemView.getContext()) ? 1 : 0);
        if (note.type == 1) {
            vh.type.setText("思维");
            vh.preview.setText(mindPreview(note.content));
        } else if (note.type == 2) {
            vh.type.setText("TXT");
            vh.preview.setText(markdownPreview(note.content));
        } else {
            vh.type.setText("MD");
            vh.preview.setText(markdownPreview(note.content));
        }
        vh.meta.setText(this.fmt.format(new java.util.Date(note.updatedAt)));
        vh.itemView.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.mori.notebook.NoteAdapter$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.mori.notebook.NoteAdapter.this.lambda$onBindViewHolder$0(note, view);
            }
        });
        vh.itemView.setOnLongClickListener(new android.view.View.OnLongClickListener() { // from class: com.mori.notebook.NoteAdapter$$ExternalSyntheticLambda1
            @Override // android.view.View.OnLongClickListener
            public final boolean onLongClick(android.view.View view) {
                boolean lambda$onBindViewHolder$1;
                lambda$onBindViewHolder$1 = com.mori.notebook.NoteAdapter.this.lambda$onBindViewHolder$1(note, view);
                return lambda$onBindViewHolder$1;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onBindViewHolder$0(com.mori.notebook.Note note, android.view.View view) {
        this.listener.onClick(note);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$onBindViewHolder$1(com.mori.notebook.Note note, android.view.View view) {
        this.listener.onLongClick(note);
        return true;
    }

    private java.lang.String plainPreview(java.lang.String str) {
        if (str == null) {
            return "";
        }
        java.lang.String trim = str.replaceAll("\\s+", " ").trim();
        return trim.isEmpty() ? "空白笔记" : trim;
    }

    private java.lang.String markdownPreview(java.lang.String str) {
        if (str == null) {
            return "";
        }
        java.lang.String trim = str.replaceAll("(?m)^#{1,6}\\s*", "").replaceAll("[`*_~>#]", "").replaceAll("(?m)^-\\s+\\[.\\]\\s*", "☐ ").replaceAll("\\s+", " ").trim();
        return trim.isEmpty() ? "空白笔记" : trim;
    }

    private java.lang.String mindPreview(java.lang.String str) {
        try {
            if (str == null || str.isEmpty()) {
                str = "[]";
            }
            org.json.JSONArray jSONArray = new org.json.JSONArray(str);
            java.util.ArrayList arrayList = new java.util.ArrayList();
            collect(jSONArray, arrayList);
            if (arrayList.isEmpty()) {
                return "空思维笔记";
            }
            java.lang.StringBuilder sb = new java.lang.StringBuilder();
            for (java.lang.String str2 : arrayList) {
                if (sb.length() > 0) {
                    sb.append(" · ");
                }
                sb.append(str2);
                if (sb.length() > 60) {
                    break;
                }
            }
            return sb.toString();
        } catch (java.lang.Exception unused) {
            return "空思维笔记";
        }
    }

    private void collect(org.json.JSONArray jSONArray, java.util.List<java.lang.String> list) {
        for (int i = 0; i < jSONArray.length(); i++) {
            org.json.JSONObject optJSONObject = jSONArray.optJSONObject(i);
            if (optJSONObject != null) {
                list.add(optJSONObject.optString("text", ""));
                collect(optJSONObject.optJSONArray("children") == null ? new org.json.JSONArray() : optJSONObject.optJSONArray("children"), list);
            }
        }
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.data.size();
    }

    static class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        android.widget.TextView meta;
        android.widget.TextView preview;
        android.widget.TextView title;
        android.widget.TextView type;

        VH(android.view.View view) {
            super(view);
            this.title = (android.widget.TextView) view.findViewById(com.mori.notebook.R.id.tv_title);
            this.preview = (android.widget.TextView) view.findViewById(com.mori.notebook.R.id.tv_preview);
            this.meta = (android.widget.TextView) view.findViewById(com.mori.notebook.R.id.tv_meta);
            this.type = (android.widget.TextView) view.findViewById(com.mori.notebook.R.id.tv_type);
        }
    }
}
