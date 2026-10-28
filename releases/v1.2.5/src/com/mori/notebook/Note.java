package com.mori.notebook;

/* loaded from: classes.dex */
public class Note {
    public java.lang.String content;
    public java.lang.String id;
    public java.lang.String title;
    public int type;
    public long updatedAt;

    public Note() {
        this.id = java.util.UUID.randomUUID().toString();
        this.title = "";
        this.content = "";
        this.type = 0;
        this.updatedAt = java.lang.System.currentTimeMillis();
    }

    public Note(java.lang.String str, java.lang.String str2, java.lang.String str3, int i, long j) {
        this.id = str;
        this.title = str2;
        this.content = str3;
        this.type = i;
        this.updatedAt = j;
    }

    public org.json.JSONObject toJson() throws org.json.JSONException {
        org.json.JSONObject jSONObject = new org.json.JSONObject();
        jSONObject.put("id", this.id);
        java.lang.String str = this.title;
        if (str == null) {
            str = "";
        }
        jSONObject.put("title", str);
        java.lang.String str2 = this.content;
        jSONObject.put("content", str2 != null ? str2 : "");
        jSONObject.put("type", this.type);
        jSONObject.put("updatedAt", this.updatedAt);
        return jSONObject;
    }

    public static com.mori.notebook.Note fromJson(org.json.JSONObject jSONObject) {
        return new com.mori.notebook.Note(jSONObject.optString("id", java.util.UUID.randomUUID().toString()), jSONObject.optString("title", ""), jSONObject.optString("content", ""), jSONObject.optInt("type", 0), jSONObject.optLong("updatedAt", java.lang.System.currentTimeMillis()));
    }
}
