package com.mori.notebook;

/* loaded from: classes.dex */
public class MindActivity extends com.mori.notebook.BaseActivity {
    private com.mori.notebook.MindAdapter adapter;
    private com.mori.notebook.Note note;
    private java.util.List<com.mori.notebook.MindNode> roots = new java.util.ArrayList();
    private com.mori.notebook.NoteStore store;

    @Override // com.mori.notebook.BaseActivity, androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        setContentView(com.mori.notebook.R.layout.activity_mind);
        this.store = com.mori.notebook.NoteStore.get(this);
        java.lang.String stringExtra = getIntent().getStringExtra("note_id");
        com.mori.notebook.Note find = stringExtra == null ? null : this.store.find(stringExtra);
        this.note = find;
        if (find == null) {
            finish();
            return;
        }
        this.roots = com.mori.notebook.MindNode.parse(find.content);
        com.google.android.material.appbar.MaterialToolbar materialToolbar = (com.google.android.material.appbar.MaterialToolbar) findViewById(com.mori.notebook.R.id.toolbar);
        materialToolbar.inflateMenu(com.mori.notebook.R.menu.menu_main);
        materialToolbar.setNavigationOnClickListener(new android.view.View.OnClickListener() { // from class: com.mori.notebook.MindActivity$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.mori.notebook.MindActivity.this.lambda$onCreate$0(view);
            }
        });
        materialToolbar.setOnMenuItemClickListener(new androidx.appcompat.widget.Toolbar.OnMenuItemClickListener() { // from class: com.mori.notebook.MindActivity$$ExternalSyntheticLambda5
            @Override // androidx.appcompat.widget.Toolbar.OnMenuItemClickListener
            public final boolean onMenuItemClick(android.view.MenuItem menuItem) {
                boolean lambda$onCreate$1;
                lambda$onCreate$1 = com.mori.notebook.MindActivity.this.lambda$onCreate$1(menuItem);
                return lambda$onCreate$1;
            }
        });
        androidx.recyclerview.widget.RecyclerView recyclerView = (androidx.recyclerview.widget.RecyclerView) findViewById(com.mori.notebook.R.id.recycler);
        recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        com.mori.notebook.MindAdapter mindAdapter = new com.mori.notebook.MindAdapter(new com.mori.notebook.MindActivity.AnonymousClass1());
        this.adapter = mindAdapter;
        recyclerView.setAdapter(mindAdapter);
        this.adapter.setData(this.roots);
        ((com.google.android.material.floatingactionbutton.FloatingActionButton) findViewById(com.mori.notebook.R.id.fab)).setOnClickListener(new android.view.View.OnClickListener() { // from class: com.mori.notebook.MindActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.mori.notebook.MindActivity.this.lambda$onCreate$3(view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0(android.view.View view) {
        finish();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$onCreate$1(android.view.MenuItem menuItem) {
        if (menuItem.getItemId() != com.mori.notebook.R.id.action_background) {
            return false;
        }
        showBackgroundPicker();
        return true;
    }

    /* renamed from: com.mori.notebook.MindActivity$1, reason: invalid class name */
    class AnonymousClass1 implements com.mori.notebook.MindAdapter.Listener {
        @Override // com.mori.notebook.MindAdapter.Listener
        public void onToggle(com.mori.notebook.MindNode mindNode) {
        }

        AnonymousClass1() {
        }

        @Override // com.mori.notebook.MindAdapter.Listener
        public void onClick(com.mori.notebook.MindNode mindNode) {
            com.mori.notebook.MindActivity.this.editNode(mindNode);
        }

        @Override // com.mori.notebook.MindAdapter.Listener
        public void onLongClick(final com.mori.notebook.MindNode mindNode) {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(com.mori.notebook.MindActivity.this).setTitle((java.lang.CharSequence) ((mindNode.text == null || mindNode.text.trim().isEmpty()) ? "空节点" : mindNode.text)).setItems((java.lang.CharSequence[]) new java.lang.String[]{"编辑文字", "添加子节点", "删除节点"}, new android.content.DialogInterface.OnClickListener() { // from class: com.mori.notebook.MindActivity$1$$ExternalSyntheticLambda0
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(android.content.DialogInterface dialogInterface, int i) {
                    com.mori.notebook.MindActivity.AnonymousClass1.this.lambda$onLongClick$0(mindNode, dialogInterface, i);
                }
            }).show();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onLongClick$0(com.mori.notebook.MindNode mindNode, android.content.DialogInterface dialogInterface, int i) {
            if (i == 0) {
                com.mori.notebook.MindActivity.this.editNode(mindNode);
            } else if (i == 1) {
                com.mori.notebook.MindActivity.this.addChild(mindNode);
            } else {
                com.mori.notebook.MindActivity.this.confirmDelete(mindNode);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$3(android.view.View view) {
        final android.widget.EditText input = input("新节点");
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle((java.lang.CharSequence) "添加根节点").setView((android.view.View) input).setPositiveButton((java.lang.CharSequence) "确定", new android.content.DialogInterface.OnClickListener() { // from class: com.mori.notebook.MindActivity$$ExternalSyntheticLambda2
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(android.content.DialogInterface dialogInterface, int i) {
                com.mori.notebook.MindActivity.this.lambda$onCreate$2(input, dialogInterface, i);
            }
        }).setNegativeButton((java.lang.CharSequence) "取消", (android.content.DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$2(android.widget.EditText editText, android.content.DialogInterface dialogInterface, int i) {
        java.lang.String trim = editText.getText().toString().trim();
        if (trim.isEmpty()) {
            return;
        }
        com.mori.notebook.MindNode mindNode = new com.mori.notebook.MindNode();
        mindNode.text = trim;
        this.roots.add(mindNode);
        this.adapter.setData(this.roots);
        saveNote();
    }

    private android.widget.EditText input(java.lang.String str) {
        android.widget.EditText editText = new android.widget.EditText(this);
        editText.setHint(str);
        int i = (int) (getResources().getDisplayMetrics().density * 20.0f);
        editText.setPadding(i, i, i, i);
        return editText;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void editNode(final com.mori.notebook.MindNode mindNode) {
        final android.widget.EditText input = input("节点内容");
        input.setText(mindNode.text);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle((java.lang.CharSequence) "编辑节点").setView((android.view.View) input).setPositiveButton((java.lang.CharSequence) "确定", new android.content.DialogInterface.OnClickListener() { // from class: com.mori.notebook.MindActivity$$ExternalSyntheticLambda1
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(android.content.DialogInterface dialogInterface, int i) {
                com.mori.notebook.MindActivity.this.lambda$editNode$4(mindNode, input, dialogInterface, i);
            }
        }).setNegativeButton((java.lang.CharSequence) "取消", (android.content.DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$editNode$4(com.mori.notebook.MindNode mindNode, android.widget.EditText editText, android.content.DialogInterface dialogInterface, int i) {
        mindNode.text = editText.getText().toString().trim();
        this.adapter.rebuild();
        saveNote();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addChild(final com.mori.notebook.MindNode mindNode) {
        final android.widget.EditText input = input("子节点内容");
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle((java.lang.CharSequence) "添加子节点").setView((android.view.View) input).setPositiveButton((java.lang.CharSequence) "确定", new android.content.DialogInterface.OnClickListener() { // from class: com.mori.notebook.MindActivity$$ExternalSyntheticLambda3
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(android.content.DialogInterface dialogInterface, int i) {
                com.mori.notebook.MindActivity.this.lambda$addChild$5(input, mindNode, dialogInterface, i);
            }
        }).setNegativeButton((java.lang.CharSequence) "取消", (android.content.DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$addChild$5(android.widget.EditText editText, com.mori.notebook.MindNode mindNode, android.content.DialogInterface dialogInterface, int i) {
        java.lang.String trim = editText.getText().toString().trim();
        if (trim.isEmpty()) {
            return;
        }
        com.mori.notebook.MindNode mindNode2 = new com.mori.notebook.MindNode();
        mindNode2.text = trim;
        mindNode.children.add(mindNode2);
        this.adapter.rebuild();
        saveNote();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void confirmDelete(final com.mori.notebook.MindNode mindNode) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setMessage((java.lang.CharSequence) "删除该节点及其子节点？").setPositiveButton((java.lang.CharSequence) "删除", new android.content.DialogInterface.OnClickListener() { // from class: com.mori.notebook.MindActivity$$ExternalSyntheticLambda0
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(android.content.DialogInterface dialogInterface, int i) {
                com.mori.notebook.MindActivity.this.lambda$confirmDelete$6(mindNode, dialogInterface, i);
            }
        }).setNegativeButton((java.lang.CharSequence) "取消", (android.content.DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$confirmDelete$6(com.mori.notebook.MindNode mindNode, android.content.DialogInterface dialogInterface, int i) {
        removeNode(this.roots, mindNode);
        this.adapter.rebuild();
        saveNote();
    }

    private boolean removeNode(java.util.List<com.mori.notebook.MindNode> list, com.mori.notebook.MindNode mindNode) {
        if (list.remove(mindNode)) {
            return true;
        }
        java.util.Iterator<com.mori.notebook.MindNode> it = list.iterator();
        while (it.hasNext()) {
            if (removeNode(it.next().children, mindNode)) {
                return true;
            }
        }
        return false;
    }

    private void saveNote() {
        this.note.content = com.mori.notebook.MindNode.serialize(this.roots);
        this.note.updatedAt = java.lang.System.currentTimeMillis();
        this.note.title = (this.roots.isEmpty() || this.roots.get(0).text.trim().isEmpty()) ? "思维笔记" : this.roots.get(0).text.trim();
        this.store.put(this.note);
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onPause() {
        saveNote();
        super.onPause();
    }
}
