package com.mori.notebook;

/* loaded from: classes.dex */
public class MindActivity extends com.mori.notebook.BaseActivity {
    private com.mori.notebook.MindAdapter adapter;
    private androidx.activity.result.ActivityResultLauncher<android.content.Intent> exportLauncher;
    private com.mori.notebook.MindMapView mindMap;
    private com.mori.notebook.Note note;
    private android.graphics.Bitmap pendingBitmap;
    private androidx.recyclerview.widget.RecyclerView recycler;
    private android.view.View scrollMap;
    private com.mori.notebook.NoteStore store;
    private java.util.List<com.mori.notebook.MindNode> roots = new java.util.ArrayList();
    private boolean mapMode = false;

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
        this.exportLauncher = registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), new androidx.activity.result.ActivityResultCallback() { // from class: com.mori.notebook.MindActivity$$ExternalSyntheticLambda5
            @Override // androidx.activity.result.ActivityResultCallback
            public final void onActivityResult(java.lang.Object obj) {
                com.mori.notebook.MindActivity.this.lambda$onCreate$0((androidx.activity.result.ActivityResult) obj);
            }
        });
        com.google.android.material.appbar.MaterialToolbar materialToolbar = (com.google.android.material.appbar.MaterialToolbar) findViewById(com.mori.notebook.R.id.toolbar);
        materialToolbar.inflateMenu(com.mori.notebook.R.menu.menu_mind);
        materialToolbar.setNavigationOnClickListener(new android.view.View.OnClickListener() { // from class: com.mori.notebook.MindActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.mori.notebook.MindActivity.this.lambda$onCreate$1(view);
            }
        });
        materialToolbar.setOnMenuItemClickListener(new androidx.appcompat.widget.Toolbar.OnMenuItemClickListener() { // from class: com.mori.notebook.MindActivity$$ExternalSyntheticLambda7
            @Override // androidx.appcompat.widget.Toolbar.OnMenuItemClickListener
            public final boolean onMenuItemClick(android.view.MenuItem menuItem) {
                boolean lambda$onCreate$2;
                lambda$onCreate$2 = com.mori.notebook.MindActivity.this.lambda$onCreate$2(menuItem);
                return lambda$onCreate$2;
            }
        });
        this.recycler = (androidx.recyclerview.widget.RecyclerView) findViewById(com.mori.notebook.R.id.recycler);
        this.scrollMap = findViewById(com.mori.notebook.R.id.scroll_map);
        this.mindMap = (com.mori.notebook.MindMapView) findViewById(com.mori.notebook.R.id.mindmap);
        this.recycler.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        com.mori.notebook.MindAdapter mindAdapter = new com.mori.notebook.MindAdapter(new com.mori.notebook.MindActivity.AnonymousClass1());
        this.adapter = mindAdapter;
        this.recycler.setAdapter(mindAdapter);
        this.adapter.setData(this.roots);
        this.mindMap.setRoots(this.roots);
        ((com.google.android.material.floatingactionbutton.FloatingActionButton) findViewById(com.mori.notebook.R.id.fab)).setOnClickListener(new android.view.View.OnClickListener() { // from class: com.mori.notebook.MindActivity$$ExternalSyntheticLambda8
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.mori.notebook.MindActivity.this.lambda$onCreate$4(view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0(androidx.activity.result.ActivityResult activityResult) {
        android.graphics.Bitmap bitmap;
        try {
            try {
                if (activityResult.getResultCode() == -1) {
                    android.content.Intent data = activityResult.getData();
                    android.net.Uri data2 = data == null ? null : data.getData();
                    if (data2 != null && (bitmap = this.pendingBitmap) != null) {
                        com.mori.notebook.ExportUtils.writeBitmap(this, data2, bitmap);
                        android.widget.Toast.makeText(this, "已导出", 0).show();
                    }
                }
            } catch (java.lang.Exception e) {
                android.widget.Toast.makeText(this, "导出失败：" + e.getMessage(), 0).show();
            }
        } finally {
            this.pendingBitmap = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$1(android.view.View view) {
        finish();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$onCreate$2(android.view.MenuItem menuItem) {
        if (menuItem.getItemId() == com.mori.notebook.R.id.action_view) {
            toggleView();
            return true;
        }
        if (menuItem.getItemId() != com.mori.notebook.R.id.action_export) {
            return false;
        }
        showExportDialog();
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
    public /* synthetic */ void lambda$onCreate$4(android.view.View view) {
        final android.widget.EditText input = input("新节点");
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle((java.lang.CharSequence) "添加根节点").setView((android.view.View) input).setPositiveButton((java.lang.CharSequence) "确定", new android.content.DialogInterface.OnClickListener() { // from class: com.mori.notebook.MindActivity$$ExternalSyntheticLambda1
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(android.content.DialogInterface dialogInterface, int i) {
                com.mori.notebook.MindActivity.this.lambda$onCreate$3(input, dialogInterface, i);
            }
        }).setNegativeButton((java.lang.CharSequence) "取消", (android.content.DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$3(android.widget.EditText editText, android.content.DialogInterface dialogInterface, int i) {
        java.lang.String trim = editText.getText().toString().trim();
        if (trim.isEmpty()) {
            return;
        }
        com.mori.notebook.MindNode mindNode = new com.mori.notebook.MindNode();
        mindNode.text = trim;
        this.roots.add(mindNode);
        refresh();
        saveNote();
    }

    private void refresh() {
        this.adapter.setData(this.roots);
        this.mindMap.setRoots(this.roots);
    }

    private void toggleView() {
        this.mapMode = !this.mapMode;
        this.mindMap.setRoots(this.roots);
        this.recycler.setVisibility(this.mapMode ? 8 : 0);
        this.scrollMap.setVisibility(this.mapMode ? 0 : 8);
        android.widget.Toast.makeText(this, this.mapMode ? "思维导图视图" : "列表视图", 0).show();
    }

    private void showExportDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle((java.lang.CharSequence) "导出").setItems((java.lang.CharSequence[]) new java.lang.String[]{"导出为图片（PNG）"}, new android.content.DialogInterface.OnClickListener() { // from class: com.mori.notebook.MindActivity$$ExternalSyntheticLambda4
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(android.content.DialogInterface dialogInterface, int i) {
                com.mori.notebook.MindActivity.this.lambda$showExportDialog$5(dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showExportDialog$5(android.content.DialogInterface dialogInterface, int i) {
        exportImage();
    }

    /* JADX WARN: Removed duplicated region for block: B:11:0x0044 A[Catch: Exception -> 0x0052, TryCatch #0 {Exception -> 0x0052, blocks: (B:3:0x0001, B:5:0x001a, B:8:0x0029, B:9:0x0034, B:11:0x0044, B:12:0x0048), top: B:2:0x0001 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void exportImage() {
        /*
            r4 = this;
            r0 = 0
            com.mori.notebook.MindMapView r1 = r4.mindMap     // Catch: java.lang.Exception -> L52
            int r1 = r1.getWidth()     // Catch: java.lang.Exception -> L52
            r2 = 1080(0x438, float:1.513E-42)
            java.lang.Math.max(r1, r2)     // Catch: java.lang.Exception -> L52
            com.mori.notebook.MindMapView r1 = r4.mindMap     // Catch: java.lang.Exception -> L52
            android.graphics.Bitmap r1 = r1.drawToBitmap(r2)     // Catch: java.lang.Exception -> L52
            r4.pendingBitmap = r1     // Catch: java.lang.Exception -> L52
            com.mori.notebook.Note r1 = r4.note     // Catch: java.lang.Exception -> L52
            java.lang.String r1 = r1.title     // Catch: java.lang.Exception -> L52
            if (r1 == 0) goto L32
            com.mori.notebook.Note r1 = r4.note     // Catch: java.lang.Exception -> L52
            java.lang.String r1 = r1.title     // Catch: java.lang.Exception -> L52
            java.lang.String r1 = r1.trim()     // Catch: java.lang.Exception -> L52
            boolean r1 = r1.isEmpty()     // Catch: java.lang.Exception -> L52
            if (r1 == 0) goto L29
            goto L32
        L29:
            com.mori.notebook.Note r1 = r4.note     // Catch: java.lang.Exception -> L52
            java.lang.String r1 = r1.title     // Catch: java.lang.Exception -> L52
            java.lang.String r1 = r1.trim()     // Catch: java.lang.Exception -> L52
            goto L34
        L32:
            java.lang.String r1 = "思维笔记"
        L34:
            java.lang.String r2 = "[\\\\/:*?\"<>|]"
            java.lang.String r3 = "_"
            java.lang.String r1 = r1.replaceAll(r2, r3)     // Catch: java.lang.Exception -> L52
            int r2 = r1.length()     // Catch: java.lang.Exception -> L52
            r3 = 30
            if (r2 <= r3) goto L48
            java.lang.String r1 = r1.substring(r0, r3)     // Catch: java.lang.Exception -> L52
        L48:
            androidx.activity.result.ActivityResultLauncher<android.content.Intent> r2 = r4.exportLauncher     // Catch: java.lang.Exception -> L52
            android.content.Intent r1 = com.mori.notebook.ExportUtils.imageIntent(r1)     // Catch: java.lang.Exception -> L52
            r2.launch(r1)     // Catch: java.lang.Exception -> L52
            goto L6d
        L52:
            r1 = move-exception
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            java.lang.String r3 = "导出失败："
            r2.<init>(r3)
            java.lang.String r1 = r1.getMessage()
            java.lang.StringBuilder r1 = r2.append(r1)
            java.lang.String r1 = r1.toString()
            android.widget.Toast r0 = android.widget.Toast.makeText(r4, r1, r0)
            r0.show()
        L6d:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.mori.notebook.MindActivity.exportImage():void");
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
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle((java.lang.CharSequence) "编辑节点").setView((android.view.View) input).setPositiveButton((java.lang.CharSequence) "确定", new android.content.DialogInterface.OnClickListener() { // from class: com.mori.notebook.MindActivity$$ExternalSyntheticLambda2
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(android.content.DialogInterface dialogInterface, int i) {
                com.mori.notebook.MindActivity.this.lambda$editNode$6(mindNode, input, dialogInterface, i);
            }
        }).setNegativeButton((java.lang.CharSequence) "取消", (android.content.DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$editNode$6(com.mori.notebook.MindNode mindNode, android.widget.EditText editText, android.content.DialogInterface dialogInterface, int i) {
        mindNode.text = editText.getText().toString().trim();
        this.adapter.rebuild();
        this.mindMap.setRoots(this.roots);
        saveNote();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addChild(final com.mori.notebook.MindNode mindNode) {
        final android.widget.EditText input = input("子节点内容");
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle((java.lang.CharSequence) "添加子节点").setView((android.view.View) input).setPositiveButton((java.lang.CharSequence) "确定", new android.content.DialogInterface.OnClickListener() { // from class: com.mori.notebook.MindActivity$$ExternalSyntheticLambda3
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(android.content.DialogInterface dialogInterface, int i) {
                com.mori.notebook.MindActivity.this.lambda$addChild$7(input, mindNode, dialogInterface, i);
            }
        }).setNegativeButton((java.lang.CharSequence) "取消", (android.content.DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$addChild$7(android.widget.EditText editText, com.mori.notebook.MindNode mindNode, android.content.DialogInterface dialogInterface, int i) {
        java.lang.String trim = editText.getText().toString().trim();
        if (trim.isEmpty()) {
            return;
        }
        com.mori.notebook.MindNode mindNode2 = new com.mori.notebook.MindNode();
        mindNode2.text = trim;
        mindNode.children.add(mindNode2);
        this.adapter.rebuild();
        this.mindMap.setRoots(this.roots);
        saveNote();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void confirmDelete(final com.mori.notebook.MindNode mindNode) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setMessage((java.lang.CharSequence) "删除该节点及其子节点？").setPositiveButton((java.lang.CharSequence) "删除", new android.content.DialogInterface.OnClickListener() { // from class: com.mori.notebook.MindActivity$$ExternalSyntheticLambda0
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(android.content.DialogInterface dialogInterface, int i) {
                com.mori.notebook.MindActivity.this.lambda$confirmDelete$8(mindNode, dialogInterface, i);
            }
        }).setNegativeButton((java.lang.CharSequence) "取消", (android.content.DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$confirmDelete$8(com.mori.notebook.MindNode mindNode, android.content.DialogInterface dialogInterface, int i) {
        removeNode(this.roots, mindNode);
        this.adapter.rebuild();
        this.mindMap.setRoots(this.roots);
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
