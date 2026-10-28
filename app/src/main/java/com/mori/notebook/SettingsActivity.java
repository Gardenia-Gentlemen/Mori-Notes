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