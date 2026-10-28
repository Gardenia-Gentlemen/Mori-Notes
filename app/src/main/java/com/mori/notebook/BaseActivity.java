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

    private static final int PAGE_TRASH = 0;
    private static final int PAGE_HOME = 1;
    private static final int PAGE_SETTINGS = 2;
    /** 当前页序号，用于左右滑动切页 */
    private int navPage = PAGE_HOME;

    /** 底部导航：回收站 | 主页 | 设置，各页面共用，当前页高亮 */
    protected void setupBottomNav(int selectedId) {
        navPage = pageOf(selectedId);
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        if (nav == null) return;
        nav.setSelectedItemId(selectedId);
        nav.setOnItemSelectedListener(item -> {
            int page = pageOf(item.getItemId());
            if (page == navPage) return true;
            goToPage(page);
            return true;
        });
    }

    private static int pageOf(int menuItemId) {
        if (menuItemId == R.id.action_nav_trash) return PAGE_TRASH;
        if (menuItemId == R.id.action_nav_settings) return PAGE_SETTINGS;
        return PAGE_HOME;
    }

    /** 无过渡动画切换页面，底部导航栏保持不动 */
    private void goToPage(int page) {
        if (page < 0 || page > 2 || page == navPage) return;
        Class<?> target = page == PAGE_TRASH ? TrashActivity.class
                : page == PAGE_SETTINGS ? SettingsActivity.class
                : MainActivity.class;
        android.content.Intent i = new android.content.Intent(this, target);
        i.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        if (!(this instanceof MainActivity)) finish();
        overridePendingTransition(0, 0);
    }

    /** 左右滑动切页：左滑下一页，右滑上一页 */
    private float swipeDownX, swipeDownY;
    private long swipeDownAt;
    private static final int SWIPE_MIN_DP = 64;

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case android.view.MotionEvent.ACTION_DOWN:
                swipeDownX = ev.getX();
                swipeDownY = ev.getY();
                swipeDownAt = System.currentTimeMillis();
                break;
            case android.view.MotionEvent.ACTION_UP:
                float dx = ev.getX() - swipeDownX;
                float dy = ev.getY() - swipeDownY;
                float min = SWIPE_MIN_DP * getResources().getDisplayMetrics().density;
                if (Math.abs(dx) > min
                        && Math.abs(dx) > Math.abs(dy) * 1.5f
                        && System.currentTimeMillis() - swipeDownAt < 800) {
                    if (dx < 0) {
                        goToPage(navPage + 1);
                    } else {
                        goToPage(navPage - 1);
                    }
                }
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
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