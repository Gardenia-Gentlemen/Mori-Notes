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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 单个倒数日的时间轴视图 */
public class CountdownTimelineActivity extends BaseActivity {
    /** 里程碑剩余天数 */
    private static final int[] MILESTONES = {365, 180, 100, 30, 14, 7, 3, 1};
    /** 是否已应用到卡片背景图（有背景时文字改白字分级） */
    private boolean hasBg = false;
    /** 该条倒数日的专属字体路径，空则跟随系统 */
    private String fontPath = "";

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
        bind(c);
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
        if (c.createdAt > 0) {
            long createdDay = CountdownTime.dayNumber(c.createdAt);
            nodes.add(new Node(createdDay, "创建", CountdownTime.formatDateShort(createdDay)));
        }
        for (int m : MILESTONES) {
            long at = CountdownTime.addDays(c.date, -m);
            if (at >= today && at <= c.date) {
                nodes.add(new Node(at, "还剩 " + m + " 天", CountdownTime.formatDateShort(at)
                        + (LunarUtil.format(at).isEmpty() ? "" : " · " + LunarUtil.format(at))));
            }
        }
        String endTitle = diff == 0 ? "就是今天" : (diff > 0 ? "目标日" : "已过去 " + (-diff) + " 天");
        nodes.add(new Node(c.date, endTitle, CountdownTime.formatDate(c.date)
                + (lunar.isEmpty() ? "" : " · " + lunar)));
        Collections.sort(nodes, (a, b) -> Long.compare(a.date, b.date));
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            addNode(box, n, n.date <= today, i == nodes.size() - 1);
        }
    }

    /** 应用该条倒数日的卡片背景与字体，与全屏页保持一致。 */
    private void applyLook(Countdown c) {
        fontPath = c.fontPath == null ? "" : c.fontPath;
        ImageView bg = findViewById(R.id.bg_image);
        View scrim = findViewById(R.id.bg_scrim);
        TextView title = findViewById(R.id.tv_title);
        TextView meta = findViewById(R.id.tv_meta);
        TextView num = findViewById(R.id.tv_num);
        TextView unit = findViewById(R.id.tv_unit);
        CardBg.apply(this, c.bgUri, bg, scrim, title, meta, meta, num, unit,
                0xFF1C1B1F, 0xFF6B6B70, 0xFF1C1B1F);
        hasBg = bg.getVisibility() == View.VISIBLE;

        title.setTypeface(Fonts.bold(this, fontPath));
        meta.setTypeface(Fonts.regular(this, fontPath));
        num.setTypeface(Fonts.bold(this, fontPath));
        unit.setTypeface(Fonts.regular(this, fontPath));

        if (hasBg) {
            LinearProgressIndicator progress2 = findViewById(R.id.progress);
            progress2.setTrackColor(0x33FFFFFF);
            progress2.setIndicatorColor(0xFFFFFFFF);
        }
    }
    private static class Node {
        final long date;
        final String title;
        final String sub;

        Node(long date, String title, String sub) {
            this.date = date;
            this.title = title;
            this.sub = sub;
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
        main.setTypeface(Typeface.DEFAULT, past ? Typeface.NORMAL : Typeface.BOLD);
        main.setTextColor(past ? 0xFF6B6B70 : 0xFF1C1B1F);
        texts.addView(main);

        TextView sub = new TextView(this);
        sub.setText(node.sub);
        sub.setTextSize(12);
        sub.setTextColor(0xFF6B6B70);
        sub.setPadding(0, dp(2), 0, dp(16));
        texts.addView(sub);

        row.addView(texts);
        box.addView(row);
    }
}
