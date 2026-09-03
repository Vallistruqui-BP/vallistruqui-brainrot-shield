# AGENTS

## Purpose

- Maintain Vallistruqui Brainrot Shield, a local Android accessibility-service MVP that exits short-form feeds.
- Keep the service limited to the official YouTube package unless the user explicitly expands scope.

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

- The accessibility service must remain package-scoped to com.google.android.youtube.
- Do not add networking, analytics, screen capture, or persistence of accessibility content without explicit user approval.
- Keep the accessibility overlay short-lived, non-focusable, and non-touchable so it cannot trap input.
- Do not reintroduce the system overlay permission while TYPE_ACCESSIBILITY_OVERLAY can provide the protection screen.
- Restore media volume after every blocking action and during service teardown.
- Keep Gradle build intermediates outside the Google Drive-synchronized workspace; desktop.ini breaks AAPT generated resources.

## Review focus

- Prevent false positives on ordinary YouTube Home and long-form video screens.
- Verify service configuration, prominent disclosure, accessibility-overlay behavior, cooldown behavior, and volume restoration.
- Update README.md when setup steps, supported apps, or permissions change.
