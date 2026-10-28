package com.mori.notebook;

/* loaded from: classes.dex */
public class BaseActivity extends androidx.appcompat.app.AppCompatActivity {
    private androidx.activity.result.ActivityResultLauncher<java.lang.String[]> imagePicker;

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        this.imagePicker = registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.OpenDocument(), new androidx.activity.result.ActivityResultCallback() { // from class: com.mori.notebook.BaseActivity$$ExternalSyntheticLambda0
            @Override // androidx.activity.result.ActivityResultCallback
            public final void onActivityResult(java.lang.Object obj) {
                com.mori.notebook.BaseActivity.this.lambda$onCreate$0((android.net.Uri) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0(android.net.Uri uri) {
        if (uri != null) {
            try {
                getContentResolver().takePersistableUriPermission(uri, 1);
            } catch (java.lang.Exception unused) {
            }
            com.mori.notebook.BackgroundPrefs.setImageUri(this, uri);
            com.mori.notebook.BackgroundPrefs.setStyle(this, 7);
            applyBackground();
        }
    }

    protected void applyBackground() {
        com.mori.notebook.BackgroundPrefs.apply(this, findViewById(com.mori.notebook.R.id.root));
    }

    protected void showBackgroundPicker() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle(com.mori.notebook.R.string.background).setItems((java.lang.CharSequence[]) com.mori.notebook.BackgroundPrefs.NAMES, new android.content.DialogInterface.OnClickListener() { // from class: com.mori.notebook.BaseActivity$$ExternalSyntheticLambda1
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(android.content.DialogInterface dialogInterface, int i) {
                com.mori.notebook.BaseActivity.this.lambda$showBackgroundPicker$1(dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showBackgroundPicker$1(android.content.DialogInterface dialogInterface, int i) {
        if (i == 7) {
            try {
                this.imagePicker.launch(new java.lang.String[]{"image/*"});
            } catch (java.lang.Exception unused) {
            }
        } else {
            com.mori.notebook.BackgroundPrefs.setStyle(this, i);
            applyBackground();
        }
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
        applyBackground();
    }
}
