package com.mori.notebook;

/* loaded from: classes.dex */
public class SettingsActivity extends com.mori.notebook.BaseActivity {
    private androidx.activity.result.ActivityResultLauncher<android.net.Uri> folderPicker;
    private android.widget.TextView tvLocation;

    @Override // com.mori.notebook.BaseActivity, androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        setContentView(com.mori.notebook.R.layout.activity_settings);
        ((com.google.android.material.appbar.MaterialToolbar) findViewById(com.mori.notebook.R.id.toolbar)).setNavigationOnClickListener(new android.view.View.OnClickListener() { // from class: com.mori.notebook.SettingsActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.mori.notebook.SettingsActivity.this.lambda$onCreate$0(view);
            }
        });
        this.folderPicker = registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree(), new androidx.activity.result.ActivityResultCallback() { // from class: com.mori.notebook.SettingsActivity$$ExternalSyntheticLambda1
            @Override // androidx.activity.result.ActivityResultCallback
            public final void onActivityResult(java.lang.Object obj) {
                com.mori.notebook.SettingsActivity.this.lambda$onCreate$1((android.net.Uri) obj);
            }
        });
        com.google.android.material.materialswitch.MaterialSwitch materialSwitch = (com.google.android.material.materialswitch.MaterialSwitch) findViewById(com.mori.notebook.R.id.sw_title_bold);
        materialSwitch.setChecked(com.mori.notebook.UiPrefs.isHomeTitleBold(this));
        materialSwitch.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() { // from class: com.mori.notebook.SettingsActivity$$ExternalSyntheticLambda2
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(android.widget.CompoundButton compoundButton, boolean z) {
                com.mori.notebook.SettingsActivity.this.lambda$onCreate$2(compoundButton, z);
            }
        });
        final android.widget.TextView textView = (android.widget.TextView) findViewById(com.mori.notebook.R.id.tv_scale_value);
        com.google.android.material.slider.Slider slider = (com.google.android.material.slider.Slider) findViewById(com.mori.notebook.R.id.slider_scale);
        float homeTitleScale = com.mori.notebook.UiPrefs.getHomeTitleScale(this);
        slider.setValue(java.lang.Math.max(0.8f, java.lang.Math.min(1.4f, homeTitleScale)));
        textView.setText(percent(homeTitleScale));
        slider.addOnChangeListener(new com.google.android.material.slider.Slider.OnChangeListener() { // from class: com.mori.notebook.SettingsActivity$$ExternalSyntheticLambda3
            @Override // com.google.android.material.slider.Slider.OnChangeListener
            public final void onValueChange(com.google.android.material.slider.Slider slider2, float f, boolean z) {
                com.mori.notebook.SettingsActivity.this.lambda$onCreate$3(textView, slider2, f, z);
            }
        });
        android.widget.TextView textView2 = (android.widget.TextView) findViewById(com.mori.notebook.R.id.tv_location);
        this.tvLocation = textView2;
        textView2.setText(com.mori.notebook.NoteStore.get(this).locationLabel());
        ((com.google.android.material.button.MaterialButton) findViewById(com.mori.notebook.R.id.btn_pick_folder)).setOnClickListener(new android.view.View.OnClickListener() { // from class: com.mori.notebook.SettingsActivity$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.mori.notebook.SettingsActivity.this.lambda$onCreate$4(view);
            }
        });
        ((com.google.android.material.button.MaterialButton) findViewById(com.mori.notebook.R.id.btn_reset_folder)).setOnClickListener(new android.view.View.OnClickListener() { // from class: com.mori.notebook.SettingsActivity$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.mori.notebook.SettingsActivity.this.lambda$onCreate$5(view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0(android.view.View view) {
        finish();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$1(android.net.Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(uri, 3);
        } catch (java.lang.Exception unused) {
        }
        com.mori.notebook.NoteStore noteStore = com.mori.notebook.NoteStore.get(this);
        boolean switchLocation = noteStore.switchLocation(uri);
        this.tvLocation.setText(noteStore.locationLabel());
        toast(switchLocation ? "已切换到该目录，加载了其中已有的笔记" : "笔记已保存到该目录");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$2(android.widget.CompoundButton compoundButton, boolean z) {
        com.mori.notebook.UiPrefs.setHomeTitleBold(this, z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$3(android.widget.TextView textView, com.google.android.material.slider.Slider slider, float f, boolean z) {
        com.mori.notebook.UiPrefs.setHomeTitleScale(this, f);
        textView.setText(percent(f));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$4(android.view.View view) {
        try {
            this.folderPicker.launch(null);
        } catch (java.lang.Exception unused) {
            toast("无法打开文件夹选择器");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$5(android.view.View view) {
        com.mori.notebook.NoteStore noteStore = com.mori.notebook.NoteStore.get(this);
        noteStore.useInternal();
        this.tvLocation.setText(noteStore.locationLabel());
        toast("已恢复为应用内部存储");
    }

    private java.lang.String percent(float f) {
        return java.lang.Math.round(f * 100.0f) + "%";
    }

    private void toast(java.lang.String str) {
        android.widget.Toast.makeText(this, str, 0).show();
    }
}
