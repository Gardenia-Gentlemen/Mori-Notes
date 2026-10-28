package com.mori.notebook;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends BaseActivity {
    private NoteStore store;
    private NoteAdapter adapter;
    private RecyclerView recycler;
    private TextView emptyView;
    /** 下拉进入隐私笔记 */
    private float pullDownY;
    private boolean pullFromTop;
    private static final int PULL_THRESHOLD_DP = 72;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        store = NoteStore.get(this);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_grid) {
                toggleGrid();
                return true;
            }
            if (item.getItemId() == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.action_trash) {
                startActivity(new Intent(this, TrashActivity.class));
                return true;
            }
            return false;
        });
        recycler = findViewById(R.id.recycler);
        emptyView = findViewById(R.id.tv_empty);
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
        applyLayoutMode();
        attachPull(findViewById(R.id.recycler));
        attachPull(findViewById(R.id.root));

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("新建笔记")
                .setItems(new String[]{"普通笔记", "思维笔记"}, (d, which) -> {
                    Note n = new Note();
                    n.type = which == 1 ? 1 : 0;
                    n.content = which == 1 ? MindNode.miroTemplate() : "";
                    store.put(n);
                    open(n);
                })
                .show());
    }

    /** 在主列表顶部向下拖超过阈值 → 进入隐私笔记 */
    private void attachPull(View v) {
        if (v == null) return;
        final int threshold = (int) (PULL_THRESHOLD_DP * getResources().getDisplayMetrics().density);
        v.setOnTouchListener((view, e) -> {
            switch (e.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    pullDownY = e.getY();
                    pullFromTop = !recycler.canScrollVertically(-1);
                    break;
                case android.view.MotionEvent.ACTION_UP:
                    if (pullFromTop && e.getY() - pullDownY > threshold) {
                        pullFromTop = false;
                        startActivity(new Intent(this, PrivateNotesActivity.class));
                    }
                    break;
                case android.view.MotionEvent.ACTION_CANCEL:
                    pullFromTop = false;
                    break;
                default:
                    break;
            }
            return false;
        });
    }
    private void toggleGrid() {
        boolean grid = !UiPrefs.isGridMode(this);
        UiPrefs.setGridMode(this, grid);
        applyLayoutMode();
        Toast.makeText(this, grid ? "已切换为宫格模式" : "已切换为列表模式", Toast.LENGTH_SHORT).show();
    }

    private void applyLayoutMode() {
        boolean grid = UiPrefs.isGridMode(this);
        adapter.setGrid(grid);
        recycler.setLayoutManager(grid
                ? new GridLayoutManager(this, 2)
                : new LinearLayoutManager(this));
    }

    private void open(Note note) {
        Intent i = new Intent(this, note.type == 1 ? MindActivity.class : NoteEditActivity.class);
        i.putExtra("note_id", note.id);
        startActivity(i);
    }

    private void showItemMenu(Note note) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(note.title == null || note.title.trim().isEmpty() ? "无标题" : note.title)
                .setItems(new String[]{"重命名", "设为隐私笔记", "分享图片", "删除"}, (d, which) -> {
                    if (which == 0) {
                        android.widget.EditText et = new android.widget.EditText(this);
                        et.setText(note.title);
                        int pad = (int) (20 * getResources().getDisplayMetrics().density);
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
                        store.setPrivate(note.id, true);
                        reload();
                        Toast.makeText(this, "已设为隐私笔记，主页下拉可进入", Toast.LENGTH_LONG).show();
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

    private void reload() {
        List<Note> notes = store.getNotes();
        adapter.setData(notes);
        emptyView.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
        applyLayoutMode();
    }
}
