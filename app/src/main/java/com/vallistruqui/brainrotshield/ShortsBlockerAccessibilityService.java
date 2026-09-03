package com.vallistruqui.brainrotshield;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public final class ShortsBlockerAccessibilityService extends AccessibilityService {
    public static final String YOUTUBE_PACKAGE = "com.google.android.youtube";

    private static final String LOG_TAG = "ShortsShield";
    private static final long TREE_SETTLE_DELAY_MS = 120L;
    private static final long BLOCK_COOLDOWN_MS = 2_500L;
    private static final long OVERLAY_DURATION_MS = 450L;
    private static final long VOLUME_RESTORE_DELAY_MS = 650L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable scanRunnable = this::scanActiveWindow;
    private final Runnable restoreVolumeRunnable = this::restoreMediaVolume;

    private AudioManager audioManager;
    private BlackOverlay blackOverlay;
    private long lastBlockAt;
    private int savedMediaVolume = -1;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        audioManager = getSystemService(AudioManager.class);
        blackOverlay = new BlackOverlay(this);

        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.packageNames = new String[]{YOUTUBE_PACKAGE};
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                    | AccessibilityEvent.TYPE_VIEW_CLICKED
                    | AccessibilityEvent.TYPE_VIEW_SCROLLED;
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            info.notificationTimeout = 100L;
            info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                    | AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
                    | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            setServiceInfo(info);
        }

        Log.i(LOG_TAG, "Accessibility service connected for YouTube only");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) {
            return;
        }
        if (!YOUTUBE_PACKAGE.contentEquals(event.getPackageName())) {
            return;
        }

        mainHandler.removeCallbacks(scanRunnable);
        mainHandler.postDelayed(scanRunnable, TREE_SETTLE_DELAY_MS);
    }

    private void scanActiveWindow() {
        if (SystemClock.elapsedRealtime() - lastBlockAt < BLOCK_COOLDOWN_MS) {
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }

        ShortsSignals signals = YouTubeShortsDetector.inspect(root);
        if (!signals.indicatesShorts()) {
            return;
        }

        Log.i(LOG_TAG, "Shorts detected: " + signals.summary());
        blockCurrentShort();
    }

    private void blockCurrentShort() {
        lastBlockAt = SystemClock.elapsedRealtime();
        mainHandler.removeCallbacks(restoreVolumeRunnable);
        muteMediaVolume();

        if (blackOverlay != null && !blackOverlay.show(OVERLAY_DURATION_MS)) {
            Log.w(LOG_TAG, "Accessibility overlay is unavailable; continuing with Back action");
        }

        boolean navigatedBack = performGlobalAction(GLOBAL_ACTION_BACK);
        if (!navigatedBack) {
            Log.w(LOG_TAG, "Android did not accept GLOBAL_ACTION_BACK");
        }

        mainHandler.postDelayed(restoreVolumeRunnable, VOLUME_RESTORE_DELAY_MS);
    }

    private void muteMediaVolume() {
        if (audioManager == null || savedMediaVolume >= 0) {
            return;
        }

        try {
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            savedMediaVolume = currentVolume;
            if (currentVolume > 0) {
                audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        0,
                        AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            }
        } catch (RuntimeException exception) {
            savedMediaVolume = -1;
            Log.e(LOG_TAG, "Unable to mute media volume", exception);
        }
    }

    private void restoreMediaVolume() {
        if (audioManager == null || savedMediaVolume < 0) {
            return;
        }

        int volumeToRestore = savedMediaVolume;
        savedMediaVolume = -1;
        if (volumeToRestore == 0) {
            return;
        }

        try {
            if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
                audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        volumeToRestore,
                        AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            }
        } catch (RuntimeException exception) {
            Log.e(LOG_TAG, "Unable to restore media volume", exception);
        }
    }

    @Override
    public void onInterrupt() {
        mainHandler.removeCallbacks(scanRunnable);
        restoreMediaVolume();
        if (blackOverlay != null) {
            blackOverlay.hide();
        }
    }

    @Override
    public void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        restoreMediaVolume();
        if (blackOverlay != null) {
            blackOverlay.hide();
        }
        super.onDestroy();
    }
}
