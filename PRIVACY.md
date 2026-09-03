# Privacy policy

Effective date: September 3, 2026

Vallistruqui Brainrot Shield is designed to perform its current blocking behavior entirely on the Android device.

## Data handled by the app

When the user explicitly enables its accessibility service, the app observes accessibility events and on-screen accessibility metadata from the official YouTube Android package (`com.google.android.youtube`). It uses that information transiently to determine whether the Shorts feed is active.

The current version:

- does not request Internet access;
- does not create user accounts;
- does not collect, sell, share, or transmit personal data;
- does not use analytics, advertising SDKs, or tracking;
- does not capture screenshots, record audio, or read typed text; and
- does not store accessibility content or browsing history.

If the detector finds strong evidence of the Shorts feed, the app briefly shows a non-interactive accessibility overlay, performs Android's global Back action, temporarily mutes media audio, and restores the prior media volume.

## Permissions and sensitive access

- **Accessibility service:** limited by its configuration to the official YouTube package. This access is required to recognize the Shorts interface and perform Back.
- **Modify audio settings:** used only to mute Shorts during the blocking action and restore the previous media volume.

The service is optional and can be disabled at any time from Android Accessibility settings. Uninstalling the app also removes it. An ordinary Android app cannot guarantee that a local PIN prevents its own system-level uninstall.

## Retention and deletion

Because the current version does not collect or persist personal data, there is no server-side personal data to retain or delete. Users can disable the accessibility service or uninstall the app to stop all processing.

## Future changes

Planned support for additional apps, remote guardians, or managed-device features is not part of the current release. Any version that introduces networking, accounts, remote configuration, or new data handling must update this policy and its Google Play Data safety disclosure before release.

## Contact

For privacy questions, open a sanitized issue in the project's GitHub repository. Do not include screenshots, usernames, accessibility dumps, or other personal content in a public issue. For a sensitive report, use GitHub's private security-reporting channel described in `SECURITY.md`.
