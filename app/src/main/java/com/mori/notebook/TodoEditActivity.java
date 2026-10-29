package com.mori.notebook;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatAutoCompleteTextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** 待办编辑页：新建或修改一条待办（截止时间 / 优先级 / 清单 / 重复 / 子任务） */
public class TodoEditActivity extends BaseActivity {
    private TodoStore store;
    private Todo todo;
    private boolean isNew;
    private EditText etTitle;
    private EditText etNote;
    private MaterialCheckBox cbDone;
    private android.widget.TextView tvCreated;
    private MaterialButton btnDue;
    private MaterialButton btnDueClear;
    private MaterialButtonToggleGroup tgPriority;
    private MaterialButtonToggleGroup tgRepeat;
    private AppCompatAutoCompleteTextView actList;
    private LinearLayout containerSubtasks;
    private long dueAt;
    private boolean saved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerBackCallback();
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
        btnDue = findViewById(R.id.btn_due);
        btnDueClear = findViewById(R.id.btn_due_clear);
        tgPriority = findViewById(R.id.tg_priority);
        tgRepeat = findViewById(R.id.tg_repeat);
        actList = findViewById(R.id.act_list);
        containerSubtasks = findViewById(R.id.container_subtasks);

        etTitle.setText(todo.title == null ? "" : todo.title);
        etNote.setText(todo.note == null ? "" : todo.note);
        cbDone.setChecked(todo.done);
        dueAt = todo.dueAt;
        updateDue();

        tgPriority.check(todo.priority == Todo.PRIORITY_LOW ? R.id.btn_p_low
                : todo.priority == Todo.PRIORITY_HIGH ? R.id.btn_p_high : R.id.btn_p_normal);
        tgRepeat.check(todo.repeatRule == Todo.REPEAT_DAILY ? R.id.btn_r_daily
                : todo.repeatRule == Todo.REPEAT_WEEKLY ? R.id.btn_r_weekly
                : todo.repeatRule == Todo.REPEAT_MONTHLY ? R.id.btn_r_monthly : R.id.btn_r_none);

        actList.setText(todo.listId == null ? "" : todo.listId);
        List<String> names = new ArrayList<>();
        String[] preset = {"工作", "生活", "学习", "重要"};
        for (String s : preset) names.add(s);
        for (String s : store.listNames()) if (!names.contains(s)) names.add(s);
        actList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names));

        if (todo.subtasks != null) {
            for (Todo.SubTask s : todo.subtasks) addSubtaskRow(s.title, s.done);
        }
        findViewById(R.id.btn_add_sub).setOnClickListener(v -> addSubtaskRow("", false));
        btnDue.setOnClickListener(v -> pickDate());
        btnDueClear.setOnClickListener(v -> {
            dueAt = 0L;
            updateDue();
        });

        if (!isNew) {
            SimpleDateFormat f = new SimpleDateFormat("M月d日 HH:mm 创建", Locale.CHINA);
            tvCreated.setText(f.format(new Date(todo.createdAt)));
            tvCreated.setVisibility(View.VISIBLE);
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

    /** 动态添加一行子任务 */
    private void addSubtaskRow(String title, boolean done) {
        View row = getLayoutInflater().inflate(R.layout.item_subtask_edit, containerSubtasks, false);
        EditText et = row.findViewById(R.id.et_sub);
        MaterialCheckBox cb = row.findViewById(R.id.cb_sub);
        et.setText(title == null ? "" : title);
        cb.setChecked(done);
        row.findViewById(R.id.btn_sub_del).setOnClickListener(v ->
                containerSubtasks.removeView(row));
        containerSubtasks.addView(row);
    }

    private List<Todo.SubTask> collectSubtasks() {
        List<Todo.SubTask> out = new ArrayList<>();
        for (int i = 0; i < containerSubtasks.getChildCount(); i++) {
            View row = containerSubtasks.getChildAt(i);
            EditText et = row.findViewById(R.id.et_sub);
            MaterialCheckBox cb = row.findViewById(R.id.cb_sub);
            String t = et.getText().toString().trim();
            if (!t.isEmpty()) out.add(new Todo.SubTask(t, cb.isChecked()));
        }
        return out;
    }

    private void updateDue() {
        if (dueAt > 0) {
            SimpleDateFormat f = new SimpleDateFormat("M月d日 HH:mm", Locale.CHINA);
            btnDue.setText(f.format(new Date(dueAt)));
            btnDueClear.setVisibility(View.VISIBLE);
        } else {
            btnDue.setText("未设置");
            btnDueClear.setVisibility(View.GONE);
        }
    }

    /** 先选日期，再选时间 */
    private void pickDate() {
        Calendar local = Calendar.getInstance();
        long base = dueAt > 0 ? dueAt : System.currentTimeMillis();
        local.setTimeInMillis(base);
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("选择截止日期")
                .setSelection(utc.getTimeInMillis())
                .build();
        picker.addOnPositiveButtonClickListener(sel -> {
            Calendar u = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            u.setTimeInMillis(sel);
            Calendar cur = Calendar.getInstance();
            if (dueAt > 0) cur.setTimeInMillis(dueAt);
            pickTime(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH),
                    cur.get(Calendar.HOUR_OF_DAY), cur.get(Calendar.MINUTE));
        });
        picker.show(getSupportFragmentManager(), "todo_due_date");
    }

    private void pickTime(int year, int month, int day, int hour, int minute) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("选择截止时间")
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            Calendar c = Calendar.getInstance();
            c.clear();
            c.set(year, month, day, picker.getHour(), picker.getMinute(), 0);
            dueAt = c.getTimeInMillis();
            updateDue();
        });
        picker.show(getSupportFragmentManager(), "todo_due_time");
    }

    /** 保存；返回是否已写入 */
    private boolean save(boolean toast) {
        String title = etTitle.getText().toString().trim();
        String note = etNote.getText().toString().trim();
        boolean done = cbDone.isChecked();
        List<Todo.SubTask> subs = collectSubtasks();
        if (title.isEmpty() && note.isEmpty() && subs.isEmpty()) {
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
        todo.dueAt = dueAt;
        todo.priority = selectedPriority();
        todo.repeatRule = selectedRepeat();
        String list = actList.getText().toString().trim();
        todo.listId = list;
        todo.subtasks = subs;
        todo.updatedAt = System.currentTimeMillis();
        store.put(todo);
        saved = true;
        setResult(RESULT_OK);
        if (toast) Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        return true;
    }

    private int selectedPriority() {
        int id = tgPriority.getCheckedButtonId();
        if (id == R.id.btn_p_low) return Todo.PRIORITY_LOW;
        if (id == R.id.btn_p_high) return Todo.PRIORITY_HIGH;
        return Todo.PRIORITY_NORMAL;
    }

    private int selectedRepeat() {
        int id = tgRepeat.getCheckedButtonId();
        if (id == R.id.btn_r_daily) return Todo.REPEAT_DAILY;
        if (id == R.id.btn_r_weekly) return Todo.REPEAT_WEEKLY;
        if (id == R.id.btn_r_monthly) return Todo.REPEAT_MONTHLY;
        return Todo.REPEAT_NONE;
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

    private void registerBackCallback() {
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                save(false);
                setEnabled(false);
                TodoEditActivity.this.getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }
}
