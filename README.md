# Vallistruqui Brainrot Shield

[![Android CI](https://github.com/Vallistruqui-BP/vallistruqui-brainrot-shield/actions/workflows/android-ci.yml/badge.svg)](https://github.com/Vallistruqui-BP/vallistruqui-brainrot-shield/actions/workflows/android-ci.yml)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

Vallistruqui Brainrot Shield is a local Android MVP reconstructed from the shared “Packet Analysis for Videos” conversation. It watches only the official YouTube app, detects strong signals that the active screen is the Shorts feed, briefly covers the screen, mutes media, performs Android Back, and restores the previous media volume.

## What is included

- A Java Android app with the permanent project-associated package name: `com.vallistruqui.brainrotshield`.
- A package-scoped AccessibilityService for com.google.android.youtube.
- A guarded detector that does not treat the ever-present Shorts navigation icon as sufficient evidence by itself.
- A short, non-interactive accessibility overlay that does not require the system display-over-other-apps permission.
- Media-volume restoration and a cooldown to avoid repeated Back actions.
- A setup screen that reports accessibility and YouTube status.
- A prominent in-app disclosure and affirmative consent step before opening Accessibility settings.
- Unit tests for the main detection heuristics.

The app does not use a server, networking, analytics, screenshots, or storage of accessibility content.

> **Project status:** early open-source release. The current implementation supports YouTube only and has not yet been published on Google Play. Instagram, TikTok, guardian-controlled settings, and managed-device uninstall restrictions are documented future work, not current features.

## Build locally

Requirements:

- JDK 17
- Android SDK Platform 36
- Android SDK Build Tools 36.1.0

The app supports Android 6.0 (API 23) through Android 16 (API 36). Android and social-app UI differences still require representative device testing before public compatibility claims are expanded.

On Windows, run:

    $env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    .\gradlew.bat test lint exportDebugApk

The debug APK is generated at:

    artifacts\vallistruqui-brainrot-shield-debug.apk

The last verified build using the legacy `com.gonzalo.shortsshield` identity is preserved locally at:

    artifacts\shorts-shield-legacy-v1.0.0-debug.apk

Because Android treats a changed application ID as a different app, installing the Vallistruqui build does not update the legacy installation. Enable and verify Brainrot Shield first, then disable and uninstall the legacy app to avoid having two accessibility services active.

Build intermediates are deliberately stored under the local Gradle cache instead of this Desktop workspace. Google Drive for Desktop injects desktop.ini files into newly created directories here, and Android's resource linker rejects those files inside generated resource folders.

If Gradle reports a PKIX certificate-chain error on this Windows installation, run the build with the Windows certificate store for that shell session:

    $env:GRADLE_OPTS = '-Djavax.net.ssl.trustStoreType=Windows-ROOT'

To install it on a connected device with USB debugging enabled:

    .\gradlew.bat installDebug

## Configure the phone

1. Open Brainrot Shield.
2. Tap “Activar servicio de accesibilidad”.
3. Read the in-app disclosure and tap “Aceptar y continuar” only if you agree.
4. In Android settings, open Installed apps or Downloaded apps under Accessibility and enable Brainrot Shield.
5. Return to the app and confirm the service status is active.
6. Open YouTube and enter the Shorts feed.

On some Samsung/Android versions, a sideloaded app can show a disabled accessibility switch. Open the Android app-info screen for Brainrot Shield, use the overflow menu, allow restricted settings, and try again.

## Google Play publication

- [Release lifecycle](docs/google-play-release-cycle.md) documents the human, policy, testing, signing, and rollout gates.
- [Future plugin blueprint](docs/google-play-plugin-blueprint.md) records what a Codex plugin can safely automate later and what must remain under explicit human control.
- [Dual-mode architecture](docs/dual-mode-architecture.md) records how Personal and Managed protection can coexist without misrepresenting what a local PIN can enforce.

The intended product remains a single app with two clearly separated modes:

- **Personal:** easy opt-in blocking and, in a future release, a local PIN or remote guardian for changing the app's own settings. Android still lets the device owner disable or uninstall an ordinary app.
- **Managed:** future support for legitimately enrolled Device Owner or Profile Owner devices, where Android's management APIs can enforce stronger restrictions. Enrollment must be explicit and appropriate for a supervised or organization-owned device.

## Expected behavior

When the active YouTube UI provides enough Shorts-specific signals, the app:

1. Saves the current media volume.
2. Mutes media if it was audible.
3. Shows a black “Shorts bloqueados” accessibility overlay for less than one second.
4. Executes the global Back action.
5. Restores the saved volume.

## Known limitation

YouTube changes its internal accessibility labels and view identifiers without notice. The detector intentionally favors avoiding false positives, so a future YouTube release or a different locale may need another recognized signal. Android also does not let an ordinary app password-protect its own uninstall; this project does not attempt to prevent removal.

## Privacy, security, and contributions

- Read the [privacy policy](PRIVACY.md) for the exact on-device data behavior.
- Report sensitive issues according to the [security policy](SECURITY.md).
- See [CONTRIBUTING.md](CONTRIBUTING.md) before proposing code or new app integrations.

## License

Licensed under the [Apache License 2.0](LICENSE). This permits personal and commercial use, modification, and redistribution while preserving the license and notices. It also includes an express patent grant from contributors.
