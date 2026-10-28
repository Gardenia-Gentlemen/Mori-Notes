package com.mori.notebook;

/* loaded from: classes.dex */
public class NoteEditActivity extends com.mori.notebook.BaseActivity {
    private android.widget.EditText et;
    private com.mori.notebook.Note note;
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
        this.et = (android.widget.EditText) findViewById(com.mori.notebook.R.id.et_content);
        this.previewScroll = (android.widget.ScrollView) findViewById(com.mori.notebook.R.id.scroll_preview);
        this.previewText = (android.widget.TextView) findViewById(com.mori.notebook.R.id.tv_preview);
        this.et.setText(this.note.content == null ? "" : this.note.content);
        com.google.android.material.appbar.MaterialToolbar materialToolbar = (com.google.android.material.appbar.MaterialToolbar) findViewById(com.mori.notebook.R.id.toolbar);
        materialToolbar.inflateMenu(com.mori.notebook.R.menu.menu_edit);
        this.toggleItem = materialToolbar.getMenu().findItem(com.mori.notebook.R.id.action_toggle);
        materialToolbar.setNavigationOnClickListener(new android.view.View.OnClickListener() { // from class: com.mori.notebook.NoteEditActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.mori.notebook.NoteEditActivity.this.lambda$onCreate$0(view);
            }
        });
        materialToolbar.setOnMenuItemClickListener(new androidx.appcompat.widget.Toolbar.OnMenuItemClickListener() { // from class: com.mori.notebook.NoteEditActivity$$ExternalSyntheticLambda1
            @Override // androidx.appcompat.widget.Toolbar.OnMenuItemClickListener
            public final boolean onMenuItemClick(android.view.MenuItem menuItem) {
                boolean lambda$onCreate$1;
                lambda$onCreate$1 = com.mori.notebook.NoteEditActivity.this.lambda$onCreate$1(menuItem);
                return lambda$onCreate$1;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0(android.view.View view) {
        finish();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$onCreate$1(android.view.MenuItem menuItem) {
        if (menuItem.getItemId() == com.mori.notebook.R.id.action_toggle) {
            setPreviewMode(!this.previewMode);
            return true;
        }
        if (menuItem.getItemId() != com.mori.notebook.R.id.action_background) {
            return false;
        }
        showBackgroundPicker();
        return true;
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
            java.lang.String trim = str2.trim().replaceAll("^#{1,6}\\s*", "").replaceAll("[*_`~>]", "").trim();
            if (!android.text.TextUtils.isEmpty(trim)) {
                return trim.length() > 40 ? trim.substring(0, 40) : trim;
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
