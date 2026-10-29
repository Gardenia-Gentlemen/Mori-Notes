package com.mori.notebook;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** 通过 SAF 在用户选择的目录里读写数据文件 */
public class StoreFile {
    public static final String NAME = "notes.json";
    public static final String NAME_TODO = "todos.json";

    public static Uri find(Context c, Uri tree) {
        return find(c, tree, NAME);
    }

    public static Uri find(Context c, Uri tree, String name) {
        try {
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(
                    tree, DocumentsContract.getTreeDocumentId(tree));
            Cursor cur = c.getContentResolver().query(children, new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null);
            if (cur != null) {
                try {
                    while (cur.moveToNext()) {
                        if (name.equals(cur.getString(1))) {
                            return DocumentsContract.buildDocumentUriUsingTree(tree, cur.getString(0));
                        }
                    }
                } finally {
                    cur.close();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** 不存在则返回 null */
    public static String read(Context c, Uri tree) throws IOException {
        return read(c, tree, NAME);
    }

    public static String read(Context c, Uri tree, String name) throws IOException {
        Uri doc = find(c, tree, name);
        if (doc == null) return null;
        try (InputStream in = c.getContentResolver().openInputStream(doc)) {
            if (in == null) return null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) bos.write(buf, 0, len);
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    public static void write(Context c, Uri tree, String data) throws IOException {
        write(c, tree, NAME, data);
    }

    public static void write(Context c, Uri tree, String name, String data) throws IOException {
        Uri doc = find(c, tree, name);
        if (doc == null) {
            Uri parent = DocumentsContract.buildDocumentUriUsingTree(
                    tree, DocumentsContract.getTreeDocumentId(tree));
            doc = DocumentsContract.createDocument(c.getContentResolver(), parent, "application/json", name);
        }
        if (doc == null) throw new IOException("无法创建 " + name);
        try (OutputStream out = c.getContentResolver().openOutputStream(doc, "wt")) {
            if (out == null) throw new IOException("无法写入 " + name);
            out.write(data.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** tree uri 转人类可读路径 */
    public static String describe(Uri tree) {
        if (tree == null) return "应用内部存储";
        try {
            String id = DocumentsContract.getTreeDocumentId(tree);
            int i = id.indexOf(':');
            String type = i >= 0 ? id.substring(0, i) : id;
            String path = i >= 0 ? id.substring(i + 1) : "";
            if ("primary".equalsIgnoreCase(type)) {
                return "/sdcard" + (path.isEmpty() ? "" : "/" + path);
            }
            return type + ":/" + path;
        } catch (Exception e) {
            return "自定义目录";
        }
    }
}