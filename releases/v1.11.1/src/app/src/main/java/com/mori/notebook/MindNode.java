package com.mori.notebook;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MindNode {
    public String id;
    public String text;
    public final List<MindNode> children = new ArrayList<>();

    public MindNode() {
        id = UUID.randomUUID().toString();
        text = "";
    }

    public MindNode(String id, String text) {
        this.id = id;
        this.text = text;
    }

    public JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("text", text == null ? "" : text);
        JSONArray arr = new JSONArray();
        for (MindNode c : children) arr.put(c.toJson());
        o.put("children", arr);
        return o;
    }

    public static MindNode fromJson(JSONObject o) {
        MindNode n = new MindNode(o.optString("id", UUID.randomUUID().toString()), o.optString("text", ""));
        JSONArray arr = o.optJSONArray("children");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject c = arr.optJSONObject(i);
                if (c != null) n.children.add(fromJson(c));
            }
        }
        return n;
    }

    public static String serialize(List<MindNode> roots) {
        try {
            JSONArray arr = new JSONArray();
            for (MindNode n : roots) arr.put(n.toJson());
            return arr.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public static List<MindNode> parse(String json) {
        List<MindNode> roots = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return roots;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) roots.add(fromJson(o));
            }
        } catch (Exception ignored) {
        }
        return roots;
    }

    /** Miro 风格思维导图初始模板：中心主题 + 3 个分主题 + 各 2 个想法。 */
    public static String miroTemplate() {
        try {
            JSONArray arr = new JSONArray();
            MindNode root = new MindNode(null, "中心主题");
            String[] subs = {"分主题 1", "分主题 2", "分主题 3"};
            for (String s : subs) {
                MindNode n = new MindNode(null, s);
                n.children.add(new MindNode(null, "想法"));
                n.children.add(new MindNode(null, "行动"));
                root.children.add(n);
            }
            arr.put(root.toJson());
            return arr.toString();
        } catch (Exception e) {
            return "[]";
        }
    }
}
