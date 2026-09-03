package com.vallistruqui.brainrotshield;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ShortsSignalsTest {
    @Test
    public void selectedShortsTabIsEnough() {
        ShortsSignals signals = new ShortsSignals();
        signals.observe("Shorts", null, "com.google.android.youtube:id/pivot_button", true);
        assertTrue(signals.indicatesShorts());
    }

    @Test
    public void localizedSelectedDescriptionIsEnough() {
        ShortsSignals signals = new ShortsSignals();
        signals.observe(null, "Shorts, seleccionado", null, false);
        assertTrue(signals.indicatesShorts());
    }

    @Test
    public void navigationIconAloneDoesNotBlockYouTubeHome() {
        ShortsSignals signals = new ShortsSignals();
        signals.observe("Shorts", null, "com.google.android.youtube:id/pivot_button", false);
        assertFalse(signals.indicatesShorts());
    }

    @Test
    public void reelSurfaceAndActionIndicateShorts() {
        ShortsSignals signals = new ShortsSignals();
        signals.observe(null, null, "com.google.android.youtube:id/reel_recycler", false);
        signals.observe(null, "Comentarios", null, false);
        assertTrue(signals.indicatesShorts());
    }

    @Test
    public void shortsLabelWithThreeDistinctActionsIndicatesFeed() {
        ShortsSignals signals = new ShortsSignals();
        signals.observe("Shorts", null, null, false);
        signals.observe(null, "Me gusta este video", null, false);
        signals.observe(null, "Comentarios", null, false);
        signals.observe(null, "Compartir", null, false);
        assertTrue(signals.indicatesShorts());
    }

    @Test
    public void longFormActionsWithoutShortsSignalDoNotBlock() {
        ShortsSignals signals = new ShortsSignals();
        signals.observe(null, "Me gusta este video", null, false);
        signals.observe(null, "No me gusta", null, false);
        signals.observe(null, "Comentarios", null, false);
        signals.observe(null, "Compartir", null, false);
        assertFalse(signals.indicatesShorts());
    }

    @Test
    public void duplicateActionsDoNotInflateConfidence() {
        ShortsSignals signals = new ShortsSignals();
        signals.observe("Shorts", null, null, false);
        signals.observe(null, "Compartir", null, false);
        signals.observe(null, "Compartir", null, false);
        signals.observe(null, "Compartir", null, false);
        assertFalse(signals.indicatesShorts());
    }
}
