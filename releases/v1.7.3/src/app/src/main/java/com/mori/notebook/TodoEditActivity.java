package com.mori.notebook;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 待办编辑页：新建或修改一条待办 */
public class TodoEditActivity extends BaseActivity {

    private TodoStore store;
    private Todo todo;
    private boolean isNew;
    private EditText etTitle;
    private EditText etNote;
    private MaterialCheckBox cbDone;
    private android.widget.TextView tvCreated;
    private boolean saved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_edit);
        store = TodoStore.get(this);
        String id = getIntent().getStringExtra("todo_id");
        todo = id == null ? null : store.find(id);
        isNew = todo == null;
        if (isNew) todo = new Todo();

        etTitle = findViewById(R.id.et_title);
        etNote = findViewById(R.id.et_note);
        cbDone = findViewById(R.id.cb_done);
        tvCreated = findViewById(R.id.tv_created);

        etTitle.setText(todo.title == null ? "" : todo.title);
        etNote.setText(todo.note == null ? "" : todo.note);
        cbDone.setChecked(todo.done);
        if (!isNew) {
            SimpleDateFormat f = new SimpleDateFormat("M月d日 HH:mm 创建", Locale.CHINA);
            tvCreated.setText(f.format(new Date(todo.createdAt)));
            tvCreated.setVisibility(android.view.View.VISIBLE);
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(isNew ? "新建待办" : "编辑待办");
        toolbar.inflateMenu(R.menu.menu_todo_edit);
        toolbar.getMenu().findItem(R.id.action_delete).setVisible(!isNew);
        toolbar.setNavigationOnClickListener(v -> {
            save(false);
            finish();
        });
        toolbar.setOnMenuItemClickListener(item -> {
            int i = item.getItemId();
            if (i == R.id.action_save) {
                if (save(true)) finish();
                return true;
            }
            if (i == R.id.action_delete) {
                confirmDelete();
                return true;
            }
            return false;
        });

        if (isNew) etTitle.requestFocus();
    }

    /** 保存；返回是否已写入 */
    private boolean save(boolean toast) {
        String title = etTitle.getText().toString().trim();
        String note = etNote.getText().toString().trim();
        boolean done = cbDone.isChecked();
        if (title.isEmpty() && note.isEmpty()) {
            if (isNew && !toast) return false;   // 空的新建直接丢弃
            Toast.makeText(this, "内容不能为空", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (isNew) {
            todo = new Todo();
            todo.createdAt = System.currentTimeMillis();
        }
        todo.title = title;
        todo.note = note;
        todo.done = done;
        if (done) {
            todo.doneAt = todo.doneAt > 0 ? todo.doneAt : System.currentTimeMillis();
        } else {
            todo.doneAt = 0L;
        }
        store.put(todo);
        saved = true;
        setResult(RESULT_OK);
        if (toast) Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        return true;
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("删除这条待办？")
                .setMessage("删除后无法恢复。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) -> {
                    store.delete(todo.id);
                    saved = true;
                    setResult(RESULT_OK);
                    finish();
                })
                .show();
    }

    @Override
    public void onBackPressed() {
        save(false);
        super.onBackPressed();
    }
}
