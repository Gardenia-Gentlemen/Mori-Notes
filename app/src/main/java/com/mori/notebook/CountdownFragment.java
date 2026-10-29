/*
 * 倒数日页。
 *
 * 参考自 TimeTrace (https://github.com/KIPPUDESU/KIPPU_Trace)
 * Copyright (c) 2026 切符
 * 依据 MIT License 使用，许可全文见项目根目录 LICENSE-KIPPU_Trace。
 */
package com.mori.notebook;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/** 倒数日页：列表、点开时间轴、长按拖动排序、多选删除与分享 */
public class CountdownFragment extends Fragment {

    private CountdownStore store;
    /** 当前等待导入字体的倒数日（来自卡片三点菜单）。 */
    private Countdown pendingFontItem;
    private CountdownAdapter adapter;
    private ItemTouchHelper dragHelper;
    private RecyclerView recycler;
    private View emptyView;
    /** 是否处于多选态 */
    private boolean selecting;
    /** 字体文件选择器 */
    private ActivityResultLauncher<Intent> fontPicker;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_countdown, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        store = CountdownStore.get(requireContext());
        recycler = view.findViewById(R.id.recycler_countdown);
        emptyView = view.findViewById(R.id.empty_countdown);
        fontPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), r -> {
                    if (r.getResultCode() != android.app.Activity.RESULT_OK || r.getData() == null) {
                        return;
                    }
                    Uri src = r.getData().getData();
                    if (src == null || !isAdded()) {
                        return;
                    }
                    Countdown target = pendingFontItem;
                    pendingFontItem = null;
                    if (target != null) {
                        String path = FontImporter.importTo(requireContext(), src);
                        if (path != null) {
                            target.fontPath = path;
                            store.put(target);
                            store.reload();
                            reload();
                            toast("字体已应用到该条");
                        }
                    } else if (FontImporter.importFrom(requireContext(), src)) {
                        reload();
                    }
                });
        adapter = new CountdownAdapter(new CountdownAdapter.Listener() {
            @Override
            public void onClick(Countdown item) {
                if (selecting) {
                    adapter.toggleSelection(item);
                    refreshTitle();
                } else {
                    openTimeline(item);
                }
            }

            @Override
            public void onLongClick(Countdown item) {
                // 长按卡片：进入多选并选中该条；多选态下弹操作菜单
                if (!selecting) {
                    enterSelection(item);
                } else {
                    showSelectionMenu();
                }
            }

            @Override
            public void onEdit(Countdown item) {
                open(item);
            }
            @Override
            public void onImportFont(Countdown item) {
                showFontMenu(item);
            }
            @Override
            public void onDragStart(RecyclerView.ViewHolder vh) {
                if (dragHelper != null && !selecting) {
                    dragHelper.startDrag(vh);
                }
            }

            @Override
            public void onSelectionChanged() {
                refreshTitle();
            }
        });
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
        recycler.setHasFixedSize(true);
        attachDragSort();
        FloatingActionButton fab = view.findViewById(R.id.fab_countdown);
        fab.setOnClickListener(v -> create());
        applyToolbar();
        reload();
    }

    /** 顶栏菜单：普通态为新建/多选/设置，多选态为全选/分享/删除/退出 */
    public void applyToolbar() {
        if (getActivity() instanceof MainActivity
                && !((MainActivity) getActivity()).isCurrentPage(this)) {
            return;
        }
        if (!isAdded() || getView() == null) {
            return;
        }
        MaterialToolbar tb = requireActivity().findViewById(R.id.toolbar);
        if (tb == null) {
            return;
        }
        tb.getMenu().clear();
        if (selecting) {
            tb.inflateMenu(R.menu.menu_countdown_select);
            tb.setTitle(selectTitle());
        } else {
            tb.inflateMenu(R.menu.menu_countdown);
            tb.setTitle("倒数日");
        }
        tb.setOnMenuItemClickListener(item -> handleMenu(item.getItemId()));
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavVisible(!selecting);
        }
    }

    private boolean handleMenu(int id) {
        if (id == R.id.action_add_countdown) {
            create();
            return true;
        }
        if (id == R.id.action_select) {
            enterSelection(null);
            return true;
        }
        if (id == R.id.action_save_image) {
            saveAllImage();
            return true;
        }
        if (id == R.id.action_share_image) {
            shareAllImage();
            return true;
        }
        if (id == R.id.action_fonts) {
            startActivity(new Intent(requireContext(), FontsActivity.class));
            return true;
        }
        if (id == R.id.action_countdown_trash) {
            startActivity(new Intent(requireContext(), CountdownTrashActivity.class));
            return true;
        }
        if (id == R.id.action_settings) {
            startActivity(new Intent(requireContext(), SettingsActivity.class));
            return true;
        }
        if (id == R.id.action_select_all) {
            selectAll();
            return true;
        }
        if (id == R.id.action_sel_save) {
            batchSave();
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
        if (id == R.id.action_sel_cancel) {
            exitSelection();
            return true;
        }
        return false;
    }

    private String selectTitle() {
        int n = adapter == null ? 0 : adapter.selectedCount();
        return "已选 " + n + " 项";
    }

    private void enterSelection(Countdown seed) {
        if (adapter == null) {
            return;
        }
        selecting = true;
        adapter.setSelectionMode(true);
        if (seed != null) {
            adapter.toggleSelection(seed);
        }
        applyToolbar();
    }

    private void exitSelection() {
        selecting = false;
        if (adapter != null) {
            adapter.setSelectionMode(false);
        }
        applyToolbar();
    }

    private void refreshTitle() {
        if (!selecting) {
            return;
        }
        MaterialToolbar tb = requireActivity().findViewById(R.id.toolbar);
        if (tb != null) {
            tb.setTitle(selectTitle());
        }
    }

    private void selectAll() {
        if (adapter == null) {
            return;
        }
        adapter.selectAll();
        refreshTitle();
    }

    /** 多选态长按：弹操作菜单 */
    private void showSelectionMenu() {
        if (adapter == null) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(selectTitle())
                .setItems(new String[]{"全选", "保存所选", "分享所选", "删除所选"}, (d, which) -> {
                    if (which == 0) {
                        selectAll();
                    } else if (which == 1) {
                        batchSave();
                    } else if (which == 2) {
                        batchShare();
                    } else {
                        batchDelete();
                    }
                })
                .show();
    }

    /** 主页：把全部倒数日卡片保存为图片 */
    private void saveAllImage() {
        if (store == null) {
            return;
        }
        List<Countdown> all = store.getAll();
        if (all == null || all.isEmpty()) {
            toast("还没有倒数日");
            return;
        }
        ShareUtils.saveCountdownsImage(requireContext(), all);
    }

    /** 主页：把全部倒数日卡片分享为一张长图 */
    private void shareAllImage() {
        if (store == null) {
            return;
        }
        List<Countdown> all = store.getAll();
        if (all == null || all.isEmpty()) {
            toast("还没有倒数日");
            return;
        }
        ShareUtils.shareCountdownsAsImage(requireContext(), all);
    }

    /** 保存所选倒数日卡片为图片 */
    private void batchSave() {
        if (adapter == null) {
            return;
        }
        List<Countdown> sel = adapter.selectedCountdowns();
        if (sel.isEmpty()) {
            toast("请选择倒数日");
            return;
        }
        ShareUtils.saveCountdownsImage(requireContext(), sel);
        exitSelection();
    }

    /** 分享所选倒数日为一张长图 */
    private void batchShare() {
        if (adapter == null) {
            return;
        }
        List<Countdown> sel = adapter.selectedCountdowns();
        if (sel.isEmpty()) {
            toast("请选择倒数日");
            return;
        }
        ShareUtils.shareCountdownsAsImage(requireContext(), sel);
        exitSelection();
    }

    /** 删除所选 */
    private void batchDelete() {
        if (adapter == null || store == null) {
            return;
        }
        final List<String> ids = adapter.selectedIds();
        if (ids.isEmpty()) {
            toast("请选择倒数日");
            return;
        }
        final int n = ids.size();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("删除 " + n + " 个倒数日")
                .setMessage("将移入回收站，可在回收站中恢复。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) -> {
                    store.deleteBatch(ids);
                    exitSelection();
                    reload();
                    toast("已删除 " + n + " 个");
                })
                .show();
    }

    private void create() {
        startActivity(new Intent(requireContext(), CountdownEditActivity.class));
    }

    /** 打开时间轴页（与旧版一致：点卡片进时间轴，页内菜单可进全屏） */
    private void openTimeline(Countdown item) {
        Intent i = new Intent(requireContext(), CountdownTimelineActivity.class);
        i.putExtra("countdown_id", item.id);
        startActivity(i);
    }

    private void openFullScreen(Countdown item) {
        Intent i = new Intent(requireContext(), CountdownFullScreenActivity.class);
        i.putExtra("countdown_id", item.id);
        startActivity(i);
    }

    private void open(Countdown item) {
        Intent i = new Intent(requireContext(), CountdownEditActivity.class);
        i.putExtra("countdown_id", item.id);
        startActivity(i);
    }

    /** 长按拖动排序：只允许在同一分组内交换卡片；多选态禁用 */
    private void attachDragSort() {
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean isLongPressDragEnabled() {
                // 卡片长按已用于进入多选，拖拽改由长按右侧三点按钮触发
                return false;
            }
            @Override
            public int getMovementFlags(@NonNull RecyclerView rv,
                                        @NonNull RecyclerView.ViewHolder vh) {
                if (selecting || adapter.itemAt(vh.getBindingAdapterPosition()) == null) {
                    return 0;
                }
                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = vh.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                if (!adapter.canMove(from, to)) {
                    return false;
                }
                adapter.moveRow(from, to);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
            }

            @Override
            public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                super.clearView(rv, vh);
                if (store != null && adapter != null) {
                    store.saveOrder(adapter.orderedItems());
                }
            }
        });
        dragHelper = helper;
        helper.attachToRecyclerView(recycler);
    }

    public void reload() {
        if (store == null || adapter == null) {
            return;
        }
        adapter.setData(store.getAll());
        if (emptyView != null) {
            emptyView.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (selecting) {
            refreshTitle();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (selecting) {
            exitSelection();
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        if (store != null) {
            store.reload();
        }
        reload();
        applyToolbar();
    }

    /** 选择该条倒数日的字体：跟随系统默认、从字体库选（选项以字体预览展示）、或导入新字体。 */
    private void showFontMenu(final Countdown item) {
        FontChooser.show(requireContext(), FontImporter.list(requireContext()), item.fontPath, item.title,
                new FontChooser.Callback() {
                    @Override
                    public void onPick(String fontPath) {
                        item.fontPath = fontPath;
                        store.put(item);
                        store.reload();
                        reload();
                        toast(fontPath.isEmpty() ? "已恢复默认字体" : "字体已更新");
                    }

                    @Override
                    public void onImport() {
                        pendingFontItem = item;
                        pickFont();
                    }
                });
    }
    /** 选择本地字体文件（ttf / otf / ttc）并导入，列表卡片立即刷新。 */
    private void pickFont() {
        if (fontPicker == null) {
            toast("字体选择器不可用");
            return;
        }
        try {
            fontPicker.launch(FontImporter.pickIntent());
        } catch (Exception e) {
            toast("无法打开文件选择器");
        }
    }

    private void toast(String s) {
        if (isAdded()) {
            Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show();
        }
    }
}