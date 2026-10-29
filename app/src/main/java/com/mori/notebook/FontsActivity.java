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

    private ActivityResultLauncher<Intent> fontPicker;
    private ActivityResultLauncher<Intent> zipPicker;
    private ActivityResultLauncher<String> exportPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fonts);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setTitle("字体");
        tb.setNavigationOnClickListener(v -> finish());
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
            return false;
        });

        list = findViewById(R.id.recycler_fonts);
        empty = findViewById(R.id.tv_font_empty);
        adapter = new FontAdapter(this::confirmDelete);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        fontPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), r -> {
                    if (r.getResultCode() != RESULT_OK || r.getData() == null) return;
                    Uri uri = r.getData().getData();
                    if (uri == null) return;
                    String path = FontImporter.importTo(this, uri);
                    if (path != null) {
                        reload();
                        toast("已加入字体库");
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
    }

    /* ---------- 添加单个字体 ---------- */

    private void pickFont() {
        if (fontPicker == null) return;
        try {
            fontPicker.launch(FontImporter.pickIntent());
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
            for (File f : found) {
                if (FontImporter.importFile(this, f) != null) ok++;
            }
            deleteDir(outDir);
            final int done = ok;
            final int all = found.size();
            runOnUiThread(() -> {
                reload();
                if (done == 0) toast("导入失败：压缩包里的字体都无法识别");
                else if (done < all) toast("已导入 " + done + " 个字体，跳过 " + (all - done) + " 个无效文件");
                else toast("已导入 " + done + " 个字体");
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