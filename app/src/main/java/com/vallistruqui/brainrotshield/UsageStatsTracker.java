package com.vallistruqui.brainrotshield;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Process;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

@SuppressWarnings("deprecation")
final class UsageStatsTracker {
    private static final long INITIAL_EVENT_LOOKBACK_MS = 24L * 60L * 60L * 1_000L;

    private final Context context;
    private final UsageStatsManager usageStatsManager;
    private String lastForegroundPackage;
    private long lastProcessedEventAt;
    private long lastQueryEndAt;
    private boolean initialized;

    UsageStatsTracker(Context context) {
        this.context = context.getApplicationContext();
        usageStatsManager = (UsageStatsManager) this.context.getSystemService(
                Context.USAGE_STATS_SERVICE);
    }

    boolean hasAccess() {
        try {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOps == null) {
                return false;
            }
            ApplicationInfo applicationInfo = context.getApplicationInfo();
            int mode = appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    applicationInfo.packageName);
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    long getTodayUsageMillis(List<ProtectedApp> apps, long nowMillis) {
        if (!hasAccess() || usageStatsManager == null || apps.isEmpty()) {
            return 0L;
        }

        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(nowMillis);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        try {
            Map<String, UsageStats> aggregated = usageStatsManager.queryAndAggregateUsageStats(
                    start.getTimeInMillis(), nowMillis);
            if (aggregated == null) {
                return 0L;
            }
            long total = 0L;
            for (ProtectedApp app : apps) {
                UsageStats stats = aggregated.get(app.packageName());
                if (stats != null) {
                    total += Math.max(0L, stats.getTotalTimeInForeground());
                }
            }
            return total;
        } catch (RuntimeException exception) {
            return 0L;
        }
    }

    String getCurrentForegroundPackage(long nowMillis) {
        if (!hasAccess() || usageStatsManager == null) {
            return null;
        }

        long startMillis = initialized
                ? Math.max(0L, lastQueryEndAt - 2_000L)
                : Math.max(0L, nowMillis - INITIAL_EVENT_LOOKBACK_MS);
        try {
            UsageEvents events = usageStatsManager.queryEvents(startMillis, nowMillis);
            if (events == null) {
                return lastForegroundPackage;
            }
            UsageEvents.Event event = new UsageEvents.Event();
            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                long eventTime = event.getTimeStamp();
                if (eventTime < lastProcessedEventAt) {
                    continue;
                }

                int type = event.getEventType();
                boolean foreground = type == UsageEvents.Event.MOVE_TO_FOREGROUND;
                boolean background = type == UsageEvents.Event.MOVE_TO_BACKGROUND;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    foreground |= type == UsageEvents.Event.ACTIVITY_RESUMED;
                    background |= type == UsageEvents.Event.ACTIVITY_PAUSED;
                }

                if (foreground) {
                    lastForegroundPackage = event.getPackageName();
                } else if (background && event.getPackageName() != null
                        && event.getPackageName().equals(lastForegroundPackage)) {
                    lastForegroundPackage = null;
                }
                lastProcessedEventAt = eventTime;
            }
            lastQueryEndAt = nowMillis;
            initialized = true;
            return lastForegroundPackage;
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
