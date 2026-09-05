package com.vallistruqui.brainrotshield;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ProtectionPresentationTest {
    @Test
    public void noSelectedAppsIsTheFirstActionToResolve() {
        ProtectionPresentation presentation = ProtectionPresentation.evaluate(
                false, false, 0, true, true, true);

        assertEquals(ProtectionPresentation.State.NO_APPS, presentation.state());
        assertFalse(presentation.isProtected());
        assertEquals(
                ProtectionPresentation.PrimaryAction.NONE,
                presentation.primaryAction());
    }

    @Test
    public void noEnabledRulesProducesAnIdleStateWithoutRequestingPermissions() {
        ProtectionPresentation presentation = ProtectionPresentation.evaluate(
                false, false, 3, false, false, false);

        assertEquals(ProtectionPresentation.State.NO_RULES, presentation.state());
        assertFalse(presentation.isProtected());
        assertEquals(
                ProtectionPresentation.PrimaryAction.NONE,
                presentation.primaryAction());
    }

    @Test
    public void enabledRulesNeedAccessibilityBeforeAnyOtherPermission() {
        ProtectionPresentation presentation = ProtectionPresentation.evaluate(
                false, false, 3, true, true, true);

        assertEquals(
                ProtectionPresentation.State.NEEDS_ACCESSIBILITY,
                presentation.state());
        assertFalse(presentation.isProtected());
        assertEquals(
                ProtectionPresentation.PrimaryAction.ACCESSIBILITY,
                presentation.primaryAction());
    }

    @Test
    public void dailyLimitNeedsUsageAccessAfterAccessibilityIsReady() {
        ProtectionPresentation presentation = ProtectionPresentation.evaluate(
                true, false, 3, true, true, true);

        assertEquals(
                ProtectionPresentation.State.NEEDS_USAGE_ACCESS,
                presentation.state());
        assertFalse(presentation.isProtected());
        assertEquals(
                ProtectionPresentation.PrimaryAction.USAGE_ACCESS,
                presentation.primaryAction());
    }

    @Test
    public void configuredRulesAndRequiredPermissionsProduceProtectedState() {
        ProtectionPresentation presentation = ProtectionPresentation.evaluate(
                true, true, 3, true, true, true);

        assertEquals(ProtectionPresentation.State.PROTECTED, presentation.state());
        assertTrue(presentation.isProtected());
        assertEquals(
                ProtectionPresentation.PrimaryAction.NONE,
                presentation.primaryAction());
    }

    @Test
    public void usageAccessIsOptionalWhenDailyLimitIsDisabled() {
        ProtectionPresentation presentation = ProtectionPresentation.evaluate(
                true, false, 3, true, false, true);

        assertEquals(ProtectionPresentation.State.PROTECTED, presentation.state());
        assertTrue(presentation.isProtected());
        assertEquals(
                ProtectionPresentation.PrimaryAction.NONE,
                presentation.primaryAction());
    }

    @Test
    public void accessibilityAnnouncementOnlyRunsForARealStateChange() {
        assertFalse(ProtectionPresentation.shouldAnnounce(
                null, ProtectionPresentation.State.PROTECTED));
        assertFalse(ProtectionPresentation.shouldAnnounce(
                ProtectionPresentation.State.PROTECTED,
                ProtectionPresentation.State.PROTECTED));
        assertTrue(ProtectionPresentation.shouldAnnounce(
                ProtectionPresentation.State.NEEDS_ACCESSIBILITY,
                ProtectionPresentation.State.PROTECTED));
    }
}
