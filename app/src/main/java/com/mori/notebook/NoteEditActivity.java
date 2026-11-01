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
    private android.widget.EditText etTitle;
    private android.widget.ScrollView previewScroll;
    private android.widget.TextView previewText;
    private boolean previewMode;
    private MenuItem toggleItem;
    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<String> imagePicker;
    private String pendingText;
    private Bitmap pendingBitmap;
    /** 撤销 / 重做：快照 = 正文 + 分隔符 + 标题 */
    private final UndoStack undoStack = new UndoStack();
    private static final char SNAP_SEP = '\u0001';
    /** 程序化 setText 时屏蔽监听，避免污染撤销栈 */
    private boolean suppress;
    /** 放弃修改退出：onPause 不再自动保存 */
    private boolean discarding;

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
        imagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) insertImage(uri);
                });

        et = findViewById(R.id.et_content);
        previewScroll = findViewById(R.id.scroll_preview);
        previewText = findViewById(R.id.tv_preview);
        et.setText(note.content == null ? "" : note.content);
        etTitle = findViewById(R.id.et_title);
        etTitle.setText(note.title == null ? "" : note.title);
        attachUndo();
        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        confirmExit();
                    }
                });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_edit);
        toggleItem = toolbar.getMenu().findItem(R.id.action_toggle);
        toolbar.setNavigationOnClickListener(v -> confirmExit());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_undo) {
                doUndo();
                return true;
            }
            if (item.getItemId() == R.id.action_redo) {
                doRedo();
                return true;
            }
            if (item.getItemId() == R.id.action_toggle) {
                setPreviewMode(!previewMode);
                return true;
            }
            if (item.getItemId() == R.id.action_share) {
                showShareDialog();
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

    /** 分享笔记：图片 / Markdown 文本 / 纯文本 */
    private void showShareDialog() {
        saveNote();
        new MaterialAlertDialogBuilder(this)
                .setTitle("分享笔记")
                .setItems(new String[]{"分享图片", "分享 Markdown 文本", "分享纯文本"}, (d, which) -> {
                    if (which == 0) {
                        ShareUtils.shareAsImage(this, note);
                    } else if (which == 1) {
                        ShareUtils.shareAsMarkdown(this, note);
                    } else {
                        ShareUtils.shareAsText(this, note);
                    }
                })
                .show();
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
        String src = et.getText().toString();
        String text = keepMarkdown ? src : MarkdownRenderer.toPlainText(src);
        try {
            String path = ExportUtils.saveText(this, defaultName(), text);
            Toast.makeText(this, "已导出到 " + path, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportImage() {
        try {
            Bitmap bmp = ExportUtils.renderMarkdown(this, et.getText().toString(), 1080);
            String path = ExportUtils.saveBitmap(this, defaultName(), bmp);
            Toast.makeText(this, "已导出到 " + path, Toast.LENGTH_LONG).show();
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

    /** 正文与标题的快照（用于撤销 / 重做） */
    private String snapshot() {
        return et.getText().toString() + SNAP_SEP + etTitle.getText().toString();
    }

    private void attachUndo() {
        android.text.TextWatcher w = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (suppress) return;
                undoStack.record(snapshot());
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        };
        et.addTextChangedListener(w);
        etTitle.addTextChangedListener(w);
    }

    /** 把快照写回两个输入框（不污染撤销栈） */
    private void applySnapshot(String snap) {
        if (snap == null) return;
        int i = snap.indexOf(SNAP_SEP);
        String c = i < 0 ? snap : snap.substring(0, i);
        String t = i < 0 ? "" : snap.substring(i + 1);
        suppress = true;
        try {
            et.setTextKeepState(c);
            et.setSelection(et.getText().length());
            etTitle.setText(t);
            etTitle.setSelection(etTitle.getText().length());
        } catch (Exception ignored) {
        } finally {
            suppress = false;
        }
    }

    private void doUndo() {
        String s = undoStack.undo(snapshot());
        if (s == null) {
            Toast.makeText(this, "没有可撤销的操作", Toast.LENGTH_SHORT).show();
            return;
        }
        applySnapshot(s);
    }

    private void doRedo() {
        String s = undoStack.redo(snapshot());
        if (s == null) {
            Toast.makeText(this, "没有可重做的操作", Toast.LENGTH_SHORT).show();
            return;
        }
        applySnapshot(s);
    }

    /** 当前标题：为空则从正文提炼，仍为空用「无标题」 */
    private String currentTitle(String content) {
        String t = etTitle == null ? "" : etTitle.getText().toString().trim();
        if (t.isEmpty()) t = deriveTitle(content);
        if (t.isEmpty()) t = "无标题";
        return t;
    }

    /** 返回：有改动时提示保存 / 放弃 / 取消 */
    private void confirmExit() {
        if (isFinishing()) return;
        String content = et.getText().toString();
        String title = currentTitle(content);
        String oldContent = note.content == null ? "" : note.content;
        String oldTitle = note.title == null ? "" : note.title;
        boolean dirty = !content.equals(oldContent) || !title.equals(oldTitle);
        if (!dirty) {
            finish();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("保存修改？")
                .setPositiveButton("保存", (d, w) -> {
                    saveNote();
                    finish();
                })
                .setNegativeButton("放弃", (d, w) -> {
                    discarding = true;
                    finish();
                })
                .setNeutralButton("取消", null)
                .show();
    }

    private void saveNote() {
        if (note == null) return;
        String content = et.getText().toString();
        String title = etTitle == null ? "" : etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            title = deriveTitle(content);
            if (etTitle != null && !title.isEmpty()) etTitle.setText(title);
        }
        if (title.isEmpty()) title = "无标题";
        String oldTitle = note.title == null ? "" : note.title;
        if (content.equals(note.content) && title.equals(oldTitle)) return;
        note.content = content;
        note.title = title;
        note.updatedAt = System.currentTimeMillis();
        store.put(note);
    }

    /** 标题为空时：取正文第一句（到句末标点或换行）作为标题 */
    private String deriveTitle(String content) {
        if (content == null) return "";
        String[] lines = content.split("\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String t = line.trim();
            if (t.startsWith(String.valueOf((char)33) + String.valueOf((char)91)) && t.contains(String.valueOf((char)93) + String.valueOf((char)40))) continue;
            t = t.replaceAll("^#{1,6}\s*", "").replaceAll("[*_`~>]", "").trim();
            if (TextUtils.isEmpty(t)) {
                if (sb.length() > 0) break;
                continue;
            }
            sb.append(t);
            int end = firstSentenceEnd(sb.toString());
            if (end > 0) return sb.substring(0, end);
            if (sb.length() > 40) break;
            sb.append(' ');
        }
        String t = sb.toString().trim();
        if (TextUtils.isEmpty(t)) return "";
        return t.length() > 40 ? t.substring(0, 40) : t;
    }
    /** 第一个句末标点之后的长度，没有则 -1 */
    private static int firstSentenceEnd(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '；' || c == '!' || c == '?' || c == ';' || c == '.') return i + 1;
        }
        return -1;
    }
    @Override
    protected void onPause() {
        if (!discarding) saveNote();
        super.onPause();
    }
}
