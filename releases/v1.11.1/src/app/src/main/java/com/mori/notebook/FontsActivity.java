/*
 * 字体库页面：查看 / 添加 / 删除自定义字体，支持导出为标准压缩包，
 * 也支持导入压缩包并自动检索其中的字体（压缩包带密码时弹窗输入）。
 */
package com.mori.notebook;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FontsActivity extends BaseActivity {

    private RecyclerView list;
    private TextView empty;
    private FontAdapter adapter;
    private MaterialToolbar toolbar;

    private ActivityResultLauncher<String[]> fontPicker;
    private ActivityResultLauncher<Intent> zipPicker;
    private ActivityResultLauncher<String> exportPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fonts);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        toolbar = tb;
        tb.setTitle("字体");
        tb.setNavigationOnClickListener(v -> {
            if (adapter != null && adapter.isSelectMode()) {
                adapter.setSelectMode(false);
            } else {
                finish();
            }
        });
        tb.inflateMenu(R.menu.menu_fonts);
        tb.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_font_add) {
                pickFont();
                return true;
            }
            if (id == R.id.action_font_import_zip) {
                pickZip();
                return true;
            }
            if (id == R.id.action_font_export_zip) {
                exportZip();
                return true;
            }
            if (id == R.id.action_font_delete_selected) {
                confirmDeleteSelected();
                return true;
            }
            if (id == R.id.action_font_select_all) {
                adapter.toggleSelectAll();
                updateSelectUi(adapter.selectedCount());
                return true;
            }
            return false;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (adapter != null && adapter.isSelectMode()) {
                    adapter.setSelectMode(false);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        list = findViewById(R.id.recycler_fonts);
        empty = findViewById(R.id.tv_font_empty);
        adapter = new FontAdapter(new FontAdapter.Listener() {
            @Override
            public void onDelete(File font) {
                confirmDelete(font);
            }

            @Override
            public void onPreview(File font) {
                showPreview(font);
            }

            @Override
            public void onSelectionChanged(int count) {
                updateSelectUi(count);
            }
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        fontPicker = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                    if (uris == null || uris.isEmpty()) return;
                    int ok = 0;
                    int dup = 0;
                    int bad = 0;
                    for (Uri uri : uris) {
                        if (uri == null) {
                            bad++;
                            continue;
                        }
                        FontImporter.ImportResult r = FontImporter.importUri(this, uri);
                        if (r.code == FontImporter.OK) ok++;
                        else if (r.code == FontImporter.DUPLICATE) dup++;
                        else bad++;
                    }
                    reload();
                    if (ok == 0 && dup == 0) {
                        toast("导入失败：选中的文件都不是有效字体");
                    } else if (ok == 0 && bad == 0) {
                        toast("已有此字体，未重复导入");
                    } else {
                        StringBuilder sb = new StringBuilder();
                        if (ok > 0) sb.append("已导入 ").append(ok).append(" 个字体");
                        if (dup > 0) {
                            if (sb.length() > 0) sb.append("，");
                            sb.append(dup).append(" 个已有此字体");
                        }
                        if (bad > 0) {
                            if (sb.length() > 0) sb.append("，");
                            sb.append(bad).append(" 个无效文件");
                        }
                        toast(sb.toString());
                    }
                });

        zipPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), r -> {
                    if (r.getResultCode() != RESULT_OK || r.getData() == null) return;
                    Uri uri = r.getData().getData();
                    if (uri == null) return;
                    extractZip(uri, null);
                });

        exportPicker = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/zip"), uri -> {
                    if (uri != null) doExport(uri);
                });

        reload();
    }

    private void reload() {
        List<File> fonts = FontImporter.list(this);
        adapter.submit(fonts);
        boolean none = fonts.isEmpty();
        empty.setVisibility(none ? View.VISIBLE : View.GONE);
        list.setVisibility(none ? View.GONE : View.VISIBLE);
        updateSelectUi(adapter.selectedCount());
    }

    /* ---------- 添加字体（支持多选） ---------- */
    private void pickFont() {
        if (fontPicker == null) return;
        try {
            fontPicker.launch(FontImporter.FONT_MIME_TYPES);
        } catch (Exception e) {
            toast("无法打开文件选择器");
        }
    }

    /* ---------- 删除 ---------- */

    private void confirmDelete(File f) {
        int used = 0;
        for (Countdown c : CountdownStore.get(this).getAll()) {
            if (c.fontPath != null && c.fontPath.equals(f.getAbsolutePath())) used++;
        }
        String defPath = CountdownPrefs.getFontPath(this);
        boolean isDefault = defPath != null && defPath.equals(f.getAbsolutePath());
        StringBuilder msg = new StringBuilder();
        msg.append(FontImporter.displayName(f)).append(" · ").append(Math.max(1, f.length() / 1024)).append(" KB");
        if (used > 0) {
            msg.append("\n有 ").append(used).append(" 个倒数日正在使用，删除后将回退系统字体");
        }
        if (isDefault) {
            msg.append("\n这是全屏页当前默认字体");
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("删除字体")
                .setMessage(msg.toString())
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) -> {
                    if (f.delete()) {
                        Fonts.clear();
                        reload();
                        toast("已删除");
                    } else {
                        toast("删除失败");
                    }
                })
                .show();
    }

    /* ---------- 多选状态 ---------- */

    private void updateSelectUi(int count) {
        if (toolbar == null) return;
        boolean on = adapter != null && adapter.isSelectMode();
        toolbar.setTitle(on ? "已选 " + count + " 项" : "字体");
        android.view.Menu menu = toolbar.getMenu();
        if (menu == null) return;
        android.view.MenuItem del = menu.findItem(R.id.action_font_delete_selected);
        android.view.MenuItem all = menu.findItem(R.id.action_font_select_all);
        if (del != null) del.setVisible(on && count > 0);
        if (all != null) all.setVisible(on);
        if (all != null) {
            boolean allSel = on && adapter != null && count == adapter.getItemCount() && count > 0;
            all.setTitle(allSel ? "取消全选" : "全选");
        }
        android.view.MenuItem add = menu.findItem(R.id.action_font_add);
        android.view.MenuItem zipIn = menu.findItem(R.id.action_font_import_zip);
        android.view.MenuItem zipOut = menu.findItem(R.id.action_font_export_zip);
        if (add != null) add.setVisible(!on);
        if (zipIn != null) zipIn.setVisible(!on);
        if (zipOut != null) zipOut.setVisible(!on);
    }

    private void confirmDeleteSelected() {
        if (adapter == null) return;
        final List<File> files = adapter.selectedFiles();
        if (files.isEmpty()) {
            toast("请先选择字体");
            return;
        }
        int used = 0;
        List<Countdown> all = CountdownStore.get(this).getAll();
        String defPath = CountdownPrefs.getFontPath(this);
        boolean hitsDefault = false;
        for (File f : files) {
            String path = f.getAbsolutePath();
            for (Countdown c : all) {
                if (c.fontPath != null && c.fontPath.equals(path)) used++;
            }
            if (defPath != null && defPath.equals(path)) hitsDefault = true;
        }
        StringBuilder msg = new StringBuilder("共选中 ").append(files.size()).append(" 个字体");
        if (used > 0) {
            msg.append("\n有 ").append(used).append(" 个倒数日正在使用，删除后将回退系统字体");
        }
        if (hitsDefault) {
            msg.append("\n其中包含全屏页当前默认字体");
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("删除所选字体")
                .setMessage(msg.toString())
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) -> {
                    int ok = 0;
                    for (File f : files) {
                        if (f.delete()) ok++;
                    }
                    Fonts.clear();
                    adapter.setSelectMode(false);
                    reload();
                    if (ok == files.size()) toast("已删除 " + ok + " 个字体");
                    else toast("已删除 " + ok + " 个，失败 " + (files.size() - ok) + " 个");
                })
                .show();
    }

    /* ---------- 预览 ---------- */

    private void showPreview(File f) {
        View v = getLayoutInflater().inflate(R.layout.dialog_font_preview, null, false);
        TextView name = v.findViewById(R.id.tv_preview_name);
        TextView meta = v.findViewById(R.id.tv_preview_meta);
        TextView sample = v.findViewById(R.id.tv_preview_sample);
        TextView numbers = v.findViewById(R.id.tv_preview_numbers);
        TextView en = v.findViewById(R.id.tv_preview_en);

        android.graphics.Typeface tf = Fonts.regular(this, f.getAbsolutePath());
        name.setText(FontImporter.displayName(f));
        String ext = f.getName().toLowerCase(Locale.ROOT);
        int dot = ext.lastIndexOf('.');
        if (dot > 0) ext = ext.substring(dot + 1);
        meta.setText(ext.toUpperCase(Locale.ROOT) + " · " + Math.max(1, f.length() / 1024) + " KB");
        sample.setText("还有 128 天就是新的一年\n愿你所有的期待都如期而至");
        numbers.setText("2026-12-31 23:59");
        en.setText("ABCDEFG abcdefg 0123456789");
        for (TextView t : new TextView[]{name, sample, numbers, en}) {
            t.setTypeface(tf);
        }

        new MaterialAlertDialogBuilder(this)
                .setView(v)
                .setNegativeButton("关闭", null)
                .show();
    }

    /* ---------- 导出压缩包 ---------- */

    private void exportZip() {
        List<File> fonts = FontImporter.list(this);
        if (fonts.isEmpty()) {
            toast("字体库为空，先添加字体");
            return;
        }
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.ROOT).format(new Date());
        try {
            exportPicker.launch("fonts-" + stamp + ".zip");
        } catch (Exception e) {
            toast("无法启动导出");
        }
    }

    private void doExport(Uri uri) {
        final List<File> fonts = FontImporter.list(this);
        final int total = fonts.size();
        new Thread(() -> {
            String err = null;
            try {
                OutputStream raw = getContentResolver().openOutputStream(uri);
                if (raw == null) throw new java.io.IOException("无法写入");
                ZipOutputStream zos = new ZipOutputStream(raw);
                try {
                    byte[] buf = new byte[8192];
                    for (File f : fonts) {
                        zos.putNextEntry(new ZipEntry(f.getName()));
                        FileInputStream in = new FileInputStream(f);
                        try {
                            int n;
                            while ((n = in.read(buf)) > 0) zos.write(buf, 0, n);
                        } finally {
                            in.close();
                        }
                        zos.closeEntry();
                    }
                    zos.finish();
                } finally {
                    zos.close();
                }
            } catch (Exception e) {
                err = e.getMessage() == null ? e.toString() : e.getMessage();
            }
            final String msg = err;
            runOnUiThread(() -> {
                if (msg == null) toast("已导出 " + total + " 个字体");
                else toast("导出失败：" + msg);
            });
        }).start();
    }

    /* ---------- 导入压缩包 ---------- */

    private void pickZip() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/zip", "application/x-zip-compressed", "application/octet-stream"});
        try {
            zipPicker.launch(i);
        } catch (Exception e) {
            toast("无法打开文件选择器");
        }
    }

    /** 把选中的压缩包拷到私有缓存再解析（zip4j 只认文件路径）。 */
    private File cacheZip(Uri uri) throws Exception {
        File out = new File(getCacheDir(), "font_import.zip");
        InputStream in = getContentResolver().openInputStream(uri);
        if (in == null) return null;
        try {
            OutputStream os = new FileOutputStream(out);
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
                os.flush();
            } finally {
                os.close();
            }
        } finally {
            in.close();
        }
        return out;
    }

    private void extractZip(Uri uri, String password) {
        new Thread(() -> {
            File zip;
            try {
                zip = cacheZip(uri);
            } catch (Exception e) {
                ui("无法读取压缩包");
                return;
            }
            if (zip == null) {
                ui("无法读取压缩包");
                return;
            }
            boolean encrypted = false;
            try {
                ZipFile probe = new ZipFile(zip);
                for (FileHeader h : probe.getFileHeaders()) {
                    if (h.isEncrypted()) {
                        encrypted = true;
                        break;
                    }
                }
            } catch (Exception e) {
                ui("不是有效的压缩包");
                return;
            }
            if (encrypted && TextUtils.isEmpty(password)) {
                runOnUiThread(() -> askPassword(uri, false));
                return;
            }
            File outDir = new File(getCacheDir(), "font_zip_out");
            deleteDir(outDir);
            if (!outDir.mkdirs()) {
                ui("无法创建临时目录");
                return;
            }
            try {
                ZipFile zf = new ZipFile(zip);
                if (encrypted) zf.setPassword(password.toCharArray());
                zf.extractAll(outDir.getAbsolutePath());
            } catch (Exception e) {
                deleteDir(outDir);
                String raw = e.getMessage() == null ? "" : e.getMessage();
                String lower = raw.toLowerCase(Locale.ROOT);
                if (encrypted || lower.contains("password") || lower.contains("密码")) {
                    runOnUiThread(() -> askPassword(uri, true));
                } else {
                    ui("解压失败：" + raw);
                }
                return;
            }
            List<File> found = new ArrayList<>();
            collectFonts(outDir, found);
            if (found.isEmpty()) {
                deleteDir(outDir);
                ui("压缩包里没有找到字体文件");
                return;
            }
            int ok = 0;
            int dup = 0;
            int bad = 0;
            for (File f : found) {
                FontImporter.ImportResult r = FontImporter.importFileResult(this, f);
                if (r.code == FontImporter.OK) ok++;
                else if (r.code == FontImporter.DUPLICATE) dup++;
                else bad++;
            }
            deleteDir(outDir);
            final int done = ok;
            final int dupN = dup;
            final int badN = bad;
            runOnUiThread(() -> {
                reload();
                if (done == 0 && dupN == 0) {
                    toast("导入失败：压缩包里的字体都无法识别");
                } else if (done == 0 && badN == 0) {
                    toast("压缩包里的字体都已在字体库中，无需重复导入");
                } else {
                    StringBuilder sb = new StringBuilder();
                    if (done > 0) sb.append("已导入 ").append(done).append(" 个字体");
                    if (dupN > 0) {
                        if (sb.length() > 0) sb.append("，");
                        sb.append(dupN).append(" 个已有此字体");
                    }
                    if (badN > 0) {
                        if (sb.length() > 0) sb.append("，");
                        sb.append(badN).append(" 个无效文件");
                    }
                    toast(sb.toString());
                }
            });
        }).start();
    }

    private void askPassword(Uri uri, boolean wrong) {
        if (isFinishing()) return;
        EditText et = new EditText(this);
        et.setHint("压缩包密码");
        et.setSingleLine(true);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout box = new FrameLayout(this);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(et, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
        new MaterialAlertDialogBuilder(this)
                .setTitle(wrong ? "密码错误，请重试" : "压缩包需要密码")
                .setView(box)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (d, w) -> {
                    String pwd = et.getText() == null ? "" : et.getText().toString();
                    if (TextUtils.isEmpty(pwd)) {
                        toast("请输入密码");
                        return;
                    }
                    extractZip(uri, pwd);
                })
                .show();
    }

    /** 递归收集压缩包内解出的字体文件。 */
    private void collectFonts(File dir, List<File> out) {
        File[] all = dir.listFiles();
        if (all == null) return;
        for (File f : all) {
            if (f.isDirectory()) {
                collectFonts(f, out);
            } else {
                String n = f.getName().toLowerCase(Locale.ROOT);
                if (!n.startsWith("__macosx") && !n.startsWith("._")
                        && (n.endsWith(".ttf") || n.endsWith(".otf") || n.endsWith(".ttc"))) {
                    out.add(f);
                }
            }
        }
    }

    private void deleteDir(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] all = dir.listFiles();
        if (all != null) {
            for (File f : all) {
                if (f.isDirectory()) deleteDir(f);
                else f.delete();
            }
        }
        dir.delete();
    }

    private void ui(String msg) {
        runOnUiThread(() -> toast(msg));
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}