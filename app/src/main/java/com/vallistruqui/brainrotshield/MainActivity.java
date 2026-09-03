package com.vallistruqui.brainrotshield;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private TextView accessibilityStatus;
    private TextView youtubeStatus;
    private Button youtubeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accessibilityStatus = findViewById(R.id.accessibility_status);
        youtubeStatus = findViewById(R.id.youtube_status);
        youtubeButton = findViewById(R.id.open_youtube_button);

        findViewById(R.id.accessibility_settings_button).setOnClickListener(
                ignored -> showAccessibilityDisclosure());
        youtubeButton.setOnClickListener(ignored -> openYoutube());

        applySystemBarInsets(findViewById(R.id.page_root));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {
        boolean serviceEnabled = isAccessibilityServiceEnabled();
        boolean youtubeInstalled = getPackageManager()
                .getLaunchIntentForPackage(ShortsBlockerAccessibilityService.YOUTUBE_PACKAGE) != null;

        setStatus(accessibilityStatus, serviceEnabled,
                R.string.status_active, R.string.status_inactive);
        setStatus(youtubeStatus, youtubeInstalled,
                R.string.status_installed, R.string.status_not_found);

        youtubeButton.setEnabled(youtubeInstalled);
        youtubeButton.setAlpha(youtubeInstalled ? 1f : 0.5f);
    }

    private void setStatus(TextView view, boolean positive, int positiveText, int negativeText) {
        view.setText(positive ? positiveText : negativeText);
        view.setTextColor(getColor(positive ? R.color.status_ok_text : R.color.status_warning_text));
        view.setBackgroundResource(
                positive ? R.drawable.status_ok_background : R.drawable.status_warning_background);
    }

    private void showAccessibilityDisclosure() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.accessibility_disclosure_title)
                .setMessage(R.string.accessibility_disclosure_body)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.accept_and_continue,
                        (dialog, which) -> startActivity(
                                new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                .show();
    }

    private void openYoutube() {
        Intent launchIntent = getPackageManager()
                .getLaunchIntentForPackage(ShortsBlockerAccessibilityService.YOUTUBE_PACKAGE);
        if (launchIntent == null) {
            Toast.makeText(this, R.string.youtube_not_installed, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(launchIntent);
    }

    private boolean isAccessibilityServiceEnabled() {
        ComponentName expected = new ComponentName(this, ShortsBlockerAccessibilityService.class);
        String enabledServices = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

        if (TextUtils.isEmpty(enabledServices)) {
            return false;
        }

        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabledServices);
        while (splitter.hasNext()) {
            ComponentName enabled = ComponentName.unflattenFromString(splitter.next());
            if (expected.equals(enabled)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private void applySystemBarInsets(View root) {
        int initialLeft = root.getPaddingLeft();
        int initialTop = root.getPaddingTop();
        int initialRight = root.getPaddingRight();
        int initialBottom = root.getPaddingBottom();

        root.setOnApplyWindowInsetsListener((view, windowInsets) -> {
            int left;
            int top;
            int right;
            int bottom;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets bars = windowInsets.getInsets(WindowInsets.Type.systemBars());
                left = bars.left;
                top = bars.top;
                right = bars.right;
                bottom = bars.bottom;
            } else {
                left = windowInsets.getSystemWindowInsetLeft();
                top = windowInsets.getSystemWindowInsetTop();
                right = windowInsets.getSystemWindowInsetRight();
                bottom = windowInsets.getSystemWindowInsetBottom();
            }

            view.setPadding(
                    initialLeft + left,
                    initialTop + top,
                    initialRight + right,
                    initialBottom + bottom);
            return windowInsets;
        });
        root.requestApplyInsets();
    }
}
