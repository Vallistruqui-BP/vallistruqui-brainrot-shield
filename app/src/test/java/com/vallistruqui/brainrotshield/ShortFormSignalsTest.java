package com.vallistruqui.brainrotshield;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ShortFormSignalsTest {
    @Test
    public void selectedYouTubeShortsTabIsEnough() {
        ShortFormSignals signals = new ShortFormSignals(ProtectedApp.YOUTUBE);
        signals.observe("Shorts", null,
                "com.google.android.youtube:id/pivot_button", null, true);
        assertTrue(signals.indicatesShortForm());
    }

    @Test
    public void localizedSelectedDescriptionIsEnoughForYouTube() {
        ShortFormSignals signals = new ShortFormSignals(ProtectedApp.YOUTUBE);
        signals.observe(null, "Shorts, seleccionado", null, null, false);
        assertTrue(signals.indicatesShortForm());
    }

    @Test
    public void youtubeReelSurfaceAndActionIndicateShorts() {
        ShortFormSignals signals = new ShortFormSignals(ProtectedApp.YOUTUBE);
        signals.observe(null, null,
                "com.google.android.youtube:id/reel_recycler", null, false);
        signals.observe(null, "Comentarios", null, null, false);
        assertTrue(signals.indicatesShortForm());
    }

    @Test
    public void youtubeNavigationLabelAndLongFormControlsDoNotBlock() {
        ShortFormSignals signals = new ShortFormSignals(ProtectedApp.YOUTUBE);
        signals.observe("Shorts", null,
                "com.google.android.youtube:id/pivot_button", null, false);
        signals.observe(null, "Me gusta este video", null, null, false);
        signals.observe(null, "Comentarios", null, null, false);
        signals.observe(null, "Compartir", null, null, false);
        assertFalse(signals.indicatesShortForm());
    }

    @Test
    public void selectedInstagramReelsTabIsEnough() {
        ShortFormSignals signals = new ShortFormSignals(ProtectedApp.INSTAGRAM);
        signals.observe(null, "Reels, seleccionado", null, null, false);
        assertTrue(signals.indicatesShortForm());
    }

    @Test
    public void instagramClipsViewerAndActionIndicateReel() {
        ShortFormSignals signals = new ShortFormSignals(ProtectedApp.INSTAGRAM);
        signals.observe(null, null,
                "com.instagram.android:id/clips_viewer", null, false);
        signals.observe(null, "Compartir", null, null, false);
        assertTrue(signals.indicatesShortForm());
    }

    @Test
    public void instagramReelsNavigationAndRegularPostActionsDoNotBlock() {
        ShortFormSignals signals = new ShortFormSignals(ProtectedApp.INSTAGRAM);
        signals.observe("Reels", null,
                "com.instagram.android:id/tab_bar", null, false);
        signals.observe(null, "Me gusta", null, null, false);
        signals.observe(null, "Comentarios", null, null, false);
        signals.observe(null, "Compartir", null, null, false);
        assertFalse(signals.indicatesShortForm());
    }

    @Test
    public void tiktokActionClusterIndicatesShortVideo() {
        ShortFormSignals signals = new ShortFormSignals(ProtectedApp.TIKTOK);
        signals.observe(null, "Me gusta", null, null, false);
        signals.observe(null, "Comentarios", null, null, false);
        signals.observe(null, "Compartir", null, null, false);
        assertTrue(signals.indicatesShortForm());
    }

    @Test
    public void tiktokSelectedLocalizedFeedAndActionIndicateShortVideo() {
        ShortFormSignals signals = new ShortFormSignals(ProtectedApp.TIKTOK);
        signals.observe("Para ti", null, null, null, true);
        signals.observe(null, "Comentarios", null, null, false);
        assertTrue(signals.indicatesShortForm());
    }

    @Test
    public void tiktokTwoGenericActionsAreNotEnough() {
        ShortFormSignals signals = new ShortFormSignals(ProtectedApp.TIKTOK);
        signals.observe(null, "Me gusta", null, null, false);
        signals.observe(null, "Compartir", null, null, false);
        assertFalse(signals.indicatesShortForm());
    }
}
