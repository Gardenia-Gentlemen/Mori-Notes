package com.mori.notebook;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * 倒数日背景的公共操作：纯色 / 自定义图片后的落盘 / 跟随应用 / 无背景。
 * 供全屏页与列表卡片菜单复用。
 */
public final class CardBgPicker {

    private CardBgPicker() {
    }

    private static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** 相册选完图后：存副本、删旧图、写回并刷新。 */
    public static void applyPickedImage(Context ctx, Countdown item, Uri src, Runnable onChanged) {
        String saved = Gallery.save(ctx, src);
        if (saved == null) {
            Toast.makeText(ctx, "图片读取失败", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!TextUtils.isEmpty(item.bgUri)) {
            Gallery.deleteCopy(ctx, item.bgUri);
        }
        item.bgUri = saved;
        item.bgColor = 0;
        CardBg.clear();
        CountdownStore.get(ctx).put(item);
        if (onChanged != null) onChanged.run();
        Toast.makeText(ctx, "背景已更新", Toast.LENGTH_SHORT).show();
    }

    /** 无背景。 */
    public static void clear(Context ctx, Countdown item, Runnable onChanged) {
        if (!TextUtils.isEmpty(item.bgUri)) {
            Gallery.deleteCopy(ctx, item.bgUri);
        }
        item.bgUri = "";
        item.bgColor = 0;
        CardBg.clear();
        CountdownStore.get(ctx).put(item);
        if (onChanged != null) onChanged.run();
        Toast.makeText(ctx, "已移除背景", Toast.LENGTH_SHORT).show();
    }

    /** 跟随应用背景。 */
    public static void useAppBg(Context ctx, Countdown item, Runnable onChanged) {
        if (!TextUtils.isEmpty(item.bgUri)) {
            Gallery.deleteCopy(ctx, item.bgUri);
        }
        item.bgUri = "";
        item.bgColor = CardBg.BG_APP;
        CardBg.clear();
        CountdownStore.get(ctx).put(item);
        if (onChanged != null) onChanged.run();
        Toast.makeText(ctx, "背景已跟随应用", Toast.LENGTH_SHORT).show();
    }

    /** 背景二级菜单：纯色 / 自定义图片 / 与应用背景相同 / 无背景。 */
    public static void showMenu(Context ctx, View anchor, Countdown item,
                                Runnable pickImage, Runnable onChanged) {
        PopupMenu pm = new PopupMenu(ctx, anchor);
        pm.getMenu().add(0, 1, 0, "纯色背景");
        pm.getMenu().add(0, 2, 1, "自定义图片");
        pm.getMenu().add(0, 3, 2, "与应用背景相同");
        pm.getMenu().add(0, 4, 3, "无背景");
        pm.setOnMenuItemClickListener(mi -> {
            switch (mi.getItemId()) {
                case 1:
                    showSolid(ctx, item, onChanged);
                    return true;
                case 2:
                    if (pickImage != null) pickImage.run();
                    return true;
                case 3:
                    useAppBg(ctx, item, onChanged);
                    return true;
                case 4:
                    clear(ctx, item, onChanged);
                    return true;
                default:
                    return false;
            }
        });
        pm.show();
    }

    /** 纯色背景：色盘取色 + 亮度 + HEX/RGB 输入 + 预览。 */
    public static void showSolid(Context ctx, Countdown item, Runnable onChanged) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(ctx, 20), dp(ctx, 8), dp(ctx, 20), dp(ctx, 4));
        final ColorWheelView wheel = new ColorWheelView(ctx);
        int init = item.bgColor > 0 ? item.bgColor : 0xFF3F6FD8;
        wheel.setColor(init);
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(dp(ctx, 230), dp(ctx, 230));
        wlp.gravity = Gravity.CENTER_HORIZONTAL;
        box.addView(wheel, wlp);
        final View preview = new View(ctx);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 48));
        plp.topMargin = dp(ctx, 14);
        box.addView(preview, plp);
        preview.setBackgroundColor(wheel.getColor());
        final SeekBar bright = new SeekBar(ctx);
        bright.setMax(100);
        bright.setProgress(Math.round(wheel.getValue() * 100f));
        box.addView(bright);
        final TextView tip = new TextView(ctx);
        tip.setTextSize(12);
        tip.setTextColor(0x99000000);
        tip.setText("亮度");
        box.addView(tip);
        final TextView hexNow = new TextView(ctx);
        hexNow.setTextSize(13);
        hexNow.setTextColor(0xFF000000);
        hexNow.setText("当前 " + ColorCodes.hex(wheel.getColor()));
        box.addView(hexNow);
        final int[] presetColors = {0xFF000000, 0xFFFFFFFF, 0xFFE53935, 0xFFFB8C00,
                0xFFFDD835, 0xFF43A047, 0xFF00ACC1, 0xFF1E88E5, 0xFF8E24AA};
        final String[] presetNames = {"黑", "白", "红", "橙", "黄", "绿", "青", "蓝", "紫"};
        LinearLayout presets = new LinearLayout(ctx);
        presets.setOrientation(LinearLayout.HORIZONTAL);
        presets.setGravity(Gravity.CENTER);
        for (int i = 0; i < presetColors.length; i++) {
            final int pc = presetColors[i];
            View dot = new View(ctx);
            android.graphics.drawable.GradientDrawable gd =
                    new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            gd.setColor(pc);
            gd.setStroke(dp(ctx, 1), 0x33000000);
            dot.setBackground(gd);
            dot.setContentDescription(presetNames[i]);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(ctx, 26), dp(ctx, 26));
            dlp.setMarginEnd(dp(ctx, 7));
            presets.addView(dot, dlp);
            dot.setOnClickListener(v -> {
                wheel.setColor(pc);
                bright.setProgress(Math.round(wheel.getValue() * 100f));
                preview.setBackgroundColor(wheel.getColor());
                hexNow.setText("当前 " + ColorCodes.hex(wheel.getColor()));
            });
        }
        LinearLayout.LayoutParams presetsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        presetsLp.topMargin = dp(ctx, 14);
        box.addView(presets, 1, presetsLp);
        final EditText et = new EditText(ctx);
        et.setSingleLine(true);
        et.setTextSize(13);
        et.setHint("HEX：#FF5733　或　RGB：255,87,51");
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        elp.topMargin = dp(ctx, 8);
        box.addView(et, elp);
        final TextView codeTip = new TextView(ctx);
        codeTip.setTextSize(12);
        codeTip.setTextColor(0x99000000);
        codeTip.setText("支持 HEX（#RRGGBB / RRGGBB）与 RGB（255,87,51），也可直接点色盘取色。");
        box.addView(codeTip);
        final TextView applyCode = new TextView(ctx);
        applyCode.setText("套用编码");
        applyCode.setTextSize(14);
        applyCode.setPadding(0, dp(ctx, 10), 0, dp(ctx, 10));
        applyCode.setTextColor(0xFF1A73E8);
        box.addView(applyCode);
        wheel.setOnColorChanged(c -> {
            preview.setBackgroundColor(c);
            hexNow.setText("当前 " + ColorCodes.hex(c));
        });
        bright.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                wheel.setValue(Math.max(0.05f, p / 100f));
                preview.setBackgroundColor(wheel.getColor());
                hexNow.setText("当前 " + ColorCodes.hex(wheel.getColor()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
            }
        });
        applyCode.setOnClickListener(v -> {
            int c = ColorCodes.parse(et.getText().toString());
            if (c == 0) {
                Toast.makeText(ctx, "格式不对，HEX 写 #RRGGBB，RGB 写 255,87,51", Toast.LENGTH_SHORT).show();
                return;
            }
            wheel.setColor(c);
            bright.setProgress(Math.round(wheel.getValue() * 100f));
            preview.setBackgroundColor(wheel.getColor());
            hexNow.setText("当前 " + ColorCodes.hex(wheel.getColor()));
        });
        ScrollView sv = new ScrollView(ctx);
        sv.addView(box);
        new MaterialAlertDialogBuilder(ctx)
                .setTitle("纯色背景")
                .setView(sv)
                .setNegativeButton("取消", null)
                .setPositiveButton("应用", (d, w) -> {
                    if (!TextUtils.isEmpty(item.bgUri)) {
                        Gallery.deleteCopy(ctx, item.bgUri);
                    }
                    item.bgUri = "";
                    item.bgColor = 0xFF000000 | (wheel.getColor() & 0x00FFFFFF);
                    CardBg.clear();
                    CountdownStore.get(ctx).put(item);
                    if (onChanged != null) onChanged.run();
                    Toast.makeText(ctx, "已应用纯色背景", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
