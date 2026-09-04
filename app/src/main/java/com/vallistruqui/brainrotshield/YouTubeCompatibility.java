package com.vallistruqui.brainrotshield;

final class YouTubeCompatibility {
    private YouTubeCompatibility() {
    }

    static boolean shouldOfferControlsGuidance(boolean accessibilityEnabled,
            boolean settingsUnlocked, boolean youtubeEnabled, boolean youtubeInstalled,
            boolean guidanceAlreadyShown) {
        return accessibilityEnabled
                && settingsUnlocked
                && youtubeEnabled
                && youtubeInstalled
                && !guidanceAlreadyShown;
    }
}
