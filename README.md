# Vallistruqui Brainrot Shield

[![Android CI](https://github.com/Vallistruqui-BP/vallistruqui-brainrot-shield/actions/workflows/android-ci.yml/badge.svg)](https://github.com/Vallistruqui-BP/vallistruqui-brainrot-shield/actions/workflows/android-ci.yml)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

Vallistruqui Brainrot Shield is a privacy-first Android attention tool. It can leave short-form video feeds, enforce a combined daily time limit across selected social apps, and block those apps during configurable focus windows.

The current app works entirely on the device and supports the official Android packages for:

- YouTube (`com.google.android.youtube`)
- Instagram (`com.instagram.android`)
- TikTok (`com.zhiliaoapp.musically`)

> **Project status:** experimental open-source release. The Android implementation and automated tests are complete for version 1.3.0, but the Instagram and TikTok heuristics and settings-access flow still need validation on representative real devices before production compatibility is claimed. The app has not yet been published on Google Play.

## What is included

- Individual app-selection switches for YouTube, Instagram, and TikTok.
- Short-form detectors for YouTube Shorts, Instagram Reels, and TikTok vertical video feeds.
- A stricter YouTube detector that does not treat the persistent Shorts navigation label plus ordinary long-form playback controls as a Shorts feed.
- A combined daily limit, configurable from 15 minutes to 12 hours in 15-minute increments.
- Up to five focus windows. A window can cross midnight; equal start and end times mean all day.
- Independent temporary pauses for short-form blocking, the daily limit, and focus windows.
- A live summary that explains which apps and restrictions are active, paused, waiting, or missing permission.
- A large full-screen explanation before the app exits because of a daily limit or focus window.
- A shorter full-screen explanation when a short-form feed is detected.
- Automatic system light/dark theme support with semantic color resources.
- An optional 6-to-12-digit administrator PIN that hides and locks the entire settings panel whenever the app is left.
- Salted PBKDF2 PIN verification, constant-time hash comparison, and progressive retry delays after repeated failures. The PIN itself is never stored.
- Media-volume restoration and a cooldown to prevent repeated navigation actions.
- Local preferences only: no server, Internet permission, accounts, analytics, advertising, screenshots, or storage of accessibility content.

## How the rules work

### Short-form blocking

When the selected app exposes strong accessibility signals for Shorts, Reels, or a TikTok video feed, Brainrot Shield briefly mutes media, shows an explanation, performs Android Back, and restores the previous media volume.

YouTube and Instagram intentionally require a selected short-form destination or a strong viewer-specific surface identifier. A generic navigation label plus Like, Comment, and Share controls is not enough. TikTok is predominantly a short-video product, so its detector also accepts a distinct cluster of video actions inside the TikTok package.

### Combined daily limit

The daily limit sums foreground time across all selected apps. For example, with a two-hour limit, 45 minutes of YouTube plus 45 minutes of Instagram leaves 30 minutes for TikTok. After the limit, opening or remaining in a selected app shows a large “Tiempo diario terminado” overlay and returns to Android Home. The total resets at local midnight.

This feature requires Android Usage Access. Without that optional access, short-form blocking and event-driven focus windows continue to work, but the daily limit does not.

### Focus windows

During any enabled focus window, the selected apps are blocked completely. The app supports up to five daily windows and checks the foreground approximately every 15 seconds while the accessibility service is running. A temporary schedule pause lifts only this rule; it does not pause the short-form or daily-limit rules.

### Temporary pauses

Each rule has its own pause control. An authorized settings user can resume immediately or pause for 15 minutes, 30 minutes, one hour, two hours, or until the next local midnight.

### Administrator PIN

Personal mode can protect every in-app setting with a local administrator PIN. After setup, leaving Brainrot Shield immediately hides the configuration; reopening it exposes only an unlock prompt. Changing or removing the PIN requires the current PIN, and **Bloquear configuración ahora** closes the panel without waiting for the app to leave the foreground.

The PIN is a deliberate in-app access boundary, not Android uninstall protection. Someone who controls the device can still clear Brainrot Shield's app data, revoke Accessibility or Usage Access, or uninstall the app. Repeated wrong PIN attempts produce progressively longer local retry delays, but clearing app data removes the PIN together with every other local setting.

## Build locally

Requirements:

- JDK 17
- Android SDK Platform 36
- Android SDK Build Tools 36.1.0

The manifest supports Android 6.0 (API 23) through Android 16 (API 36). Supporting an API range does not by itself guarantee that every vendor skin or target-app version exposes the same accessibility metadata; representative-device testing remains required.

On Windows:

    $env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    .\gradlew.bat test lint exportDebugApk

On Linux or macOS:

    ./gradlew test lint assembleDebug

The exported local debug APK is written to:

    artifacts\vallistruqui-brainrot-shield-debug.apk

Build intermediates are deliberately stored under the local Gradle cache instead of this Desktop workspace. Google Drive for Desktop can inject `desktop.ini` files into new directories here, and Android's resource tools reject those files inside generated resource folders.

If Gradle reports a PKIX certificate-chain error on Windows, run the build with the Windows certificate store for that shell session:

    $env:GRADLE_OPTS = '-Djavax.net.ssl.trustStoreType=Windows-ROOT'

## Configure the phone

1. Install and open Brainrot Shield.
2. Optionally create a **PIN de administrador**. Record it securely; there is no local recovery flow.
3. Under **Apps protegidas**, choose YouTube, Instagram, and/or TikTok.
4. Tap **Configurar accesibilidad**, read the disclosure, accept it, and enable Brainrot Shield in Android Accessibility settings.
5. Enable or disable **Bloquear videos cortos**.
6. To use the combined daily limit, enable it, accept the separate Usage Access disclosure, and grant access in Android settings.
7. Configure the daily allowance and any focus windows.
8. Check **Protección actual**, then tap **Bloquear configuración ahora** before handing the device back to the protected user.

On some Samsung and Android versions, a sideloaded app can show a disabled accessibility switch. Open the Android app-info screen for Brainrot Shield, use the overflow menu, allow restricted settings, and try again.

Because the application ID changed from the earliest local prototype, installing this package does not update an old `com.gonzalo.shortsshield` installation. Disable and uninstall that legacy build after verifying this one to avoid two accessibility services running at once.

## Dark mode

Brainrot Shield follows the Android system theme. The light and dark palettes define independent canvas, surface, text, divider, status, accent, and interaction colors rather than relying on automatic color inversion. Full visual verification still requires a physical device or emulator in both modes.

## YouTube playback-controls regression

Version 1.2.0 and later remove click events from the accessibility-service subscription, stop requesting unnecessary interactive-window and not-important-view flags, and delete the previous weak rule that combined an unselected Shorts navigation label with three ordinary video actions. A unit regression test verifies that showing Like, Comments, Share, pause, skip, or timeline controls on a long-form YouTube video does not classify that screen as Shorts.

If YouTube still keeps its controls visible after this change, test the same video once with Brainrot Shield disabled in Accessibility settings. If the behavior occurs only while the service is enabled, it may be YouTube adapting its player to the presence of any accessibility service rather than Brainrot Shield performing a touch or holding the screen; this app never sends touch gestures or long-press actions.

## Google Play publication

- [Release lifecycle](docs/google-play-release-cycle.md) documents the human, policy, testing, signing, and rollout gates.
- [Future plugin blueprint](docs/google-play-plugin-blueprint.md) records what a Codex plugin can safely automate and what must remain under explicit human control.
- [Dual-mode architecture](docs/dual-mode-architecture.md) records how Personal and Managed protection can coexist without misrepresenting what a local PIN can enforce.

The added Usage Access and broader Accessibility scope require updated prominent disclosures, privacy documentation, Data safety answers, accessibility declarations, reviewer instructions, and real-device evidence before Play submission. They must not be presented to Play as already approved.

## Known limitations

- YouTube, Instagram, and TikTok can change labels and view identifiers without notice.
- App language, A/B tests, regional builds, accessibility metadata, and device-vendor changes can affect detection.
- An ordinary Android app cannot password-protect its own system uninstall. This Personal build does not intercept uninstall or security interfaces.
- Settings and pauses can be protected by the local administrator PIN, but clearing app data removes that access boundary and resets the configuration.
- A future remote guardian mode requires networking, identity, recovery, abuse prevention, and a new privacy/security review.
- Strong uninstall restrictions are only legitimate in a separately enrolled Android Device Owner or Profile Owner mode.

## Privacy, security, and contributions

- Read the [privacy policy](PRIVACY.md) for the exact on-device data behavior.
- Report sensitive issues according to the [security policy](SECURITY.md).
- See [CONTRIBUTING.md](CONTRIBUTING.md) before proposing code, detector signals, or new app integrations.

## License

Licensed under the [Apache License 2.0](LICENSE). This permits personal and commercial use, modification, and redistribution while preserving the license and notices. It also includes an express patent grant from contributors.
