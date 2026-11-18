package com.mori.notebook;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * 隐私笔记密码设置。
 * mode：0 = 未设置，1 = 数字密码（加盐 SHA-256），2 = 手机锁屏密码
 */
public class PrivacyPrefs {

    private static final String P = "privacy_prefs";
    private static final String K_MODE = "mode";
    private static final String K_SALT = "salt";
    private static final String K_HASH = "hash";

    private PrivacyPrefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(P, Context.MODE_PRIVATE);
    }

    public static boolean isConfigured(Context c) {
        return getMode(c) != 0;
    }

    public static int getMode(Context c) {
        return sp(c).getInt(K_MODE, 0);
    }

    public static boolean isDeviceMode(Context c) {
        return getMode(c) == 2;
    }

    /** 改为使用手机锁屏密码 */
    public static void useDeviceCredential(Context c) {
        sp(c).edit().putInt(K_MODE, 2).remove(K_SALT).remove(K_HASH).apply();
    }

    /** 设置数字密码 */
    public static void setPin(Context c, String pin) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String s = hex(salt);
        sp(c).edit().putInt(K_MODE, 1).putString(K_SALT, s).putString(K_HASH, hash(s, pin)).apply();
    }

    public static boolean verifyPin(Context c, String pin) {
        String salt = sp(c).getString(K_SALT, null);
        String hash = sp(c).getString(K_HASH, null);
        if (salt == null || hash == null || pin == null) return false;
        return hash.equals(hash(salt, pin));
    }

    /** 本机是否已设置锁屏密码 */
    public static boolean hasDeviceCredential(Context c) {
        KeyguardManager km = (KeyguardManager) c.getSystemService(Context.KEYGUARD_SERVICE);
        return km != null && km.isDeviceSecure();
    }

    /** 调用系统验证锁屏密码；不可用时返回 null */
    public static Intent deviceCredentialIntent(Context c, String title, String description) {
        KeyguardManager km = (KeyguardManager) c.getSystemService(Context.KEYGUARD_SERVICE);
        if (km == null || !km.isDeviceSecure()) return null;
        return km.createConfirmDeviceCredentialIntent(title, description);
    }

    private static String hash(String salt, String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((salt + ":" + pin).getBytes("UTF-8"));
            return hex(md.digest());
        } catch (Exception e) {
            return "";
        }
    }

    private static String hex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
