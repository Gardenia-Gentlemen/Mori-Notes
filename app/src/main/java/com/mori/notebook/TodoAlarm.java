package com.mori.notebook;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import java.util.List;

/** 待办提醒的闹钟调度。 */
public final class TodoAlarm {
    private static final String SCHEME = "mori://todo/";

    private TodoAlarm() {
    }

    private static PendingIntent pending(Context c, String id, String action, int extraFlags) {
        Intent i = new Intent(c, TodoReceiver.class);
        i.setAction(action);
        i.setData(Uri.parse(SCHEME + (id == null ? "" : id)));
        int flags = extraFlags | PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c, 0, i, flags);
    }

    private static PendingIntent fire(Context c, String id) {
        return pending(c, id, TodoReceiver.ACTION_FIRE, 0);
    }

    /** 为该待办安排提醒；无截止时间 / 已完成 / 已过期则跳过 */
    public static void schedule(Context c, Todo todo) {
        if (c == null || todo == null) return;
        cancel(c, todo.id);
        if (todo.dueAt <= 0L || todo.done) return;
        if (todo.dueAt <= System.currentTimeMillis()) return;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        setExact(am, todo.dueAt, fire(c, todo.id));
    }

    public static void cancel(Context c, String id) {
        if (c == null || id == null) return;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        try {
            PendingIntent pi = pending(c, id, TodoReceiver.ACTION_FIRE, PendingIntent.FLAG_NO_CREATE);
            if (pi != null) {
                am.cancel(pi);
                pi.cancel();
            }
        } catch (Exception ignored) {
        }
    }

    /** 稍后提醒 */
    public static void snooze(Context c, String id, long delayMs) {
        if (c == null || id == null) return;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        setExact(am, System.currentTimeMillis() + Math.max(0L, delayMs), fire(c, id));
    }

    /** 全量重排（开机、时间变动后调用） */
    public static void rescheduleAll(Context c) {
        if (c == null) return;
        try {
            List<Todo> list = TodoStore.get(c).getActive();
            for (Todo t : list) schedule(c, t);
        } catch (Exception ignored) {
        }
    }

    private static void setExact(AlarmManager am, long at, PendingIntent pi) {
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                }
            } else if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (Exception e) {
            try {
                am.set(AlarmManager.RTC_WAKEUP, at, pi);
            } catch (Exception ignored) {
            }
        }
    }
}