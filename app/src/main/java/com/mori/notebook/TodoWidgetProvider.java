package com.mori.notebook;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 2×2 待办小组件：显示进行中的待办前 4 条。 */
public class TodoWidgetProvider extends AppWidgetProvider {
    private static final int MAX_ITEMS = 4;

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        if (context == null || mgr == null || ids == null) return;
        for (int id : ids) render(context, mgr, id);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager mgr, int id,
                                          android.os.Bundle newOptions) {
        render(context, mgr, id);
    }

    static void refreshAll(Context c) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(c);
        if (mgr == null) return;
        int[] ids = mgr.getAppWidgetIds(new ComponentName(c, TodoWidgetProvider.class));
        if (ids == null || ids.length == 0) return;
        for (int id : ids) render(c, mgr, id);
    }

    static void render(Context c, AppWidgetManager mgr, int widgetId) {
        try {
            boolean dark = Widgets.isDark(c);
            int bg = dark ? 0xFF1C1B1F : 0xFFFFFFFF;
            int fg = dark ? 0xFFEDE7E1 : 0xFF1B1B1B;
            int sub = dark ? 0xFF9AA0A6 : 0xFF6B6B6B;

            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_todo);
            v.setInt(R.id.widget_root, "setBackgroundColor", bg);
            v.setTextColor(R.id.widget_title, fg);
            v.setTextColor(R.id.widget_count, sub);
            v.removeAllViews(R.id.widget_list);

            List<Todo> list = TodoStore.get(c).getActive();
            int total = 0;
            for (Todo t : list) {
                if (t != null && !t.done) total++;
            }
            v.setTextViewText(R.id.widget_count, total == 0 ? "全部完成" : total + " 项待办");

            int shown = 0;
            long now = System.currentTimeMillis();
            SimpleDateFormat hm = new SimpleDateFormat("M/d HH:mm", Locale.getDefault());
            for (Todo t : list) {
                if (shown >= MAX_ITEMS) break;
                if (t == null || t.done) continue;
                String title = t.title == null || t.title.trim().isEmpty() ? "无标题" : t.title.trim();
                if (title.length() > 16) title = title.substring(0, 16) + "…";
                String line = "☐ " + title;
                if (t.dueAt > 0L) {
                    line += "  " + hm.format(new Date(t.dueAt));
                }
                RemoteViews row = new RemoteViews(c.getPackageName(), R.layout.widget_row);
                row.setTextViewText(R.id.widget_row_text, line);
                row.setTextColor(R.id.widget_row_text, t.dueAt > 0L && t.dueAt < now ? 0xFFE53935 : fg);
                v.addView(R.id.widget_list, row);
                shown++;
            }
            if (shown == 0) {
                RemoteViews row = new RemoteViews(c.getPackageName(), R.layout.widget_row);
                row.setTextViewText(R.id.widget_row_text, "暂无待办，点一下开始记录");
                row.setTextColor(R.id.widget_row_text, sub);
                v.addView(R.id.widget_list, row);
            }

            Intent open = new Intent(c, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT
                    | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
            PendingIntent pi = PendingIntent.getActivity(c, 0, open, flags);
            v.setOnClickPendingIntent(R.id.widget_root, pi);

            mgr.updateAppWidget(widgetId, v);
        } catch (Throwable ignored) {
        }
    }
}