package com.mori.notebook;

/* loaded from: classes.dex */
public class NoteStore {
    private static com.mori.notebook.NoteStore instance;
    private final java.io.File file;
    private final java.util.List<com.mori.notebook.Note> notes = new java.util.ArrayList();

    private NoteStore(android.content.Context context) {
        this.file = new java.io.File(context.getFilesDir(), "notes.json");
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

    private void load() {
        int i;
        this.notes.clear();
        if (this.file.exists()) {
            try {
                java.io.FileInputStream fileInputStream = new java.io.FileInputStream(this.file);
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
                    org.json.JSONArray jSONArray = new org.json.JSONArray(new java.lang.String(byteArrayOutputStream.toByteArray(), java.nio.charset.StandardCharsets.UTF_8));
                    for (i = 0; i < jSONArray.length(); i++) {
                        this.notes.add(com.mori.notebook.Note.fromJson(jSONArray.getJSONObject(i)));
                    }
                    fileInputStream.close();
                } finally {
                }
            } catch (java.lang.Exception unused) {
            }
        }
    }

    public void save() {
        try {
            org.json.JSONArray jSONArray = new org.json.JSONArray();
            java.util.Iterator<com.mori.notebook.Note> it = this.notes.iterator();
            while (it.hasNext()) {
                jSONArray.put(it.next().toJson());
            }
            java.io.FileOutputStream fileOutputStream = new java.io.FileOutputStream(this.file);
            try {
                fileOutputStream.write(jSONArray.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
