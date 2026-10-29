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
            // file:// 在 Android 13+ 无权读 /sdcard，明确提示改走设置页导入，避免静默失败
            String msg = "读取失败：" + (e.getMessage() == null ? "未知错误" : e.getMessage());
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                msg += "；请改用「设置 → 整机笔记导入」";
            }
            Toast.makeText(act, msg, Toast.LENGTH_LONG).show();
            if (cb != null) cb.onFinished();
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
        MoriFile.Package pkg;
        try {
            pkg = MoriFile.parse(text, pin);
        } catch (MoriFile.BadPasswordException e) {
            Toast.makeText(act, "密码错误，无法解密", Toast.LENGTH_SHORT).show();
            if (cb != null) cb.onFinished();
            return;
        } catch (Exception e) {
            Toast.makeText(act, "导入失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            if (cb != null) cb.onFinished();
            return;
        }
        if (pkg.total() == 0) {
            Toast.makeText(act, "文件中没有可导入的内容", Toast.LENGTH_SHORT).show();
            if (cb != null) cb.onFinished();
            return;
        }
        askStrategy(act, pkg, cb);
    }

    /** 解析成功后再询问重复条目处理方式，避免给错密码的人看到内容概要 */
    private static void askStrategy(Activity act, MoriFile.Package pkg, Callback cb) {
        StringBuilder msg = new StringBuilder();
        msg.append("合并：本机已有的同一条目用文件内容覆盖，其余新增。\n")
                .append("追加：全部作为新条目加入，本机原有内容保持不变。");
        boolean hasPrivate = false;
        for (Note n : pkg.notes) {
            if (n != null && n.isPrivate) {
                hasPrivate = true;
                break;
            }
        }
        if (hasPrivate) msg.append("\n\n本包含隐私笔记，导入后需输入隐私密码才能查看。");
        new MaterialAlertDialogBuilder(act)
                .setTitle("导入 " + pkg.total() + " 项内容")
                .setMessage(msg.toString())
                .setPositiveButton("合并", (d, w) -> apply(act, pkg, true, cb))
                .setNegativeButton("追加", (d, w) -> apply(act, pkg, false, cb))
                .setNeutralButton("取消", (d, w) -> {
                    if (cb != null) cb.onFinished();
                })
                .setOnCancelListener(d -> {
                    if (cb != null) cb.onFinished();
                })
                .show();
    }

    private static void apply(Activity act, MoriFile.Package pkg, boolean merge, Callback cb) {
        try {
            int noteCount = NoteStore.get(act).importNotes(pkg.notes, merge);
            int cdCount = importCountdowns(act, pkg.countdowns, merge);
            int todoCount = importTodos(act, pkg.todos, merge);
            int fontCount = importFonts(act, pkg.fonts);
            Toast.makeText(act,
                    (merge ? "已合并：" : "已追加：") + summary(noteCount, cdCount, todoCount, fontCount),
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(act, "导入失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        if (cb != null) cb.onFinished();
    }

    /** 导入倒数日；追加时 id 冲突换新 id，合并时同 id 覆盖（回收站中的视为不冲突） */
    private static int importCountdowns(Activity act, java.util.List<Countdown> list, boolean merge) {
        if (list == null || list.isEmpty()) return 0;
        CountdownStore store = CountdownStore.get(act);
        store.reload();
        int added = 0;
        for (Countdown c : list) {
            if (c == null) continue;
            boolean hasId = c.id != null && !c.id.trim().isEmpty();
            Countdown existing = hasId ? store.find(c.id) : null;
            boolean overwrite = merge && existing != null && !existing.inTrash();
            if (!overwrite && (!hasId || existing != null)) {
                c.id = java.util.UUID.randomUUID().toString();
            }
            // 备份导入的倒数日不参与 24 小时补录窗口
            c.imported = true;
            store.put(c);
            added++;
        }
        return added;
    }

    /** 导入待办；追加时 id 冲突换新 id，合并时同 id 覆盖 */
    private static int importTodos(Activity act, java.util.List<Todo> list, boolean merge) {
        if (list == null || list.isEmpty()) return 0;
        TodoStore store = TodoStore.get(act);
        store.reload();
        int added = 0;
        for (Todo t : list) {
            if (t == null) continue;
            boolean hasId = t.id != null && !t.id.trim().isEmpty();
            Todo existing = hasId ? store.find(t.id) : null;
            boolean overwrite = merge && existing != null;
            if (!overwrite && (!hasId || existing != null)) {
                t.id = java.util.UUID.randomUUID().toString();
            }
            store.put(t);
            added++;
        }
        return added;
    }

    /** 导入字体库文件，重名自动加序号，不覆盖已有字体 */
    private static int importFonts(Activity act, java.util.List<MoriFile.FontItem> list) {
        if (list == null || list.isEmpty()) return 0;
        java.io.File dir = FontImporter.fontDir(act);
        if (dir == null) return 0;
        int added = 0;
        for (MoriFile.FontItem item : list) {
            if (item == null || item.data == null || item.data.length == 0) continue;
            String raw = item.name == null || item.name.trim().isEmpty() ? "font.ttf" : item.name;
            String base = raw;
            String ext = "";
            int dot = raw.lastIndexOf('.');
            if (dot > 0) {
                base = raw.substring(0, dot);
                ext = raw.substring(dot);
            }
            java.io.File target = new java.io.File(dir, base + ext);
            int i = 2;
            while (target.exists()) {
                target = new java.io.File(dir, base + "-" + i + ext);
                i++;
            }
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(target)) {
                out.write(item.data);
                out.flush();
                added++;
            } catch (Exception ignored) {
            }
        }
        if (added > 0) Fonts.clear();
        return added;
    }
    /** 拼接导入结果文案，跳过 0 项 */
    private static String summary(int notes, int countdowns, int todos, int fonts) {
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
        if (fonts > 0) {
            if (sb.length() > 0) sb.append("、");
            sb.append(fonts).append(" 个字体");
        }
        if (sb.length() == 0) return "没有内容";
        return sb.toString();
    }

    private static String displayName(Activity act, Uri uri) {
        // file:// 不是 ContentProvider Uri，ContentResolver 查不到名字，
        // 这里直接取路径最后一段（含中文文件名时需 URL 解码）。
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            String path = uri.getPath();
            if (path == null) return "";
            int i = path.lastIndexOf('/');
            String n = i >= 0 ? path.substring(i + 1) : path;
            if (n.isEmpty()) return "";
            try {
                return java.net.URLDecoder.decode(n, "UTF-8");
            } catch (Exception e) {
                return n;
            }
        }
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

    /** 打开输入流：file:// 走 FileInputStream，其余走 ContentResolver。 */
    private static java.io.InputStream open(Activity act, Uri uri) throws Exception {
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            String path = uri.getPath();
            if (path == null || path.isEmpty()) throw new Exception("无效文件路径");
            java.io.File f = new java.io.File(path);
            if (!f.exists()) throw new Exception("文件不存在");
            if (!f.canRead()) throw new Exception("没有读取权限");
            return new java.io.FileInputStream(f);
        }
        java.io.InputStream in = act.getContentResolver().openInputStream(uri);
        if (in == null) throw new Exception("无法打开文件");
        return in;
    }

    private static String readAll(Activity act, Uri uri) throws Exception {
        try (java.io.InputStream in = open(act, uri);
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