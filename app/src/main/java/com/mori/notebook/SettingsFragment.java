package com.mori.notebook;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

public class SettingsFragment extends Fragment {

    private TextView tvBgName;
    private TextView tvPrivacyState;

    private ActivityResultLauncher<String[]> cardBgLauncher;
    private ActivityResultLauncher<Intent> deviceLauncher;
    private ActivityResultLauncher<Intent> authLauncher;
    private ActivityResultLauncher<String[]> importLauncher;
    private Runnable pendingAuthAction;

    /** 导出：等用户在系统文件选择器里指定保存位置 */
    private ActivityResultLauncher<String> exportLauncher;
    private byte[] pendingExportBytes;
    private int pendingExportCount;
    private String pendingExportLabel = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    Runnable r = pendingAuthAction;
                    pendingAuthAction = null;
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && r != null) {
                        r.run();
                    } else {
                        toast("未通过验证，已取消导出");
                    }
                });

        importLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), uri -> {
                    if (uri != null) doImportMori(uri);
                });

        cardBgLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), uri -> {
                    if (uri == null) {
                        return;
                    }
                    try {
                        requireContext().getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignored) {
                    }
                    CountdownPrefs.setCardBg(requireContext(), uri);
                    CardBg.clear();
                    if (getView() != null) {
                        bindCountdownCard(getView());
                    }
                    toast("已设置卡片背景图");
                });
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/octet-stream"),
                uri -> {
                    byte[] data = pendingExportBytes;
                    int count = pendingExportCount;
                    pendingExportBytes = null;
                    if (uri == null || data == null) {
                        toast("已取消导出");
                        return;
                    }
                    try {
                        ExportUtils.writeBytes(requireContext(), uri, data);
                        toast("已导出 " + pendingExportLabel + " 到所选位置");
                    } catch (Exception e) {
                        toast("写入失败：" + e.getMessage());
                    }
                });


        deviceLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                        chooseNewPasswordMode();
                    } else {
                        toast("未通过验证，无法重置");
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        bindCountdownCard(v);
        RadioGroup rgStart = v.findViewById(R.id.rg_start_page);
        int sp = UiPrefs.getStartPage(requireContext());
        rgStart.check(sp == 0 ? R.id.radio_start_countdown
                : sp == 2 ? R.id.radio_start_todo : R.id.radio_start_notes);
        rgStart.setOnCheckedChangeListener((group, checkedId) -> {
            int page = checkedId == R.id.radio_start_countdown ? 0
                    : checkedId == R.id.radio_start_todo ? 2 : 1;
            if (page == UiPrefs.getStartPage(requireContext())) return;
            UiPrefs.setStartPage(requireContext(), page);
            toast("下次打开应用生效");
        });

        RadioGroup rg = v.findViewById(R.id.rg_theme);
        int mode = UiPrefs.getThemeMode(requireContext());
        rg.check(mode == 1 ? R.id.radio_light : mode == 2 ? R.id.radio_dark : R.id.radio_follow);
        rg.setOnCheckedChangeListener((group, checkedId) -> {
            int m = checkedId == R.id.radio_light ? 1 : checkedId == R.id.radio_dark ? 2 : 0;
            if (m == UiPrefs.getThemeMode(requireContext())) return;
            UiPrefs.setThemeMode(requireContext(), m);
            NotebookApp.applyTheme(requireContext());
            requireActivity().recreate();
        });

        tvBgName = v.findViewById(R.id.tv_bg_name);
        updateBgName();
        MaterialButton btnBg = v.findViewById(R.id.btn_background);
        btnBg.setOnClickListener(x -> {
            if (getActivity() instanceof BaseActivity) {
                ((BaseActivity) getActivity()).showBackgroundPicker();
            }
        });

        MaterialSwitch swBold = v.findViewById(R.id.sw_title_bold);
        swBold.setChecked(UiPrefs.isHomeTitleBold(requireContext()));
        swBold.setOnCheckedChangeListener((b, checked) ->
                UiPrefs.setHomeTitleBold(requireContext(), checked));

        final TextView tvValue = v.findViewById(R.id.tv_scale_value);
        Slider slider = v.findViewById(R.id.slider_scale);
        float cur = UiPrefs.getHomeTitleScale(requireContext());
        slider.setValue(Math.max(0.8f, Math.min(1.4f, cur)));
        tvValue.setText(percent(cur));
        slider.addOnChangeListener((s, value, fromUser) -> {
            UiPrefs.setHomeTitleScale(requireContext(), value);
            tvValue.setText(percent(value));
        });


        TextView tvVer = v.findViewById(R.id.tv_version);
        tvVer.setText(versionLabel());
        View cardAbout = v.findViewById(R.id.card_about);
        cardAbout.setOnClickListener(x -> showAboutDialog());
        MaterialButton btnLic = v.findViewById(R.id.btn_licenses);
        btnLic.setOnClickListener(x -> showOpenSourceLicense());


        tvPrivacyState = v.findViewById(R.id.tv_privacy_state);
        updatePrivacyState();

        MaterialButton btnResetPrivacy = v.findViewById(R.id.btn_reset_privacy);
        btnResetPrivacy.setOnClickListener(x -> startResetPassword());

        MaterialButton btnExportNotes = v.findViewById(R.id.btn_export_notes);
        btnExportNotes.setOnClickListener(x -> startExportMori());

        MaterialButton btnImportNotes = v.findViewById(R.id.btn_import_notes);
        btnImportNotes.setOnClickListener(x -> startImportMori());

    }

    @Override
    public void onResume() {
        super.onResume();
        updateBgName();
        updatePrivacyState();
    }

    private void updatePrivacyState() {
        if (tvPrivacyState == null || !isAdded()) return;
        String mode;
        if (!PrivacyPrefs.isConfigured(requireContext())) {
            mode = "未设置";
        } else if (PrivacyPrefs.isDeviceMode(requireContext())) {
            mode = "手机锁屏密码";
        } else {
            mode = "数字密码";
        }
        tvPrivacyState.setText("当前方式：" + mode);
    }

    /** 重置密码：先验证旧密码或手机锁屏密码 */
    private void startResetPassword() {
        if (!PrivacyPrefs.isConfigured(requireContext())) {
            chooseNewPasswordMode();
            return;
        }
        if (PrivacyPrefs.isDeviceMode(requireContext())) {
            Intent i = PrivacyPrefs.deviceCredentialIntent(requireContext(),
                    "验证身份", "验证手机锁屏密码以重置隐私笔记密码");
            if (i == null) {
                toast("本机未设置锁屏密码，请用旧密码验证");
                askOldPin();
                return;
            }
            try {
                deviceLauncher.launch(i);
            } catch (Exception e) {
                toast("无法打开系统验证：" + e.getMessage());
            }
        } else {
            askOldPin();
        }
    }

    private void askOldPin() {
        final android.widget.EditText et = pinInput("旧密码");
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("验证旧密码")
                .setView(wrap(et))
                .setPositiveButton("下一步", (d, w) -> {
                    if (PrivacyPrefs.verifyPin(requireContext(), et.getText().toString().trim())) {
                        chooseNewPasswordMode();
                    } else {
                        toast("密码错误");
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void chooseNewPasswordMode() {
        boolean deviceOk = PrivacyPrefs.hasDeviceCredential(requireContext());
        String[] items = deviceOk
                ? new String[]{"设置新的数字密码", "改用手机锁屏密码"}
                : new String[]{"设置新的数字密码"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("重置隐私笔记密码")
                .setItems(items, (d, which) -> {
                    if (which == 1) {
                        PrivacyPrefs.useDeviceCredential(requireContext());
                        updatePrivacyState();
                        toast("已改为使用手机锁屏密码");
                    } else {
                        askNewPin();
                    }
                })
                .show();
    }

    private void askNewPin() {
        final android.widget.EditText p1 = pinInput("新密码（4 位以上数字）");
        final android.widget.EditText p2 = pinInput("再次输入");
        android.widget.LinearLayout box = new android.widget.LinearLayout(requireContext());
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(p1);
        box.addView(p2);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("设置新的数字密码")
                .setView(box)
                .setPositiveButton("确定", (d, w) -> {
                    String a = p1.getText().toString().trim();
                    String c = p2.getText().toString().trim();
                    if (a.length() < 4) {
                        toast("密码至少 4 位数字");
                        return;
                    }
                    if (!a.equals(c)) {
                        toast("两次输入不一致");
                        return;
                    }
                    PrivacyPrefs.setPin(requireContext(), a);
                    updatePrivacyState();
                    toast("隐私密码已更新");
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private android.widget.EditText pinInput(String hint) {
        android.widget.EditText et = new android.widget.EditText(requireContext());
        et.setHint(hint);
        et.setSingleLine(true);
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        return et;
    }

    private android.view.View wrap(android.view.View v) {
        android.widget.LinearLayout box = new android.widget.LinearLayout(requireContext());
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(v);
        return box;
    }

    // ================= v1.5 整机笔记导出 / 导入（v1.7：可选内容备份） =================
    /** 勾选要备份的内容 */
    private static class BackupSel {
        boolean notes;
        boolean priv;
        boolean countdowns;
        boolean todos;
        boolean fonts;
        boolean any() {
            return notes || priv || countdowns || todos || fonts;
        }
    }
    private void startExportMori() {
        final BackupSel sel = new BackupSel();
        android.widget.LinearLayout box = new android.widget.LinearLayout(requireContext());
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        final android.widget.CheckBox cbNotes = checkBox("常规笔记");
        final android.widget.CheckBox cbPriv = checkBox("隐私笔记（需验证密码）");
        final android.widget.CheckBox cbCd = checkBox("倒数日");
        final android.widget.CheckBox cbTodo = checkBox("待办");
        final android.widget.CheckBox cbFont = checkBox("字体库（自定义字体文件）");
        cbNotes.setChecked(true);
        cbFont.setChecked(true);
        box.addView(cbNotes);
        box.addView(cbPriv);
        box.addView(cbCd);
        box.addView(cbTodo);
        box.addView(cbFont);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("选择要备份的内容")
                .setView(box)
                .setPositiveButton("下一步", (d, w) -> {
                    sel.notes = cbNotes.isChecked();
                    sel.priv = cbPriv.isChecked();
                    sel.countdowns = cbCd.isChecked();
                    sel.todos = cbTodo.isChecked();
                    sel.fonts = cbFont.isChecked();
                    if (!sel.any()) {
                        toast("请至少选择一项内容");
                        return;
                    }
                    if (sel.priv) {
                        requirePrivacyAuth(() -> askExportPassword(sel));
                    } else {
                        askExportPassword(sel);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    private android.widget.CheckBox checkBox(String text) {
        android.widget.CheckBox cb = new android.widget.CheckBox(requireContext());
        cb.setText(text);
        int v = (int) (6 * getResources().getDisplayMetrics().density);
        cb.setPadding(0, v, 0, v);
        return cb;
    }
    /** 导出隐私笔记前必须通过隐私密码认证 */
    private void requirePrivacyAuth(Runnable next) {
        if (!PrivacyPrefs.isConfigured(requireContext())) {
            toast("请先在本页设置隐私笔记密码");
            return;
        }
        pendingAuthAction = next;
        if (PrivacyPrefs.isDeviceMode(requireContext())) {
            Intent i = PrivacyPrefs.deviceCredentialIntent(requireContext(),
                    "验证身份", "验证手机锁屏密码后才能导出隐私笔记");
            if (i == null) {
                askPrivacyPin(next);
                return;
            }
            try {
                authLauncher.launch(i);
            } catch (Exception e) {
                pendingAuthAction = null;
                askPrivacyPin(next);
            }
        } else {
            askPrivacyPin(next);
        }
    }
    private void askPrivacyPin(Runnable next) {
        final android.widget.EditText et = pinInput("隐私笔记密码");
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("验证隐私密码")
                .setView(wrap(et))
                .setPositiveButton("下一步", (d, w) -> {
                    if (PrivacyPrefs.verifyPin(requireContext(), et.getText().toString().trim())) {
                        next.run();
                    } else {
                        toast("密码错误");
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    /** 含隐私笔记时必须为文件设置 8 位数字密码；其余组合明文导出 */
    private void askExportPassword(BackupSel sel) {
        if (!willExportPrivate(sel)) {
            doExportMori(sel, null);
            return;
        }
        final android.widget.EditText p1 = pinInput("8 位数字密码");
        final android.widget.EditText p2 = pinInput("再次输入");
        android.widget.LinearLayout box = new android.widget.LinearLayout(requireContext());
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(p1);
        box.addView(p2);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("设置文件密码")
                .setMessage("备份含隐私笔记，文件将用该密码加密，导入时需要输入")
                .setView(box)
                .setPositiveButton("导出", (d, w) -> {
                    String a = p1.getText().toString().trim();
                    String c = p2.getText().toString().trim();
                    if (!MoriFile.isValidPin(a)) {
                        toast("密码必须是 8 位数字");
                        return;
                    }
                    if (!a.equals(c)) {
                        toast("两次输入不一致");
                        return;
                    }
                    doExportMori(sel, a);
                })
                .setNegativeButton("取消", null)
                .show();
    }
    /** 本次选择是否真的会带出隐私笔记（选了隐私但库里没有时不需要密码） */
    private boolean willExportPrivate(BackupSel sel) {
        if (!sel.priv) return false;
        for (Note n : NoteStore.get(requireContext()).notesForScope(MoriFile.SCOPE_ALL)) {
            if (n != null && n.isPrivate) return true;
        }
        return false;
    }

    private void doExportMori(BackupSel sel, String pin) {
        MoriFile.Content content = new MoriFile.Content();
        NoteStore store = NoteStore.get(requireContext());
        if (sel.notes && sel.priv) {
            content.notes.addAll(store.notesForScope(MoriFile.SCOPE_ALL));
        } else if (sel.priv) {
            content.notes.addAll(store.notesForScope(MoriFile.SCOPE_PRIVATE));
        } else if (sel.notes) {
            content.notes.addAll(store.notesForScope(MoriFile.SCOPE_NORMAL));
        }
        content.includesPrivate = sel.priv;
        if (sel.countdowns) {
            CountdownStore cds = CountdownStore.get(requireContext());
            cds.reload();
            content.countdowns.addAll(cds.getAll());
        }
        if (sel.todos) {
            TodoStore tds = TodoStore.get(requireContext());
            tds.reload();
            content.todos.addAll(tds.getActive());
            content.todos.addAll(tds.getDone());
        }
        if (sel.fonts) {
            for (java.io.File f : FontImporter.list(requireContext())) {
                byte[] bytes = readBytes(f);
                if (bytes != null && bytes.length > 0) {
                    content.fonts.add(new MoriFile.FontItem(f.getName(), bytes));
                }
            }
        }
        if (content.size() == 0) {
            toast("没有可导出的内容");
            return;
        }
        boolean enc = pin != null && sel.priv;
        String label = describe(content);
        try {
            byte[] data = MoriFile.build(content, MoriFile.SCOPE_CUSTOM, enc, pin);
            String ts = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss",
                    java.util.Locale.US).format(new java.util.Date());
            String name = "Mori-backup-" + ts + MoriFile.EXT;
            pendingExportBytes = data;
            pendingExportCount = content.size();
            pendingExportLabel = label;
            try {
                exportLauncher.launch(name);
            } catch (Exception noPicker) {
                String path = ExportUtils.saveBytes(requireContext(), name,
                        "application/octet-stream", data);
                pendingExportBytes = null;
                toast("已导出 " + label + " 到 " + path);
            }
        } catch (Exception e) {
            toast("导出失败：" + e.getMessage());
        }
    }
    /** 读取整机导出需要的字体文件字节；失败返回 null。 */
    private static byte[] readBytes(java.io.File f) {
        if (f == null || !f.isFile()) return null;
        try (java.io.FileInputStream in = new java.io.FileInputStream(f);
             java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            return bos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
    /** 备份内容摘要文案 */
    private static String describe(MoriFile.Content c) {
        StringBuilder sb = new StringBuilder();
        if (!c.notes.isEmpty()) sb.append(c.notes.size()).append(" 条笔记");
        if (!c.countdowns.isEmpty()) {
            if (sb.length() > 0) sb.append("、");
            sb.append(c.countdowns.size()).append(" 个倒数日");
        }
        if (!c.todos.isEmpty()) {
            if (sb.length() > 0) sb.append("、");
            sb.append(c.todos.size()).append(" 条待办");
        }
        if (!c.fonts.isEmpty()) {
            if (sb.length() > 0) sb.append("、");
            sb.append(c.fonts.size()).append(" 个字体");
        }
        return sb.toString();
    }
    private void startImportMori() {
        try {
            importLauncher.launch(new String[]{"application/octet-stream", "application/x-mori", "text/plain", "text/*"});
        } catch (Exception e) {
            toast("无法打开文件选择器");
        }
    }

    private void doImportMori(Uri uri) {
        MoriImporter.handle(requireActivity(), uri, null);
    }

    private void updateBgName() {
        if (tvBgName == null || !isAdded()) return;
        int style = BackgroundPrefs.getStyle(requireContext());
        String name = (style >= 0 && style < BackgroundPrefs.NAMES.length)
                ? BackgroundPrefs.NAMES[style] : BackgroundPrefs.NAMES[0];
        tvBgName.setText("当前：" + name);
    }

    private void showOpenSourceLicense() {
        String text;
        try {
            java.io.InputStream in = getResources().openRawResource(R.raw.kipu_trace_license);
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            in.close();
            text = new String(bos.toByteArray(), "UTF-8");
        } catch (Exception e) {
            text = "无法读取许可文本";
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("开源许可 · TimeTrace (KIPPU_Trace)")
                .setMessage(text)
                .setPositiveButton("关闭", null)
                .show();
    }

    private String percent(float v) {
        return Math.round(v * 100) + "%";
    }

    /** 倒数日卡片设置：日切时间 / 背景图 / 蒙版强度 */
    private void bindCountdownCard(View v) {
        MaterialButton day = v.findViewById(R.id.btn_day_start);
        MaterialButton bg = v.findViewById(R.id.btn_card_bg);
        MaterialButton clear = v.findViewById(R.id.btn_card_bg_clear);
        TextView mask = v.findViewById(R.id.tv_mask_label);
        Slider slider = v.findViewById(R.id.slider_card_mask);
        MaterialButton tzBtn = v.findViewById(R.id.btn_timezone);
        if (tzBtn != null) {
            tzBtn.setText("时区：" + timeZoneLabel());
            tzBtn.setOnClickListener(x -> showTimeZonePicker());
        }
        if (day == null || bg == null || clear == null || mask == null || slider == null) {
            return;
        }
        final int hour = CountdownPrefs.getDayStartHour(requireContext());
        day.setText("日切时间：" + hour + " 点");
        day.setOnClickListener(x -> pickDayStart(day));

        Uri cur = CountdownPrefs.getCardBg(requireContext());
        bg.setText(cur == null ? "卡片背景图：未设置" : "卡片背景图：已设置（点击更换）");
        bg.setOnClickListener(x -> cardBgLauncher.launch(new String[]{"image/*"}));
        clear.setVisibility(cur == null ? View.GONE : View.VISIBLE);
        clear.setOnClickListener(x -> {
            CountdownPrefs.setCardBg(requireContext(), null);
            CardBg.clear();
            bindCountdownCard(v);
            toast("已清除背景图");
        });

        int m = CountdownPrefs.getCardMask(requireContext());
        m = Math.round(m / 5f) * 5;
        if (m < 0) m = 0;
        if (m > 80) m = 80;
        mask.setText("蒙版强度：" + m + "%");
        slider.setValue(m);
        slider.addOnChangeListener((sl, value, fromUser) -> {
            int mv = (int) value;
            mask.setText("蒙版强度：" + mv + "%");
            CountdownPrefs.setCardMask(requireContext(), mv);
        });
    }

    /** 当前时区显示名。 */
    private String timeZoneLabel() {
        if (CountdownPrefs.isFollowSystem(requireContext())) {
            return "跟随系统（" + java.util.TimeZone.getDefault().getID() + "）";
        }
        return CountdownPrefs.resolveTimeZoneId(requireContext());
    }

    /** 时区选择：跟随系统 + 常用时区。 */
    private void showTimeZonePicker() {
        final java.util.List<String> zones = new java.util.ArrayList<>();
        zones.add("");
        java.util.Collections.addAll(zones, "Asia/Shanghai", "Asia/Hong_Kong", "Asia/Taipei",
                "Asia/Tokyo", "Asia/Seoul", "Asia/Singapore", "Asia/Bangkok", "Asia/Kolkata",
                "Asia/Dubai", "Europe/London", "Europe/Paris", "Europe/Berlin", "Europe/Moscow",
                "America/New_York", "America/Chicago", "America/Los_Angeles",
                "America/Sao_Paulo", "Australia/Sydney", "Pacific/Auckland", "UTC");
        final String cur = CountdownPrefs.getTimeZoneId(requireContext()).trim();
        if (!cur.isEmpty() && !zones.contains(cur)) zones.add(cur);
        String[] labels = new String[zones.size()];
        int checked = 0;
        for (int i = 0; i < zones.size(); i++) {
            String id = zones.get(i);
            if (id.isEmpty()) {
                labels[i] = "跟随系统（" + java.util.TimeZone.getDefault().getID() + "）";
            } else {
                labels[i] = id + "（" + offsetLabel(id) + "）";
            }
            if (id.equals(cur)) checked = i;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("时区")
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    CountdownPrefs.setTimeZoneId(requireContext(), zones.get(which));
                    d.dismiss();
                    View root = getView();
                    if (root != null) bindCountdownCard(root);
                    toast("时区已切换，软件内时间按新时区换算");
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** GMT±HH:mm 形式的偏移标签。 */
    private String offsetLabel(String id) {
        int off = java.util.TimeZone.getTimeZone(id).getOffset(System.currentTimeMillis()) / 60000;
        int abs = Math.abs(off);
        return String.format(java.util.Locale.CHINA, "GMT%s%02d:%02d",
                off < 0 ? "-" : "+", abs / 60, abs % 60);
    }

    private void pickDayStart(MaterialButton btn) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("日切时间（几点算新的一天）")
                .setSingleChoiceItems(new String[]{"0 点（自然日）", "1 点", "2 点", "3 点",
                        "4 点", "5 点", "6 点"}, CountdownPrefs.getDayStartHour(requireContext()),
                        (d, which) -> {
                            CountdownPrefs.setDayStartHour(requireContext(), which);
                            btn.setText("日切时间：" + which + " 点");
                            d.dismiss();
                        })
                .setNegativeButton("取消", null)
                .show();
    }

    private void toast(String s) {
        if (isAdded()) Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show();
    }

    private String versionLabel() {
        try {
            android.content.pm.PackageInfo pi = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            return "v" + pi.versionName;
        } catch (Exception e) {
            return "v未知";
        }
    }

    private void showAboutDialog() {
        View dv = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_about, null);
        TextView tv = dv.findViewById(R.id.tv_about_version);
        tv.setText(versionLabel());
        androidx.appcompat.app.AlertDialog dlg =
                new MaterialAlertDialogBuilder(requireContext()).setView(dv).create();
        dv.findViewById(R.id.btn_about_close).setOnClickListener(x -> dlg.dismiss());
        dlg.show();
    }
}
