package com.mori.notebook;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 待办提醒：到点发通知，并处理「完成 / 稍后提醒 / 开机重排」。 */
public class TodoReceiver extends BroadcastReceiver {

    public static final String ACTION_FIRE = "com.mori.notebook.action.TODO_FIRE";
    public static final String ACTION_DONE = "com.mori.notebook.action.TODO_DONE";
    public static final String ACTION_SNOOZE = "com.mori.notebook.action.TODO_SNOOZE";
    public static final String CHANNEL_ID = "todo_reminder";
    public static final long SNOOZE_MS = 10L * 60 * 1000;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        Context c = context.getApplicationContext();
        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            TodoAlarm.rescheduleAll(c);
            return;
        }

        String id = intent.getData() != null ? intent.getData().getLastPathSegment()
                : intent.getStringExtra("todo_id");
        if (id == null || id.isEmpty()) return;

        if (ACTION_DONE.equals(action)) {
            try {
                TodoStore.get(c).setDone(id, true);
            } catch (Exception ignored) {
            }
            cancel(c, id);
            Widgets.updateAll(c);
            return;
        }

        if (ACTION_SNOOZE.equals(action)) {
            cancel(c, id);
            TodoAlarm.snooze(c, id, SNOOZE_MS);
            return;
        }

        // ACTION_FIRE
        Todo t = null;
        try {
            t = TodoStore.get(c).find(id);
        } catch (Exception ignored) {
        }
        if (t == null || t.done) {
            cancel(c, id);
            return;
        }
        show(c, t);
    }

    private static void show(Context c, Todo t) {
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                if (!NotificationManagerCompat.from(c).areNotificationsEnabled()) return;
            } catch (Exception ignored) {
            }
        }
        String title = t.title == null || t.title.trim().isEmpty() ? "待办提醒" : t.title;
        String text;
        if (t.note != null && !t.note.trim().isEmpty()) {
            text = t.note.trim();
            if (text.length() > 60) text = text.substring(0, 60) + "…";
        } else if (t.dueAt > 0L) {
            text = "截止 " + new SimpleDateFormat("M月d日 HH:mm", Locale.getDefault())
                    .format(new Date(t.dueAt));
        } else {
            text = "该待办了";
        }

        Intent open = new Intent(c, TodoEditActivity.class);
        open.putExtra("todo_id", t.id);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(c, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        Intent done = actionIntent(c, t.id, ACTION_DONE);
        Intent snooze = actionIntent(c, t.id, ACTION_SNOOZE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_todo_notify)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(content)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .addAction(0, "标记完成", broadcast(c, done, 1))
                .addAction(0, "10 分钟后", broadcast(c, snooze, 2));

        try {
            NotificationManagerCompat.from(c).notify(notifId(t.id), b.build());
        } catch (Exception ignored) {
        }
    }

    private static Intent actionIntent(Context c, String id, String action) {
        Intent i = new Intent(c, TodoReceiver.class);
        i.setAction(action);
        i.setData(android.net.Uri.parse("mori://todo/" + id));
        return i;
    }

    private static PendingIntent broadcast(Context c, Intent i, int code) {
        return PendingIntent.getBroadcast(c, code, i,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
    }

    private static int notifId(String id) {
        return id == null ? 0 : Math.abs(id.hashCode() % 100000);
    }

    static void cancel(Context c, String id) {
        try {
            NotificationManagerCompat.from(c).cancel(notifId(id));
        } catch (Exception ignored) {
        }
    }

    /** 创建通知渠道（幂等） */
    public static void ensureChannel(Context c) {
        if (c == null) return;
        if (Build.VERSION.SDK_INT < 26) return;
        try {
            android.app.NotificationManager nm =
                    (android.app.NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (nm.getNotificationChannel(CHANNEL_ID) != null) return;
            android.app.NotificationChannel ch = new android.app.NotificationChannel(
                    CHANNEL_ID, "待办提醒", android.app.NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("待办事项到点提醒");
            ch.enableVibration(true);
            nm.createNotificationChannel(ch);
        } catch (Exception ignored) {
        }
    }
}