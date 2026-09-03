# AGENTS

## Purpose

- Maintain Vallistruqui Brainrot Shield, a local Android accessibility and digital-wellbeing app that exits short-form feeds and enforces user-selected time rules.
- Keep the service limited to the official YouTube, Instagram, and TikTok packages unless the user explicitly expands scope.

## Stack

- Java 17
- Android Gradle Plugin 8.13.2 and Gradle 8.13
- compileSdk/targetSdk 36, minSdk 23
- Platform Android views only; avoid unnecessary runtime dependencies.

## Commands

- Windows checks: .\gradlew.bat test lint exportDebugApk
- Install on a connected phone: .\gradlew.bat installDebug
- Exported APK: artifacts\vallistruqui-brainrot-shield-debug.apk

## Safety and privacy

- The accessibility service must remain package-scoped to the official YouTube, Instagram, and TikTok packages.
- Usage Access must be optional, disclosed separately, and limited to local foreground detection plus aggregate supported-app time.
- Do not add networking, analytics, screen capture, detailed usage history, or persistence of accessibility content without explicit user approval.
- Keep the accessibility overlay short-lived, non-focusable, and non-touchable so it cannot trap input.
- Do not reintroduce the system overlay permission while TYPE_ACCESSIBILITY_OVERLAY can provide the protection screen.
- Restore media volume after every blocking action and during service teardown.
- Keep short-form, daily-limit, and focus-window rules independently configurable and pausable.
- Keep Gradle build intermediates outside the Google Drive-synchronized workspace; desktop.ini breaks AAPT generated resources.

## Review focus

- Prevent false positives on ordinary YouTube/Instagram screens and long-form YouTube video controls.
- Treat a Shorts/Reels navigation label plus generic post controls as insufficient evidence on YouTube and Instagram.
- Verify light and dark palettes, large text, 48dp touch targets, overnight windows, pause expiry, and missing Usage Access states.
- Verify service configuration, prominent disclosure, accessibility-overlay behavior, cooldown behavior, and volume restoration.
- Update README.md when setup steps, supported apps, or permissions change.
