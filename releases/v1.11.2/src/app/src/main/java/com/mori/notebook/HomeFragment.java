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

import java.util.ArrayList;
import java.util.List;

/** 主页：笔记列表、多选、下拉进隐私笔记、外部 .mori 打开 */
public class HomeFragment extends Fragment {

    private NoteStore store;
    private NoteAdapter adapter;
    private RecyclerView recycler;
    private TextView emptyView;
    private TextView emptySub;
    private View emptyBox;
    /** 多选模式 */
    private boolean selecting;
    /** 下拉进入隐私笔记 */
    private float pullDownY;
    private boolean pullFromTop;
    private static final int PULL_THRESHOLD_DP = 72;
    /** 已处理过的外部文件 Uri，避免重复导入 */
    private Uri lastOpenedUri;
    private boolean toolbarReady;
    /** 搜索关键词（空 = 不搜索） */
    private String query = "";

    private static final String[] SORT_NAMES = {"按更新时间", "按创建时间", "按标题"};

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
        emptySub = view.findViewById(R.id.tv_empty_sub);
        emptyBox = view.findViewById(R.id.empty_view);
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
        recycler.setHasFixedSize(true);
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
        recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 8) fab.hide();
                else if (dy < -8) fab.show();
            }
        });
        toolbarReady = true;
        applyToolbar();
    }

    /** 顶栏菜单随多选状态切换 */
    public void applyToolbar() {
        if (getActivity() instanceof MainActivity
                && !((MainActivity) getActivity()).isCurrentPage(this)) {
            return;
        }
        if (!isAdded() || getView() == null) return;
        MaterialToolbar tb = requireActivity().findViewById(R.id.toolbar);
        if (tb == null) return;
        tb.getMenu().clear();
        tb.inflateMenu(selecting ? R.menu.menu_select : R.menu.menu_main);
        tb.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_grid) {
                toggleGrid();
                return true;
            }
            if (id == R.id.action_sort) {
                showSortDialog();
                return true;
            }
            if (id == R.id.action_tag_filter) {
                showTagFilterDialog();
                return true;
            }
            if (id == R.id.action_fav) {
                boolean v = !UiPrefs.isFavOnly(requireContext());
                UiPrefs.setFavOnly(requireContext(), v);
                reload();
                applyToolbar();
                toast(v ? "只看收藏" : "显示全部笔记");
                return true;
            }
            if (id == R.id.action_trash) {
                startActivity(new Intent(requireContext(), TrashActivity.class));
                return true;
            }
            if (id == R.id.action_settings) {
                startActivity(new Intent(requireContext(), SettingsActivity.class));
                return true;
            }
            return handleSelectAction(id);
        });
        if (selecting) {
            tb.getMenu().findItem(R.id.action_sel_normal).setVisible(false);
            tb.getMenu().findItem(R.id.action_select).setVisible(false);
            updateSelectTitle();
        } else {
            tb.setTitle(R.string.app_name);
            android.view.MenuItem fav = tb.getMenu().findItem(R.id.action_fav);
            if (fav != null) {
                fav.setIcon(UiPrefs.isFavOnly(requireContext())
                        ? R.drawable.ic_star : R.drawable.ic_star_border);
            }
        }
        setNavVisible(!selecting);
        attachSearch(tb);
    }

    private void showSortDialog() {
        int cur = UiPrefs.getSortMode(requireContext());
        String[] items = new String[SORT_NAMES.length];
        for (int i = 0; i < SORT_NAMES.length; i++) {
            items[i] = (i == cur ? "✓ " : "") + SORT_NAMES[i];
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("排序方式")
                .setMessage("置顶笔记始终排在最前，组内按置顶时间倒序。")
                .setItems(items, (d, which) -> {
                    UiPrefs.setSortMode(requireContext(), which);
                    reload();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showTagFilterDialog() {
        final List<String> tags = store.allTags();
        final List<String> items = new ArrayList<>();
        items.add("全部标签");
        items.addAll(tags);
        items.add("管理标签…");
        final int manageIndex = items.size() - 1;
        String cur = UiPrefs.getTagFilter(requireContext());
        String[] arr = new String[items.size()];
        for (int i = 0; i < items.size(); i++) {
            String v = (i == 0 || i == manageIndex) ? "" : items.get(i);
            arr[i] = (v.equals(cur) ? "✓ " : "") + items.get(i);
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(tags.isEmpty() ? "还没有标签" : "按标签筛选")
                .setItems(arr, (d, which) -> {
                    if (which == manageIndex) {
                        manageTags();
                        return;
                    }
                    UiPrefs.setTagFilter(requireContext(), which == 0 ? "" : items.get(which));
                    reload();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 标签管理：列出全部标签与条数 */
    private void manageTags() {
        final List<String> tags = store.allTags();
        if (tags.isEmpty()) {
            toast("还没有标签");
            return;
        }
        java.util.Map<String, Integer> counts = store.tagCounts();
        String[] arr = new String[tags.size()];
        for (int i = 0; i < tags.size(); i++) {
            String t = tags.get(i);
            Integer c = counts.get(t);
            arr[i] = "#" + t + "  (" + (c == null ? 0 : c) + ")";
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("管理标签")
                .setItems(arr, (d, which) -> showTagActions(tags.get(which)))
                .setNegativeButton("关闭", null)
                .show();
    }

    private void showTagActions(final String tag) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("#" + tag)
                .setItems(new String[]{"重命名", "删除该标签"}, (d, which) -> {
                    if (which == 0) askRenameTag(tag);
                    else confirmRemoveTag(tag);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void askRenameTag(final String tag) {
        final android.widget.EditText et = new android.widget.EditText(requireContext());
        et.setText(tag);
        et.setSingleLine(true);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        et.setPadding(pad, pad, pad, pad);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("重命名标签")
                .setView(et)
                .setPositiveButton("确定", (d, w) -> {
                    String to = NoteStore.normalizeTag(et.getText().toString());
                    if (to.isEmpty()) {
                        toast("标签不能为空");
                        return;
                    }
                    if (to.equals(tag)) return;
                    int n = store.renameTag(tag, to);
                    if (tag.equals(UiPrefs.getTagFilter(requireContext()))) {
                        UiPrefs.setTagFilter(requireContext(), to);
                    }
                    reload();
                    toast("已重命名，影响 " + n + " 条");
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmRemoveTag(final String tag) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage("从所有笔记中删除标签 #" + tag + "？笔记本身不会被删除。")
                .setPositiveButton("删除", (d, w) -> {
                    int n = store.removeTag(tag);
                    if (tag.equals(UiPrefs.getTagFilter(requireContext()))) {
                        UiPrefs.setTagFilter(requireContext(), "");
                    }
                    reload();
                    toast("已从 " + n + " 条笔记移除");
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 顶部搜索框：关键词实时过滤列表 */
    private void attachSearch(MaterialToolbar tb) {
        android.view.MenuItem item = tb.getMenu().findItem(R.id.action_search);
        if (item == null) return;
        android.view.View av = item.getActionView();
        if (!(av instanceof androidx.appcompat.widget.SearchView)) return;
        androidx.appcompat.widget.SearchView sv = (androidx.appcompat.widget.SearchView) av;
        sv.setQuery(query, false);
        if (!query.isEmpty()) {
            sv.setIconified(false);
            item.expandActionView();
        }
        sv.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                query = s == null ? "" : s.trim();
                reload();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                query = s == null ? "" : s.trim();
                reload();
                return true;
            }
        });
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

    private Boolean layoutGridApplied;

    private void applyLayoutMode() {
        if (adapter == null || recycler == null) return;
        boolean grid = UiPrefs.isGridMode(requireContext());
        adapter.setGrid(grid);
        if (recycler.getLayoutManager() != null
                && layoutGridApplied != null && layoutGridApplied == grid) return;
        layoutGridApplied = grid;
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

    private void showItemMenu(final Note note) {
        final String[] items = {
                note.isPinned() ? "取消置顶" : "置顶",
                note.starred ? "取消收藏" : "收藏",
                "标签",
                "重命名",
                "设为隐私笔记",
                "分享图片",
                "删除",
                "多选"
        };
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(note.title == null || note.title.trim().isEmpty() ? "无标题" : note.title)
                .setItems(items, (d, which) -> {
                    switch (which) {
                        case 0:
                            store.setPinned(note.id, !note.isPinned());
                            reload();
                            toast(note.isPinned() ? "已置顶" : "已取消置顶");
                            break;
                        case 1:
                            store.setStarred(note.id, !note.starred);
                            reload();
                            toast(note.starred ? "已收藏" : "已取消收藏");
                            break;
                        case 2:
                            editTags(note);
                            break;
                        case 3:
                            rename(note);
                            break;
                        case 4:
                            store.setPrivate(note.id, true);
                            reload();
                            Toast.makeText(requireContext(), "已设为隐私笔记，主页下拉可进入",
                                    Toast.LENGTH_LONG).show();
                            break;
                        case 5:
                            ShareUtils.shareAsImage(requireActivity(), note);
                            break;
                        case 6:
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setMessage("删除这条笔记？将移入回收站，10 天后自动销毁。")
                                    .setPositiveButton("删除", (dd, w) -> {
                                        store.moveToTrash(note.id);
                                        reload();
                                        toast("已移入回收站");
                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
                            break;
                        default:
                            enterSelection();
                            adapter.toggleSelection(note);
                            break;
                    }
                })
                .show();
    }

    private void rename(final Note note) {
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
    }

    /** 编辑标签：空格分隔多个 */
    private void editTags(final Note note) {
        android.widget.EditText et = new android.widget.EditText(requireContext());
        StringBuilder sb = new StringBuilder();
        if (note.tags != null) {
            for (String t : note.tags) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(t);
            }
        }
        et.setText(sb.toString());
        et.setHint("多个标签用空格分隔");
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        et.setPadding(pad, pad, pad, pad);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("标签")
                .setView(et)
                .setPositiveButton("确定", (dd, w) -> {
                    List<String> tags = new ArrayList<>();
                    String[] parts = et.getText().toString().trim().split("\\s+");
                    for (String p : parts) {
                        if (!p.trim().isEmpty()) tags.add(p);
                    }
                    store.setTags(note.id, tags);
                    reload();
                    toast(tags.isEmpty() ? "已清空标签" : "已保存标签");
                })
                .setNegativeButton("取消", null)
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
        List<Note> notes = query.isEmpty() ? store.getNotes() : store.search(query);
        String tag = UiPrefs.getTagFilter(requireContext());
        boolean favOnly = UiPrefs.isFavOnly(requireContext());
        if (!tag.isEmpty() || favOnly) {
            List<Note> filtered = new ArrayList<>();
            for (Note n : notes) {
                if (favOnly && !n.starred) continue;
                if (!tag.isEmpty() && (n.tags == null || !n.tags.contains(tag))) continue;
                filtered.add(n);
            }
            notes = filtered;
        }
        adapter.setData(notes);
        if (emptyBox != null) emptyBox.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
        if (emptyView != null) {
            if (!query.isEmpty()) {
                emptyView.setText("没有匹配「" + query + "」的笔记");
            } else if (!tag.isEmpty()) {
                emptyView.setText("没有标签为 #" + tag + " 的笔记");
            } else if (favOnly) {
                emptyView.setText("还没有收藏的笔记");
            } else {
                emptyView.setText(getString(R.string.empty_hint));
            }
            if (emptySub != null) {
                boolean clean = query.isEmpty() && tag.isEmpty() && !favOnly;
                emptySub.setVisibility(clean ? View.VISIBLE : View.GONE);
            }
        }
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
