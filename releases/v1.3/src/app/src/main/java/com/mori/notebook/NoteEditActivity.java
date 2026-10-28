package com.mori.notebook;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class NoteEditActivity extends BaseActivity {
    private Note note;
    private NoteStore store;
    private android.widget.EditText et;
    private android.widget.ScrollView previewScroll;
    private android.widget.TextView previewText;
    private boolean previewMode;
    private MenuItem toggleItem;
    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<String> imagePicker;
    private String pendingText;
    private Bitmap pendingBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        store = NoteStore.get(this);
        String id = getIntent().getStringExtra("note_id");
        note = id == null ? null : store.find(id);
        if (note == null) {
            finish();
            return;
        }
        if (note.inTrash()) {
            Toast.makeText(this, "该笔记已在回收站，不可编辑", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;
                    Uri uri = result.getData().getData();
                    if (uri == null) return;
                    try {
                        if (pendingBitmap != null) {
                            ExportUtils.writeBitmap(this, uri, pendingBitmap);
                        } else {
                            ExportUtils.writeText(this, uri, pendingText == null ? "" : pendingText);
                        }
                        Toast.makeText(this, "已导出", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    } finally {
                        pendingBitmap = null;
                        pendingText = null;
                    }
                });
        imagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) insertImage(uri);
                });

        et = findViewById(R.id.et_content);
        previewScroll = findViewById(R.id.scroll_preview);
        previewText = findViewById(R.id.tv_preview);
        et.setText(note.content == null ? "" : note.content);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_edit);
        toggleItem = toolbar.getMenu().findItem(R.id.action_toggle);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_toggle) {
                setPreviewMode(!previewMode);
                return true;
            }
            if (item.getItemId() == R.id.action_share) {
                ShareUtils.shareAsImage(this, note);
                return true;
            }
            if (item.getItemId() == R.id.action_export) {
                showExportDialog();
                return true;
            }
            if (item.getItemId() == R.id.action_image) {
                try {
                    imagePicker.launch("image/*");
                } catch (Exception e) {
                    Toast.makeText(this, "无法打开图片选择器", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });
    }

    private void showExportDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("导出笔记")
                .setItems(new String[]{"导出为 TXT（保留 Markdown 标记）", "导出为 TXT（纯文本）", "导出为图片（PNG）"}, (d, which) -> {
                    if (which == 0) exportTxt(true);
                    else if (which == 1) exportTxt(false);
                    else exportImage();
                })
                .show();
    }

    private void insertImage(Uri uri) {
        try {
            File dir = new File(getFilesDir(), "images");
            if (!dir.exists()) dir.mkdirs();
            String name = "img_" + System.currentTimeMillis() + ".jpg";
            File out = new File(dir, name);
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) throw new Exception("无法读取图片");
            FileOutputStream fos = new FileOutputStream(out);
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
                fos.flush();
            } finally {
                fos.close();
                in.close();
            }
            int start = Math.max(0, et.getSelectionStart());
            String md = "\n![" + name + "](file://" + out.getAbsolutePath() + ")\n";
            et.getText().insert(start, md);
            Toast.makeText(this, "已插入图片", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "插入失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportTxt(boolean keepMarkdown) {
        pendingBitmap = null;
        String src = et.getText().toString();
        pendingText = keepMarkdown ? src : MarkdownRenderer.toPlainText(src);
        try {
            exportLauncher.launch(ExportUtils.textIntent(defaultName()));
        } catch (Exception e) {
            Toast.makeText(this, "无法打开文件选择器", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportImage() {
        try {
            pendingText = null;
            pendingBitmap = ExportUtils.renderMarkdown(this, et.getText().toString(), 1080);
            exportLauncher.launch(ExportUtils.imageIntent(defaultName()));
        } catch (Exception e) {
            Toast.makeText(this, "生成图片失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String defaultName() {
        String t = note.title == null ? "" : note.title.trim();
        if (t.isEmpty()) t = "笔记";
        t = t.replaceAll("[\\/:*?\"<>|]", "_");
        if (t.length() > 30) t = t.substring(0, 30);
        return t;
    }

    private void setPreviewMode(boolean preview) {
        previewMode = preview;
        if (preview) {
            previewText.setText(MarkdownRenderer.render(et.getText().toString()));
            previewScroll.setVisibility(android.view.View.VISIBLE);
            et.setVisibility(android.view.View.GONE);
            previewScroll.scrollTo(0, 0);
            if (toggleItem != null) toggleItem.setTitle(R.string.edit);
        } else {
            previewScroll.setVisibility(android.view.View.GONE);
            et.setVisibility(android.view.View.VISIBLE);
            if (toggleItem != null) toggleItem.setTitle(R.string.preview);
        }
    }

    private void saveNote() {
        if (note == null) return;
        String content = et.getText().toString();
        if (content.equals(note.content)) return;
        note.content = content;
        note.title = deriveTitle(content);
        note.updatedAt = System.currentTimeMillis();
        store.put(note);
    }

    private String deriveTitle(String content) {
        String[] lines = content.split("\n");
        for (String line : lines) {
            String t = line.trim();
            if (t.startsWith(String.valueOf((char)33) + String.valueOf((char)91)) && t.contains(String.valueOf((char)93) + String.valueOf((char)40))) continue;
            t = t.replaceAll("^#{1,6}\\s*", "").replaceAll("[*_`~>]", "").trim();
            if (!TextUtils.isEmpty(t)) {
                return t.length() > 40 ? t.substring(0, 40) : t;
            }
        }
        return "无标题";
    }

    @Override
    protected void onPause() {
        saveNote();
        super.onPause();
    }
}
