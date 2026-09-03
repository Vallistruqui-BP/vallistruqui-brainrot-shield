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

    boolean show(long durationMs) {
        hide();

        FrameLayout container = new FrameLayout(context);
        container.setBackgroundColor(Color.BLACK);
        container.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);

        TextView message = new TextView(context);
        message.setText(R.string.overlay_message);
        message.setTextColor(Color.WHITE);
        message.setTextSize(18f);
        message.setGravity(Gravity.CENTER);
        message.setAlpha(0.92f);
        message.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_shield_white, 0, 0);
        message.setCompoundDrawablePadding(dp(14));

        FrameLayout.LayoutParams messageParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        container.addView(message, messageParams);

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
