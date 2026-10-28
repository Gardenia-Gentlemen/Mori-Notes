package com.mori.notebook;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * .mori 整机备份文件。
 *
 * 文件结构（JSON，UTF-8）：
 * {
 *   "magic": "MORI",
 *   "version": 1,
 *   "scope": "normal" | "private" | "all",
 *   "encrypted": true/false,
 *   "createdAt": 毫秒时间戳,
 *   "count": 笔记条数,
 *   "salt": Base64(16 字节盐),   // 仅加密时
 *   "iv": Base64(12 字节 IV),    // 仅加密时
 *   "data": 笔记数组 JSON 字符串（明文）或 Base64(AES/GCM 密文)
 * }
 *
 * 加密：PBKDF2WithHmacSHA256(8 位数字密码, salt, 12000 次) -> 256 位密钥，
 *      AES/GCM/NoPadding，128 位 tag。GCM 自带完整性校验，密码错误会抛 BadPasswordException。
 */
public final class MoriFile {

    public static final String EXT = ".mori";
    public static final String SCOPE_NORMAL = "normal";
    public static final String SCOPE_PRIVATE = "private";
    public static final String SCOPE_ALL = "all";

    private static final String MAGIC = "MORI";
    private static final int VERSION = 1;
    private static final int ITERATIONS = 12000;
    private static final int KEY_BITS = 256;

    private MoriFile() {
    }

    /** 密码错误（GCM tag 校验失败） */
    public static class BadPasswordException extends Exception {
        public BadPasswordException() {
            super("密码错误");
        }
    }

    public static class Package {
        public boolean encrypted;
        public String scope = SCOPE_ALL;
        public int count;
        public long createdAt;
        public final List<Note> notes = new ArrayList<>();
    }

    public static boolean isValidPin(String pin) {
        if (pin == null || pin.length() != 8) return false;
        for (int i = 0; i < pin.length(); i++) {
            char c = pin.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    /** 生成 .mori 文件内容 */
    public static byte[] build(List<Note> notes, String scope, boolean encrypted, String pin)
            throws Exception {
        JSONArray arr = new JSONArray();
        if (notes != null) {
            for (Note n : notes) {
                if (n != null) arr.put(n.toJson());
            }
        }
        String payload = arr.toString();
        JSONObject o = new JSONObject();
        o.put("magic", MAGIC);
        o.put("version", VERSION);
        o.put("scope", scope == null ? SCOPE_ALL : scope);
        o.put("encrypted", encrypted);
        o.put("createdAt", System.currentTimeMillis());
        o.put("count", arr.length());
        if (encrypted) {
            byte[] salt = new byte[16];
            byte[] iv = new byte[12];
            SecureRandom rnd = new SecureRandom();
            rnd.nextBytes(salt);
            rnd.nextBytes(iv);
            byte[] cipherText = cipher(Cipher.ENCRYPT_MODE, pin, salt, iv,
                    payload.getBytes(StandardCharsets.UTF_8));
            o.put("salt", b64(salt));
            o.put("iv", b64(iv));
            o.put("data", b64(cipherText));
        } else {
            o.put("data", payload);
        }
        return o.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** 只读文件头，用于判断是否需要密码 */
    public static JSONObject head(String text) throws Exception {
        if (text == null) throw new Exception("空文件");
        JSONObject o = new JSONObject(text);
        if (!MAGIC.equals(o.optString("magic", ""))) throw new Exception("不是 .mori 文件");
        return o;
    }

    /** 解析文件；未加密时 pin 可传 null */
    public static Package parse(String text, String pin) throws Exception {
        JSONObject o = head(text);
        Package pkg = new Package();
        pkg.encrypted = o.optBoolean("encrypted", false);
        pkg.scope = o.optString("scope", SCOPE_ALL);
        pkg.count = o.optInt("count", 0);
        pkg.createdAt = o.optLong("createdAt", 0L);

        String data = o.optString("data", "");
        String payload;
        if (pkg.encrypted) {
            if (pin == null) throw new BadPasswordException();
            byte[] salt = Base64.decode(o.optString("salt", ""), Base64.NO_WRAP);
            byte[] iv = Base64.decode(o.optString("iv", ""), Base64.NO_WRAP);
            byte[] ct = Base64.decode(data, Base64.NO_WRAP);
            byte[] plain = cipher(Cipher.DECRYPT_MODE, pin, salt, iv, ct);
            payload = new String(plain, StandardCharsets.UTF_8);
        } else {
            payload = data;
        }

        JSONArray arr = new JSONArray(payload);
        for (int i = 0; i < arr.length(); i++) {
            pkg.notes.add(Note.fromJson(arr.getJSONObject(i)));
        }
        return pkg;
    }

    private static byte[] cipher(int mode, String pin, byte[] salt, byte[] iv, byte[] input)
            throws Exception {
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(pin == null ? new char[0] : pin.toCharArray(), salt,
                ITERATIONS, KEY_BITS);
        byte[] key = f.generateSecret(spec).getEncoded();
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            return c.doFinal(input);
        } catch (javax.crypto.AEADBadTagException e) {
            throw new BadPasswordException();
        } catch (javax.crypto.BadPaddingException e) {
            throw new BadPasswordException();
        }
    }

    private static String b64(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }
}
