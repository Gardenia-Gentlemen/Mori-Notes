/*
 * 倒数日全屏展示：大字号天数铺满整屏，保留状态栏。
 * 右上角菜单可更换背景图、更改标题、保存不含界面元素的图片。
 *
 * 参考自 TimeTrace (https://github.com/KIPPUDESU/KIPPU_Trace)
 * Copyright (c) 2026 切符
 * 依据 MIT License 使用，许可全文见项目根目录 LICENSE-KIPPU_Trace。
 */
package com.mori.notebook;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 倒数日全屏展示 */
public class CountdownFullScreenActivity extends BaseActivity {

    private Countdown item;
    private ImageView bgView;
    private View scrimView;
    private TextView titleView;
    private TextView dateView;
    private TextView lunarView;
    private TextView annivView;
    private TextView daysView;
    private TextView unitView;
    private TextView clockView;
    private TextView noteView;
    private TextView hintView;
    private TextView menuView;
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            if (clockView != null) {
                clockView.setText(new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        .format(new Date()));
            }
            clockHandler.postDelayed(this, 1000L);
        }
    };
    private ActivityResultLauncher<Intent> bgPicker;
    private ActivityResultLauncher<Intent> fontPicker;
    private Typeface fontNormal;
    private Typeface fontBold;
    private int onSurfaceC = 0xFF1C1B1F;
    private int onVariantC = 0xFF49454F;

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        bgPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), r -> {
                    Intent data = r.getData();
                    Uri src = data == null ? null : data.getData();
                    if (src == null || item == null) return;
                    String saved = Gallery.save(this, src);
                    if (saved == null) {
                        toast("无法读取这张图片");
                        return;
                    }
                    Gallery.deleteCopy(this, item.bgUri);
                    item.bgUri = saved;
                    CardBg.clear();
                    CountdownStore.get(this).put(item);
                    refreshBg();
                    toast("背景已更新");
                });
        fontPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), r -> {
                    Intent data = r.getData();
                    Uri src = data == null ? null : data.getData();
                    if (src == null) return;
                    importFont(src);
                });
        String id = getIntent().getStringExtra("countdown_id");
        item = id == null ? null : CountdownStore.get(this).find(id);
        if (item == null) {
            toast("找不到这个日子");
            finish();
            return;
        }
        build();
        setupBars();
        clockHandler.post(clockTick);
    }

    /** 保留状态栏，内容延伸到系统栏区域并自行避让。 */
    private void setupBars() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (Build.VERSION.SDK_INT >= 29) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        WindowInsetsControllerCompat ic =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        ic.show(WindowInsetsCompat.Type.systemBars());
        ic.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT);
    }

    private void build() {
        onSurfaceC = MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurface, 0xFF1C1B1F);
        onVariantC = MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF49454F);
        int surface = MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorSurface, 0xFFFEF7FF);

        final long today = CountdownTime.today();
        long diff = CountdownTime.dayDiff(today, item.date);
        boolean up = Countdown.MODE_UP.equals(item.mode);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(surface);
        root.setOnClickListener(v -> finish());

        bgView = new ImageView(this);
        bgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bgView.setVisibility(View.GONE);
        root.addView(bgView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        scrimView = new View(this);
        scrimView.setBackgroundColor(0xFF000000);
        scrimView.setVisibility(View.GONE);
        root.addView(scrimView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        root.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        titleView = new TextView(this);
        titleView.setText(TextUtils.isEmpty(item.title) ? "无标题" : wrapTitle(item.title));
        titleView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 20);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        titleView.setLetterSpacing(0.16f);
        titleView.setLineSpacing(dp(6), 1f);
        titleView.setGravity(Gravity.CENTER);
        titleView.setIncludeFontPadding(false);
        content.addView(titleView);
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.BOTTOM);
        hero.setBaselineAligned(true);
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        heroLp.topMargin = dp(16);
        hero.setLayoutParams(heroLp);
        daysView = new TextView(this);
        daysView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        daysView.setIncludeFontPadding(false);
        daysView.setGravity(Gravity.CENTER);
        unitView = new TextView(this);
        unitView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        unitView.setLetterSpacing(0.05f);
        unitView.setIncludeFontPadding(false);
        LinearLayout.LayoutParams unitLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        unitLp.setMarginStart(dp(6));
        unitView.setLayoutParams(unitLp);
        String heroText;
        if (diff == 0 && !up) {
            heroText = "今天";
            unitView.setText("");
        } else {
            heroText = String.valueOf(Math.abs(diff));
            if (up) {
                unitView.setText(diff > 0 ? "天后开始" : "天");
            } else {
                unitView.setText(diff > 0 ? "天后" : "天前");
            }
        }
        daysView.setText(heroText);
        daysView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, heroSize(heroText));
        hero.addView(daysView);
        hero.addView(unitView);
        content.addView(hero);

        dateView = new TextView(this);
        dateView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        dateView.setLetterSpacing(0.05f);
        dateView.setGravity(Gravity.CENTER);
        dateView.setText(CountdownTime.formatDate(item.date));
        LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dateLp.topMargin = dp(12);
        dateView.setLayoutParams(dateLp);
        content.addView(dateView);

        String lunar = LunarUtil.format(item.date);
        lunarView = new TextView(this);
        lunarView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        lunarView.setLetterSpacing(0.05f);
        lunarView.setGravity(Gravity.CENTER);
        lunarView.setText(TextUtils.isEmpty(lunar) ? "" : lunar);
        lunarView.setVisibility(TextUtils.isEmpty(lunar) ? View.GONE : View.VISIBLE);
        LinearLayout.LayoutParams lunarLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lunarLp.topMargin = dp(4);
        lunarView.setLayoutParams(lunarLp);
        content.addView(lunarView);

        String anniv = CountdownTime.anniversaryText(item, today);
        annivView = new TextView(this);
        annivView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        annivView.setLetterSpacing(0.05f);
        annivView.setGravity(Gravity.CENTER);
        annivView.setText(TextUtils.isEmpty(anniv) ? "" : anniv);
        annivView.setVisibility(TextUtils.isEmpty(anniv) ? View.GONE : View.VISIBLE);
        LinearLayout.LayoutParams annivLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        annivLp.topMargin = dp(4);
        annivView.setLayoutParams(annivLp);
        content.addView(annivView);

        clockView = new TextView(this);
        clockView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        clockView.setLetterSpacing(0.1f);
        clockView.setGravity(Gravity.CENTER);
        clockView.setIncludeFontPadding(false);
        clockView.setText(new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(new Date()));
        LinearLayout.LayoutParams clockLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clockLp.topMargin = dp(14);
        clockView.setLayoutParams(clockLp);
        content.addView(clockView);

        noteView = new TextView(this);
        noteView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13);
        noteView.setGravity(Gravity.CENTER);
        noteView.setLetterSpacing(0.03f);
        noteView.setText(TextUtils.isEmpty(item.note) ? "" : item.note);
        noteView.setVisibility(TextUtils.isEmpty(item.note) ? View.GONE : View.VISIBLE);
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        noteLp.topMargin = dp(18);
        noteView.setLayoutParams(noteLp);
        content.addView(noteView);

        hintView = new TextView(this);
        hintView.setText("轻触任意处退出");
        hintView.setTextSize(12);
        hintView.setGravity(Gravity.CENTER);
        hintView.setAlpha(0.7f);
        FrameLayout.LayoutParams hintLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.gravity = Gravity.BOTTOM;
        hintLp.bottomMargin = dp(36);
        root.addView(hintView, hintLp);

        menuView = new TextView(this);
        menuView.setText("⋮");
        menuView.setTextSize(24);
        menuView.setGravity(Gravity.CENTER);
        menuView.setPadding(dp(14), dp(6), dp(14), dp(10));
        android.util.TypedValue tvAttr = new android.util.TypedValue();
        if (getTheme().resolveAttribute(
                android.R.attr.selectableItemBackgroundBorderless, tvAttr, true)) {
            menuView.setBackgroundResource(tvAttr.resourceId);
        }
        menuView.setOnClickListener(v -> showMenu());
        FrameLayout.LayoutParams menuLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        menuLp.gravity = Gravity.TOP | Gravity.END;
        menuLp.topMargin = dp(8);
        menuLp.setMarginEnd(dp(8));
        root.addView(menuView, menuLp);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            content.setPadding(dp(28), dp(40) + bars.top, dp(28),
                    dp(40) + bars.top + (bars.bottom - bars.top));
            FrameLayout.LayoutParams mlp = (FrameLayout.LayoutParams) menuView.getLayoutParams();
            mlp.topMargin = dp(8) + bars.top;
            menuView.setLayoutParams(mlp);
            FrameLayout.LayoutParams hlp = (FrameLayout.LayoutParams) hintView.getLayoutParams();
            hlp.bottomMargin = dp(36) + bars.bottom;
            hintView.setLayoutParams(hlp);
            return insets;
        });

        setContentView(root);
        refreshBg();
        applyTypeface();
    }

    /** 应用背景并按需切换系统栏图标颜色。 */
    private void refreshBg() {
        CardBg.apply(this, item.bgUri, bgView, scrimView, titleView, dateView, annivView,
                daysView, unitView, onSurfaceC, onVariantC, onSurfaceC);
        boolean hasBg = bgView.getVisibility() == View.VISIBLE;
        // TimeTrace 式白字分级透明度：单位 60%、日期/农历/纪念日 75%、秒表 80%
        daysView.setTextColor(hasBg ? 0xFFFFFFFF : onSurfaceC);
        titleView.setTextColor(hasBg ? 0xFFFFFFFF : onSurfaceC);
        unitView.setTextColor(hasBg ? 0x99FFFFFF : onVariantC);
        dateView.setTextColor(hasBg ? 0xBFFFFFFF : onVariantC);
        if (lunarView != null) lunarView.setTextColor(hasBg ? 0xBFFFFFFF : onVariantC);
        if (annivView != null) annivView.setTextColor(hasBg ? 0xBFFFFFFF : onVariantC);
        if (clockView != null) clockView.setTextColor(hasBg ? 0xCCFFFFFF : onVariantC);
        if (noteView != null) noteView.setTextColor(hasBg ? 0x99FFFFFF : onVariantC);
        WindowInsetsControllerCompat ic =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        ic.setAppearanceLightStatusBars(!hasBg);
        ic.setAppearanceLightNavigationBars(!hasBg);
        menuView.setTextColor(hasBg ? 0xFFFFFFFF : onVariantC);
        hintView.setTextColor(hasBg ? 0xF2FFFFFF : onVariantC);
    }

    /** 标题排版：超 35 字截断为 32 字加省略号，否则每 9 字符换行。 */
    private static String wrapTitle(String raw) {
        String t = raw == null ? "" : raw.trim();
        if (t.length() > 35) {
            t = t.substring(0, 32) + "...";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < t.length(); i += 9) {
            if (i > 0) sb.append('\n');
            sb.append(t, i, Math.min(i + 9, t.length()));
        }
        return sb.toString();
    }

    /** 天数按位数取字号：8 位 60sp、7 位 72sp、6 位 88sp、5 位 100sp，其余 120sp。 */
    private static int heroSize(String text) {
        int len = text == null ? 0 : text.length();
        if (len >= 8) return 60;
        if (len >= 7) return 72;
        if (len >= 6) return 88;
        if (len >= 5) return 100;
        if (len >= 3) return 120;
        return 100;
    }

    private void showMenu() {
        PopupMenu pm = new PopupMenu(this, menuView);
        pm.getMenu().add(0, 1, 0, "更换背景图");
        pm.getMenu().add(0, 2, 1, "更改标题");
        pm.getMenu().add(0, 3, 2, "保存图片");
        pm.getMenu().add(0, 5, 3, "分享图片");
        pm.getMenu().add(0, 6, 5, "导入自定义字体");
        pm.getMenu().add(0, 8, 6, "选择字体");
        if (!TextUtils.isEmpty(item.fontPath)) {
            pm.getMenu().add(0, 7, 7, "恢复默认字体");
        }
        if (!TextUtils.isEmpty(item.bgUri)) {
            pm.getMenu().add(0, 4, 4, "移除背景图");
        }
        pm.setOnMenuItemClickListener(mi -> {
            switch (mi.getItemId()) {
                case 1:
                    pickBg();
                    return true;
                case 2:
                    editTitle();
                    return true;
                case 3:
                    savePicture();
                    return true;
                case 4:
                    clearBg();
                    return true;
                case 5:
                    sharePicture();
                    return true;
                case 6:
                    pickFont();
                    return true;
                case 7:
                    resetFont();
                    return true;
                case 8:
                    chooseFont();
                    return true;
                default:
                    return false;
            }
        });
        pm.show();
    }

    private void pickBg() {
        if (bgPicker == null) return;
        try {
            bgPicker.launch(Gallery.intent());
        } catch (Exception e) {
            toast("无法打开相册");
        }
    }

    private void clearBg() {
        Gallery.deleteCopy(this, item.bgUri);
        item.bgUri = "";
        CardBg.clear();
        CountdownStore.get(this).put(item);
        refreshBg();
        toast("已移除背景图");
    }

    private void editTitle() {
        EditText et = new EditText(this);
        et.setText(item.title == null ? "" : item.title);
        et.setSelection(et.getText().length());
        et.setHint("标题");
        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(dp(20), dp(8), dp(20), 0);
        wrap.addView(et, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        new MaterialAlertDialogBuilder(this)
                .setTitle("更改标题")
                .setView(wrap)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (d, w) -> {
                    String t = et.getText().toString().trim();
                    item.title = t;
                    CountdownStore.get(this).put(item);
                    titleView.setText(t.isEmpty() ? "无标题" : wrapTitle(t));
                    toast("标题已更新");
                })
                .show();
    }

    /** 把整屏画成一张图（临时隐藏菜单与提示文字）。 */
    private Bitmap snapshot() {
        View contentRoot = getWindow().getDecorView().findViewById(android.R.id.content);
        if (!(contentRoot instanceof ViewGroup) || ((ViewGroup) contentRoot).getChildCount() == 0) {
            return null;
        }
        View root = ((ViewGroup) contentRoot).getChildAt(0);
        if (root.getWidth() <= 0 || root.getHeight() <= 0) {
            return null;
        }
        int menuVis = menuView.getVisibility();
        int hintVis = hintView.getVisibility();
        menuView.setVisibility(View.INVISIBLE);
        hintView.setVisibility(View.INVISIBLE);
        try {
            Bitmap bmp = Bitmap.createBitmap(root.getWidth(), root.getHeight(),
                    Bitmap.Config.ARGB_8888);
            root.draw(new Canvas(bmp));
            return bmp;
        } catch (Exception e) {
            return null;
        } finally {
            menuView.setVisibility(menuVis);
            hintView.setVisibility(hintVis);
        }
    }

    /** 图片文件名：标题去掉非法字符后加时间戳。 */
    private String shotName() {
        String base = TextUtils.isEmpty(item.title) ? "倒数日" : item.title.trim();
        base = base.replaceAll("[\\\\/:*?\"<>|\\s]+", "-");
        return base + "-"
                + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINA).format(new Date());
    }

    /** 保存整屏图片到相册。 */
    private void savePicture() {
        Bitmap bmp = snapshot();
        if (bmp == null) {
            toast("画面还没准备好");
            return;
        }
        try {
            String path = ExportUtils.saveBitmap(this, shotName(), bmp);
            toast("已保存到 " + path);
        } catch (Exception e) {
            toast("保存失败：" + e.getMessage());
        } finally {
            bmp.recycle();
        }
    }

    /** 分享整屏图片：先写缓存，再交给系统分享面板。 */
    private void sharePicture() {
        Bitmap bmp = snapshot();
        if (bmp == null) {
            toast("画面还没准备好");
            return;
        }
        File file = new File(getCacheDir(), "share/" + shotName() + ".png");
        try {
            File dir = file.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
            FileOutputStream os = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.flush();
            os.close();
        } catch (Exception e) {
            toast("分享失败：" + e.getMessage());
            return;
        } finally {
            bmp.recycle();
        }
        try {
            Uri uri = androidx.core.content.FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("image/png");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, "分享图片"));
        } catch (Exception e) {
            toast("没有可用的分享应用");
        }
    }

    /** 选择本地字体文件（ttf / otf / ttc）。 */
    private void pickFont() {
        if (fontPicker == null) return;
        try {
            fontPicker.launch(FontImporter.pickIntent());
        } catch (Exception e) {
            toast("无法打开文件选择器");
        }
    }

    /** 导入字体并绑定到当前这条倒数日。 */
    private void importFont(Uri uri) {
        String path = FontImporter.importTo(this, uri);
        if (path == null) return;
        item.fontPath = path;
        CountdownStore.get(this).put(item);
        applyTypeface();
        toast("字体已应用");
    }

    /** 从字体库里为当前这条倒数日选择字体（选项以字体预览展示）。 */
    private void chooseFont() {
        FontChooser.show(this, FontImporter.list(this), item.fontPath, item.title,
                new FontChooser.Callback() {
                    @Override
                    public void onPick(String fontPath) {
                        item.fontPath = fontPath;
                        CountdownStore.get(CountdownFullScreenActivity.this).put(item);
                        applyTypeface();
                        toast(fontPath.isEmpty() ? "已恢复默认字体" : "字体已更新");
                    }

                    @Override
                    public void onImport() {
                        pickFont();
                    }
                });
    }

    /** 读取已保存字体，失效时静默回退系统字体。 */
    private void applyTypeface() {
        Typeface tf = Fonts.regular(this, item.fontPath);
        Typeface tb = Fonts.bold(this, item.fontPath);
        fontNormal = tf == null ? Typeface.DEFAULT : tf;
        fontBold = tb == null ? Typeface.DEFAULT_BOLD : tb;
        if (titleView != null) titleView.setTypeface(fontNormal, Typeface.NORMAL);
        if (daysView != null) daysView.setTypeface(fontBold, Typeface.BOLD);
        if (unitView != null) unitView.setTypeface(fontNormal, Typeface.NORMAL);
        if (dateView != null) dateView.setTypeface(fontNormal, Typeface.NORMAL);
        if (lunarView != null) lunarView.setTypeface(fontNormal, Typeface.NORMAL);
        if (annivView != null) annivView.setTypeface(fontNormal, Typeface.NORMAL);
        if (clockView != null) clockView.setTypeface(fontNormal, Typeface.NORMAL);
        if (noteView != null) noteView.setTypeface(fontNormal, Typeface.NORMAL);
    }

    /** 恢复默认字体（仅解绑本条，字体库文件保留）。 */
    private void resetFont() {
        item.fontPath = "";
        CountdownStore.get(this).put(item);
        applyTypeface();
        toast("已恢复默认字体");
    }

    @Override
    protected void onDestroy() {
        clockHandler.removeCallbacks(clockTick);
        super.onDestroy();
    }
}
