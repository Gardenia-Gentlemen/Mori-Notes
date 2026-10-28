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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

public class SettingsFragment extends Fragment {

    private TextView tvLocation;
    private TextView tvBgName;
    private TextView tvPrivacyState;

    private ActivityResultLauncher<Intent> deviceLauncher;
    private ActivityResultLauncher<Uri> folderPicker;
    private ActivityResultLauncher<Intent> authLauncher;
    private ActivityResultLauncher<String[]> importLauncher;
    private Runnable pendingAuthAction;

    /** 导出：等用户在系统文件选择器里指定保存位置 */
    private ActivityResultLauncher<String> exportLauncher;
    private byte[] pendingExportBytes;
    private int pendingExportCount;

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
                        toast("已导出 " + count + " 条笔记到所选位置");
                    } catch (Exception e) {
                        toast("写入失败：" + e.getMessage());
                    }
                });

        folderPicker = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
            if (uri == null) return;
            try {
                requireContext().getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Exception ignored) {
            }
            NoteStore store = NoteStore.get(requireContext());
            boolean loaded = store.switchLocation(uri);
            if (tvLocation != null) tvLocation.setText(store.locationLabel());
            toast(loaded ? "已切换到该目录，加载了其中已有的笔记" : "笔记已保存到该目录");
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

        tvLocation = v.findViewById(R.id.tv_location);
        tvLocation.setText(NoteStore.get(requireContext()).locationLabel());

        TextView tvVer = v.findViewById(R.id.tv_version);
        try {
            android.content.pm.PackageInfo pi = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            tvVer.setText("版本 " + pi.versionName + "  (" + pi.versionCode + ")");
        } catch (Exception e) {
            tvVer.setText("版本未知");
        }

        MaterialButton pick = v.findViewById(R.id.btn_pick_folder);
        pick.setOnClickListener(x -> {
            try {
                folderPicker.launch(null);
            } catch (Exception e) {
                toast("无法打开文件夹选择器");
            }
        });

        tvPrivacyState = v.findViewById(R.id.tv_privacy_state);
        updatePrivacyState();

        MaterialButton btnResetPrivacy = v.findViewById(R.id.btn_reset_privacy);
        btnResetPrivacy.setOnClickListener(x -> startResetPassword());

        MaterialButton btnExportNotes = v.findViewById(R.id.btn_export_notes);
        btnExportNotes.setOnClickListener(x -> startExportMori());

        MaterialButton btnImportNotes = v.findViewById(R.id.btn_import_notes);
        btnImportNotes.setOnClickListener(x -> startImportMori());

        MaterialButton reset = v.findViewById(R.id.btn_reset_folder);
        reset.setOnClickListener(x -> {
            NoteStore store = NoteStore.get(requireContext());
            store.useInternal();
            tvLocation.setText(store.locationLabel());
            toast("已恢复为应用内部存储");
        });
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

    // ================= v1.5 整机笔记导出 / 导入 =================

    private void startExportMori() {
        String[] items = {"仅导出常规笔记", "仅导出隐私笔记", "全部导出"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("整机笔记导出")
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        askExportPassword(MoriFile.SCOPE_NORMAL);
                        return;
                    }
                    requirePrivacyAuth(() -> askExportPassword(
                            which == 1 ? MoriFile.SCOPE_PRIVATE : MoriFile.SCOPE_ALL));
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 导出隐私/全部前必须通过隐私密码认证 */
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

    /** 为导出文件设置 8 位数字密码（仅常规笔记导出时不加密） */
    private void askExportPassword(String scope) {
        if (MoriFile.SCOPE_NORMAL.equals(scope)) {
            doExportMori(scope, null);
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
                .setMessage("导出文件将用该密码加密，导入时需要输入")
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
                    doExportMori(scope, a);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void doExportMori(String scope, String pin) {
        NoteStore store = NoteStore.get(requireContext());
        java.util.List<Note> list = store.notesForScope(scope);
        if (list.isEmpty()) {
            toast("没有可导出的笔记");
            return;
        }
        boolean enc = !MoriFile.SCOPE_NORMAL.equals(scope);
        try {
            byte[] data = MoriFile.build(list, scope, enc, pin);
            String ts = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss",
                    java.util.Locale.US).format(new java.util.Date());
            String name = "Mori-notes-" + ts + MoriFile.EXT;
            pendingExportBytes = data;
            pendingExportCount = list.size();
            try {
                exportLauncher.launch(name);
            } catch (Exception noPicker) {
                String path = ExportUtils.saveBytes(requireContext(), name,
                        "application/octet-stream", data);
                pendingExportBytes = null;
                toast("已导出 " + list.size() + " 条笔记到 " + path);
            }
        } catch (Exception e) {
            toast("导出失败：" + e.getMessage());
        }
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

    private String percent(float v) {
        return Math.round(v * 100) + "%";
    }

    private void toast(String s) {
        if (isAdded()) Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show();
    }
}
