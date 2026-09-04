package com.vallistruqui.brainrotshield;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class YouTubeCompatibilityTest {
    @Test
    public void offersGuidanceOnceWhenYouTubeAndAccessibilityAreReady() {
        assertTrue(YouTubeCompatibility.shouldOfferControlsGuidance(
                true, true, true, true, false));
        assertFalse(YouTubeCompatibility.shouldOfferControlsGuidance(
                true, true, true, true, true));
    }

    @Test
    public void doesNotInterruptLockedOrIncompleteSetup() {
        assertFalse(YouTubeCompatibility.shouldOfferControlsGuidance(
                true, false, true, true, false));
        assertFalse(YouTubeCompatibility.shouldOfferControlsGuidance(
                false, true, true, true, false));
        assertFalse(YouTubeCompatibility.shouldOfferControlsGuidance(
                true, true, false, true, false));
        assertFalse(YouTubeCompatibility.shouldOfferControlsGuidance(
                true, true, true, false, false));
    }
}
