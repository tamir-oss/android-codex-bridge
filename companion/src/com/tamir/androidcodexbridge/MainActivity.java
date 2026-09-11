package com.tamir.androidcodexbridge;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.app.AlertDialog;
import android.content.DialogInterface;

import java.util.List;

public final class MainActivity extends Activity {
    private TextView serviceStatus;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Android Codex Bridge");
        setContentView(buildTestScreen());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();
    }

    private View buildTestScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(24), dp(20), dp(32));
        scroll.addView(body, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("Android Codex Bridge — מסך בדיקה", 24, Color.BLACK);
        title.setGravity(Gravity.START);
        body.addView(title, matchWrap());

        TextView explanation = text(
                "שירות הנגישות מופעל רק באישור מפורש. החיבור מאזין רק ב־127.0.0.1 ודורש מפתח מקומי.",
                16, Color.DKGRAY);
        body.addView(explanation, withTop(10));

        serviceStatus = text("", 16, Color.rgb(31, 41, 55));
        serviceStatus.setId(R.id.service_status);
        body.addView(serviceStatus, withTop(14));

        Button settingsButton = button("פתיחת הגדרות נגישות");
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });
        body.addView(settingsButton, withTop(10));

        Button copyToken = button("העתק מפתח צימוד ל־Termux");
        copyToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String token = BridgeCredentialStore.getOrCreate(MainActivity.this);
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("Android Codex Bridge token", token));
                serviceStatus.setText("המפתח הועתק. עבור ל־Termux והריץ: android-ui pair-from-clipboard");
            }
        });
        body.addView(copyToken, withTop(8));

        Button rotateToken = button("בטל מפתח קיים וצור חדש");
        rotateToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("החלפת מפתח")
                        .setMessage("המפתח השמור ב־Termux יפסיק לעבוד עד לצימוד מחדש.")
                        .setNegativeButton("ביטול", null)
                        .setPositiveButton("החלף", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                BridgeCredentialStore.rotate(MainActivity.this);
                                serviceStatus.setText("נוצר מפתח חדש. יש להעתיק ולצמד מחדש.");
                            }
                        })
                        .show();
            }
        });
        body.addView(rotateToken, withTop(8));

        EditText hebrew = input(R.id.input_hebrew, "עברית");
        EditText english = input(R.id.input_english, "English");
        EditText mixed = input(R.id.input_mixed, "עברית + English + 123");
        body.addView(hebrew, withTop(18));
        body.addView(english, withTop(10));
        body.addView(mixed, withTop(10));

        TextView actionResult = text("לא בוצעה פעולה", 15, Color.DKGRAY);
        body.addView(actionResult, withTop(12));

        Button primary = button("בדוק כפתור");
        primary.setId(R.id.button_primary);
        primary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int filled = 0;
                if (!TextUtils.isEmpty(hebrew.getText())) filled++;
                if (!TextUtils.isEmpty(english.getText())) filled++;
                if (!TextUtils.isEmpty(mixed.getText())) filled++;
                actionResult.setText("הכפתור הופעל; שדות שאינם ריקים: " + filled);
            }
        });
        body.addView(primary, withTop(10));

        Button clear = button("נקה שדות");
        clear.setId(R.id.button_clear);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hebrew.setText("");
                english.setText("");
                mixed.setText("");
                actionResult.setText("השדות נוקו");
            }
        });
        body.addView(clear, withTop(8));

        for (int i = 1; i <= 9; i++) {
            body.addView(text("שורת גלילה " + i, 17, Color.DKGRAY), withTop(24));
        }

        Button bottom = button("כפתור בתחתית המסך");
        bottom.setId(R.id.button_bottom);
        bottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                actionResult.setText("הכפתור התחתון הופעל");
            }
        });
        body.addView(bottom, withTop(24));

        return scroll;
    }

    private void updateServiceStatus() {
        if (serviceStatus == null) return;
        boolean enabled = isBridgeServiceEnabled(this);
        serviceStatus.setText(enabled
                ? "שירות הנגישות: פעיל — חיבור מקומי מאומת מוכן"
                : "שירות הנגישות: כבוי — יש להפעיל ידנית");
        serviceStatus.setTextColor(enabled ? Color.rgb(0, 110, 60) : Color.rgb(170, 45, 45));
    }

    static boolean isBridgeServiceEnabled(Context context) {
        AccessibilityManager manager =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<android.accessibilityservice.AccessibilityServiceInfo> services =
                manager.getEnabledAccessibilityServiceList(
                        AccessibilityServiceInfoCompat.FEEDBACK_ALL_MASK);
        ComponentName expected = new ComponentName(context, BridgeAccessibilityService.class);
        for (android.accessibilityservice.AccessibilityServiceInfo info : services) {
            if (info.getResolveInfo() == null || info.getResolveInfo().serviceInfo == null) continue;
            ComponentName actual = new ComponentName(
                    info.getResolveInfo().serviceInfo.packageName,
                    info.getResolveInfo().serviceInfo.name);
            if (expected.equals(actual)) return true;
        }
        return false;
    }

    private static final class AccessibilityServiceInfoCompat {
        static final int FEEDBACK_ALL_MASK = -1;
    }

    private EditText input(int id, String hint) {
        EditText field = new EditText(this);
        field.setId(id);
        field.setHint(hint);
        field.setSingleLine(false);
        field.setMinLines(2);
        field.setTextSize(18);
        field.setGravity(Gravity.START | Gravity.TOP);
        field.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        return field;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        return button;
    }

    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams withTop(int topDp) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(topDp);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
