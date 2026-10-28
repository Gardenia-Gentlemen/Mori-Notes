package com.mori.notebook;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/** 主页：笔记列表、多选、下拉进隐私笔记、外部 .mori 打开 */
public class HomeFragment extends Fragment {

    private NoteStore store;
    private NoteAdapter adapter;
    private RecyclerView recycler;
    private TextView emptyView;
    /** 多选模式 */
    private boolean selecting;
    /** 下拉进入隐私笔记 */
    private float pullDownY;
    private boolean pullFromTop;
    private static final int PULL_THRESHOLD_DP = 72;
    /** 已处理过的外部文件 Uri，避免重复导入 */
    private Uri lastOpenedUri;
    private boolean toolbarReady;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        store = NoteStore.get(requireContext());
        recycler = view.findViewById(R.id.recycler);
        emptyView = view.findViewById(R.id.tv_empty);
        adapter = new NoteAdapter(new NoteAdapter.Listener() {
            @Override
            public void onClick(Note note) {
                open(note);
            }

            @Override
            public void onLongClick(Note note) {
                showItemMenu(note);
            }

            @Override
            public void onSelectionChanged(int count) {
                updateSelectTitle();
            }

            @Override
            public void onSelectionLongClick() {
                showSelectionMenu();
            }
        });
        recycler.setAdapter(adapter);
        applyLayoutMode();
        attachPull(recycler);
        View root = requireActivity().findViewById(R.id.root);
        attachPull(root);

        FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle("新建笔记")
                .setItems(new String[]{"普通笔记", "思维笔记"}, (d, which) -> {
                    Note n = new Note();
                    n.type = which == 1 ? 1 : 0;
                    n.content = which == 1 ? MindNode.miroTemplate() : "";
                    store.put(n);
                    open(n);
                })
                .show());
        toolbarReady = true;
        applyToolbar();
    }

    /** 顶栏菜单随多选状态切换 */
    public void applyToolbar() {
        if (!isAdded() || getView() == null) return;
        MaterialToolbar tb = requireActivity().findViewById(R.id.toolbar);
        if (tb == null) return;
        tb.getMenu().clear();
        tb.inflateMenu(selecting ? R.menu.menu_select : R.menu.menu_main);
        tb.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_grid) {
                toggleGrid();
                return true;
            }
            if (item.getItemId() == R.id.action_trash) {
                startActivity(new Intent(requireContext(), TrashActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.action_settings) {
                startActivity(new Intent(requireContext(), SettingsActivity.class));
                return true;
            }
            return handleSelectAction(item.getItemId());
        });
        if (selecting) {
            tb.getMenu().findItem(R.id.action_sel_normal).setVisible(false);
            tb.getMenu().findItem(R.id.action_select).setVisible(false);
            updateSelectTitle();
        } else {
            tb.setTitle(R.string.app_name);
        }
        setNavVisible(!selecting);
    }

    private void setNavVisible(boolean visible) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavVisible(visible);
        }
    }

    /** 主页顶部向下拖超过阈值 → 进入隐私笔记 */
    private void attachPull(View v) {
        if (v == null) return;
        final int threshold = (int) (PULL_THRESHOLD_DP * getResources().getDisplayMetrics().density);
        v.setOnTouchListener((view, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    pullDownY = e.getY();
                    pullFromTop = recycler != null && !recycler.canScrollVertically(-1);
                    break;
                case MotionEvent.ACTION_UP:
                    if (pullFromTop && e.getY() - pullDownY > threshold) {
                        pullFromTop = false;
                        startActivity(new Intent(requireContext(), PrivateNotesActivity.class));
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    pullFromTop = false;
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    private void toggleGrid() {
        boolean grid = !UiPrefs.isGridMode(requireContext());
        UiPrefs.setGridMode(requireContext(), grid);
        applyLayoutMode();
        toast(grid ? "已切换为宫格模式" : "已切换为列表模式");
    }

    private void applyLayoutMode() {
        if (adapter == null || recycler == null) return;
        boolean grid = UiPrefs.isGridMode(requireContext());
        adapter.setGrid(grid);
        recycler.setLayoutManager(grid
                ? new GridLayoutManager(requireContext(), 2)
                : new LinearLayoutManager(requireContext()));
    }

    private void open(Note note) {
        Intent i = new Intent(requireContext(),
                note.type == 1 ? MindActivity.class : NoteEditActivity.class);
        i.putExtra("note_id", note.id);
        startActivity(i);
    }

    private void showItemMenu(Note note) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(note.title == null || note.title.trim().isEmpty() ? "无标题" : note.title)
                .setItems(new String[]{"重命名", "设为隐私笔记", "分享图片", "删除", "多选"}, (d, which) -> {
                    if (which == 0) {
                        android.widget.EditText et = new android.widget.EditText(requireContext());
                        et.setText(note.title);
                        int pad = (int) (20 * getResources().getDisplayMetrics().density);
                        et.setPadding(pad, pad, pad, pad);
                        new MaterialAlertDialogBuilder(requireContext())
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
                        Toast.makeText(requireContext(), "已设为隐私笔记，主页下拉可进入",
                                Toast.LENGTH_LONG).show();
                    } else if (which == 2) {
                        ShareUtils.shareAsImage(requireActivity(), note);
                    } else if (which == 3) {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setMessage("删除这条笔记？将移入回收站，10 天后自动销毁。")
                                .setPositiveButton("删除", (dd, w) -> {
                                    store.moveToTrash(note.id);
                                    reload();
                                    toast("已移入回收站");
                                })
                                .setNegativeButton("取消", null)
                                .show();
                    } else {
                        enterSelection();
                        adapter.toggleSelection(note);
                    }
                })
                .show();
    }

    // ================= 多选 =================

    private boolean handleSelectAction(int id) {
        if (id == R.id.action_select) {
            enterSelection();
            return true;
        }
        if (id == R.id.action_select_all) {
            if (adapter.selectedCount() >= adapter.getItemCount()) adapter.clearSelection();
            else adapter.selectAll();
            return true;
        }
        if (id == R.id.action_sel_cancel) {
            exitSelection();
            return true;
        }
        if (id == R.id.action_sel_private) {
            batchSetPrivate();
            return true;
        }
        if (id == R.id.action_sel_share) {
            batchShare();
            return true;
        }
        if (id == R.id.action_sel_delete) {
            batchDelete();
            return true;
        }
        return false;
    }

    private void enterSelection() {
        selecting = true;
        adapter.setSelectionMode(true);
        applyToolbar();
    }

    private void exitSelection() {
        selecting = false;
        adapter.setSelectionMode(false);
        applyToolbar();
    }

    private void updateSelectTitle() {
        if (!selecting || !isAdded()) return;
        MaterialToolbar tb = requireActivity().findViewById(R.id.toolbar);
        if (tb != null) tb.setTitle("已选 " + adapter.selectedCount() + " 项");
    }

    /** 多选态长按：分享 / 删除 */
    private void showSelectionMenu() {
        if (adapter.selectedCount() == 0) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("已选 " + adapter.selectedCount() + " 项")
                .setItems(new String[]{"分享为图片", "删除所选"}, (d, which) -> {
                    if (which == 0) batchShare();
                    else batchDelete();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void batchShare() {
        final List<Note> notes = adapter.selectedNotes();
        if (notes == null || notes.isEmpty()) {
            toast("请先选择笔记");
            return;
        }
        ShareUtils.shareNotesAsImage(requireContext(), notes);
        exitSelection();
    }

    private void batchSetPrivate() {
        final List<String> ids = adapter.selectedIds();
        if (ids.isEmpty()) {
            toast("请先选择笔记");
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage("把选中的 " + ids.size() + " 条笔记设为隐私笔记？它们将从主页消失，下拉进入隐私笔记查看。")
                .setPositiveButton("确定", (d, w) -> {
                    int n = store.setPrivateBatch(ids, true);
                    exitSelection();
                    reload();
                    toast("已设为隐私笔记 " + n + " 条");
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void batchDelete() {
        final List<String> ids = adapter.selectedIds();
        if (ids.isEmpty()) {
            toast("请先选择笔记");
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage("删除选中的 " + ids.size() + " 条笔记？将移入回收站，10 天后自动销毁。")
                .setPositiveButton("删除", (d, w) -> {
                    int n = store.moveToTrashBatch(ids);
                    exitSelection();
                    reload();
                    toast("已移入回收站 " + n + " 条");
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 返回键：先退出多选，返回是否已消费 */
    public boolean handleBack() {
        if (selecting) {
            exitSelection();
            return true;
        }
        return false;
    }

    private void reload() {
        if (store == null || adapter == null) return;
        List<Note> notes = store.getNotes();
        adapter.setData(notes);
        if (emptyView != null) emptyView.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** 处理「用本软件打开 .mori 文件」 */
    public void handleOpenUri(Uri uri) {
        if (uri == null) return;
        if (uri.equals(lastOpenedUri)) return;
        lastOpenedUri = uri;
        MoriImporter.handle(requireActivity(), uri, this::reload);
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
        applyLayoutMode();
        applyToolbar();
        if (getActivity() instanceof MainActivity) {
            handleOpenUri(((MainActivity) getActivity()).consumePendingUri());
        }
    }

    private void toast(String s) {
        if (isAdded()) Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show();
    }
}