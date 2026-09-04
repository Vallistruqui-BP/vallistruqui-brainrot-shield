package com.vallistruqui.brainrotshield;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ShieldPreferences {
    private static final String PREFERENCES_NAME = "brainrot_shield_preferences";
    private static final String KEY_APP_PREFIX = "app_enabled_";
    private static final String KEY_SHORT_FORM_ENABLED = "short_form_enabled";
    private static final String KEY_DAILY_LIMIT_ENABLED = "daily_limit_enabled";
    private static final String KEY_DAILY_LIMIT_MINUTES = "daily_limit_minutes";
    private static final String KEY_SCHEDULE_ENABLED = "focus_schedule_enabled";
    private static final String KEY_TIME_WINDOWS = "focus_time_windows";
    private static final String KEY_PAUSE_SHORTS_UNTIL = "pause_shorts_until";
    private static final String KEY_PAUSE_LIMIT_UNTIL = "pause_limit_until";
    private static final String KEY_PAUSE_SCHEDULE_UNTIL = "pause_schedule_until";
    private static final String KEY_YOUTUBE_CONTROLS_GUIDANCE_SHOWN =
            "youtube_controls_guidance_shown";

    static final int DEFAULT_DAILY_LIMIT_MINUTES = 120;
    static final int MIN_DAILY_LIMIT_MINUTES = 15;
    static final int MAX_DAILY_LIMIT_MINUTES = 12 * 60;
    static final int MAX_TIME_WINDOWS = 5;

    private final SharedPreferences preferences;

    ShieldPreferences(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    boolean isAppEnabled(ProtectedApp app) {
        return preferences.getBoolean(KEY_APP_PREFIX + app.preferenceSuffix(), true);
    }

    void setAppEnabled(ProtectedApp app, boolean enabled) {
        preferences.edit().putBoolean(KEY_APP_PREFIX + app.preferenceSuffix(), enabled).apply();
    }

    boolean isShortFormEnabled() {
        return preferences.getBoolean(KEY_SHORT_FORM_ENABLED, true);
    }

    void setShortFormEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_SHORT_FORM_ENABLED, enabled).apply();
    }

    boolean isDailyLimitEnabled() {
        return preferences.getBoolean(KEY_DAILY_LIMIT_ENABLED, false);
    }

    void setDailyLimitEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_DAILY_LIMIT_ENABLED, enabled).apply();
    }

    int getDailyLimitMinutes() {
        return clampLimit(preferences.getInt(
                KEY_DAILY_LIMIT_MINUTES, DEFAULT_DAILY_LIMIT_MINUTES));
    }

    void setDailyLimitMinutes(int minutes) {
        preferences.edit().putInt(KEY_DAILY_LIMIT_MINUTES, clampLimit(minutes)).apply();
    }

    boolean isFocusScheduleEnabled() {
        return preferences.getBoolean(KEY_SCHEDULE_ENABLED, false);
    }

    void setFocusScheduleEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_SCHEDULE_ENABLED, enabled).apply();
    }

    List<TimeWindow> getTimeWindows() {
        String encoded = preferences.getString(KEY_TIME_WINDOWS, "540-1020");
        if (encoded == null || encoded.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<TimeWindow> windows = new ArrayList<>();
        String[] values = encoded.split(",");
        for (String value : values) {
            TimeWindow window = TimeWindow.parse(value);
            if (window != null && !windows.contains(window)) {
                windows.add(window);
            }
            if (windows.size() == MAX_TIME_WINDOWS) {
                break;
            }
        }
        return windows;
    }

    void setTimeWindows(List<TimeWindow> windows) {
        StringBuilder encoded = new StringBuilder();
        int count = 0;
        for (TimeWindow window : windows) {
            if (window == null || count == MAX_TIME_WINDOWS) {
                continue;
            }
            if (count > 0) {
                encoded.append(',');
            }
            encoded.append(window.serialize());
            count++;
        }
        preferences.edit().putString(KEY_TIME_WINDOWS, encoded.toString()).apply();
    }

    TimeWindow getActiveTimeWindow(long nowMillis) {
        for (TimeWindow window : getTimeWindows()) {
            if (window.contains(nowMillis)) {
                return window;
            }
        }
        return null;
    }

    long getPauseUntil(RestrictionType restriction) {
        return preferences.getLong(pauseKey(restriction), 0L);
    }

    void pause(RestrictionType restriction, long untilMillis) {
        preferences.edit().putLong(pauseKey(restriction), Math.max(0L, untilMillis)).apply();
    }

    void clearPause(RestrictionType restriction) {
        preferences.edit().remove(pauseKey(restriction)).apply();
    }

    boolean isPaused(RestrictionType restriction, long nowMillis) {
        return getPauseUntil(restriction) > nowMillis;
    }

    List<ProtectedApp> getEnabledApps() {
        List<ProtectedApp> enabled = new ArrayList<>();
        for (ProtectedApp app : ProtectedApp.ALL) {
            if (isAppEnabled(app)) {
                enabled.add(app);
            }
        }
        return enabled;
    }

    boolean hasShownYouTubeControlsGuidance() {
        return preferences.getBoolean(KEY_YOUTUBE_CONTROLS_GUIDANCE_SHOWN, false);
    }

    void markYouTubeControlsGuidanceShown() {
        preferences.edit().putBoolean(KEY_YOUTUBE_CONTROLS_GUIDANCE_SHOWN, true).apply();
    }

    private static String pauseKey(RestrictionType restriction) {
        switch (restriction) {
            case SHORT_FORM:
                return KEY_PAUSE_SHORTS_UNTIL;
            case DAILY_LIMIT:
                return KEY_PAUSE_LIMIT_UNTIL;
            case FOCUS_SCHEDULE:
                return KEY_PAUSE_SCHEDULE_UNTIL;
            default:
                throw new IllegalArgumentException("Unsupported restriction " + restriction);
        }
    }

    private static int clampLimit(int minutes) {
        return Math.max(MIN_DAILY_LIMIT_MINUTES,
                Math.min(MAX_DAILY_LIMIT_MINUTES, minutes));
    }
}
