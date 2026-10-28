package com.mori.notebook;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

public class SettingsActivity extends BaseActivity {
    private TextView tvLocation;
    private TextView tvBgName;
    private TextView tvPrivacyState;
    private ActivityResultLauncher<Intent> deviceLauncher;
    private ActivityResultLauncher<Uri> folderPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        folderPicker = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
            if (uri == null) return;
            try {
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Exception ignored) {
            }
            NoteStore store = NoteStore.get(this);
            boolean loaded = store.switchLocation(uri);
            tvLocation.setText(store.locationLabel());
            toast(loaded ? "已切换到该目录，加载了其中已有的笔记" : "笔记已保存到该目录");
        });

        RadioGroup rg = findViewById(R.id.rg_theme);
        int mode = UiPrefs.getThemeMode(this);
        rg.check(mode == 1 ? R.id.radio_light : mode == 2 ? R.id.radio_dark : R.id.radio_follow);
        rg.setOnCheckedChangeListener((group, checkedId) -> {
            int m = checkedId == R.id.radio_light ? 1 : checkedId == R.id.radio_dark ? 2 : 0;
            if (m == UiPrefs.getThemeMode(this)) return;
            UiPrefs.setThemeMode(this, m);
            NotebookApp.applyTheme(this);
            recreate();
        });

        tvBgName = findViewById(R.id.tv_bg_name);
        updateBgName();
        MaterialButton btnBg = findViewById(R.id.btn_background);
        btnBg.setOnClickListener(v -> showBackgroundPicker());

        MaterialSwitch swBold = findViewById(R.id.sw_title_bold);
        swBold.setChecked(UiPrefs.isHomeTitleBold(this));
        swBold.setOnCheckedChangeListener((b, checked) -> UiPrefs.setHomeTitleBold(this, checked));

        final TextView tvValue = findViewById(R.id.tv_scale_value);
        Slider slider = findViewById(R.id.slider_scale);
        float cur = UiPrefs.getHomeTitleScale(this);
        slider.setValue(Math.max(0.8f, Math.min(1.4f, cur)));
        tvValue.setText(percent(cur));
        slider.addOnChangeListener((s, value, fromUser) -> {
            UiPrefs.setHomeTitleScale(this, value);
            tvValue.setText(percent(value));
        });

        tvLocation = findViewById(R.id.tv_location);
        tvLocation.setText(NoteStore.get(this).locationLabel());
        TextView tvVer = findViewById(R.id.tv_version);
        try {
            android.content.pm.PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            tvVer.setText("版本 " + pi.versionName + "  (" + pi.versionCode + ")");
        } catch (Exception e) {
            tvVer.setText("版本未知");
        }

        MaterialButton pick = findViewById(R.id.btn_pick_folder);
        pick.setOnClickListener(v -> {
            try {
                folderPicker.launch(null);
            } catch (Exception e) {
                toast("无法打开文件夹选择器");
            }
        });

        tvPrivacyState = findViewById(R.id.tv_privacy_state);
        updatePrivacyState();
        deviceLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        chooseNewPasswordMode();
                    } else {
                        toast("未通过验证，无法重置");
                    }
                });
        MaterialButton btnResetPrivacy = findViewById(R.id.btn_reset_privacy);
        btnResetPrivacy.setOnClickListener(v -> startResetPassword());
        MaterialButton reset = findViewById(R.id.btn_reset_folder);
        reset.setOnClickListener(v -> {
            NoteStore store = NoteStore.get(this);
            store.useInternal();
            tvLocation.setText(store.locationLabel());
            toast("已恢复为应用内部存储");
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBgName();
        updatePrivacyState();
    }

    private void updatePrivacyState() {
        if (tvPrivacyState == null) return;
        String mode;
        if (!PrivacyPrefs.isConfigured(this)) {
            mode = "未设置";
        } else if (PrivacyPrefs.isDeviceMode(this)) {
            mode = "手机锁屏密码";
        } else {
            mode = "数字密码";
        }
        tvPrivacyState.setText("当前方式：" + mode);
    }

    /** 重置密码：先验证旧密码或手机锁屏密码 */
    private void startResetPassword() {
        if (!PrivacyPrefs.isConfigured(this)) {
            chooseNewPasswordMode();
            return;
        }
        if (PrivacyPrefs.isDeviceMode(this)) {
            Intent i = PrivacyPrefs.deviceCredentialIntent(this, "验证身份", "验证手机锁屏密码以重置隐私笔记密码");
            if (i == null) {
                toast("本机未设置锁屏密码，请用旧密码验证");
                askOldPin();
                return;
            }
            try {
                deviceLauncher.launch(i);
            } catch (Exception e) {
                toast("无法打开系统验证：" + e.getMessage());
            }
        } else {
            askOldPin();
        }
    }

    private void askOldPin() {
        final android.widget.EditText et = pinInput("旧密码");
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("验证旧密码")
                .setView(wrap(et))
                .setPositiveButton("下一步", (d, w) -> {
                    if (PrivacyPrefs.verifyPin(this, et.getText().toString().trim())) {
                        chooseNewPasswordMode();
                    } else {
                        toast("密码错误");
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void chooseNewPasswordMode() {
        boolean deviceOk = PrivacyPrefs.hasDeviceCredential(this);
        String[] items = deviceOk
                ? new String[]{"设置新的数字密码", "改用手机锁屏密码"}
                : new String[]{"设置新的数字密码"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("重置隐私笔记密码")
                .setItems(items, (d, which) -> {
                    if (which == 1) {
                        PrivacyPrefs.useDeviceCredential(this);
                        updatePrivacyState();
                        toast("已改为使用手机锁屏密码");
                    } else {
                        askNewPin();
                    }
                })
                .show();
    }

    private void askNewPin() {
        final android.widget.EditText p1 = pinInput("新密码（4 位以上数字）");
        final android.widget.EditText p2 = pinInput("再次输入");
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(p1);
        box.addView(p2);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("设置新的数字密码")
                .setView(box)
                .setPositiveButton("确定", (d, w) -> {
                    String a = p1.getText().toString().trim();
                    String c = p2.getText().toString().trim();
                    if (a.length() < 4) {
                        toast("密码至少 4 位数字");
                        return;
                    }
                    if (!a.equals(c)) {
                        toast("两次输入不一致");
                        return;
                    }
                    PrivacyPrefs.setPin(this, a);
                    updatePrivacyState();
                    toast("隐私密码已更新");
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private android.widget.EditText pinInput(String hint) {
        android.widget.EditText et = new android.widget.EditText(this);
        et.setHint(hint);
        et.setSingleLine(true);
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        return et;
    }

    private android.view.View wrap(android.view.View v) {
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(v);
        return box;
    }

    private void updateBgName() {
        if (tvBgName == null) return;
        int style = BackgroundPrefs.getStyle(this);
        String name = (style >= 0 && style < BackgroundPrefs.NAMES.length)
                ? BackgroundPrefs.NAMES[style] : BackgroundPrefs.NAMES[0];
        tvBgName.setText("当前：" + name);
    }

    private String percent(float v) {
        return Math.round(v * 100) + "%";
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}