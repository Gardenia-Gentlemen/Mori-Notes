/*
 * 倒数日编辑页：新建或修改一个日子。
 *
 * 参考自 TimeTrace (https://github.com/KIPPUDESU/KIPPU_Trace)
 * Copyright (c) 2026 切符
 * 依据 MIT License 使用，许可全文见项目根目录 LICENSE-KIPPU_Trace。
 */
package com.mori.notebook;
import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
public class CountdownEditActivity extends BaseActivity {
    private CountdownStore store;
    private Countdown item;
    private boolean isNew;
    private boolean saved;
    private String initialSnapshot;
    private long pickedDate;
    private EditText etTitle;
    private TextView tvDate;
    private EditText etNote;
    private MaterialSwitch swPinned;
    private RadioGroup rgMode;
    private TextView labelAnniv;
    private TextView labelRepeat;
    private RadioGroup rgAnniv;
    private View groupCustomDays;
    private EditText etCustomDays;
    private EditText etAnnivMessage;
    private RadioGroup rgRepeat;
    private TextView tvCreated;
    private ImageView ivBgPreview;
    private TextView tvBgHint;
    private TextView tvBgClear;
    private String bgUri = "";
    private ActivityResultLauncher<android.content.Intent> bgPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerBackCallback();
        setContentView(R.layout.activity_countdown_edit);
        store = CountdownStore.get(this);
        String id = getIntent().getStringExtra("countdown_id");
        item = id == null ? null : store.find(id);
        isNew = item == null;
        if (isNew) item = new Countdown();

        etTitle = findViewById(R.id.et_title);
        tvDate = findViewById(R.id.tv_date);
        etNote = findViewById(R.id.et_note);
        swPinned = findViewById(R.id.sw_pinned);
        rgMode = findViewById(R.id.rg_mode);
        labelAnniv = findViewById(R.id.label_anniv);
        labelRepeat = findViewById(R.id.label_repeat);
        rgAnniv = findViewById(R.id.rg_anniv);
        groupCustomDays = findViewById(R.id.group_custom_days);
        etCustomDays = findViewById(R.id.et_custom_days);
        etAnnivMessage = findViewById(R.id.et_anniv_message);
        rgRepeat = findViewById(R.id.rg_repeat);
        tvCreated = findViewById(R.id.tv_created);
        ivBgPreview = findViewById(R.id.iv_bg_preview);
        tvBgHint = findViewById(R.id.tv_bg_hint);
        tvBgClear = findViewById(R.id.tv_bg_clear);
        bgUri = item.bgUri == null ? "" : item.bgUri;

        pickedDate = item.date > 0 ? item.date : CountdownTime.today();

        etTitle.setText(item.title == null ? "" : item.title);
        etNote.setText(item.note == null ? "" : item.note);
        swPinned.setChecked(item.pinned);
        etCustomDays.setText(item.customDays > 0 ? String.valueOf(item.customDays) : "");
        etAnnivMessage.setText(item.annivMessage == null ? "" : item.annivMessage);
        etCustomDays.setInputType(InputType.TYPE_CLASS_NUMBER);

        rgMode.check(Countdown.MODE_UP.equals(item.mode) ? R.id.rb_up : R.id.rb_down);
        if (item.annivType == Countdown.ANNIV_CUSTOM_DAYS) {
            rgAnniv.check(R.id.rb_anniv_days);
        } else if (item.annivType == Countdown.ANNIV_CALENDAR) {
            rgAnniv.check(R.id.rb_anniv_calendar);
        } else {
            rgAnniv.check(R.id.rb_anniv_none);
        }
        switch (item.repeatType) {
            case Countdown.REPEAT_YEARLY:
                rgRepeat.check(R.id.rb_repeat_yearly);
                break;
            case Countdown.REPEAT_MONTHLY:
                rgRepeat.check(R.id.rb_repeat_monthly);
                break;
            case Countdown.REPEAT_WEEKLY:
                rgRepeat.check(R.id.rb_repeat_weekly);
                break;
            default:
                rgRepeat.check(R.id.rb_repeat_none);
                break;
        }
        syncVisibility();
        refreshDateText();

