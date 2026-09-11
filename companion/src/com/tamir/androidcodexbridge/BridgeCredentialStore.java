package com.tamir.androidcodexbridge;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.SecureRandom;

final class BridgeCredentialStore {
    private static final String PREFERENCES = "local_bridge_credentials";
    private static final String TOKEN_KEY = "termux_token_v1";

    private BridgeCredentialStore() {}

    static String getOrCreate(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        String existing = preferences.getString(TOKEN_KEY, null);
        if (existing != null && existing.matches("[0-9a-f]{64}")) return existing;
        return rotate(context);
    }

    static String rotate(Context context) {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        StringBuilder token = new StringBuilder(64);
        for (byte value : bytes) token.append(String.format("%02x", value & 0xff));
        String result = token.toString();
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putString(TOKEN_KEY, result)
                .apply();
        return result;
    }
}
