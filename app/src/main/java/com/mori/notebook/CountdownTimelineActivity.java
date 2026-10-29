/*
 * 倒数日时间轴：目标日期的前后节点、剩余天数与进度。
 *
 * 参考自 TimeTrace (https://github.com/KIPPUDESU/KIPPU_Trace)
 * Copyright (c) 2026 切符
 * 依据 MIT License 使用，许可全文见项目根目录 LICENSE-KIPPU_Trace。
 */
package com.mori.notebook;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.List;
import java.util.TimeZone;

/** 单个倒数日的时间轴视图 */
public class CountdownTimelineActivity extends BaseActivity {
    /** 里程碑剩余天数 */
    private static final int[] MILESTONES = {365, 180, 100, 30, 14, 7, 3, 1};
    /** 是否已应用到卡片背景图（有背景时文字改白字分级） */
    private boolean hasBg = false;
    /** 该条倒数日的专属字体路径，空则跟随系统 */
    private String fontPath = "";
    /** 当前展示的倒数日 */
    private Countdown item;

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_countdown_timeline);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        String id = getIntent().getStringExtra("countdown_id");
        Countdown c = id == null ? null : CountdownStore.get(this).find(id);
        if (c == null) {
            Toast.makeText(this, "找不到这个日子", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!Countdown.MODE_UP.equals(c.mode)) {
            finish();
            return;
        }
        item = c;
        bind(c);
        applyCardRatio();
        final String cid = c.id;
        toolbar.inflateMenu(R.menu.menu_countdown_timeline);
        toolbar.setOnMenuItemClickListener(m -> {
            if (m.getItemId() == R.id.action_fullscreen) {
                Intent it = new Intent(this, CountdownFullScreenActivity.class);
                it.putExtra("countdown_id", cid);
                startActivity(it);
                return true;
            }
            return false;
        });
        findViewById(R.id.fab_add).setOnClickListener(v -> showAddDialog());
    }

    /** 新建 / 修改回忆：时间只在新建时取本机当前时间，修改不改时间。 */
    private void showNoteDialog(TimelineNote editing) {
        if (item == null) return;
        boolean isNew = editing == null;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(8), dp(24), 0);

        final EditText etText = new EditText(this);
        etText.setHint("事件");
        etText.setSingleLine(false);
        if (!isNew) etText.setText(editing.text);
        box.addView(etText);

        final EditText etPlace = new EditText(this);
        etPlace.setHint("地点");
        if (!isNew) etPlace.setText(editing.place);
        box.addView(etPlace);

        TimeZone tz = CountdownTime.timeZone();
        long at = isNew ? System.currentTimeMillis() : editing.timeMs;
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        fmt.setTimeZone(tz);
        TextView tvTime = new TextView(this);
        tvTime.setTextSize(12);
        tvTime.setPadding(0, dp(8), 0, 0);
        tvTime.setText((isNew ? "时间：保存时自动取本机当前时间\n" : "时间：创建后固定，修改不会改变\n")
                + fmt.format(new Date(at)));
        box.addView(tvTime);

        new MaterialAlertDialogBuilder(this)
                .setTitle(isNew ? "新建回忆" : "修改回忆")
                .setView(box)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (d, w) -> {
                    String text = etText.getText().toString().trim();
                    String place = etPlace.getText().toString().trim();
                    if (text.isEmpty() && place.isEmpty()) {
                        Toast.makeText(this, "写点什么吧", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    TimelineNoteStore store = TimelineNoteStore.get(this);
                    if (isNew) {
                        TimelineNote n = new TimelineNote();
                        n.id = java.util.UUID.randomUUID().toString();
                        n.countdownId = item.id;
                        n.text = text;
                        n.place = place;
                        n.timeMs = System.currentTimeMillis();
                        n.timeZone = CountdownTime.timeZone().getID();
                        store.add(n);
                    } else {
                        editing.text = text;
                        editing.place = place;
                        store.update(editing);
                    }
                    bind(item);
                })
                .show();
    }

    private void showAddDialog() {
        showNoteDialog(null);
    }

    /** 长按回忆节点：修改回忆 / 删除。 */
    private void showNoteMenu(TimelineNote n) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(TextUtils.isEmpty(n.text) ? "这条回忆" : n.text)
                .setItems(new String[]{"修改回忆", "删除这条回忆"}, (d, which) -> {
                    if (which == 0) {
                        showNoteDialog(n);
                    } else {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("删除这条回忆")
                                .setMessage("删除后不可恢复。")
                                .setNegativeButton("取消", null)
                                .setPositiveButton("删除", (dd, ww) -> {
                                    TimelineNoteStore.get(this).remove(n.id);
                                    bind(item);
                                })
                                .show();
                    }
                })
                .show();
    }

    private void bind(Countdown c) {
        applyLook(c);
        final long today = CountdownTime.today();
        long diff = CountdownTime.dayDiff(today, c.date);
        boolean up = Countdown.MODE_UP.equals(c.mode);

        TextView title = findViewById(R.id.tv_title);
        title.setText(TextUtils.isEmpty(c.title) ? "无标题" : c.title);

        String lunar = LunarUtil.format(c.date);
        StringBuilder meta = new StringBuilder(CountdownTime.formatDate(c.date));
        if (!lunar.isEmpty()) {
            meta.append(" · ").append(lunar);
        }
        String anniv = CountdownTime.anniversaryText(c, today);
        if (!TextUtils.isEmpty(anniv)) {
            meta.append("\n").append(anniv);
        }
        TextView metaView = findViewById(R.id.tv_meta);
        metaView.setText(meta.toString());

        TextView num = findViewById(R.id.tv_num);
        TextView unit = findViewById(R.id.tv_unit);
        if (diff == 0 && !up) {
            num.setText("今天");
            unit.setText("");
        } else {
            num.setText(String.valueOf(Math.abs(diff)));
            if (up) {
                unit.setText(diff > 0 ? "天后开始" : "天");
            } else {
                unit.setText(diff > 0 ? "天后" : "天前");
            }
        }

        LinearProgressIndicator progress = findViewById(R.id.progress);
        long startMs = c.createdAt > 0 ? c.createdAt : c.date - 30L * 86400000L;
        float p = 1f;
        if (c.date > startMs) {
            p = (today - startMs) / (float) (c.date - startMs);
        }
        if (p < 0f) p = 0f;
        if (p > 1f) p = 1f;
        progress.setProgressCompat(Math.round(p * 100), true);

        LinearLayout box = findViewById(R.id.timeline_box);
        box.removeAllViews();

        List<Node> nodes = new ArrayList<>();
        for (int m : MILESTONES) {
            long at = CountdownTime.addDays(c.date, -m);
            if (at >= today && at <= c.date) {
                nodes.add(new Node(at, "还剩 " + m + " 天", CountdownTime.formatDateShort(at)
                        + (LunarUtil.format(at).isEmpty() ? "" : " · " + LunarUtil.format(at))));
            }
        }
        // 手记节点：文字 + 地点 + 记录时间（含时区）
        for (TimelineNote n : TimelineNoteStore.get(this).list(c.id)) {
            StringBuilder sub = new StringBuilder();
            if (!TextUtils.isEmpty(n.place)) sub.append(n.place).append(" · ");
            sub.append(CountdownTime.formatDateTime(n.timeMs));
            String main = TextUtils.isEmpty(n.text) ? "记录" : n.text;
            nodes.add(new Node(n.timeMs, main, sub.toString(), n));
        }
        Collections.sort(nodes, (x, y) -> Long.compare(x.date, y.date));

        String endTitle = diff == 0 ? "就是今天" : (diff > 0 ? "目标日" : "已过去 " + (-diff) + " 天");
        nodes.add(new Node(c.date, endTitle, CountdownTime.formatDate(c.date)
                + (lunar.isEmpty() ? "" : " · " + lunar)));
        Collections.sort(nodes, (a, b) -> Long.compare(a.date, b.date));
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            addNode(box, n, n.date <= today, i == nodes.size() - 1);
        }
    }

    /** 卡片按 16:9（宽:高）定高，并贴顶展示。 */
    private void applyCardRatio() {
        View card = findViewById(R.id.card);
        card.post(() -> {
            int w = card.getWidth();
            if (w <= 0) return;
            ViewGroup.LayoutParams lp = card.getLayoutParams();
            lp.height = Math.round(w * 9f / 16f);
            card.setLayoutParams(lp);
        });
    }

    /** 应用该条倒数日的卡片背景与字体，与全屏页保持一致。 */
    private void applyLook(Countdown c) {
        fontPath = c.fontPath == null ? "" : c.fontPath;
        int onSurfaceC = com.google.android.material.color.MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurface, 0xFF1C1B1F);
        int onVariantC = com.google.android.material.color.MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF49454F);
        ImageView bg = findViewById(R.id.bg_image);
        View scrim = findViewById(R.id.bg_scrim);
        TextView title = findViewById(R.id.tv_title);
        TextView meta = findViewById(R.id.tv_meta);
        TextView num = findViewById(R.id.tv_num);
        TextView unit = findViewById(R.id.tv_unit);
        CardBg.apply(this, c.bgUri, c.bgColor, bg, scrim, title, meta, meta, num, unit,
                onSurfaceC, onVariantC, onSurfaceC);
        hasBg = c.bgColor == CardBg.BG_APP
                ? !BackgroundPrefs.isLightStyle(this)
                : (c.bgColor != 0 || bg.getVisibility() == View.VISIBLE);

        title.setTypeface(Fonts.bold(this, fontPath));
        meta.setTypeface(Fonts.regular(this, fontPath));
        num.setTypeface(Fonts.bold(this, fontPath));
        unit.setTypeface(Fonts.regular(this, fontPath));

        // 与全屏页相同的白字分级透明度
        num.setTextColor(hasBg ? 0xFFFFFFFF : onSurfaceC);
        title.setTextColor(hasBg ? 0xFFFFFFFF : onSurfaceC);
        unit.setTextColor(hasBg ? 0x99FFFFFF : onVariantC);
        meta.setTextColor(hasBg ? 0xBFFFFFFF : onVariantC);

        LinearProgressIndicator progress2 = findViewById(R.id.progress);
        progress2.setTrackColor(hasBg ? 0x33FFFFFF : 0x331C1B1F);
        progress2.setIndicatorColor(hasBg ? 0xFFFFFFFF : onSurfaceC);
    }

    private static class Node {
        final long date;
        final String title;
        final String sub;
        /** 是否为用户回忆（样式上突出显示）。 */
        final boolean note;
        /** 回忆节点对应的数据，里程碑为 null。 */
        final TimelineNote ref;

        Node(long date, String title, String sub) {
            this(date, title, sub, null);
        }

        Node(long date, String title, String sub, TimelineNote ref) {
            this.date = date;
            this.title = title;
            this.sub = sub;
            this.ref = ref;
            this.note = ref != null;
        }
    }

    private void addNode(LinearLayout box, Node node, boolean past, boolean last) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout axis = new LinearLayout(this);
        axis.setOrientation(LinearLayout.VERTICAL);
        axis.setGravity(Gravity.CENTER_HORIZONTAL);
        axis.setLayoutParams(new LinearLayout.LayoutParams(dp(20), ViewGroup.LayoutParams.MATCH_PARENT));

        View dot = new View(this);
        dot.setBackgroundResource(past ? R.drawable.dot_past : R.drawable.dot_now);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(12), dp(12));
        dotLp.topMargin = dp(4);
        axis.addView(dot, dotLp);
        if (!last) {
            View line = new View(this);
            line.setBackgroundResource(R.drawable.dot_past);
            LinearLayout.LayoutParams lineLp =
                    new LinearLayout.LayoutParams(dp(2), ViewGroup.LayoutParams.MATCH_PARENT);
            lineLp.topMargin = dp(4);
            axis.addView(line, lineLp);
        }
        row.addView(axis);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textsLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textsLp.setMarginStart(dp(12));
        texts.setLayoutParams(textsLp);

        TextView main = new TextView(this);
        main.setText(node.title);
        main.setTextSize(15);
        main.setTypeface(Typeface.DEFAULT, (past && !node.note) ? Typeface.NORMAL : Typeface.BOLD);
        main.setTextColor(node.note
                ? com.google.android.material.color.MaterialColors.getColor(this,
                        com.google.android.material.R.attr.colorPrimary, 0xFF6750A4)
                : (past ? 0xFF6B6B70 : 0xFF1C1B1F));
        texts.addView(main);

        TextView sub = new TextView(this);
        sub.setText(node.sub);
        sub.setTextSize(12);
        sub.setTextColor(0xFF6B6B70);
        sub.setPadding(0, dp(2), 0, dp(16));
        texts.addView(sub);

        row.addView(texts);
        if (node.ref != null) {
            final TimelineNote ref = node.ref;
            row.setLongClickable(true);
            row.setOnLongClickListener(v -> {
                showNoteMenu(ref);
                return true;
            });
        }
        box.addView(row);
    }
}
