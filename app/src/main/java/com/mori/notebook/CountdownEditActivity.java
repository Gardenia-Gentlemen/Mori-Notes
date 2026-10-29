/*
 * 倒数日编辑页：新建或修改一个日子。
 *
 * 参考自 TimeTrace (https://github.com/KIPPUDESU/KIPPU_Trace)
 * Copyright (c) 2026 切符
 * 依据 MIT License 使用，许可全文见项目根目录 LICENSE-KIPPU_Trace。
 */
package com.mori.notebook;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
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
    private TextView tvLunar;
    private TextView tvElapsed;
    private EditText etNote;
    private MaterialSwitch swPinned;
    private RadioGroup rgMode;
    private TextView labelAnniv;
    private TextView labelRepeat;
    private RadioGroup rgAnniv;
    private View content;
    private View groupCustomDays;
    private EditText etCustomDays;
    private EditText etAnnivMessage;
    private RadioGroup rgRepeat;
    private TextView tvCreated;
    private ImageView ivBgPreview;
    private TextView tvBgHint;
    private TextView tvBgClear;
    private String bgUri = "";
    private MaterialButton btnDayStart;
    private TextView tvMaskLabel;
    private Slider sliderMask;
    private MaterialButton btnResetGlobal;
    /** -1 表示跟随全局设置 */
    private int editDayStart = -1;
    private int editCardMask = -1;
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

        content = findViewById(R.id.content);
        etTitle = findViewById(R.id.et_title);
        tvDate = findViewById(R.id.tv_date);
        tvLunar = findViewById(R.id.tv_lunar);
        tvElapsed = findViewById(R.id.tv_elapsed);
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
        btnDayStart = findViewById(R.id.btn_day_start_edit);
        tvMaskLabel = findViewById(R.id.tv_mask_label_edit);
        sliderMask = findViewById(R.id.slider_mask_edit);
        btnResetGlobal = findViewById(R.id.btn_reset_global_edit);
        bgUri = item.bgUri == null ? "" : item.bgUri;

        pickedDate = item.date > 0 ? item.date : CountdownTime.today();
        editDayStart = item.dayStartHour < 0 ? -1 : item.dayStartHour;
        editCardMask = item.cardMask < 0 ? -1 : item.cardMask;
        bindDayStartMask();
        btnDayStart.setOnClickListener(x -> pickDayStart());
        sliderMask.addOnChangeListener((sl, value, fromUser) -> {
            int mv = (int) value;
            tvMaskLabel.setText("蒙版强度：" + mv + "%");
            if (fromUser) editCardMask = mv;
        });
        btnResetGlobal.setOnClickListener(x -> {
            editDayStart = -1;
            editCardMask = -1;
            bindDayStartMask();
            Toast.makeText(this, "已恢复跟随全局设置", Toast.LENGTH_SHORT).show();
        });

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

        findViewById(R.id.row_date).setOnClickListener(v -> {
            press(v);
            pickDate();
        });
        findViewById(R.id.row_lunar).setOnClickListener(v -> {
            press(v);
            pickLunarDate();
        });
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
            press(v);
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

        // 进场：整页内容淡入上浮，标题先到、其余稍后，避免生硬弹出
        content.setAlpha(0f);
        content.setTranslationY(dp(14));
        content.animate().alpha(1f).translationY(0f)
                .setDuration(260)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    /** 轻按反馈：按下微缩后回弹。 */
    private void press(View v) {
        if (v == null) return;
        v.animate().scaleX(0.985f).scaleY(0.985f).setDuration(70).start();
        v.postDelayed(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(140)
                .setInterpolator(new DecelerateInterpolator()).start(), 80);
    }

    /** 公历选择器：年 / 月 / 日三列滚动，交互与农历选择器保持一致。 */
    private void pickDate() {
        int[] a = CountdownTime.ymd(pickedDate);
        final android.widget.NumberPicker npY = new android.widget.NumberPicker(this);
        npY.setMinValue(1900);
        npY.setMaxValue(2100);
        npY.setValue(a[0]);
        npY.setFormatter(v -> v + "年");
        final android.widget.NumberPicker npM = new android.widget.NumberPicker(this);
        npM.setMinValue(1);
        npM.setMaxValue(12);
        npM.setValue(a[1]);
        npM.setFormatter(v -> v + "月");
        final android.widget.NumberPicker npD = new android.widget.NumberPicker(this);
        npD.setMinValue(1);
        npD.setMaxValue(31);
        npD.setFormatter(v -> v + "日");
        final Runnable syncD = () -> {
            int max = CountdownTime.daysInMonth(npY.getValue(), npM.getValue());
            if (max < 1) max = 31;
            int cur = npD.getValue();
            npD.setDisplayedValues(null);
            npD.setMinValue(1);
            npD.setMaxValue(max);
            npD.setValue(Math.min(cur, max));
        };
        syncD.run();
        npD.setValue(Math.min(a[2], npD.getMaxValue()));
        npY.setOnValueChangedListener((p, o, nv) -> syncD.run());
        npM.setOnValueChangedListener((p, o, nv) -> syncD.run());
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        int pad = (int) dp(20);
        box.setPadding(pad, (int) dp(8), pad, 0);
        box.addView(npY, new android.widget.LinearLayout.LayoutParams(0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(npM, new android.widget.LinearLayout.LayoutParams(0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(npD, new android.widget.LinearLayout.LayoutParams(0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        new MaterialAlertDialogBuilder(this)
                .setTitle("按公历选择")
                .setView(box)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (d, w) -> {
                    pickedDate = CountdownTime.millisOf(npY.getValue(), npM.getValue(), npD.getValue());
                    refreshDateText();
                })
                .show();
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
        if (lunar.startsWith("农历")) lunar = lunar.substring(2);
        setRow(tvDate, CountdownTime.formatDate(pickedDate), 0);
        setRow(tvLunar, lunar.isEmpty() ? "—" : lunar, 45);
        setRow(tvElapsed, tail, 90);
    }
    /** 只有内容真的变了才重放动画，避免重复刷新时闪烁。 */
    private void setRow(TextView v, String text, long delay) {
        if (v == null) return;
        if (text.contentEquals(v.getText())) return;
        v.setText(text);
        fadeIn(v, delay);
    }
    /** 文本轻微上浮淡入；delay 让三行自上而下错峰，像水波一样铺开。 */
    private void fadeIn(View v, long delay) {
        if (v == null || !v.isLaidOut()) return;
        v.animate().cancel();
        v.setAlpha(0f);
        v.setTranslationY(dp(8));
        v.setScaleX(0.985f);
        v.setScaleY(0.985f);
        v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setStartDelay(delay)
                .setDuration(260)
                .setInterpolator(new PathInterpolator(0.22f, 0f, 0f, 1f))
                .start();
    }
    /** 自建农历选择器：年 / 月（含闰月）/ 日三级联动，确认后换算成公历时间戳。 */
    private void pickLunarDate() {
        LunarUtil.LunarDate cur = LunarUtil.toLunar(pickedDate);
        final int year0 = cur == null ? 2000 : cur.year;
        final int month0 = cur == null ? 1 : cur.month;
        final int day0 = cur == null ? 1 : cur.day;
        final boolean leap0 = cur != null && cur.leap;

        final android.widget.NumberPicker npY = new android.widget.NumberPicker(this);
        npY.setMinValue(1900);
        npY.setMaxValue(2100);
        npY.setValue(year0);
        npY.setFormatter(v -> v + " 年");

        final android.widget.NumberPicker npM = new android.widget.NumberPicker(this);
        final android.widget.NumberPicker npD = new android.widget.NumberPicker(this);
        npD.setMinValue(1);
        npD.setMaxValue(30);
        npD.setFormatter(v -> LunarUtil.dayName(v));

        final int[] monthNums = new int[13];
        final boolean[] monthLeaps = new boolean[13];
        final int[] count = {0};
        final int[] selM = {0};

        final Runnable[] syncM = new Runnable[1];
        final Runnable[] syncD = new Runnable[1];

        syncM[0] = () -> {
            int y = npY.getValue();
            int leap = LunarUtil.leapMonthOf(y);
            int keepMonth = count[0] > 0 ? monthNums[Math.min(selM[0], count[0] - 1)] : month0;
            boolean keepLeap = count[0] > 0 && monthLeaps[Math.min(selM[0], count[0] - 1)];
            int n = 0;
            String[] labels = new String[13];
            for (int m = 1; m <= 12; m++) {
                monthNums[n] = m;
                monthLeaps[n] = false;
                labels[n] = LunarUtil.monthName(m);
                n++;
                if (leap == m) {
                    monthNums[n] = m;
                    monthLeaps[n] = true;
                    labels[n] = "闰" + LunarUtil.monthName(m);
                    n++;
                }
            }
            count[0] = n;
            String[] shown = new String[n];
            System.arraycopy(labels, 0, shown, 0, n);
            int idx = 0;
            for (int i = 0; i < n; i++) {
                if (monthNums[i] == keepMonth && monthLeaps[i] == keepLeap) {
                    idx = i;
                    break;
                }
            }
            npM.setDisplayedValues(null);
            npM.setMinValue(0);
            npM.setMaxValue(n - 1);
            npM.setDisplayedValues(shown);
            npM.setValue(idx);
            selM[0] = idx;
        };

        syncD[0] = () -> {
            int y = npY.getValue();
            int i = Math.min(selM[0], Math.max(count[0] - 1, 0));
            int max = LunarUtil.daysOfMonth(y, monthNums[i], monthLeaps[i]);
            if (max < 1) max = 30;
            int cur2 = npD.getValue();
            npD.setDisplayedValues(null);
            npD.setMinValue(1);
            npD.setMaxValue(max);
            npD.setValue(Math.min(cur2, max));
        };

        syncM[0].run();
        int idx0 = 0;
        for (int i = 0; i < count[0]; i++) {
            if (monthNums[i] == month0 && monthLeaps[i] == leap0) {
                idx0 = i;
                break;
            }
        }
        npM.setValue(idx0);
        selM[0] = idx0;
        syncD[0].run();
        npD.setValue(Math.min(day0, npD.getMaxValue()));

        npY.setOnValueChangedListener((p, o, nv) -> {
            syncM[0].run();
            syncD[0].run();
        });
        npM.setOnValueChangedListener((p, o, nv) -> {
            selM[0] = nv;
            syncD[0].run();
        });

        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        int pad = (int) dp(20);
        box.setPadding(pad, (int) dp(8), pad, 0);
        android.widget.LinearLayout.LayoutParams lp =
                new android.widget.LinearLayout.LayoutParams(0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        box.addView(npY, lp);
        box.addView(npM, new android.widget.LinearLayout.LayoutParams(0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f));
        box.addView(npD, new android.widget.LinearLayout.LayoutParams(0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        new MaterialAlertDialogBuilder(this)
                .setTitle("按农历选择")
                .setView(box)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (d, w) -> {
                    int y = npY.getValue();
                    int i = selM[0];
                    long t = LunarUtil.fromLunar(y, monthNums[i], npD.getValue(), monthLeaps[i]);
                    if (t <= 0) {
                        Toast.makeText(this, "这个农历日期不存在", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    pickedDate = t;
                    refreshDateText();
                })
                .show();
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
        syncVisibility(content != null && content.isLaidOut());
    }

    private void syncVisibility(boolean animate) {
        boolean up = rgMode.getCheckedRadioButtonId() == R.id.rb_up;
        boolean annivOn = up && rgAnniv.getCheckedRadioButtonId() != R.id.rb_anniv_none;
        boolean custom = up && rgAnniv.getCheckedRadioButtonId() == R.id.rb_anniv_days;

        if (animate && content instanceof ViewGroup) {
            AutoTransition t = new AutoTransition();
            t.setOrdering(TransitionSet.ORDERING_TOGETHER);
            t.setDuration(220);
            t.setInterpolator(new DecelerateInterpolator());
            TransitionManager.beginDelayedTransition((ViewGroup) content, t);
        }
        labelAnniv.setVisibility(up ? View.VISIBLE : View.GONE);
        rgAnniv.setVisibility(up ? View.VISIBLE : View.GONE);
        groupCustomDays.setVisibility(custom ? View.VISIBLE : View.GONE);
        etAnnivMessage.setVisibility(annivOn ? View.VISIBLE : View.GONE);
        labelRepeat.setVisibility(up ? View.GONE : View.VISIBLE);
        rgRepeat.setVisibility(up ? View.GONE : View.VISIBLE);
    }

    /** 把日切时间与蒙版强度的当前值刷到界面（-1 时从全局取缺省）。 */
    private void bindDayStartMask() {
        int h = editDayStart < 0 ? CountdownPrefs.getDayStartHour(this) : editDayStart;
        btnDayStart.setText("日切时间：" + h + " 点");
        int m = editCardMask < 0 ? CountdownPrefs.getCardMask(this) : editCardMask;
        m = Math.round(m / 5f) * 5;
        if (m < 0) m = 0;
        if (m > 80) m = 80;
        tvMaskLabel.setText("蒙版强度：" + m + "%");
        sliderMask.setValue(m);
    }

    /** 选择这条倒数日的日切时间（0~6 点）。 */
    private void pickDayStart() {
        int cur = editDayStart < 0 ? CountdownPrefs.getDayStartHour(this) : editDayStart;
        new MaterialAlertDialogBuilder(this)
                .setTitle("日切时间（几点算新的一天）")
                .setSingleChoiceItems(new String[]{"0 点（自然日）", "1 点", "2 点", "3 点",
                        "4 点", "5 点", "6 点"}, cur, (d, which) -> {
                    editDayStart = which;
                    btnDayStart.setText("日切时间：" + which + " 点");
                    d.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
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
        // 不设置纪念日时，卡片不该再出现自定义文案：直接清空，避免残留
        item.annivMessage = item.annivType == Countdown.ANNIV_NONE
                ? "" : etAnnivMessage.getText().toString().trim();
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
        item.dayStartHour = editDayStart;
        item.cardMask = editCardMask;
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
        b.append(bgUri == null ? "" : bgUri).append('\u0001');
        b.append(editDayStart).append('\u0001');
        b.append(editCardMask);
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
