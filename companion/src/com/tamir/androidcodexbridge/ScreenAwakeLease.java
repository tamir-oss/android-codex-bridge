package com.tamir.androidcodexbridge;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;

/** Main-thread-only, volatile lease. Never changes keyguard or system settings. */
final class ScreenAwakeLease {
    private final Context context;
    private final Handler handler;
    private TextView badge;
    private String leaseId;
    private long deadline;
    private boolean registered;
    private String lastEnd = "not_started";
    private final Runnable watchdog = new Runnable() {
        @Override public void run() {
            check();
            if (leaseId != null) handler.postDelayed(this, Math.min(1000, remaining()));
        }
    };
    private final BroadcastReceiver screenOff = new BroadcastReceiver() {
        @Override public void onReceive(Context ignored, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) release("screen_off");
        }
    };

    ScreenAwakeLease(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;
    }

    private boolean available() {
        KeyguardManager keyguard = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return keyguard != null && !keyguard.isKeyguardLocked() && !keyguard.isDeviceLocked()
                && power != null && power.isInteractive();
    }

    private long remaining() { return Math.max(0, deadline - SystemClock.elapsedRealtime()); }

    private void check() {
        if (leaseId == null) return;
        if (!available()) release("screen_off_or_locked");
        else if (remaining() == 0) release("expired");
    }

    JSONObject status() throws JSONException {
        check();
        return new JSONObject().put("active", leaseId != null)
                .put("remaining_ms", leaseId == null ? 0 : remaining())
                .put("mechanism", "visible_accessibility_overlay")
                .put("last_end", lastEnd).put("max_seconds", 600);
    }

    JSONObject command(JSONObject args) throws JSONException {
        check();
        String action = args.optString("action", "");
        if (!action.equals("start") && !action.equals("renew") && !action.equals("stop"))
            throw new IllegalArgumentException("BAD_AWAKE_ACTION");
        if (!action.equals("start") && (leaseId == null
                || !leaseId.equals(args.optString("lease_id", ""))))
            throw new IllegalArgumentException("STALE_AWAKE_LEASE");
        if (action.equals("stop")) {
            release("released");
            return status();
        }
        Object rawSeconds = args.opt("seconds");
        if (!(rawSeconds instanceof Integer) || ((Integer) rawSeconds) < 5
                || ((Integer) rawSeconds) > 600)
            throw new IllegalArgumentException("AWAKE_SECONDS_OUT_OF_RANGE");
        int seconds = (Integer) rawSeconds;
        if (!available()) throw new IllegalArgumentException("UI_REQUIRES_UNLOCK");
        if (action.equals("start")) {
            if (leaseId != null) throw new IllegalArgumentException("AWAKE_LEASE_BUSY");
            try {
                context.registerReceiver(screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF));
                registered = true;
                badge = new TextView(context);
                badge.setText("Codex · מסך פעיל זמנית");
                badge.setTextSize(11);
                badge.setTextColor(Color.WHITE);
                badge.setBackgroundColor(0xdd263238);
                badge.setPadding(8, 4, 8, 4);
                badge.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                        PixelFormat.TRANSLUCENT);
                params.gravity = Gravity.TOP | Gravity.END;
                params.setTitle("CodexTaskAwake");
                ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).addView(badge, params);
                leaseId = UUID.randomUUID().toString();
            } catch (RuntimeException error) {
                release("overlay_unavailable");
                throw new IllegalArgumentException("AWAKE_UNAVAILABLE");
            }
        }
        deadline = SystemClock.elapsedRealtime() + seconds * 1000L;
        handler.removeCallbacks(watchdog);
        handler.postDelayed(watchdog, 1000);
        JSONObject result = status();
        if (leaseId == null) throw new IllegalArgumentException("UI_REQUIRES_UNLOCK");
        return result.put("lease_id", leaseId);
    }

    void release(String reason) {
        handler.removeCallbacks(watchdog);
        if (badge != null) {
            try {
                ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).removeViewImmediate(badge);
            } catch (IllegalArgumentException ignored) { /* Window already gone. */ }
            badge = null;
        }
        if (registered) {
            context.unregisterReceiver(screenOff);
            registered = false;
        }
        leaseId = null;
        deadline = 0;
        lastEnd = reason;
    }
}
