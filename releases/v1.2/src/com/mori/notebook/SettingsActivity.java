package com.mori.notebook;

/* loaded from: classes.dex */
public class SettingsActivity extends com.mori.notebook.BaseActivity {
    private androidx.activity.result.ActivityResultLauncher<android.net.Uri> folderPicker;
    private android.widget.TextView tvBgName;
    private android.widget.TextView tvLocation;

    @Override // com.mori.notebook.BaseActivity, androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(android.os.Bundle bundle) {
        int i;
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
        android.widget.RadioGroup radioGroup = (android.widget.RadioGroup) findViewById(com.mori.notebook.R.id.rg_theme);
        int themeMode = com.mori.notebook.UiPrefs.getThemeMode(this);
        if (themeMode == 1) {
            i = com.mori.notebook.R.id.radio_light;
        } else {
            i = themeMode == 2 ? com.mori.notebook.R.id.radio_dark : com.mori.notebook.R.id.radio_follow;
        }
        radioGroup.check(i);
        radioGroup.setOnCheckedChangeListener(new android.widget.RadioGroup.OnCheckedChangeListener() { // from class: com.mori.notebook.SettingsActivity$$ExternalSyntheticLambda2
            @Override // android.widget.RadioGroup.OnCheckedChangeListener
            public final void onCheckedChanged(android.widget.RadioGroup radioGroup2, int i2) {
                com.mori.notebook.SettingsActivity.this.lambda$onCreate$2(radioGroup2, i2);
            }
        });
        this.tvBgName = (android.widget.TextView) findViewById(com.mori.notebook.R.id.tv_bg_name);
        updateBgName();
        ((com.google.android.material.button.MaterialButton) findViewById(com.mori.notebook.R.id.btn_background)).setOnClickListener(new android.view.View.OnClickListener() { // from class: com.mori.notebook.SettingsActivity$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.mori.notebook.SettingsActivity.this.lambda$onCreate$3(view);
            }
        });
        com.google.android.material.materialswitch.MaterialSwitch materialSwitch = (com.google.android.material.materialswitch.MaterialSwitch) findViewById(com.mori.notebook.R.id.sw_title_bold);
        materialSwitch.setChecked(com.mori.notebook.UiPrefs.isHomeTitleBold(this));
        materialSwitch.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() { // from class: com.mori.notebook.SettingsActivity$$ExternalSyntheticLambda4
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(android.widget.CompoundButton compoundButton, boolean z) {
                com.mori.notebook.SettingsActivity.this.lambda$onCreate$4(compoundButton, z);
            }
        });
        final android.widget.TextView textView = (android.widget.TextView) findViewById(com.mori.notebook.R.id.tv_scale_value);
        com.google.android.material.slider.Slider slider = (com.google.android.material.slider.Slider) findViewById(com.mori.notebook.R.id.slider_scale);
        float homeTitleScale = com.mori.notebook.UiPrefs.getHomeTitleScale(this);
        slider.setValue(java.lang.Math.max(0.8f, java.lang.Math.min(1.4f, homeTitleScale)));
        textView.setText(percent(homeTitleScale));
        slider.addOnChangeListener(new com.google.android.material.slider.Slider.OnChangeListener() { // from class: com.mori.notebook.SettingsActivity$$ExternalSyntheticLambda5
            @Override // com.google.android.material.slider.Slider.OnChangeListener
            public final void onValueChange(com.google.android.material.slider.Slider slider2, float f, boolean z) {
                com.mori.notebook.SettingsActivity.this.lambda$onCreate$5(textView, slider2, f, z);
            }
        });
        android.widget.TextView textView2 = (android.widget.TextView) findViewById(com.mori.notebook.R.id.tv_location);
        this.tvLocation = textView2;
        textView2.setText(com.mori.notebook.NoteStore.get(this).locationLabel());
        ((com.google.android.material.button.MaterialButton) findViewById(com.mori.notebook.R.id.btn_pick_folder)).setOnClickListener(new android.view.View.OnClickListener() { // from class: com.mori.notebook.SettingsActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.mori.notebook.SettingsActivity.this.lambda$onCreate$6(view);
            }
        });
        ((com.google.android.material.button.MaterialButton) findViewById(com.mori.notebook.R.id.btn_reset_folder)).setOnClickListener(new android.view.View.OnClickListener() { // from class: com.mori.notebook.SettingsActivity$$ExternalSyntheticLambda7
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.mori.notebook.SettingsActivity.this.lambda$onCreate$7(view);
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
    public /* synthetic */ void lambda$onCreate$2(android.widget.RadioGroup radioGroup, int i) {
        int i2 = i == com.mori.notebook.R.id.radio_light ? 1 : i == com.mori.notebook.R.id.radio_dark ? 2 : 0;
        if (i2 == com.mori.notebook.UiPrefs.getThemeMode(this)) {
            return;
        }
        com.mori.notebook.UiPrefs.setThemeMode(this, i2);
        com.mori.notebook.NotebookApp.applyTheme(this);
        recreate();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$3(android.view.View view) {
        showBackgroundPicker();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$4(android.widget.CompoundButton compoundButton, boolean z) {
        com.mori.notebook.UiPrefs.setHomeTitleBold(this, z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$5(android.widget.TextView textView, com.google.android.material.slider.Slider slider, float f, boolean z) {
        com.mori.notebook.UiPrefs.setHomeTitleScale(this, f);
        textView.setText(percent(f));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$6(android.view.View view) {
        try {
            this.folderPicker.launch(null);
        } catch (java.lang.Exception unused) {
            toast("无法打开文件夹选择器");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$7(android.view.View view) {
        com.mori.notebook.NoteStore noteStore = com.mori.notebook.NoteStore.get(this);
        noteStore.useInternal();
        this.tvLocation.setText(noteStore.locationLabel());
        toast("已恢复为应用内部存储");
    }

    @Override // com.mori.notebook.BaseActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
        updateBgName();
    }

    private void updateBgName() {
        if (this.tvBgName == null) {
            return;
        }
        int style = com.mori.notebook.BackgroundPrefs.getStyle(this);
        this.tvBgName.setText("当前：" + ((style < 0 || style >= com.mori.notebook.BackgroundPrefs.NAMES.length) ? com.mori.notebook.BackgroundPrefs.NAMES[0] : com.mori.notebook.BackgroundPrefs.NAMES[style]));
    }

    private java.lang.String percent(float f) {
        return java.lang.Math.round(f * 100.0f) + "%";
    }

    private void toast(java.lang.String str) {
        android.widget.Toast.makeText(this, str, 0).show();
    }
}
