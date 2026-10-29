package com.mori.notebook;

import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class BaseActivity extends AppCompatActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyCutoutMode();
    }
    /** 内容布局就绪后统一处理 edge-to-edge 内边距（Android 15+ 强制生效）。 */
    @Override
    public void onContentChanged() {
        super.onContentChanged();
        applyDefaultInsets();
    }

    /** 全屏页面可覆盖关闭默认内边距处理。 */
    protected boolean useDefaultInsets() {
        return true;
    }

    private void applyDefaultInsets() {
        if (!useDefaultInsets()) return;
        final android.view.View root = findViewById(R.id.root);
        if (root == null) return;
        final int l = root.getPaddingLeft();
        final int t = root.getPaddingTop();
        final int r = root.getPaddingRight();
        final int b = root.getPaddingBottom();
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(l + bars.left, t + bars.top, r + bars.right, b + bars.bottom);
            return insets;
        });
        androidx.core.view.ViewCompat.requestApplyInsets(root);
    }

    /** 让窗口延伸到刘海/挖孔区域，背景铺满，横屏不再出现黑边。 */
    private void applyCutoutMode() {
        try {
            android.view.Window w = getWindow();
            android.view.WindowManager.LayoutParams lp = w.getAttributes();
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                lp.layoutInDisplayCutoutMode =
                        android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            } else if (android.os.Build.VERSION.SDK_INT >= 28) {
                lp.layoutInDisplayCutoutMode =
                        android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
            w.setAttributes(lp);
            w.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setStatusBarColor(android.graphics.Color.TRANSPARENT);
            w.setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        } catch (Exception ignored) {
        }
    }

    /** 读当前主题色，随深/浅色主题自动变化。 */
    protected int themeColor(int attr, int fallback) {
        return com.google.android.material.color.MaterialColors.getColor(this, attr, fallback);
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

    protected void showBackgroundPicker() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.background)
                .setItems(BackgroundPrefs.NAMES, (dialog, which) -> {
                    BackgroundPrefs.setStyle(this, which);
                    applyBackground();
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyBackground();
        applyTopBar();
    }

    /** 顶栏统一透明，文字/返回箭头跟随主题前景色。 */
    protected void applyTopBar() {
        try {
            android.view.View v = findViewById(R.id.toolbar);
            if (!(v instanceof com.google.android.material.appbar.MaterialToolbar)) return;
            com.google.android.material.appbar.MaterialToolbar tb =
                    (com.google.android.material.appbar.MaterialToolbar) v;
            tb.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            int onSurface = themeColor(com.google.android.material.R.attr.colorOnSurface, 0xFF1C1B1F);
            tb.setTitleTextColor(onSurface);
            if (tb.getNavigationIcon() != null) tb.setNavigationIconTint(onSurface);
        } catch (Exception ignored) {
        }
    }
}
