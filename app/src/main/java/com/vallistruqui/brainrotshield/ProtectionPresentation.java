package com.vallistruqui.brainrotshield;

final class ProtectionPresentation {
    enum State {
        PROTECTED,
        NEEDS_ACCESSIBILITY,
        NEEDS_USAGE_ACCESS,
        NO_APPS,
        NO_RULES
    }

    enum PrimaryAction {
        ACCESSIBILITY,
        USAGE_ACCESS,
        NONE
    }

    private final State state;

    private ProtectionPresentation(State state) {
        this.state = state;
    }

    static ProtectionPresentation evaluate(
            boolean accessibilityEnabled,
            boolean usageAccessEnabled,
            int enabledAppCount,
            boolean shortFormEnabled,
            boolean dailyLimitEnabled,
            boolean focusScheduleEnabled) {
        if (enabledAppCount == 0) {
            return new ProtectionPresentation(State.NO_APPS);
        }
        if (!shortFormEnabled && !dailyLimitEnabled && !focusScheduleEnabled) {
            return new ProtectionPresentation(State.NO_RULES);
        }
        if (!accessibilityEnabled) {
            return new ProtectionPresentation(State.NEEDS_ACCESSIBILITY);
        }
        if (dailyLimitEnabled && !usageAccessEnabled) {
            return new ProtectionPresentation(State.NEEDS_USAGE_ACCESS);
        }
        return new ProtectionPresentation(State.PROTECTED);
    }

    State state() {
        return state;
    }

    boolean isProtected() {
        return state == State.PROTECTED;
    }

    PrimaryAction primaryAction() {
        if (state == State.NEEDS_ACCESSIBILITY) {
            return PrimaryAction.ACCESSIBILITY;
        }
        if (state == State.NEEDS_USAGE_ACCESS) {
            return PrimaryAction.USAGE_ACCESS;
        }
        return PrimaryAction.NONE;
    }

    static boolean shouldAnnounce(State previousState, State currentState) {
        return previousState != null && previousState != currentState;
    }
}