        if (!isNew) {
            SimpleDateFormat f = new SimpleDateFormat("M月d日 HH:mm 创建", Locale.CHINA);
            tvCreated.setText(f.format(new Date(item.createdAt)));
            tvCreated.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.row_date).setOnClickListener(v -> pickDate());
        bgPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), r -> {
                    android.content.Intent data = r.getData();
                    android.net.Uri src = data == null ? null : data.getData();
                    if (src == null) return;
                    String saved = Gallery.save(this, src);
                    if (saved == null) {
                        Toast.makeText(this, "无法读取这张图片", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Gallery.deleteCopy(this, bgUri);
                    bgUri = saved;
                    CardBg.clear();
                    refreshBgRow();
                });
        refreshBgRow();
        findViewById(R.id.row_bg).setOnClickListener(v -> {
            if (bgPicker == null) return;
            try {
                bgPicker.launch(Gallery.intent());
            } catch (Exception e) {
                Toast.makeText(this, "无法打开相册", Toast.LENGTH_SHORT).show();
            }
        });
        tvBgClear.setOnClickListener(v -> {
            Gallery.deleteCopy(this, bgUri);
            bgUri = "";
            CardBg.clear();
            refreshBgRow();
        });
        rgMode.setOnCheckedChangeListener((g, checkedId) -> syncVisibility());
        rgAnniv.setOnCheckedChangeListener((g, checkedId) -> syncVisibility());

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(isNew ? "新建倒数日" : "编辑倒数日");
        toolbar.inflateMenu(R.menu.menu_countdown_edit);
        toolbar.getMenu().findItem(R.id.action_delete).setVisible(!isNew);
        toolbar.setNavigationOnClickListener(v -> handleBack());
        toolbar.setOnMenuItemClickListener(m -> {
            int i = m.getItemId();
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
        initialSnapshot = snapshot();
        if (isNew) etTitle.requestFocus();
    }

    private void pickDate() {
        int[] a = CountdownTime.ymd(pickedDate);
        DatePickerDialog d = new DatePickerDialog(this, (view, y, m, day) -> {
            pickedDate = CountdownTime.millisOf(y, m + 1, day);
            refreshDateText();
        }, a[0], a[1] - 1, a[2]);
        d.show();
    }

    private void refreshDateText() {
        long today = CountdownTime.today();
        long diff = CountdownTime.dayDiff(today, pickedDate);
        String tail;
        if (diff > 0) {
            tail = "还有 " + diff + " 天";
        } else if (diff == 0) {
            tail = "就是今天";
        } else {
            tail = "已经 " + (-diff) + " 天";
        }
        String lunar = LunarUtil.format(pickedDate);
        tvDate.setText(CountdownTime.formatDate(pickedDate)
                + (lunar.isEmpty() ? "" : " · " + lunar) + " · " + tail);
    }

    /** 刷新背景行：有图显示缩略图与移除，无图提示选择。 */
    private void refreshBgRow() {
        if (ivBgPreview == null) return;
        boolean has = bgUri != null && !bgUri.isEmpty();
        Bitmap bmp = has ? CardBg.preview(this, bgUri) : null;
        if (bmp != null) {
            ivBgPreview.setVisibility(View.VISIBLE);
            ivBgPreview.setImageBitmap(bmp);
            tvBgClear.setVisibility(View.VISIBLE);
            tvBgHint.setText("更换");
        } else {
            ivBgPreview.setVisibility(View.GONE);
            ivBgPreview.setImageDrawable(null);
            tvBgClear.setVisibility(View.GONE);
            tvBgHint.setText("选择");
        }
    }

    /** 计数方式决定纪念日 / 重复两块的显隐：累计只看纪念日，倒数只看重复。 */
    private void syncVisibility() {
        boolean up = rgMode.getCheckedRadioButtonId() == R.id.rb_up;
        labelAnniv.setVisibility(up ? View.VISIBLE : View.GONE);
        rgAnniv.setVisibility(up ? View.VISIBLE : View.GONE);
        boolean custom = up && rgAnniv.getCheckedRadioButtonId() == R.id.rb_anniv_days;
        groupCustomDays.setVisibility(custom ? View.VISIBLE : View.GONE);
        etAnnivMessage.setVisibility(up ? View.VISIBLE : View.GONE);
        labelRepeat.setVisibility(up ? View.GONE : View.VISIBLE);
        rgRepeat.setVisibility(up ? View.GONE : View.VISIBLE);
    }

    /** 保存；返回是否已写入。 */
    private boolean save(boolean toast) {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            if (isNew && !toast) return false;
            Toast.makeText(this, "写点什么吧", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (pickedDate <= 0) {
            Toast.makeText(this, "先选一个日子", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (isNew) {
            item = Countdown.create(title, pickedDate);
        }
        item.title = title;
        item.note = etNote.getText().toString().trim();
        item.date = pickedDate;
        item.pinned = swPinned.isChecked();
        item.mode = rgMode.getCheckedRadioButtonId() == R.id.rb_up ? Countdown.MODE_UP : Countdown.MODE_DOWN;

        int checked = rgAnniv.getCheckedRadioButtonId();
        if (checked == R.id.rb_anniv_days) {
            item.annivType = Countdown.ANNIV_CUSTOM_DAYS;
        } else if (checked == R.id.rb_anniv_calendar) {
            item.annivType = Countdown.ANNIV_CALENDAR;
        } else {
            item.annivType = Countdown.ANNIV_NONE;
        }
        item.customDays = parseInt(etCustomDays.getText().toString());
        item.annivMessage = etAnnivMessage.getText().toString().trim();
        item.showYear = true;
        item.showMonth = true;
        item.showWeek = true;

        int rep = rgRepeat.getCheckedRadioButtonId();
        if (rep == R.id.rb_repeat_yearly) {
            item.repeatType = Countdown.REPEAT_YEARLY;
        } else if (rep == R.id.rb_repeat_monthly) {
            item.repeatType = Countdown.REPEAT_MONTHLY;
        } else if (rep == R.id.rb_repeat_weekly) {
            item.repeatType = Countdown.REPEAT_WEEKLY;
        } else {
            item.repeatType = Countdown.REPEAT_NONE;
        }
        if (item.repeatType != Countdown.REPEAT_NONE && item.repeatAnchorDate <= 0) {
            item.repeatAnchorDate = pickedDate;
        }

        item.bgUri = bgUri == null ? "" : bgUri;
        store.put(item);
        saved = true;
        setResult(RESULT_OK);
        if (toast) {
            if (isNew && Countdown.MODE_UP.equals(item.mode)) {
                Toast.makeText(this, "已创建累计模式，你有 24 小时时间添加旧回忆",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }

    /** 当前界面状态的快照，用于判断是否有未保存的修改 */
    private String snapshot() {
        StringBuilder b = new StringBuilder();
        b.append(etTitle.getText()).append('\u0001');
        b.append(etNote.getText()).append('\u0001');
        b.append(pickedDate).append('\u0001');
        b.append(swPinned.isChecked()).append('\u0001');
        b.append(rgMode.getCheckedRadioButtonId()).append('\u0001');
        b.append(rgAnniv.getCheckedRadioButtonId()).append('\u0001');
        b.append(etCustomDays.getText()).append('\u0001');
        b.append(etAnnivMessage.getText()).append('\u0001');
        b.append(rgRepeat.getCheckedRadioButtonId()).append('\u0001');
        b.append(bgUri == null ? "" : bgUri);
        return b.toString();
    }

    private boolean isDirty() {
        if (initialSnapshot == null) return false;
        return !initialSnapshot.equals(snapshot());
    }

    /** 返回：有未保存修改时先确认 */
    private void handleBack() {
        if (saved || !isDirty()) {
            finish();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("放弃未保存的修改？")
                .setMessage("这次的改动还没有保存。")
                .setNegativeButton("继续编辑", null)
                .setPositiveButton("放弃", (d, w) -> {
                    saved = true;
                    finish();
                })
                .show();
    }

    private int parseInt(String s) {
        if (s == null) return 0;
        try {
            int v = Integer.parseInt(s.trim());
            return Math.max(0, Math.min(v, 99999));
        } catch (Exception e) {
            return 0;
        }
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("删除这个日子？")
                .setMessage("将移入回收站，可在回收站中恢复。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) -> {
                    store.delete(item.id);
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
                if (!saved && isDirty()) {
                    handleBack();
                    return;
                }
                setEnabled(false);
                CountdownEditActivity.this.getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }
}
