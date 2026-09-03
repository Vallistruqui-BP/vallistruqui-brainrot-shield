package com.vallistruqui.brainrotshield;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

final class BlackOverlay {
    private final Context context;
    private final WindowManager windowManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideRunnable = this::hide;

    private View overlayView;

    BlackOverlay(Context context) {
        this.context = context;
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    boolean show(CharSequence title, CharSequence detail, long durationMs) {
        hide();

        FrameLayout container = new FrameLayout(context);
        container.setBackgroundColor(Color.BLACK);
        container.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);

        LinearLayout messageGroup = new LinearLayout(context);
        messageGroup.setOrientation(LinearLayout.VERTICAL);
        messageGroup.setGravity(Gravity.CENTER_HORIZONTAL);
        messageGroup.setPadding(dp(32), dp(32), dp(32), dp(32));

        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.ic_shield_white);
        icon.setContentDescription(null);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(88), dp(88));
        messageGroup.addView(icon, iconParams);

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(30f);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(24);
        messageGroup.addView(titleView, titleParams);

        TextView detailView = new TextView(context);
        detailView.setText(detail);
        detailView.setTextColor(Color.rgb(213, 225, 230));
        detailView.setTextSize(17f);
        detailView.setGravity(Gravity.CENTER);
        detailView.setLineSpacing(0f, 1.15f);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        detailParams.topMargin = dp(14);
        messageGroup.addView(detailView, detailParams);

        FrameLayout.LayoutParams messageParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        container.addView(messageGroup, messageParams);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.OPAQUE);
        params.gravity = Gravity.FILL;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        try {
            windowManager.addView(container, params);
            overlayView = container;
            mainHandler.postDelayed(hideRunnable, durationMs);
            return true;
        } catch (RuntimeException exception) {
            overlayView = null;
            return false;
        }
    }

    void hide() {
        mainHandler.removeCallbacks(hideRunnable);
        if (overlayView == null) {
            return;
        }

        View viewToRemove = overlayView;
        overlayView = null;
        try {
            windowManager.removeViewImmediate(viewToRemove);
        } catch (RuntimeException ignored) {
            // The system may already have removed the overlay during service teardown.
        }
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
