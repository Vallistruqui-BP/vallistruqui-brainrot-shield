package com.vallistruqui.brainrotshield;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TimeWindowTest {
    @Test
    public void daytimeWindowIncludesStartAndExcludesEnd() {
        TimeWindow window = new TimeWindow(9 * 60, 17 * 60);
        assertTrue(window.containsMinute(9 * 60));
        assertTrue(window.containsMinute(12 * 60));
        assertFalse(window.containsMinute(17 * 60));
    }

    @Test
    public void overnightWindowWrapsAcrossMidnight() {
        TimeWindow window = new TimeWindow(22 * 60, 7 * 60);
        assertTrue(window.containsMinute(23 * 60));
        assertTrue(window.containsMinute(6 * 60 + 59));
        assertFalse(window.containsMinute(12 * 60));
    }

    @Test
    public void equalTimesRepresentAllDay() {
        TimeWindow window = new TimeWindow(8 * 60, 8 * 60);
        assertTrue(window.containsMinute(0));
        assertTrue(window.containsMinute(23 * 60 + 59));
        assertEquals("Todo el día", window.format());
    }

    @Test
    public void serializationRoundTrips() {
        TimeWindow original = new TimeWindow(8 * 60 + 30, 12 * 60 + 15);
        assertEquals(original, TimeWindow.parse(original.serialize()));
    }

    @Test
    public void malformedSerializationIsRejected() {
        assertNull(TimeWindow.parse("wrong"));
        assertNull(TimeWindow.parse("-1-10"));
        assertNull(TimeWindow.parse("10-1440"));
    }
}
