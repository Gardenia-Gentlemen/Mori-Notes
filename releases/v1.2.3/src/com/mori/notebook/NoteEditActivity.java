package com.mori.notebook;

/* loaded from: classes.dex */
public class NoteEditActivity extends com.mori.notebook.BaseActivity {
    private android.widget.EditText et;
    private androidx.activity.result.ActivityResultLauncher<android.content.Intent> exportLauncher;
    private androidx.activity.result.ActivityResultLauncher<java.lang.String> imagePicker;
    private com.mori.notebook.Note note;
    private android.graphics.Bitmap pendingBitmap;
    private java.lang.String pendingText;
    private boolean previewMode;
    private android.widget.ScrollView previewScroll;
    private android.widget.TextView previewText;
    private com.mori.notebook.NoteStore store;
    private android.view.MenuItem toggleItem;

    @Override // com.mori.notebook.BaseActivity, androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        setContentView(com.mori.notebook.R.layout.activity_edit);
        this.store = com.mori.notebook.NoteStore.get(this);
        java.lang.String stringExtra = getIntent().getStringExtra("note_id");
        com.mori.notebook.Note find = stringExtra == null ? null : this.store.find(stringExtra);
        this.note = find;
        if (find == null) {
            finish();
            return;
        }
        this.exportLauncher = registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), new androidx.activity.result.ActivityResultCallback() { // from class: com.mori.notebook.NoteEditActivity$$ExternalSyntheticLambda0
            @Override // androidx.activity.result.ActivityResultCallback
            public final void onActivityResult(java.lang.Object obj) {
                com.mori.notebook.NoteEditActivity.this.lambda$onCreate$0((androidx.activity.result.ActivityResult) obj);
            }
        });
        this.imagePicker = registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.GetContent(), new androidx.activity.result.ActivityResultCallback() { // from class: com.mori.notebook.NoteEditActivity$$ExternalSyntheticLambda1
            @Override // androidx.activity.result.ActivityResultCallback
            public final void onActivityResult(java.lang.Object obj) {
                com.mori.notebook.NoteEditActivity.this.lambda$onCreate$1((android.net.Uri) obj);
            }
        });
        this.et = (android.widget.EditText) findViewById(com.mori.notebook.R.id.et_content);
        this.previewScroll = (android.widget.ScrollView) findViewById(com.mori.notebook.R.id.scroll_preview);
        this.previewText = (android.widget.TextView) findViewById(com.mori.notebook.R.id.tv_preview);
        this.et.setText(this.note.content == null ? "" : this.note.content);
        com.google.android.material.appbar.MaterialToolbar materialToolbar = (com.google.android.material.appbar.MaterialToolbar) findViewById(com.mori.notebook.R.id.toolbar);
        materialToolbar.inflateMenu(com.mori.notebook.R.menu.menu_edit);
        this.toggleItem = materialToolbar.getMenu().findItem(com.mori.notebook.R.id.action_toggle);
        materialToolbar.setNavigationOnClickListener(new android.view.View.OnClickListener() { // from class: com.mori.notebook.NoteEditActivity$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.mori.notebook.NoteEditActivity.this.lambda$onCreate$2(view);
            }
        });
        materialToolbar.setOnMenuItemClickListener(new androidx.appcompat.widget.Toolbar.OnMenuItemClickListener() { // from class: com.mori.notebook.NoteEditActivity$$ExternalSyntheticLambda3
            @Override // androidx.appcompat.widget.Toolbar.OnMenuItemClickListener
            public final boolean onMenuItemClick(android.view.MenuItem menuItem) {
                boolean lambda$onCreate$3;
                lambda$onCreate$3 = com.mori.notebook.NoteEditActivity.this.lambda$onCreate$3(menuItem);
                return lambda$onCreate$3;
            }
        });
        applyFont();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Multi-variable type inference failed */
    public /* synthetic */ void lambda$onCreate$0(androidx.activity.result.ActivityResult activityResult) {
        android.net.Uri data;
        if (activityResult.getResultCode() != -1 || activityResult.getData() == null || (data = activityResult.getData().getData()) == null) {
            return;
        }
        try {
            try {
                android.graphics.Bitmap bitmap = this.pendingBitmap;
                if (bitmap != null) {
                    com.mori.notebook.ExportUtils.writeBitmap(this, data, bitmap);
                } else {
                    java.lang.String str = this.pendingText;
                    if (str == null) {
                        str = "";
                    }
                    com.mori.notebook.ExportUtils.writeText(this, data, str);
                }
                android.widget.Toast.makeText(this, "已导出", 0).show();
            } catch (java.lang.Exception e) {
                android.widget.Toast.makeText(this, "导出失败：" + e.getMessage(), 0).show();
            }
        } finally {
            this.pendingBitmap = null;
            this.pendingText = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$1(android.net.Uri uri) {
        if (uri != null) {
            insertImage(uri);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$2(android.view.View view) {
        finish();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$onCreate$3(android.view.MenuItem menuItem) {
        if (menuItem.getItemId() == com.mori.notebook.R.id.action_toggle) {
            setPreviewMode(!this.previewMode);
            return true;
        }
        if (menuItem.getItemId() == com.mori.notebook.R.id.action_export) {
            showExportDialog();
            return true;
        }
        if (menuItem.getItemId() == com.mori.notebook.R.id.action_image) {
            try {
                this.imagePicker.launch("image/*");
            } catch (java.lang.Exception unused) {
                android.widget.Toast.makeText(this, "无法打开图片选择器", 0).show();
            }
            return true;
        }
        if (menuItem.getItemId() != com.mori.notebook.R.id.action_font) {
            return false;
        }
        showFontDialog();
        return true;
    }

    private void showExportDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle((java.lang.CharSequence) "导出笔记").setItems((java.lang.CharSequence[]) new java.lang.String[]{"导出为 TXT", "导出为图片（PNG）"}, new android.content.DialogInterface.OnClickListener() { // from class: com.mori.notebook.NoteEditActivity$$ExternalSyntheticLambda4
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(android.content.DialogInterface dialogInterface, int i) {
                com.mori.notebook.NoteEditActivity.this.lambda$showExportDialog$4(dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showExportDialog$4(android.content.DialogInterface dialogInterface, int i) {
        if (i == 0) {
            exportTxt();
        } else {
            exportImage();
        }
    }

    private void showFontDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle((java.lang.CharSequence) "字体设置").setItems((java.lang.CharSequence[]) new java.lang.String[]{"字号：小", "字号：标准", "字号：大", "字号：特大", "字体：默认", "字体：衬线", "字体：等宽"}, new android.content.DialogInterface.OnClickListener() { // from class: com.mori.notebook.NoteEditActivity$$ExternalSyntheticLambda5
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(android.content.DialogInterface dialogInterface, int i) {
                com.mori.notebook.NoteEditActivity.this.lambda$showFontDialog$5(dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showFontDialog$5(android.content.DialogInterface dialogInterface, int i) {
        if (i <= 3) {
            com.mori.notebook.UiPrefs.setEditorFontSp(this, new float[]{14.0f, 16.0f, 18.0f, 21.0f}[i]);
        } else {
            com.mori.notebook.UiPrefs.setEditorFontFamily(this, new java.lang.String[]{"sans", "serif", "mono"}[i - 4]);
        }
        applyFont();
    }

    private void applyFont() {
        android.graphics.Typeface typeface;
        float editorFontSp = com.mori.notebook.UiPrefs.getEditorFontSp(this);
        this.et.setTextSize(2, editorFontSp);
        this.previewText.setTextSize(2, editorFontSp);
        java.lang.String editorFontFamily = com.mori.notebook.UiPrefs.getEditorFontFamily(this);
        if ("serif".equals(editorFontFamily)) {
            typeface = android.graphics.Typeface.SERIF;
        } else {
            typeface = "mono".equals(editorFontFamily) ? android.graphics.Typeface.MONOSPACE : android.graphics.Typeface.DEFAULT;
        }
        this.et.setTypeface(typeface);
        this.previewText.setTypeface(typeface);
        if (this.previewMode) {
            this.previewText.setText(com.mori.notebook.MarkdownRenderer.render(this.et.getText().toString()));
        }
    }

    private void insertImage(android.net.Uri uri) {
        try {
            java.io.File file = new java.io.File(getFilesDir(), "images");
            if (!file.exists()) {
                file.mkdirs();
            }
            java.lang.String str = "img_" + java.lang.System.currentTimeMillis() + ".jpg";
            java.io.File file2 = new java.io.File(file, str);
            java.io.InputStream openInputStream = getContentResolver().openInputStream(uri);
            if (openInputStream == null) {
                throw new java.lang.Exception("无法读取图片");
            }
            java.io.FileOutputStream fileOutputStream = new java.io.FileOutputStream(file2);
            try {
                byte[] bArr = new byte[8192];
                while (true) {
                    int read = openInputStream.read(bArr);
                    if (read <= 0) {
                        fileOutputStream.flush();
                        fileOutputStream.close();
                        openInputStream.close();
                        this.et.getText().insert(java.lang.Math.max(0, this.et.getSelectionStart()), "\n![" + str + "](file://" + file2.getAbsolutePath() + ")\n");
                        android.widget.Toast.makeText(this, "已插入图片", 0).show();
                        return;
                    }
                    fileOutputStream.write(bArr, 0, read);
                }
            } catch (java.lang.Throwable th) {
                fileOutputStream.close();
                openInputStream.close();
                throw th;
            }
        } catch (java.lang.Exception e) {
            android.widget.Toast.makeText(this, "插入失败：" + e.getMessage(), 0).show();
        }
    }

    private void exportTxt() {
        this.pendingBitmap = null;
        this.pendingText = this.et.getText().toString();
        try {
            this.exportLauncher.launch(com.mori.notebook.ExportUtils.textIntent(defaultName()));
        } catch (java.lang.Exception unused) {
            android.widget.Toast.makeText(this, "无法打开文件选择器", 0).show();
        }
    }

    private void exportImage() {
        try {
            this.pendingText = null;
            this.pendingBitmap = com.mori.notebook.ExportUtils.renderMarkdown(this, this.et.getText().toString(), 1080);
            this.exportLauncher.launch(com.mori.notebook.ExportUtils.imageIntent(defaultName()));
        } catch (java.lang.Exception e) {
            android.widget.Toast.makeText(this, "生成图片失败：" + e.getMessage(), 0).show();
        }
    }

    private java.lang.String defaultName() {
        java.lang.String trim = this.note.title == null ? "" : this.note.title.trim();
        if (trim.isEmpty()) {
            trim = "笔记";
        }
        java.lang.String replaceAll = trim.replaceAll("[\\/:*?\"<>|]", "_");
        return replaceAll.length() > 30 ? replaceAll.substring(0, 30) : replaceAll;
    }

    private void setPreviewMode(boolean z) {
        this.previewMode = z;
        if (z) {
            this.previewText.setText(com.mori.notebook.MarkdownRenderer.render(this.et.getText().toString()));
            this.previewScroll.setVisibility(0);
            this.et.setVisibility(8);
            this.previewScroll.scrollTo(0, 0);
            android.view.MenuItem menuItem = this.toggleItem;
            if (menuItem != null) {
                menuItem.setTitle(com.mori.notebook.R.string.edit);
                return;
            }
            return;
        }
        this.previewScroll.setVisibility(8);
        this.et.setVisibility(0);
        android.view.MenuItem menuItem2 = this.toggleItem;
        if (menuItem2 != null) {
            menuItem2.setTitle(com.mori.notebook.R.string.preview);
        }
    }

    private void saveNote() {
        if (this.note == null) {
            return;
        }
        java.lang.String obj = this.et.getText().toString();
        if (obj.equals(this.note.content)) {
            return;
        }
        this.note.content = obj;
        this.note.title = deriveTitle(obj);
        this.note.updatedAt = java.lang.System.currentTimeMillis();
        this.store.put(this.note);
    }

    private java.lang.String deriveTitle(java.lang.String str) {
        for (java.lang.String str2 : str.split("\n")) {
            java.lang.String trim = str2.trim();
            if (!trim.startsWith(java.lang.String.valueOf('!') + java.lang.String.valueOf('[')) || !trim.contains(java.lang.String.valueOf(']') + java.lang.String.valueOf('('))) {
                java.lang.String trim2 = trim.replaceAll("^#{1,6}\\s*", "").replaceAll("[*_`~>]", "").trim();
                if (!android.text.TextUtils.isEmpty(trim2)) {
                    return trim2.length() > 40 ? trim2.substring(0, 40) : trim2;
                }
            }
        }
        return "无标题";
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onPause() {
        saveNote();
        super.onPause();
    }
}
