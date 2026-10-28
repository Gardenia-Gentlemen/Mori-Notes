package com.mori.notebook;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

/**
 * 统一的 .mori 导入流程：设置页「导入」与外部用本软件打开 .mori 文件共用。
 */
public final class MoriImporter {

    private MoriImporter() {
    }

    public interface Callback {
        void onFinished();
    }

    /** 从启动 Intent 中取出目标文件 Uri（VIEW / SEND / SEND_MULTIPLE） */
    public static Uri uriFromIntent(Intent intent) {
        if (intent == null) return null;
        Uri data = intent.getData();
        if (data != null) return data;
        try {
            if (intent.getClipData() != null && intent.getClipData().getItemCount() > 0) {
                return intent.getClipData().getItemAt(0).getUri();
            }
        } catch (Exception ignored) {
        }
        Object extra = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        return extra instanceof Uri ? (Uri) extra : null;
    }

    /** 判断 Intent 是否是一个「打开文件」请求 */
    public static boolean isOpenFileIntent(Intent intent) {
        if (intent == null) return false;
        String a = intent.getAction();
        if (!Intent.ACTION_VIEW.equals(a)
                && !Intent.ACTION_SEND.equals(a)
                && !Intent.ACTION_SEND_MULTIPLE.equals(a)
                && !"android.intent.action.FILE".equals(a)) {
            return false;
        }
        return uriFromIntent(intent) != null;
    }

    public static void handle(Activity act, Uri uri, Callback cb) {
        if (act == null || uri == null) return;
        String name = displayName(act, uri);
        // 导入严格限定 .mori：无文件名或后缀不符一律拒绝
        if (name == null || name.isEmpty()
                || !name.toLowerCase(java.util.Locale.US).endsWith(MoriFile.EXT)) {
            Toast.makeText(act, "仅支持导入 .mori 文件", Toast.LENGTH_SHORT).show();
            return;
        }
        String text;
        try {
            text = readAll(act, uri);
        } catch (Exception e) {
            Toast.makeText(act, "读取失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        JSONObject head;
        try {
            head = MoriFile.head(text);
        } catch (Exception e) {
            Toast.makeText(act, "不是有效的 .mori 文件", Toast.LENGTH_SHORT).show();
            return;
        }
        if (head.optBoolean("encrypted", false)) {
            askPassword(act, text, cb);
        } else {
            finish(act, text, null, cb);
        }
    }

    private static void askPassword(Activity act, String text, Callback cb) {
        final EditText et = pinInput(act);
        new MaterialAlertDialogBuilder(act)
                .setTitle("文件已加密")
                .setMessage("请输入导出时设置的 8 位数字密码")
                .setView(wrap(act, et))
                .setPositiveButton("导入", (d, w) -> {
                    String pin = et.getText().toString().trim();
                    if (!MoriFile.isValidPin(pin)) {
                        Toast.makeText(act, "密码必须是 8 位数字", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    finish(act, text, pin, cb);
                })
                .setNegativeButton("取消", (d, w) -> {
                    if (cb != null) cb.onFinished();
                })
                .show();
    }

    private static void finish(Activity act, String text, String pin, Callback cb) {
        try {
            MoriFile.Package pkg = MoriFile.parse(text, pin);
            if (pkg.total() == 0) {
                Toast.makeText(act, "文件中没有可导入的内容", Toast.LENGTH_SHORT).show();
            } else {
                int noteCount = NoteStore.get(act).importNotes(pkg.notes);
                int cdCount = importCountdowns(act, pkg.countdowns);
                int todoCount = importTodos(act, pkg.todos);
                Toast.makeText(act, "已导入：" + summary(noteCount, cdCount, todoCount),
                        Toast.LENGTH_LONG).show();
            }
        } catch (MoriFile.BadPasswordException e) {
            Toast.makeText(act, "密码错误，无法解密", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(act, "导入失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        if (cb != null) cb.onFinished();
    }

    /** 导入倒数日，id 冲突时重新生成，避免覆盖已有记录 */
    private static int importCountdowns(Activity act, java.util.List<Countdown> list) {
        if (list == null || list.isEmpty()) return 0;
        CountdownStore store = CountdownStore.get(act);
        store.reload();
        int added = 0;
        for (Countdown c : list) {
            if (c == null) continue;
            if (c.id == null || c.id.trim().isEmpty() || store.find(c.id) != null) {
                c.id = java.util.UUID.randomUUID().toString();
            }
            store.put(c);
            added++;
        }
        return added;
    }

    /** 导入待办，id 冲突时重新生成 */
    private static int importTodos(Activity act, java.util.List<Todo> list) {
        if (list == null || list.isEmpty()) return 0;
        TodoStore store = TodoStore.get(act);
        store.reload();
        int added = 0;
        for (Todo t : list) {
            if (t == null) continue;
            if (t.id == null || t.id.trim().isEmpty() || store.find(t.id) != null) {
                t.id = java.util.UUID.randomUUID().toString();
            }
            store.put(t);
            added++;
        }
        return added;
    }

    /** 拼接导入结果文案，跳过 0 项 */
    private static String summary(int notes, int countdowns, int todos) {
        StringBuilder sb = new StringBuilder();
        if (notes > 0) sb.append(notes).append(" 条笔记");
        if (countdowns > 0) {
            if (sb.length() > 0) sb.append("、");
            sb.append(countdowns).append(" 个倒数日");
        }
        if (todos > 0) {
            if (sb.length() > 0) sb.append("、");
            sb.append(todos).append(" 条待办");
        }
        if (sb.length() == 0) return "没有内容";
        return sb.toString();
    }

    private static String displayName(Activity act, Uri uri) {
        try (android.database.Cursor c = act.getContentResolver()
                .query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return c.getString(idx);
            }
        } catch (Exception ignored) {
        }
        String p = uri.getLastPathSegment();
        return p == null ? "" : p;
    }

    private static String readAll(Activity act, Uri uri) throws Exception {
        try (java.io.InputStream in = act.getContentResolver().openInputStream(uri);
             java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
            if (in == null) throw new Exception("无法打开文件");
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) bos.write(buf, 0, len);
            return new String(bos.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private static EditText pinInput(Activity act) {
        EditText et = new EditText(act);
        et.setHint("8 位数字密码");
        et.setSingleLine(true);
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        return et;
    }

    private static android.view.View wrap(Activity act, android.view.View v) {
        android.widget.LinearLayout box = new android.widget.LinearLayout(act);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (20 * act.getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(v);
        return box;
    }
}