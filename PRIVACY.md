# Privacy policy

Effective date: September 4, 2026

Vallistruqui Brainrot Shield performs its current blocking and time-management behavior entirely on the Android device.

## Data handled by the app

### Accessibility service

When the user explicitly enables its accessibility service, the app observes accessibility events and visible accessibility metadata from these official Android packages:

- YouTube: `com.google.android.youtube`
- Instagram: `com.instagram.android`
- TikTok: `com.zhiliaoapp.musically`

This information is processed transiently to recognize short-form feeds, determine whether a configured rule applies, show a blocking explanation, and execute Android Back or Home. The app does not store the text or interface content it observes.

### Usage Access

When the user separately grants Android Usage Access, the app queries on-device usage statistics to:

- add the foreground time of the selected supported apps since local midnight;
- display progress toward the configured combined daily limit; and
- determine whether a protected app remains in the foreground so a daily limit or focus window can be enforced.

Android's Usage Access interface may expose events for other installed applications. Brainrot Shield uses those events only transiently to identify the current foreground package. It does not store, transmit, display, or build a history of other app usage.

### Local preferences

The app stores the following configuration locally in Android private app storage:

- selected protected apps;
- enabled or disabled restrictions;
- combined daily-limit duration;
- configured focus windows; and
- expiration timestamps for temporary pauses.

If the administrator PIN is enabled, a separate private preference file also stores:

- a random cryptographic salt;
- a PBKDF2-derived PIN hash;
- the number of consecutive failed attempts; and
- the expiration time of any temporary retry lockout.

The numeric PIN itself is not stored. The derived hash is used only on the device to decide whether to reveal the settings panel.

These preferences are not backed up by the app and are removed when the app's data is cleared or the app is uninstalled.

## What the current version does not do

The current version:

- does not request Internet access;
- does not create user accounts;
- does not collect, sell, share, or transmit personal data;
- does not use analytics, advertising SDKs, or tracking;
- does not capture screenshots, record audio, or read passwords typed in other applications;
- does not store accessibility content or detailed app-usage history; and
- does not provide remote guardian or device-management functionality.

## On-device actions

When a rule applies, the app may briefly mute media, show a non-interactive accessibility overlay, perform Android Back or Home, and restore the previous media volume. The overlay does not receive touches and the app does not inject touch gestures.

## Choice and control

Accessibility and Usage Access are separate optional settings. Short-form detection requires Accessibility. The combined daily limit requires Usage Access. An optional administrator PIN can restrict who sees or edits the in-app configuration. After the app leaves the foreground, reopening the panel requires that PIN.

An ordinary Android app cannot guarantee that a local PIN prevents its own system-level uninstall. A person with device-level control can revoke access, clear app data, or uninstall Brainrot Shield. Clearing app data deletes the PIN hash and every configured rule. The current Personal build does not attempt to obstruct Android security or uninstall interfaces.

## Retention and deletion

Because the current version does not send data to a server, there is no server-side personal data to retain or delete. Clearing Brainrot Shield's app data removes its local configuration; disabling permissions or uninstalling the app stops processing.

## Future changes

Remote guardians, accounts, networking, additional packages, or managed-device features are not part of the current release. Any version that adds them must update this policy, the in-app disclosures, security design, Google Play Data safety declaration, reviewer instructions, and user controls before release.

## Contact

For privacy questions, open a sanitized issue in the project's GitHub repository. Do not include screenshots, usernames, accessibility dumps, app-usage exports, or other personal content in a public issue. For a sensitive report, use GitHub's private security-reporting channel described in `SECURITY.md`.
