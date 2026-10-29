package com.mori.notebook;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TrashActivity extends BaseActivity {

    private NoteStore store;
    private RecyclerView recycler;
    private TextView tvEmpty;
    private Adapter adapter;
    private MaterialToolbar toolbar;
    private final List<Note> data = new ArrayList<>();
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);
        store = NoteStore.get(this);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_trash);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_clear_trash) {
                confirmClearTrash();
                return true;
            }
            return false;
        });

        recycler = findViewById(R.id.recycler);
        tvEmpty = findViewById(R.id.tv_empty);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setHasFixedSize(true);
        adapter = new Adapter();
        recycler.setAdapter(adapter);
        reload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        store.purgeExpired();
        data.clear();
        data.addAll(store.getTrash());
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
        recycler.setVisibility(data.isEmpty() ? View.GONE : View.VISIBLE);
        android.view.MenuItem clear = toolbar.getMenu().findItem(R.id.action_clear_trash);
        if (clear != null) clear.setVisible(!data.isEmpty());
    }

    private void confirmClearTrash() {
        final int count = data.size();
        if (count == 0) {
            Toast.makeText(this, "回收站已经是空的", Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("清空回收站")
                .setMessage("将彻底删除回收站中的 " + count + " 条笔记，删除后无法恢复。")
                .setPositiveButton("清空", (d, w) -> {
                    int removed = store.clearTrash();
                    reload();
                    Toast.makeText(this, "已清空 " + removed + " 条", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showMenu(Note note) {
        String[] items = {"恢复", "彻底删除"};
        new MaterialAlertDialogBuilder(this)
                .setTitle(note.title)
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        store.restore(note.id);
                        reload();
                        Toast.makeText(this, "已恢复", Toast.LENGTH_SHORT).show();
                    } else {
                        confirmDeleteForever(note);
                    }
                })
                .show();
    }

    private void confirmDeleteForever(Note note) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("彻底删除")
                .setMessage("删除后无法恢复，确定删除这条笔记？")
                .setPositiveButton("删除", (d, w) -> {
                    store.deleteForever(note.id);
                    reload();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.VH> {

        class VH extends RecyclerView.ViewHolder {
            TextView title, type, preview, meta;

            VH(View v) {
                super(v);
                title = v.findViewById(R.id.tv_title);
                type = v.findViewById(R.id.tv_type);
                preview = v.findViewById(R.id.tv_preview);
                meta = v.findViewById(R.id.tv_meta);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_trash, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Note n = data.get(position);
            h.title.setText(n.title == null || n.title.trim().isEmpty() ? "无标题" : n.title);
            h.type.setText(n.type == 1 ? "思维" : "文档");
            if (n.type == 1) {
                h.preview.setText("思维笔记");
            } else {
                String text = n.content == null ? "" : n.content.replaceAll("[#*>`\\-]", " ").replaceAll("\\s+", " ").trim();
                h.preview.setText(text.isEmpty() ? "（空）" : text);
            }
            long remain = store.trashRemainMs(n);
            long days = (remain + 24L * 60 * 60 * 1000 - 1) / (24L * 60 * 60 * 1000);
            h.meta.setText("删除于 " + fmt.format(new Date(n.deletedAt)) + " · 剩余 " + days + " 天");
            h.itemView.setOnClickListener(v -> showMenu(n));
            h.itemView.setOnLongClickListener(v -> {
                showMenu(n);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
