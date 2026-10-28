package com.mori.notebook;

/* loaded from: classes.dex */
public class NoteStore {
    private static com.mori.notebook.NoteStore instance;
    private final android.content.Context app;
    private final java.io.File defaultFile;
    private final java.util.List<com.mori.notebook.Note> notes = new java.util.ArrayList();
    private android.net.Uri tree;

    private NoteStore(android.content.Context context) {
        this.app = context;
        this.defaultFile = new java.io.File(context.getFilesDir(), com.mori.notebook.StoreFile.NAME);
        this.tree = com.mori.notebook.UiPrefs.getTreeUri(context);
        load();
    }

    public static synchronized com.mori.notebook.NoteStore get(android.content.Context context) {
        com.mori.notebook.NoteStore noteStore;
        synchronized (com.mori.notebook.NoteStore.class) {
            if (instance == null) {
                instance = new com.mori.notebook.NoteStore(context.getApplicationContext());
            }
            noteStore = instance;
        }
        return noteStore;
    }

    public android.net.Uri getTree() {
        return this.tree;
    }

    public java.lang.String locationLabel() {
        return com.mori.notebook.StoreFile.describe(this.tree);
    }

    public void reload() {
        load();
    }

    public boolean switchLocation(android.net.Uri uri) {
        java.lang.String read;
        this.tree = uri;
        com.mori.notebook.UiPrefs.setTreeUri(this.app, uri);
        android.net.Uri uri2 = this.tree;
        if (uri2 != null) {
            try {
                read = com.mori.notebook.StoreFile.read(this.app, uri2);
            } catch (java.lang.Exception unused) {
            }
            if (read == null && !read.trim().isEmpty()) {
                load();
                return true;
            }
            save();
            return false;
        }
        read = null;
        if (read == null) {
        }
        save();
        return false;
    }

    public void useInternal() {
        this.tree = null;
        com.mori.notebook.UiPrefs.setTreeUri(this.app, null);
        try {
            if (this.defaultFile.exists()) {
                load();
            } else {
                save();
            }
        } catch (java.lang.Exception unused) {
            save();
        }
    }

    public void exportTo(android.net.Uri uri) {
        try {
            com.mori.notebook.StoreFile.write(this.app, uri, serialize());
        } catch (java.lang.Exception unused) {
        }
    }

    private void load() {
        this.notes.clear();
        java.lang.String str = null;
        try {
            android.net.Uri uri = this.tree;
            if (uri != null) {
                str = com.mori.notebook.StoreFile.read(this.app, uri);
            } else if (this.defaultFile.exists()) {
                java.io.FileInputStream fileInputStream = new java.io.FileInputStream(this.defaultFile);
                try {
                    java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
                    byte[] bArr = new byte[8192];
                    while (true) {
                        int read = fileInputStream.read(bArr);
                        if (read <= 0) {
                            break;
                        } else {
                            byteArrayOutputStream.write(bArr, 0, read);
                        }
                    }
                    java.lang.String str2 = new java.lang.String(byteArrayOutputStream.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
                    try {
                        fileInputStream.close();
                    } catch (java.lang.Exception unused) {
                    }
                    str = str2;
                } finally {
                }
            }
        } catch (java.lang.Exception unused2) {
        }
        if (str == null || str.trim().isEmpty()) {
            return;
        }
        try {
            org.json.JSONArray jSONArray = new org.json.JSONArray(str);
            for (int i = 0; i < jSONArray.length(); i++) {
                this.notes.add(com.mori.notebook.Note.fromJson(jSONArray.getJSONObject(i)));
            }
        } catch (java.lang.Exception unused3) {
        }
    }

    private java.lang.String serialize() {
        org.json.JSONArray jSONArray = new org.json.JSONArray();
        java.util.Iterator<com.mori.notebook.Note> it = this.notes.iterator();
        while (it.hasNext()) {
            try {
                jSONArray.put(it.next().toJson());
            } catch (java.lang.Exception unused) {
            }
        }
        return jSONArray.toString();
    }

    public void save() {
        java.lang.String serialize = serialize();
        try {
            android.net.Uri uri = this.tree;
            if (uri != null) {
                com.mori.notebook.StoreFile.write(this.app, uri, serialize);
                return;
            }
            java.io.FileOutputStream fileOutputStream = new java.io.FileOutputStream(this.defaultFile);
            try {
                fileOutputStream.write(serialize.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                fileOutputStream.close();
            } finally {
            }
        } catch (java.lang.Exception unused) {
        }
    }

    public java.util.List<com.mori.notebook.Note> getNotes() {
        java.util.ArrayList arrayList = new java.util.ArrayList(this.notes);
        java.util.Collections.sort(arrayList, new java.util.Comparator() { // from class: com.mori.notebook.NoteStore$$ExternalSyntheticLambda1
            @Override // java.util.Comparator
            public final int compare(java.lang.Object obj, java.lang.Object obj2) {
                int compare;
                compare = java.lang.Long.compare(((com.mori.notebook.Note) obj2).updatedAt, ((com.mori.notebook.Note) obj).updatedAt);
                return compare;
            }
        });
        return arrayList;
    }

    public com.mori.notebook.Note find(java.lang.String str) {
        for (com.mori.notebook.Note note : this.notes) {
            if (note.id.equals(str)) {
                return note;
            }
        }
        return null;
    }

    public void put(com.mori.notebook.Note note) {
        com.mori.notebook.Note find = find(note.id);
        if (find != null) {
            find.title = note.title;
            find.content = note.content;
            find.type = note.type;
            find.updatedAt = note.updatedAt;
        } else {
            this.notes.add(note);
        }
        save();
    }

    public void delete(final java.lang.String str) {
        this.notes.removeIf(new java.util.function.Predicate() { // from class: com.mori.notebook.NoteStore$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(java.lang.Object obj) {
                boolean equals;
                equals = ((com.mori.notebook.Note) obj).id.equals(str);
                return equals;
            }
        });
        save();
    }
}
