package com.mori.notebook;

/* loaded from: classes.dex */
public class StoreFile {
    public static final java.lang.String NAME = "notes.json";

    public static android.net.Uri find(android.content.Context context, android.net.Uri uri) {
        try {
            android.database.Cursor query = context.getContentResolver().query(android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(uri, android.provider.DocumentsContract.getTreeDocumentId(uri)), new java.lang.String[]{"document_id", "_display_name"}, null, null, null);
            if (query == null) {
                return null;
            }
            do {
                try {
                    if (!query.moveToNext()) {
                        query.close();
                        return null;
                    }
                } finally {
                    query.close();
                }
            } while (!NAME.equals(query.getString(1)));
            return android.provider.DocumentsContract.buildDocumentUriUsingTree(uri, query.getString(0));
        } catch (java.lang.Exception unused) {
            return null;
        }
    }

    public static java.lang.String read(android.content.Context context, android.net.Uri uri) throws java.io.IOException {
        android.net.Uri find = find(context, uri);
        if (find == null) {
            return null;
        }
        java.io.InputStream openInputStream = context.getContentResolver().openInputStream(find);
        if (openInputStream == null) {
            if (openInputStream != null) {
                openInputStream.close();
            }
            return null;
        }
        try {
            java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
            byte[] bArr = new byte[8192];
            while (true) {
                int read = openInputStream.read(bArr);
                if (read <= 0) {
                    break;
                }
                byteArrayOutputStream.write(bArr, 0, read);
            }
            java.lang.String str = new java.lang.String(byteArrayOutputStream.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
            if (openInputStream != null) {
                openInputStream.close();
            }
            return str;
        } catch (java.lang.Throwable th) {
            if (openInputStream != null) {
                try {
                    openInputStream.close();
                } catch (java.lang.Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    public static void write(android.content.Context context, android.net.Uri uri, java.lang.String str) throws java.io.IOException {
        android.net.Uri find = find(context, uri);
        if (find == null) {
            find = android.provider.DocumentsContract.createDocument(context.getContentResolver(), android.provider.DocumentsContract.buildDocumentUriUsingTree(uri, android.provider.DocumentsContract.getTreeDocumentId(uri)), "application/json", NAME);
        }
        if (find == null) {
            throw new java.io.IOException("无法创建 notes.json");
        }
        java.io.OutputStream openOutputStream = context.getContentResolver().openOutputStream(find, "wt");
        try {
            if (openOutputStream == null) {
                throw new java.io.IOException("无法写入 notes.json");
            }
            openOutputStream.write(str.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if (openOutputStream != null) {
                openOutputStream.close();
            }
        } catch (java.lang.Throwable th) {
            if (openOutputStream != null) {
                try {
                    openOutputStream.close();
                } catch (java.lang.Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    public static java.lang.String describe(android.net.Uri uri) {
        if (uri == null) {
            return "应用内部存储";
        }
        try {
            java.lang.String treeDocumentId = android.provider.DocumentsContract.getTreeDocumentId(uri);
            int indexOf = treeDocumentId.indexOf(58);
            java.lang.String substring = indexOf >= 0 ? treeDocumentId.substring(0, indexOf) : treeDocumentId;
            java.lang.String str = "";
            java.lang.String substring2 = indexOf >= 0 ? treeDocumentId.substring(indexOf + 1) : "";
            if ("primary".equalsIgnoreCase(substring)) {
                java.lang.StringBuilder sb = new java.lang.StringBuilder("/sdcard");
                if (!substring2.isEmpty()) {
                    str = "/" + substring2;
                }
                return sb.append(str).toString();
            }
            return substring + ":/" + substring2;
        } catch (java.lang.Exception unused) {
            return "自定义目录";
        }
    }
}
