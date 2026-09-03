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
    public static final String INSTAGRAM_PACKAGE = "com.instagram.android";
    public static final String TIKTOK_PACKAGE = "com.zhiliaoapp.musically";

    private static final String LOG_TAG = "ShortsShield";
    private static final long TREE_SETTLE_DELAY_MS = 160L;
    private static final long BLOCK_COOLDOWN_MS = 2_500L;
    private static final long POLICY_TICK_MS = 15_000L;
    private static final long SHORT_OVERLAY_DURATION_MS = 800L;
    private static final long STRONG_OVERLAY_DURATION_MS = 1_800L;
    private static final long SHORT_VOLUME_RESTORE_DELAY_MS = 1_000L;
    private static final long STRONG_VOLUME_RESTORE_DELAY_MS = 2_100L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable scanRunnable = this::scanActiveWindow;
    private final Runnable restoreVolumeRunnable = this::restoreMediaVolume;
    private final Runnable policyTickRunnable = new Runnable() {
        @Override
        public void run() {
            enforceForegroundPolicy();
            mainHandler.postDelayed(this, POLICY_TICK_MS);
        }
    };

    private AudioManager audioManager;
    private BlackOverlay blackOverlay;
    private ShieldPreferences preferences;
    private UsageStatsTracker usageStatsTracker;
    private long lastBlockAt;
    private int savedMediaVolume = -1;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        audioManager = getSystemService(AudioManager.class);
        blackOverlay = new BlackOverlay(this);
        preferences = new ShieldPreferences(this);
        usageStatsTracker = new UsageStatsTracker(this);

        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.packageNames = ProtectedApp.packageNames();
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                    | AccessibilityEvent.TYPE_VIEW_SCROLLED;
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            info.notificationTimeout = 100L;
            info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
            setServiceInfo(info);
        }

        mainHandler.removeCallbacks(policyTickRunnable);
        mainHandler.post(policyTickRunnable);
        Log.i(LOG_TAG, "Accessibility service connected for YouTube, Instagram, and TikTok");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) {
            return;
        }
        ProtectedApp app = ProtectedApp.fromPackage(event.getPackageName());
        if (app == null || preferences == null || !preferences.isAppEnabled(app)) {
            return;
        }

        mainHandler.removeCallbacks(scanRunnable);
        mainHandler.postDelayed(scanRunnable, TREE_SETTLE_DELAY_MS);
    }

    private void scanActiveWindow() {
        if (preferences == null || SystemClock.elapsedRealtime() - lastBlockAt < BLOCK_COOLDOWN_MS) {
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }

        ProtectedApp app = ProtectedApp.fromPackage(root.getPackageName());
        if (app == null || !preferences.isAppEnabled(app)) {
            return;
        }

        evaluateAndBlock(app, root, System.currentTimeMillis());
    }

    private void enforceForegroundPolicy() {
        if (preferences == null) {
            return;
        }
        long now = System.currentTimeMillis();
        AccessibilityNodeInfo root = getRootInActiveWindow();
        ProtectedApp app = root == null
                ? null
                : ProtectedApp.fromPackage(root.getPackageName());
        if (app == null && usageStatsTracker != null && usageStatsTracker.hasAccess()) {
            app = ProtectedApp.fromPackage(usageStatsTracker.getCurrentForegroundPackage(now));
        }
        if (app == null || !preferences.isAppEnabled(app)) {
            return;
        }

        if (root != null && app == ProtectedApp.fromPackage(root.getPackageName())) {
            evaluateAndBlock(app, root, now);
        } else {
            evaluateAndBlock(app, null, now);
        }
    }

    private void evaluateAndBlock(ProtectedApp app, AccessibilityNodeInfo root, long nowMillis) {
        if (SystemClock.elapsedRealtime() - lastBlockAt < BLOCK_COOLDOWN_MS) {
            return;
        }

        TimeWindow activeWindow = preferences.getActiveTimeWindow(nowMillis);
        if (preferences.isFocusScheduleEnabled()
                && !preferences.isPaused(RestrictionType.FOCUS_SCHEDULE, nowMillis)
                && activeWindow != null) {
            blockCurrentApp(
                    RestrictionType.FOCUS_SCHEDULE,
                    app,
                    getString(R.string.overlay_schedule_title),
                    getString(R.string.overlay_schedule_detail,
                            app.displayName(), activeWindow.format()));
            return;
        }

        if (preferences.isDailyLimitEnabled()
                && !preferences.isPaused(RestrictionType.DAILY_LIMIT, nowMillis)
                && usageStatsTracker != null
                && usageStatsTracker.hasAccess()) {
            long usedMillis = usageStatsTracker.getTodayUsageMillis(
                    preferences.getEnabledApps(), nowMillis);
            long limitMillis = preferences.getDailyLimitMinutes() * 60_000L;
            if (usedMillis >= limitMillis) {
                blockCurrentApp(
                        RestrictionType.DAILY_LIMIT,
                        app,
                        getString(R.string.overlay_limit_title),
                        getString(R.string.overlay_limit_detail,
                                formatMinutes(usedMillis / 60_000L),
                                formatMinutes(preferences.getDailyLimitMinutes())));
                return;
            }
        }

        if (root == null
                || !preferences.isShortFormEnabled()
                || preferences.isPaused(RestrictionType.SHORT_FORM, nowMillis)) {
            return;
        }

        ShortFormSignals signals = ShortFormDetector.inspect(app, root);
        if (!signals.indicatesShortForm()) {
            return;
        }

        Log.i(LOG_TAG, "Short-form feed detected: " + signals.summary());
        blockCurrentApp(
                RestrictionType.SHORT_FORM,
                app,
                getString(R.string.overlay_shorts_title),
                getString(R.string.overlay_shorts_detail, app.displayName()));
    }

    private void blockCurrentApp(RestrictionType restriction, ProtectedApp app,
            CharSequence title, CharSequence detail) {
        lastBlockAt = SystemClock.elapsedRealtime();
        mainHandler.removeCallbacks(restoreVolumeRunnable);
        muteMediaVolume();

        boolean shortForm = restriction == RestrictionType.SHORT_FORM;
        long overlayDuration = shortForm
                ? SHORT_OVERLAY_DURATION_MS
                : STRONG_OVERLAY_DURATION_MS;
        if (blackOverlay != null && !blackOverlay.show(title, detail, overlayDuration)) {
            Log.w(LOG_TAG, "Accessibility overlay is unavailable; continuing with Back action");
        }

        int globalAction = shortForm ? GLOBAL_ACTION_BACK : GLOBAL_ACTION_HOME;
        boolean navigatedAway = performGlobalAction(globalAction);
        if (!navigatedAway) {
            Log.w(LOG_TAG, "Android did not accept global action for " + restriction
                    + " in " + app.displayName());
        }

        long restoreDelay = shortForm
                ? SHORT_VOLUME_RESTORE_DELAY_MS
                : STRONG_VOLUME_RESTORE_DELAY_MS;
        mainHandler.postDelayed(restoreVolumeRunnable, restoreDelay);
    }

    private static String formatMinutes(long totalMinutes) {
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        if (hours == 0L) {
            return minutes + " min";
        }
        if (minutes == 0L) {
            return hours + " h";
        }
        return hours + " h " + minutes + " min";
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
