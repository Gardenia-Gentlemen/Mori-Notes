package com.mori.notebook;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * 隐私笔记。入口只有一个：主页面下拉。
 * 首次进入需设置数字密码或使用手机锁屏密码，之后每次进入都要验证。
 */
public class PrivateNotesActivity extends BaseActivity {

    private NoteStore store;
    private NoteAdapter adapter;
    private RecyclerView recycler;
    private TextView emptyView;
    private View lockPanel;
    private TextView tvLockTitle;
    private MaterialButton btnUnlock;
    private FloatingActionButton fab;

    private boolean unlocked;
    private boolean authenticating;
    private ActivityResultLauncher<Intent> deviceLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private);
        store = NoteStore.get(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recycler = findViewById(R.id.recycler);
        emptyView = findViewById(R.id.tv_empty);
        lockPanel = findViewById(R.id.lock_panel);
        tvLockTitle = findViewById(R.id.tv_lock_title);
        btnUnlock = findViewById(R.id.btn_unlock);
        fab = findViewById(R.id.fab);

        adapter = new NoteAdapter(new NoteAdapter.Listener() {
            @Override
            public void onClick(Note note) {
                open(note);
            }

            @Override
            public void onLongClick(Note note) {
                showItemMenu(note);
            }
        });
        recycler.setAdapter(adapter);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        fab.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("新建隐私笔记")
                .setItems(new String[]{"普通笔记", "思维笔记"}, (d, which) -> {
                    Note n = new Note();
                    n.type = which == 1 ? 1 : 0;
                    n.content = which == 1 ? MindNode.miroTemplate() : "";
                    n.isPrivate = true;
                    store.put(n);
                    open(n);
                })
                .show());

        deviceLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    authenticating = false;
                    if (result.getResultCode() == RESULT_OK) {
                        unlockSuccess();
                    } else {
                        Toast.makeText(this, "未通过验证", Toast.LENGTH_SHORT).show();
                    }
                });

        btnUnlock.setOnClickListener(v -> requestUnlock());

        if (PrivacyPrefs.isConfigured(this)) {
            showLocked();
        } else {
            tvLockTitle.setText("先设置隐私笔记密码");
            showLocked();
            showSetupDialog();
        }
    }

    private void showLocked() {
        unlocked = false;
        lockPanel.setVisibility(View.VISIBLE);
        recycler.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        fab.setVisibility(View.GONE);
        adapter.setData(new java.util.ArrayList<>());
        btnUnlock.setText(PrivacyPrefs.isDeviceMode(this) ? "使用手机锁屏密码解锁" : "输入密码解锁");
    }

    private void unlockSuccess() {
        unlocked = true;
        lockPanel.setVisibility(View.GONE);
        recycler.setVisibility(View.VISIBLE);
        fab.setVisibility(View.VISIBLE);
        reload();
    }

    private void reload() {
        List<Note> notes = store.getPrivateNotes();
        adapter.setData(notes);
        emptyView.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (unlocked) {
            reload();
        }
    }

    /** 首次进入：设置密码或使用手机锁屏密码 */
    private void showSetupDialog() {
        boolean deviceOk = PrivacyPrefs.hasDeviceCredential(this);
        String[] items = deviceOk
                ? new String[]{"设置数字密码", "使用手机锁屏密码"}
                : new String[]{"设置数字密码"};
        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(this)
                .setTitle("设置隐私笔记密码")
                .setItems(items, (d, which) -> {
                    if (which == 1) {
                        PrivacyPrefs.useDeviceCredential(this);
                        Toast.makeText(this, "已使用手机锁屏密码", Toast.LENGTH_SHORT).show();
                        requestUnlock();
                    } else {
                        askNewPin(this::unlockSuccess);
                    }
                })
                .setCancelable(false);
        if (!deviceOk) {
            b.setMessage("本机没有设置锁屏密码，只能使用数字密码");
        }
        b.show();
    }

    private void requestUnlock() {
        if (!PrivacyPrefs.isConfigured(this)) {
            showSetupDialog();
            return;
        }
        if (PrivacyPrefs.isDeviceMode(this)) {
            Intent i = PrivacyPrefs.deviceCredentialIntent(this, "解锁隐私笔记", "验证手机锁屏密码");
            if (i == null) {
                Toast.makeText(this, "本机未设置锁屏密码，请在设置里重设隐私密码", Toast.LENGTH_LONG).show();
                return;
            }
            authenticating = true;
            try {
                deviceLauncher.launch(i);
            } catch (Exception e) {
                authenticating = false;
                Toast.makeText(this, "无法打开系统验证：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            askPin();
        }
    }

    private void askPin() {
        EditText et = pinInput("隐私密码");
        new MaterialAlertDialogBuilder(this)
                .setTitle("输入隐私密码")
                .setView(wrap(et))
                .setPositiveButton("解锁", (d, w) -> {
                    if (PrivacyPrefs.verifyPin(this, et.getText().toString().trim())) {
                        unlockSuccess();
                    } else {
                        Toast.makeText(this, "密码错误", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 设置新密码：两次输入 */
    private void askNewPin(Runnable done) {
        EditText p1 = pinInput("新密码（4 位以上数字）");
        EditText p2 = pinInput("再次输入");
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        box.setPadding(pad, dp(8), pad, 0);
        box.addView(p1);
        box.addView(p2);
        new MaterialAlertDialogBuilder(this)
                .setTitle("设置隐私笔记密码")
                .setView(box)
                .setPositiveButton("确定", (d, w) -> {
                    String a = p1.getText().toString().trim();
                    String c = p2.getText().toString().trim();
                    if (a.length() < 4) {
                        Toast.makeText(this, "密码至少 4 位数字", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!a.equals(c)) {
                        Toast.makeText(this, "两次输入不一致", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    PrivacyPrefs.setPin(this, a);
                    Toast.makeText(this, "隐私密码已设置", Toast.LENGTH_SHORT).show();
                    done.run();
                })
                .setNegativeButton("取消", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    private void open(Note note) {
        Intent i = new Intent(this, note.type == 1 ? MindActivity.class : NoteEditActivity.class);
        i.putExtra("note_id", note.id);
        startActivity(i);
    }

    private void showItemMenu(Note note) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(note.title == null || note.title.trim().isEmpty() ? "无标题" : note.title)
                .setItems(new String[]{"重命名", "移出隐私笔记", "分享图片", "删除"}, (d, which) -> {
                    if (which == 0) {
                        EditText et = new EditText(this);
                        et.setText(note.title);
                        int pad = dp(20);
                        et.setPadding(pad, pad, pad, pad);
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("重命名")
                                .setView(et)
                                .setPositiveButton("确定", (dd, w) -> {
                                    note.title = et.getText().toString().trim();
                                    note.updatedAt = System.currentTimeMillis();
                                    store.put(note);
                                    reload();
                                })
                                .setNegativeButton("取消", null)
                                .show();
                    } else if (which == 1) {
                        store.setPrivate(note.id, false);
                        reload();
                        Toast.makeText(this, "已移出隐私笔记", Toast.LENGTH_SHORT).show();
                    } else if (which == 2) {
                        ShareUtils.shareAsImage(this, note);
                    } else {
                        new MaterialAlertDialogBuilder(this)
                                .setMessage("删除这条笔记？将移入回收站，10 天后自动销毁。")
                                .setPositiveButton("删除", (dd, w) -> {
                                    store.moveToTrash(note.id);
                                    reload();
                                    Toast.makeText(this, "已移入回收站", Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("取消", null)
                                .show();
                    }
                })
                .show();
    }

    private EditText pinInput(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setSingleLine(true);
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        return et;
    }

    private View wrap(View v) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        box.setPadding(pad, dp(8), pad, 0);
        box.addView(v);
        return box;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}