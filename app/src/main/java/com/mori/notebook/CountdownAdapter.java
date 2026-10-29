/*
 * 倒数日列表适配器。
 *
 * 参考自 TimeTrace (https://github.com/KIPPUDESU/KIPPU_Trace)
 * Copyright (c) 2026 切符
 * 依据 MIT License 使用，许可全文见项目根目录 LICENSE-KIPPU_Trace。
 */
package com.mori.notebook;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.graphics.Typeface;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 倒数日列表：分组头 + 卡片；标题与天数争地方时改用堆叠排版 */
public class CountdownAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_STACK = 2;

    /** 横向排版可用宽度（以中日韩字符宽为 1.0 计），超过则改为堆叠 */
    private static final double WIDTH_LIMIT = 14.0;

    public interface Listener {
        void onClick(Countdown item);
        void onLongClick(Countdown item);

        /** 三点菜单中的「编辑」 */
        void onEdit(Countdown item);

        /** 三点菜单中的「选择字体」 */
        void onImportFont(Countdown item);
        /** 三点菜单中的「设置背景」，anchor 为三点按钮 */
        void onSetBackground(Countdown item, View anchor);
        /** 三点菜单中的「删除倒数日」 */
        void onDelete(Countdown item);

        /** 多选态下选中项数量变化 */
        void onSelectionChanged();
        /** 长按三点按钮开始拖拽排序 */
        void onDragStart(RecyclerView.ViewHolder vh);
    }

    private static class Row {
        String header;
        Countdown item;
        String dateText;
        String num;
        String unit;
        String anniv;
        /** 天数位是否已被纪念日自定义文案占用 */
        boolean message;
        boolean stack;
        int group = -1;

        Row(String header) {
            this.header = header;
        }
    }

    /** 局部刷新的载荷：只改多选态样式。 */
    static final Object PAYLOAD_SELECTION = new Object();
    private final List<Row> rows = new ArrayList<>();
    /** 上一次 setData 的数据签名，相同则跳过重建 */
    private String lastDataKey;
    /** 仅用于读取全局显示配置（蒙版强度、夜间模式） */
    private Context appContext;
    private final Listener listener;
    /** 当前分组序号，拖拽仅允许在同组内进行 */
    private int groupSeq;
    /** 是否处于多选态 */
    private boolean selectionMode;
    /** 已选中项的 id */
    private final Set<String> selected = new LinkedHashSet<>();

    public CountdownAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setData(List<Countdown> source) {
        // 数据没有实质变化时不重建列表（onResume 会重复调用）
        String key = signature(source);
        if (key.equals(lastDataKey) && !rows.isEmpty()) {
            pruneSelection(source);
            return;
        }
        lastDataKey = key;
        rows.clear();
        pruneSelection(source);
        List<Countdown> pinned = new ArrayList<>();
        List<Countdown> upcoming = new ArrayList<>();
        List<Countdown> past = new ArrayList<>();
        for (Countdown c : source) {
            if (c.pinned) {
                pinned.add(c);
            } else if (isPast(c)) {
                past.add(c);
            } else {
                upcoming.add(c);
            }
        }
        addGroup("置顶 · " + pinned.size(), pinned, true);
        addGroup("还没到 · " + upcoming.size(), upcoming, true);
        addGroup("已经过 · " + past.size(), past, false);
        notifyDataSetChanged();
    }

    /** 影响列表展示的字段快照，用于跳过无变化的整屏重绑。 */
    private String signature(List<Countdown> source) {
        int globalMask = 0;
        int night = 0;
        if (appContext != null) {
            globalMask = CountdownPrefs.getCardMask(appContext);
            night = appContext.getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        }
        StringBuilder b = new StringBuilder(source.size() * 48);
        b.append(globalMask).append('\u0001').append(night).append('\u0001')
                .append(CountdownTime.dayStartHour()).append('\u0001');
        for (Countdown c : source) {
            b.append(c.id).append('\u0001').append(c.title).append('\u0001')
                    .append(c.date).append('\u0001').append(c.mode).append('\u0001')
                    .append(c.pinned).append('\u0001').append(c.annivType).append('\u0001')
                    .append(c.customDays).append('\u0001').append(c.annivMessage).append('\u0001')
                    .append(c.showYear).append(c.showMonth).append(c.showWeek).append('\u0001')
                    .append(c.repeatType).append('\u0001').append(c.repeatInterval).append('\u0001')
                    .append(c.bgUri).append('\u0001').append(c.bgColor).append('\u0001')
                    .append(c.fontPath).append('\u0001').append(c.sortIndex).append('\u0001')
                    .append(c.deletedAt).append('\u0001').append(c.cardMask).append('\u0001')
                    .append(c.dayStartHour).append('\u0001');
        }
        return b.toString();
    }

    private void addGroup(String header, List<Countdown> group, boolean ascending) {
        if (group.isEmpty()) {
            return;
        }
        Row head = new Row(header);
        head.group = groupSeq;
        rows.add(head);
        for (Countdown c : group) {
            Row r = buildRow(c);
            r.group = groupSeq;
            rows.add(r);
        }
        groupSeq++;
    }

    /** 累计态且日期已过、或重复推进后仍不该出现的，归为“已过去” */
    private boolean isPast(Countdown c) {
        long diff = CountdownTime.dayDiff(CountdownTime.today(c.dayStartHourOf()), c.date); // 正数=将来
        return Countdown.MODE_UP.equals(c.mode) ? diff > 0 : diff < 0;
    }

    private Row buildRow(Countdown c) {
        Row r = new Row(null);
        r.item = c;
        long today = CountdownTime.today(c.dayStartHourOf());
        long diff = CountdownTime.dayDiff(today, c.date); // 正数=将来
        boolean up = Countdown.MODE_UP.equals(c.mode);
        if (up) {
            if (diff > 0) {
                r.num = String.valueOf(diff);
                r.unit = "天后开始";
            } else {
                r.num = String.valueOf(-diff);
                r.unit = "天";
            }
        } else {
            if (diff > 0) {
                r.num = String.valueOf(diff);
                r.unit = "天";
            } else if (diff == 0) {
                r.num = "今天";
                r.unit = "";
            } else {
                r.num = String.valueOf(-diff);
                r.unit = "天前";
            }
        }
        r.dateText = CountdownTime.formatDate(c.date);
        String anniv = CountdownTime.anniversaryText(c, today);
        r.anniv = TextUtils.isEmpty(anniv) ? null : anniv;
        String msg = c.annivMessage == null ? "" : c.annivMessage.trim();
        if (!msg.isEmpty() && CountdownTime.isAnnivHit(c, today)) {
            r.num = msg;
            r.unit = "";
            r.message = true;
            r.anniv = null; // 天数位已展示文案，避免重复
        }
        String title = TextUtils.isEmpty(c.title) ? "无标题" : c.title;
        double width = CountdownTime.visualWidth(title) + CountdownTime.visualWidth(r.num + r.unit);
        r.stack = r.anniv != null || width > WIDTH_LIMIT;
        return r;
    }

    @Override
    public int getItemViewType(int position) {
        Row r = rows.get(position);
        if (r.item == null) {
            return TYPE_HEADER;
        }
        return r.stack ? TYPE_STACK : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_countdown_header, parent, false));
        }
        if (viewType == TYPE_STACK) {
            return bindItemListeners(new ItemHolder(
                    inflater.inflate(R.layout.item_countdown_stack, parent, false)));
        }
        return bindItemListeners(new ItemHolder(
                inflater.inflate(R.layout.item_countdown, parent, false)));
    }

    /** 点击/长按监听只在创建时挂一次，绑定阶段不再新建监听对象。 */
    private ItemHolder bindItemListeners(final ItemHolder h) {
        if (appContext == null) {
            appContext = h.card.getContext();
        }
        h.card.setOnClickListener(v -> {
            Countdown c = h.current;
            if (c == null) {
                return;
            }
            if (selectionMode) {
                toggleSelection(c);
                listener.onSelectionChanged();
            } else {
                listener.onClick(c);
            }
        });
        h.card.setOnLongClickListener(v -> {
            if (h.current != null) {
                listener.onLongClick(h.current);
            }
            return true;
        });
        h.menu.setOnClickListener(v -> {
            if (h.current != null) {
                showMenu(v, h.current);
            }
        });
        h.menu.setOnLongClickListener(v -> {
            if (selectionMode) {
                return true;
            }
            listener.onDragStart(h);
            return true;
        });
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).text.setText(row.header);
            return;
        }
        ItemHolder h = (ItemHolder) holder;
        Countdown c = row.item;
        h.current = c;
        h.title.setText(TextUtils.isEmpty(c.title) ? "无标题" : c.title);
        float daysSize = row.message ? h.tfDaysSize * 0.55f : h.tfDaysSize;
        if (h.appliedDaysSize != daysSize) {
            h.days.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, daysSize);
            h.appliedDaysSize = daysSize;
        }
        h.days.setText(row.num);
        h.unit.setText(row.unit);
        h.unit.setVisibility(TextUtils.isEmpty(row.unit) ? View.GONE : View.VISIBLE);
        h.date.setText(row.dateText);
        if (row.anniv == null) {
            h.anniv.setVisibility(View.GONE);
        } else {
            h.anniv.setVisibility(View.VISIBLE);
            h.anniv.setText(row.anniv);
        }
        // 背景解码与字体解析开销大，同一份配置只应用一次
        String bgKey = String.valueOf(c.bgUri) + "#" + c.bgColor + "#"
                + c.maskOf(h.itemView.getContext());
        if (!bgKey.equals(h.appliedBgKey)) {
            CardBg.apply(h.itemView.getContext(), c.bgUri, c.bgColor, h.bg, h.scrim,
                    h.title, h.date, h.anniv, h.days, h.unit,
                    c.maskOf(h.itemView.getContext()),
                    h.defaultTitle, h.defaultSub, h.defaultHero);
            h.appliedBgKey = bgKey;
        }
        String fontKey = c.fontPath == null ? "" : c.fontPath;
        if (!fontKey.equals(h.appliedFontKey)) {
            applyFont(h, c);
            h.appliedFontKey = fontKey;
        }
        h.menu.setVisibility(selectionMode ? View.GONE : View.VISIBLE);
        applySelectionStyle(h, selectionMode && selected.contains(c.id));
    }

    /** 多选态变化只更新描边与三点按钮，不重铺背景、不重设字体。 */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position,
                                 @NonNull java.util.List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.contains(PAYLOAD_SELECTION)
                && holder instanceof ItemHolder) {
            ItemHolder h = (ItemHolder) holder;
            Countdown c = rows.get(position).item;
            if (c != null) {
                h.menu.setVisibility(selectionMode ? View.GONE : View.VISIBLE);
                applySelectionStyle(h, selectionMode && selected.contains(c.id));
                return;
            }
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    /** 右侧三点菜单：分享图片、设置背景、编辑、选择字体、删除倒数日 */
    private void showMenu(View anchor, final Countdown c) {
        PopupMenu pm = new PopupMenu(anchor.getContext(), anchor);
        pm.getMenu().add(0, 1, 0, "分享图片");
        pm.getMenu().add(0, 2, 1, "设置背景");
        pm.getMenu().add(0, 3, 2, "编辑");
        pm.getMenu().add(0, 4, 3, "选择字体");
        pm.getMenu().add(0, 5, 4, "删除倒数日");
        pm.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    ShareUtils.shareCountdownAsImage(anchor.getContext(), c);
                    return true;
                case 2:
                    listener.onSetBackground(c, anchor);
                    return true;
                case 3:
                    listener.onEdit(c);
                    return true;
                case 4:
                    listener.onImportFont(c);
                    return true;
                case 5:
                    listener.onDelete(c);
                    return true;
                default:
                    return false;
            }
        });
        pm.show();
    }

    /** 同组且两侧都是卡片才允许拖拽交换 */
    public boolean canMove(int from, int to) {
        if (selectionMode) return false;
        if (from == to) return false;
        if (from < 0 || to < 0 || from >= rows.size() || to >= rows.size()) return false;
        Row a = rows.get(from);
        Row b = rows.get(to);
        return a.item != null && b.item != null && a.group == b.group;
    }

    public void moveRow(int from, int to) {
        Row r = rows.remove(from);
        rows.add(to, r);
        notifyItemMoved(from, to);
    }

    /** 数据刷新后剔除已不存在的选中项 */
    private void pruneSelection(List<Countdown> source) {
        if (selected.isEmpty()) {
            return;
        }
        List<String> keep = new ArrayList<>();
        for (Countdown c : source) {
            keep.add(c.id);
        }
        selected.retainAll(keep);
    }

    private int positionOf(String id) {
        for (int i = 0; i < rows.size(); i++) {
            Countdown c = rows.get(i).item;
            if (c != null && c.id.equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /** 进入 / 退出多选态，退出时清空选中 */
    public void setSelectionMode(boolean on) {
        selectionMode = on;
        if (!on) {
            selected.clear();
        }
        if (!rows.isEmpty()) {
            notifyItemRangeChanged(0, rows.size(), PAYLOAD_SELECTION);
        }
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public void toggleSelection(Countdown c) {
        if (c == null) {
            return;
        }
        if (!selected.remove(c.id)) {
            selected.add(c.id);
        }
        int pos = positionOf(c.id);
        if (pos >= 0) {
            notifyItemChanged(pos, PAYLOAD_SELECTION);
        }
    }

    public void selectAll() {
        selected.clear();
        for (Row r : rows) {
            if (r.item != null) {
                selected.add(r.item.id);
            }
        }
        if (!rows.isEmpty()) {
            notifyItemRangeChanged(0, rows.size(), PAYLOAD_SELECTION);
        }
    }

    public int selectedCount() {
        return selected.size();
    }

    public List<String> selectedIds() {
        return new ArrayList<>(selected);
    }

    public List<Countdown> selectedCountdowns() {
        List<Countdown> out = new ArrayList<>();
        for (Row r : rows) {
            if (r.item != null && selected.contains(r.item.id)) {
                out.add(r.item);
            }
        }
        return out;
    }

    /** 多选态卡片外观：选中描边，未选中淡化 */
    private void applySelectionStyle(ItemHolder h, boolean sel) {
        if (!(h.card instanceof com.google.android.material.card.MaterialCardView)) {
            return;
        }
        com.google.android.material.card.MaterialCardView mc =
                (com.google.android.material.card.MaterialCardView) h.card;
        int density = (int) h.itemView.getResources().getDisplayMetrics().density;
        if (!selectionMode) {
            mc.setStrokeWidth(0);
            h.itemView.setAlpha(1f);
            return;
        }
        mc.setStrokeWidth(sel ? 2 * density : 0);
        if (sel) {
            mc.setStrokeColor(themeColor(h.itemView, androidx.appcompat.R.attr.colorPrimary, 0xFF6750A4));
        }
        h.itemView.setAlpha(sel ? 1f : 0.6f);
    }

    private static int themeColor(View v, int attr, int fallback) {
        android.util.TypedValue tv = new android.util.TypedValue();
        if (v.getContext().getTheme().resolveAttribute(attr, tv, true)) {
            return tv.data;
        }
        return fallback;
    }

    /** 当前列表里卡片的顺序 */
    public List<Countdown> orderedItems() {
        List<Countdown> out = new ArrayList<>();
        for (Row r : rows) {
            if (r.item != null) out.add(r.item);
        }
        return out;
    }

    public Countdown itemAt(int position) {
        if (position < 0 || position >= rows.size()) {
            return null;
        }
        return rows.get(position).item;
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView text;

        HeaderHolder(View v) {
            super(v);
            text = v.findViewById(R.id.tv_header);
        }
    }

    /** 应用倒数日全屏页导入的自定义字体；无自定义字体时回退到布局默认。 */
    private static void applyFont(ItemHolder h, Countdown c) {
        Context ctx = h.itemView.getContext();
        String path = c == null ? null : c.fontPath;
        Typeface reg = Fonts.regular(ctx, path);
        Typeface bold = Fonts.bold(ctx, path);
        h.title.setTypeface(reg != null ? reg : h.tfTitle);
        h.date.setTypeface(reg != null ? reg : h.tfDate);
        h.unit.setTypeface(reg != null ? reg : h.tfUnit);
        h.anniv.setTypeface(reg != null ? reg : h.tfAnniv);
        h.days.setTypeface(bold != null ? bold : h.tfDays);
    }

    static class ItemHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView date;
        final TextView days;
        final TextView unit;
        final TextView anniv;
        final ImageView bg;
        final View scrim;
        final View card;
        final View menu;
        final int defaultTitle;
        final int defaultSub;
        final int defaultHero;
        final Typeface tfTitle;
        final Typeface tfDate;
        final Typeface tfDays;
        final Typeface tfUnit;
        final Typeface tfAnniv;
        final float tfDaysSize;
        /** 当前绑定的数据，供常驻监听器读取 */
        Countdown current;
        /** 已应用的背景签名与字体，相同则跳过重活 */
        String appliedBgKey;
        String appliedFontKey;
        float appliedDaysSize;

        ItemHolder(View v) {
            super(v);
            title = v.findViewById(R.id.tv_title);
            date = v.findViewById(R.id.tv_date);
            days = v.findViewById(R.id.tv_days);
            unit = v.findViewById(R.id.tv_unit);
            anniv = v.findViewById(R.id.tv_anniv);
            bg = v.findViewById(R.id.iv_bg);
            scrim = v.findViewById(R.id.v_scrim);
            card = v.findViewById(R.id.card_countdown);
            menu = v.findViewById(R.id.btn_more);
            defaultTitle = title.getCurrentTextColor();
            defaultSub = date.getCurrentTextColor();
            defaultHero = days.getCurrentTextColor();
            tfTitle = title.getTypeface();
            tfDate = date.getTypeface();
            tfDays = days.getTypeface();
            tfUnit = unit.getTypeface();
            tfAnniv = anniv.getTypeface();
            tfDaysSize = days.getTextSize();
        }
    }
}
