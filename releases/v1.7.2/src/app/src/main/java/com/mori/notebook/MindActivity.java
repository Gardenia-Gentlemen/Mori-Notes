package com.mori.notebook;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class MindActivity extends BaseActivity {

    private Note note;
    private NoteStore store;
    private List<MindNode> roots = new ArrayList<>();
    private MindAdapter adapter;
    private MindMapView mindMap;
    private RecyclerView recycler;
    private View scrollMap;
    private boolean mapMode = false;
    private Bitmap pendingBitmap;
    private ActivityResultLauncher<Intent> exportLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mind);
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
        roots = MindNode.parse(note.content);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_mind);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_view) {
                toggleView();
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
            return false;
        });

        recycler = findViewById(R.id.recycler);
        scrollMap = findViewById(R.id.scroll_map);
        mindMap = findViewById(R.id.mindmap);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MindAdapter(new MindAdapter.Listener() {
            @Override
            public void onClick(MindNode node) {
                editNode(node);
            }

            @Override
            public void onLongClick(MindNode node) {
                new MaterialAlertDialogBuilder(MindActivity.this)
                        .setTitle(node.text == null || node.text.trim().isEmpty() ? "空节点" : node.text)
                        .setItems(new String[]{"编辑文字", "添加子节点", "添加同级节点", "删除节点"}, (d, which) -> {
                            if (which == 0) editNode(node);
                            else if (which == 1) addChild(node);
                            else if (which == 2) addSibling(node);
                            else confirmDelete(node);
                        })
                        .show();
            }

            @Override
            public void onToggle(MindNode node) {
            }
        });
        recycler.setAdapter(adapter);
        adapter.setData(roots);
        mindMap.setRoots(roots);
        mindMap.setOnNodeTapListener(this::showNodeMenu);
        mapMode = false;
        recycler.setVisibility(View.VISIBLE);
        scrollMap.setVisibility(View.GONE);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            EditText et = input("新节点");
            new MaterialAlertDialogBuilder(this)
                    .setTitle("添加根节点")
                    .setView(et)
                    .setPositiveButton("确定", (d, w) -> {
                        String text = et.getText().toString().trim();
                        if (text.isEmpty()) return;
                        MindNode n = new MindNode();
                        n.text = text;
                        roots.add(n);
                        refresh();
                        saveNote();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    private void refresh() {
        adapter.setData(roots);
        mindMap.setRoots(roots);
    }

    private void toggleView() {
        mapMode = !mapMode;
        mindMap.setRoots(roots);
        recycler.setVisibility(mapMode ? View.GONE : View.VISIBLE);
        scrollMap.setVisibility(mapMode ? View.VISIBLE : View.GONE);
        Toast.makeText(this, mapMode ? "脑图视图" : "大纲视图", Toast.LENGTH_SHORT).show();
    }

    private void showNodeMenu(final MindNode node) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(node.text == null || node.text.trim().isEmpty() ? "空节点" : node.text)
                .setItems(new String[]{"编辑文字", "添加子节点", "添加同级节点", "删除节点"}, (d, which) -> {
                    if (which == 0) editNode(node);
                    else if (which == 1) addChild(node);
                    else if (which == 2) addSibling(node);
                    else confirmDelete(node);
                })
                .show();
    }

    private MindNode findParent(List<MindNode> list, MindNode target) {
        for (MindNode n : list) {
            for (MindNode c : n.children) {
                if (c == target) return n;
            }
            MindNode p = findParent(n.children, target);
            if (p != null) return p;
        }
        return null;
    }

    private void addSibling(final MindNode node) {
        final MindNode parent = findParent(roots, node);
        if (parent == null) {
            Toast.makeText(this, "根节点没有同级节点", Toast.LENGTH_SHORT).show();
            return;
        }
        EditText et = input("同级节点内容");
        new MaterialAlertDialogBuilder(this)
                .setTitle("添加同级节点")
                .setView(et)
                .setPositiveButton("确定", (d, w) -> {
                    String text = et.getText().toString().trim();
                    if (text.isEmpty()) return;
                    MindNode n = new MindNode();
                    n.text = text;
                    int idx = parent.children.indexOf(node);
                    if (idx < 0 || idx >= parent.children.size()) parent.children.add(n);
                    else parent.children.add(idx + 1, n);
                    refresh();
                    saveNote();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showExportDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("导出")
                .setItems(new String[]{"导出为图片（PNG）"}, (d, which) -> exportImage())
                .show();
    }

    private void exportImage() {
        try {
            Bitmap bmp = mindMap.drawToBitmap(1080);
            String name = note.title == null || note.title.trim().isEmpty() ? "思维笔记" : note.title.trim();
            name = name.replaceAll("[\\\\/:*?\"<>|]", "_");
            if (name.length() > 30) name = name.substring(0, 30);
            String path = ExportUtils.saveBitmap(this, name, bmp);
            Toast.makeText(this, "已导出到 " + path, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private EditText input(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        et.setPadding(pad, pad, pad, pad);
        return et;
    }

    private void editNode(MindNode node) {
        EditText et = input("节点内容");
        et.setText(node.text);
        new MaterialAlertDialogBuilder(this)
                .setTitle("编辑节点")
                .setView(et)
                .setPositiveButton("确定", (d, w) -> {
                    node.text = et.getText().toString().trim();
                    refresh();
                    saveNote();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void addChild(MindNode parent) {
        EditText et = input("子节点内容");
        new MaterialAlertDialogBuilder(this)
                .setTitle("添加子节点")
                .setView(et)
                .setPositiveButton("确定", (d, w) -> {
                    String text = et.getText().toString().trim();
                    if (text.isEmpty()) return;
                    MindNode n = new MindNode();
                    n.text = text;
                    parent.children.add(n);
                    refresh();
                    saveNote();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmDelete(final MindNode node) {
        new MaterialAlertDialogBuilder(this)
                .setMessage("删除该节点及其子节点？")
                .setPositiveButton("删除", (d, w) -> {
                    removeNode(roots, node);
                    refresh();
                    saveNote();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private boolean removeNode(List<MindNode> list, MindNode target) {
        if (list.remove(target)) return true;
        for (MindNode n : list) {
            if (removeNode(n.children, target)) return true;
        }
        return false;
    }

    private void saveNote() {
        note.content = MindNode.serialize(roots);
        note.updatedAt = System.currentTimeMillis();
        note.title = roots.isEmpty() || roots.get(0).text.trim().isEmpty() ? "思维笔记" : roots.get(0).text.trim();
        store.put(note);
    }

    @Override
    protected void onPause() {
        saveNote();
        super.onPause();
    }
}
