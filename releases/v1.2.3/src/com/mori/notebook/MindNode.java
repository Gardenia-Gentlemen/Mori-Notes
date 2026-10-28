package com.mori.notebook;

/* loaded from: classes.dex */
public class MindNode {
    public final java.util.List<com.mori.notebook.MindNode> children;
    public java.lang.String id;
    public java.lang.String text;

    public MindNode() {
        this.children = new java.util.ArrayList();
        this.id = java.util.UUID.randomUUID().toString();
        this.text = "";
    }

    public MindNode(java.lang.String str, java.lang.String str2) {
        this.children = new java.util.ArrayList();
        this.id = str;
        this.text = str2;
    }

    public org.json.JSONObject toJson() throws java.lang.Exception {
        org.json.JSONObject jSONObject = new org.json.JSONObject();
        jSONObject.put("id", this.id);
        java.lang.String str = this.text;
        if (str == null) {
            str = "";
        }
        jSONObject.put("text", str);
        org.json.JSONArray jSONArray = new org.json.JSONArray();
        java.util.Iterator<com.mori.notebook.MindNode> it = this.children.iterator();
        while (it.hasNext()) {
            jSONArray.put(it.next().toJson());
        }
        jSONObject.put("children", jSONArray);
        return jSONObject;
    }

    public static com.mori.notebook.MindNode fromJson(org.json.JSONObject jSONObject) {
        com.mori.notebook.MindNode mindNode = new com.mori.notebook.MindNode(jSONObject.optString("id", java.util.UUID.randomUUID().toString()), jSONObject.optString("text", ""));
        org.json.JSONArray optJSONArray = jSONObject.optJSONArray("children");
        if (optJSONArray != null) {
            for (int i = 0; i < optJSONArray.length(); i++) {
                org.json.JSONObject optJSONObject = optJSONArray.optJSONObject(i);
                if (optJSONObject != null) {
                    mindNode.children.add(fromJson(optJSONObject));
                }
            }
        }
        return mindNode;
    }

    public static java.lang.String serialize(java.util.List<com.mori.notebook.MindNode> list) {
        try {
            org.json.JSONArray jSONArray = new org.json.JSONArray();
            java.util.Iterator<com.mori.notebook.MindNode> it = list.iterator();
            while (it.hasNext()) {
                jSONArray.put(it.next().toJson());
            }
            return jSONArray.toString();
        } catch (java.lang.Exception unused) {
            return "[]";
        }
    }

    public static java.util.List<com.mori.notebook.MindNode> parse(java.lang.String str) {
        java.util.ArrayList arrayList = new java.util.ArrayList();
        if (str != null && !str.trim().isEmpty()) {
            try {
                org.json.JSONArray jSONArray = new org.json.JSONArray(str);
                for (int i = 0; i < jSONArray.length(); i++) {
                    org.json.JSONObject optJSONObject = jSONArray.optJSONObject(i);
                    if (optJSONObject != null) {
                        arrayList.add(fromJson(optJSONObject));
                    }
                }
            } catch (java.lang.Exception unused) {
            }
        }
        return arrayList;
    }
}
