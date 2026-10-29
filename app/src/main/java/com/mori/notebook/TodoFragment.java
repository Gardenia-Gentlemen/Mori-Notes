package com.mori.notebook;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/** 待办页：筛选、分组列表、勾选完成、左滑删除 */
public class TodoFragment extends Fragment {
    private static final int FILTER_ALL = 0;
    private static final int FILTER_TODAY = 1;
    private static final int FILTER_OVERDUE = 2;
    private static final int FILTER_DONE = 3;

    private TodoStore store;
    private TodoAdapter adapter;
    private RecyclerView recycler;
    private View emptyView;
    private TextView emptyTitle;
    private TextView emptySub;
    private MaterialButtonToggleGroup tgFilter;
    private int filter = FILTER_ALL;

    /** 滑动删除时绘制红色背景 */
    private Paint swipePaint;
    private final RectF swipeRect = new RectF();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_todo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        store = TodoStore.get(requireContext());
        recycler = view.findViewById(R.id.recycler_todo);
        emptyView = view.findViewById(R.id.empty_todo);
        emptyTitle = view.findViewById(R.id.tv_empty_title);
        emptySub = view.findViewById(R.id.tv_empty_sub);
        tgFilter = view.findViewById(R.id.tg_filter);
        tgFilter.check(R.id.btn_f_all);
        tgFilter.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            filter = checkedId == R.id.btn_f_today ? FILTER_TODAY
                    : checkedId == R.id.btn_f_overdue ? FILTER_OVERDUE
                    : checkedId == R.id.btn_f_done ? FILTER_DONE : FILTER_ALL;
            reload();
        });

        adapter = new TodoAdapter(new TodoAdapter.Listener() {
            @Override
            public void onToggle(Todo todo, boolean done) {
                store.setDone(todo.id, done);
                reload();
            }

            @Override
            public void onClick(Todo todo) {
                edit(todo);
            }
        });
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
        recycler.setHasFixedSize(true);
        attachSwipe();
        FloatingActionButton fab = view.findViewById(R.id.fab_todo);
        fab.setOnClickListener(v -> startActivity(new Intent(requireContext(), TodoEditActivity.class)));
        applyToolbar();
        reload();
    }

    /** 顶栏菜单（清除已完成） */
    public void applyToolbar() {
        if (getActivity() instanceof MainActivity
                && !((MainActivity) getActivity()).isCurrentPage(this)) {
            return;
        }
        if (!isAdded() || getView() == null) return;
        MaterialToolbar tb = requireActivity().findViewById(R.id.toolbar);
        if (tb == null) return;
        tb.getMenu().clear();
        tb.inflateMenu(R.menu.menu_todo);
        tb.setTitle("待办");
        tb.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_clear_done) {
                confirmClearDone();
                return true;
            }
            if (item.getItemId() == R.id.action_settings) {
                startActivity(new Intent(requireContext(), SettingsActivity.class));
                return true;
            }
            return false;
        });
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavVisible(true);
        }
    }

    private void confirmClearDone() {
        final List<Todo> done = store.getDone();
        if (done.isEmpty()) {
            toast("没有已完成的待办");
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage("清除 " + done.size() + " 条已完成的待办？")
                .setPositiveButton("清除", (d, w) -> {
                    int n = store.clearDone();
                    reload();
                    toast("已清除 " + n + " 条");
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void edit(Todo todo) {
        Intent i = new Intent(requireContext(), TodoEditActivity.class);
        i.putExtra("todo_id", todo.id);
        startActivity(i);
    }

    private void attachSwipe() {
        swipePaint = new Paint();
        swipePaint.setColor(0xFFB3261E);
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                // 分组头不可滑
                return vh instanceof TodoAdapter.ItemHolder ? ItemTouchHelper.LEFT : 0;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                int pos = vh.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                Todo todo = adapter.todoAt(pos);
                reload();
                if (todo != null) confirmDelete(todo);
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv,
                                    @NonNull RecyclerView.ViewHolder vh, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                View item = vh.itemView;
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    swipeRect.set(item.getRight() + dX, item.getTop(), item.getRight(),
                            item.getBottom());
                    c.drawRoundRect(swipeRect, dp(20), dp(20), swipePaint);
                    c.save();
                    c.drawText("删除", item.getRight() - dp(30), item.getTop() + item.getHeight() / 2f,
                            textPaint());
                    c.restore();
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive);
            }
        });
        helper.attachToRecyclerView(recycler);
    }

    private Paint textPaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFFFFFFFF);
        p.setTextSize(dp(14));
        p.setTextAlign(Paint.Align.CENTER);
        return p;
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    /** 左滑后确认删除 */
    private void confirmDelete(Todo target) {
        final Todo t = target;
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage("删除这条待办？")
                .setPositiveButton("删除", (d, w) -> {
                    store.delete(t.id);
                    reload();
                    toast("已删除");
                })
                .setNegativeButton("取消", (d, w) -> reload())
                .setOnCancelListener(d -> reload())
                .show();
    }

    public void reload() {
        if (store == null || adapter == null) return;
        List<Todo> active = store.getActive();
        List<Todo> done = store.getDone();
        List<Todo> showActive = new ArrayList<>();
        List<Todo> showDone = new ArrayList<>();
        if (filter == FILTER_ALL) {
            showActive.addAll(active);
            showDone.addAll(done);
        } else if (filter == FILTER_TODAY) {
            for (Todo t : active) if (t.isDueToday()) showActive.add(t);
        } else if (filter == FILTER_OVERDUE) {
            for (Todo t : active) if (t.isOverdue()) showActive.add(t);
        } else if (filter == FILTER_DONE) {
            showDone.addAll(done);
        }
        adapter.setData(showActive, showDone);
        boolean empty = showActive.isEmpty() && showDone.isEmpty();
        if (emptyView != null) emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty && emptyTitle != null) {
            if (filter == FILTER_TODAY) {
                emptyTitle.setText("今天没有截止的待办");
                emptySub.setText("享受一下空闲");
            } else if (filter == FILTER_OVERDUE) {
                emptyTitle.setText("没有逾期的待办");
                emptySub.setText("保持这个状态");
            } else if (filter == FILTER_DONE) {
                emptyTitle.setText("还没有已完成的待办");
                emptySub.setText("去把事情做完吧");
            } else {
                emptyTitle.setText("还没有待办");
                emptySub.setText("点右下角 + 记一件要做的事");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (store != null) store.reload();
        applyToolbar();
        reload();
    }

    private void toast(String s) {
        if (isAdded()) Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show();
    }
}
