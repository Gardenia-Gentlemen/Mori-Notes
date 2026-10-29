/*
 * 字体选择弹窗：选项直接以「字体预览」呈现，用该字体实际渲染一段示例文字，
 * 不再只显示字体文件名。倒数日主页与全屏页共用。
 */
package com.mori.notebook;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 字体选择器：跟随默认字体 / 字体库预览列表 / 导入新字体。 */
public final class FontChooser {

    /** 回调。onPick 传入字体绝对路径，空字符串表示跟随默认字体。 */
    public interface Callback {
        void onPick(String fontPath);

        void onImport();
    }

    private static final Map<String, Typeface> CACHE = new HashMap<>();

    private FontChooser() {
    }

    /**
     * 弹出字体选择框。
     *
     * @param sample 旧参数，保留签名兼容；预览文字已固定为「字体名称 ABC123 天地人」
     */
    public static void show(Context ctx, List<File> fonts, String currentPath, String sample, Callback cb) {
        if (ctx == null || fonts == null || cb == null) {
            return;
        }
        final String cur = currentPath == null ? "" : currentPath;
        final List<File> list = fonts;
        final int count = list.size() + 2;

        BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return count;
            }

            @Override
            public Object getItem(int position) {
                if (position == 0 || position == count - 1) {
                    return null;
                }
                return list.get(position - 1);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    v = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_font_option, parent, false);
                }
                TextView preview = v.findViewById(R.id.tv_option_preview);
                TextView current = v.findViewById(R.id.tv_option_current);
                if (position == 0) {
                    preview.setText("跟随默认字体");
                    preview.setTypeface(Typeface.DEFAULT);
                    current.setVisibility(cur.isEmpty() ? View.VISIBLE : View.GONE);
                    return v;
                }
                if (position == count - 1) {
                    preview.setText("导入新字体…");
                    preview.setTypeface(Typeface.DEFAULT);
                    current.setVisibility(View.GONE);
                    return v;
                }
                File f = list.get(position - 1);
                preview.setText(fontName(f) + "  ABC123 天地人");
                Typeface tf = typeface(f);
                preview.setTypeface(tf == null ? Typeface.DEFAULT : tf);
                current.setVisibility(cur.equals(f.getAbsolutePath()) ? View.VISIBLE : View.GONE);
                return v;
            }
        };

        new MaterialAlertDialogBuilder(ctx)
                .setTitle("选择字体")
                .setAdapter(adapter, (d, which) -> {
                    if (which == 0) {
                        cb.onPick("");
                    } else if (which == count - 1) {
                        cb.onImport();
                    } else {
                        cb.onPick(list.get(which - 1).getAbsolutePath());
                    }
                })
                .show();
    }

    /** 字体显示名：文件名去掉扩展名。 */
    private static String fontName(File f) {
        String n = f.getName();
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }

    /** 读取并缓存字体；加载失败返回 null（该行用系统默认字体显示）。 */
    private static Typeface typeface(File f) {
        String key = f.getAbsolutePath();
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }
        Typeface tf = null;
        try {
            tf = Typeface.createFromFile(f);
        } catch (Throwable ignored) {
            tf = null;
        }
        CACHE.put(key, tf);
        return tf;
    }

    /** 清空字体缓存（字体库增删后调用）。 */
    public static void clearCache() {
        CACHE.clear();
    }
}
