package com.mori.notebook;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class BaseActivity extends AppCompatActivity {

    private ActivityResultLauncher<String[]> imagePicker;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                try {
                    getContentResolver().takePersistableUriPermission(uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {
                }
                BackgroundPrefs.setImageUri(this, uri);
                BackgroundPrefs.setStyle(this, 7);
                applyBackground();
            }
        });
    }

    protected void applyBackground() {
        android.view.View root = findViewById(R.id.root);
        if (root == null) return;
        BackgroundPrefs.apply(this, root);
        // 同步窗口背景，使状态栏/导航栏区域也铺满同一背景
        try {
            android.graphics.drawable.Drawable d = root.getBackground();
            if (d != null) {
                android.graphics.drawable.Drawable.ConstantState cs = d.getConstantState();
                getWindow().setBackgroundDrawable(cs != null ? cs.newDrawable(getResources()) : d);
            }
        } catch (Exception ignored) {
        }
    }

    /** 底部导航：回收站 | 主页 | 设置，各页面共用，当前页高亮 */
    protected void setupBottomNav(int selectedId) {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        if (nav == null) return;
        nav.setSelectedItemId(selectedId);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == selectedId) return true;
            Class<?> target = id == R.id.action_nav_trash ? TrashActivity.class
                    : id == R.id.action_nav_settings ? SettingsActivity.class
                    : MainActivity.class;
            android.content.Intent i = new android.content.Intent(this, target);
            i.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            if (!(this instanceof MainActivity)) finish();
            return true;
        });
    }
    protected void showBackgroundPicker() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.background)
                .setItems(BackgroundPrefs.NAMES, (dialog, which) -> {
                    if (which == 7) {
                        try {
                            imagePicker.launch(new String[]{"image/*"});
                        } catch (Exception ignored) {
                        }
                    } else {
                        BackgroundPrefs.setStyle(this, which);
                        applyBackground();
                    }
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyBackground();
    }
}