package com.mori.notebook;

/* loaded from: classes.dex */
public class MainActivity extends com.mori.notebook.BaseActivity {
    private com.mori.notebook.NoteAdapter adapter;
    private android.widget.TextView emptyView;
    private androidx.recyclerview.widget.RecyclerView recycler;
    private com.mori.notebook.NoteStore store;

    @Override // com.mori.notebook.BaseActivity, androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        setContentView(com.mori.notebook.R.layout.activity_main);
        this.store = com.mori.notebook.NoteStore.get(this);
        com.google.android.material.appbar.MaterialToolbar materialToolbar = (com.google.android.material.appbar.MaterialToolbar) findViewById(com.mori.notebook.R.id.toolbar);
        materialToolbar.inflateMenu(com.mori.notebook.R.menu.menu_main);
        materialToolbar.setOnMenuItemClickListener(new androidx.appcompat.widget.Toolbar.OnMenuItemClickListener() { // from class: com.mori.notebook.MainActivity$$ExternalSyntheticLambda3
            @Override // androidx.appcompat.widget.Toolbar.OnMenuItemClickListener
            public final boolean onMenuItemClick(android.view.MenuItem menuItem) {
                boolean lambda$onCreate$0;
                lambda$onCreate$0 = com.mori.notebook.MainActivity.this.lambda$onCreate$0(menuItem);
                return lambda$onCreate$0;
            }
        });
        this.recycler = (androidx.recyclerview.widget.RecyclerView) findViewById(com.mori.notebook.R.id.recycler);
        this.emptyView = (android.widget.TextView) findViewById(com.mori.notebook.R.id.tv_empty);
        this.recycler.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        com.mori.notebook.NoteAdapter noteAdapter = new com.mori.notebook.NoteAdapter(new com.mori.notebook.NoteAdapter.Listener() { // from class: com.mori.notebook.MainActivity.1
            @Override // com.mori.notebook.NoteAdapter.Listener
            public void onClick(com.mori.notebook.Note note) {
                com.mori.notebook.MainActivity.this.open(note);
            }

            @Override // com.mori.notebook.NoteAdapter.Listener
            public void onLongClick(com.mori.notebook.Note note) {
                com.mori.notebook.MainActivity.this.showItemMenu(note);
            }
        });
        this.adapter = noteAdapter;
        this.recycler.setAdapter(noteAdapter);
        ((com.google.android.material.floatingactionbutton.FloatingActionButton) findViewById(com.mori.notebook.R.id.fab)).setOnClickListener(new android.view.View.OnClickListener() { // from class: com.mori.notebook.MainActivity$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.mori.notebook.MainActivity.this.lambda$onCreate$2(view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$onCreate$0(android.view.MenuItem menuItem) {
        if (menuItem.getItemId() == com.mori.notebook.R.id.action_background) {
            showBackgroundPicker();
            return true;
        }
        if (menuItem.getItemId() != com.mori.notebook.R.id.action_settings) {
            return false;
        }
        startActivity(new android.content.Intent(this, (java.lang.Class<?>) com.mori.notebook.SettingsActivity.class));
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$2(android.view.View view) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle((java.lang.CharSequence) "新建笔记").setItems((java.lang.CharSequence[]) new java.lang.String[]{"Markdown 笔记", "思维笔记"}, new android.content.DialogInterface.OnClickListener() { // from class: com.mori.notebook.MainActivity$$ExternalSyntheticLambda2
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(android.content.DialogInterface dialogInterface, int i) {
                com.mori.notebook.MainActivity.this.lambda$onCreate$1(dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$1(android.content.DialogInterface dialogInterface, int i) {
        com.mori.notebook.Note note = new com.mori.notebook.Note();
        note.type = i;
        note.content = i == 1 ? "[]" : "";
        this.store.put(note);
        open(note);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void open(com.mori.notebook.Note note) {
        android.content.Intent intent = new android.content.Intent(this, (java.lang.Class<?>) (note.type == 1 ? com.mori.notebook.MindActivity.class : com.mori.notebook.NoteEditActivity.class));
        intent.putExtra("note_id", note.id);
        startActivity(intent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showItemMenu(final com.mori.notebook.Note note) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle((java.lang.CharSequence) ((note.title == null || note.title.trim().isEmpty()) ? "无标题" : note.title)).setItems((java.lang.CharSequence[]) new java.lang.String[]{"重命名", "删除"}, new android.content.DialogInterface.OnClickListener() { // from class: com.mori.notebook.MainActivity$$ExternalSyntheticLambda5
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(android.content.DialogInterface dialogInterface, int i) {
                com.mori.notebook.MainActivity.this.lambda$showItemMenu$5(note, dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showItemMenu$5(final com.mori.notebook.Note note, android.content.DialogInterface dialogInterface, int i) {
        if (i == 0) {
            getLayoutInflater().inflate(com.mori.notebook.R.layout.activity_edit, (android.view.ViewGroup) null);
            final android.widget.EditText editText = new android.widget.EditText(this);
            editText.setText(note.title);
            int i2 = (int) (getResources().getDisplayMetrics().density * 20.0f);
            editText.setPadding(i2, i2, i2, i2);
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle((java.lang.CharSequence) "重命名").setView((android.view.View) editText).setPositiveButton((java.lang.CharSequence) "确定", new android.content.DialogInterface.OnClickListener() { // from class: com.mori.notebook.MainActivity$$ExternalSyntheticLambda0
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(android.content.DialogInterface dialogInterface2, int i3) {
                    com.mori.notebook.MainActivity.this.lambda$showItemMenu$3(note, editText, dialogInterface2, i3);
                }
            }).setNegativeButton((java.lang.CharSequence) "取消", (android.content.DialogInterface.OnClickListener) null).show();
            return;
        }
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setMessage((java.lang.CharSequence) "删除这条笔记？不可恢复。").setPositiveButton((java.lang.CharSequence) "删除", new android.content.DialogInterface.OnClickListener() { // from class: com.mori.notebook.MainActivity$$ExternalSyntheticLambda1
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(android.content.DialogInterface dialogInterface2, int i3) {
                com.mori.notebook.MainActivity.this.lambda$showItemMenu$4(note, dialogInterface2, i3);
            }
        }).setNegativeButton((java.lang.CharSequence) "取消", (android.content.DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showItemMenu$3(com.mori.notebook.Note note, android.widget.EditText editText, android.content.DialogInterface dialogInterface, int i) {
        note.title = editText.getText().toString().trim();
        note.updatedAt = java.lang.System.currentTimeMillis();
        this.store.put(note);
        reload();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showItemMenu$4(com.mori.notebook.Note note, android.content.DialogInterface dialogInterface, int i) {
        this.store.delete(note.id);
        reload();
    }

    private void reload() {
        java.util.List<com.mori.notebook.Note> notes = this.store.getNotes();
        this.adapter.setData(notes);
        this.emptyView.setVisibility(notes.isEmpty() ? 0 : 8);
    }

    @Override // com.mori.notebook.BaseActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
        reload();
    }
}
