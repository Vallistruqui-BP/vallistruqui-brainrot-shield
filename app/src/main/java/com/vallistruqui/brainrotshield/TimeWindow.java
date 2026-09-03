package com.vallistruqui.brainrotshield;

import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

final class TimeWindow {
    static final int MINUTES_PER_DAY = 24 * 60;

    private final int startMinute;
    private final int endMinute;

    TimeWindow(int startMinute, int endMinute) {
        if (startMinute < 0 || startMinute >= MINUTES_PER_DAY
                || endMinute < 0 || endMinute >= MINUTES_PER_DAY) {
            throw new IllegalArgumentException("Minutes must be inside one day");
        }
        this.startMinute = startMinute;
        this.endMinute = endMinute;
    }

    int startMinute() {
        return startMinute;
    }

    int endMinute() {
        return endMinute;
    }

    boolean containsMinute(int minuteOfDay) {
        if (minuteOfDay < 0 || minuteOfDay >= MINUTES_PER_DAY) {
            return false;
        }
        if (startMinute == endMinute) {
            return true;
        }
        if (startMinute < endMinute) {
            return minuteOfDay >= startMinute && minuteOfDay < endMinute;
        }
        return minuteOfDay >= startMinute || minuteOfDay < endMinute;
    }

    boolean contains(long timestampMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestampMillis);
        int minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60
                + calendar.get(Calendar.MINUTE);
        return containsMinute(minuteOfDay);
    }

    String format() {
        if (startMinute == endMinute) {
            return "Todo el día";
        }
        return formatMinute(startMinute) + "–" + formatMinute(endMinute);
    }

    String serialize() {
        return startMinute + "-" + endMinute;
    }

    static TimeWindow parse(String serialized) {
        if (serialized == null) {
            return null;
        }
        String[] parts = serialized.trim().split("-", -1);
        if (parts.length != 2) {
            return null;
        }
        try {
            return new TimeWindow(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String formatMinute(int minute) {
        return String.format(Locale.ROOT, "%02d:%02d", minute / 60, minute % 60);
    }

    @Override
    public boolean equals(Object value) {
        if (this == value) {
            return true;
        }
        if (!(value instanceof TimeWindow)) {
            return false;
        }
        TimeWindow other = (TimeWindow) value;
        return startMinute == other.startMinute && endMinute == other.endMinute;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startMinute, endMinute);
    }
}
