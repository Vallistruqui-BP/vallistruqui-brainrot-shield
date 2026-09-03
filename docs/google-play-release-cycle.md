# Google Play release lifecycle

Last verified: 2026-09-03

This playbook records the release process for Vallistruqui Brainrot Shield. It is deliberately split into human gates and automatable operations so a future release plugin never hides policy, identity, signing, or production-impact decisions.

## Product identity

- Repository/project name: `vallistruqui-brainrot-shield`
- Android application ID: `com.vallistruqui.brainrotshield`
- Display name: Brainrot Shield
- Supported Android range: API 23 through API 36
- Current target SDK: Android 16 / API 36
- Current distribution artifact: debug APK for local testing only
- Google Play artifact: signed Android App Bundle (`.aab`), not the debug APK

The application ID is a permanent public identity. Do not change it after creating the Play Console app or publishing releases. Every release must increment `versionCode`.

## Release gates

### Gate 0: developer identity and ownership

Human actions:

1. Create or select the correct Play Console developer account.
2. Complete personal or organization identity verification.
3. Keep the public developer name, support email, contact phone, legal identity, and payment profile accurate.
4. Register or confirm ownership of `com.vallistruqui.brainrotshield` under Android developer verification.
5. Decide who legally owns the application and signing material before public distribution.

Google Play charges a one-time developer registration fee and requires verified contact and identity information. Package-name registration becomes part of Android developer verification in 2026.

References:

- https://support.google.com/googleplay/android-developer/answer/6112435
- https://support.google.com/googleplay/android-developer/answer/13628312
- https://support.google.com/googleplay/android-developer/answer/16984799

### Gate 1: create the Play Console application

Human actions in Play Console:

1. Choose the default language and public app name.
2. Declare app versus game and free versus paid.
3. Add the support email.
4. Accept the Developer Program Policies, export-law declaration, and Play App Signing terms.
5. Create the application with the exact application ID.

The future plugin should verify that the app exists and matches the expected package, but it should not attempt to invent these legal and product choices.

Reference: https://support.google.com/googleplay/android-developer/answer/9859152

### Gate 2: release signing

Human/bootstrap actions:

1. Create a dedicated upload keystore outside the repository.
2. Store the keystore and credentials in a secure secret manager with a tested backup.
3. Configure Play App Signing.
4. Record the public SHA-256 certificates for the upload key and Play signing key.
5. Decide whether the same signing identity must support GitHub or other app stores.

Automatable actions after bootstrap:

1. Build the release Android App Bundle.
2. Sign it with the upload key supplied at runtime.
3. Verify the signature and application ID.
4. Confirm that the `versionCode` is greater than the latest Play version.
5. Produce checksums and a software bill of materials when dependencies are added.

Never commit a keystore, service-account JSON, passwords, access tokens, or generated secret-bearing Gradle properties.

References:

- https://developer.android.com/studio/publish/app-signing
- https://developer.android.com/studio/publish/upload-bundle

### Gate 3: store listing and policy declarations

Human-reviewed inputs:

- App name, short description, full description, icon, feature graphic, screenshots, category, support details, and privacy-policy URL.
- Ads declaration.
- App access instructions.
- Content rating questionnaire.
- Target audience and content declaration.
- Data safety declaration.
- AccessibilityService declaration and supporting review video.
- Any parental-control or device-management declarations introduced by a future Managed edition.

Brainrot Shield is not a general accessibility tool for people with disabilities and must not set `isAccessibilityTool=true`. Before sending users to Android Accessibility settings, the app must provide a separate prominent disclosure that states:

- which applications it can inspect;
- that it reads the visible accessibility hierarchy to recognize short-form feeds;
- that it performs a deterministic Back action;
- whether any data leaves the device;
- how the user can disable the feature.

The user must affirmatively accept that disclosure. A privacy policy and Data safety form remain required even if the current build collects and shares no user data.

The protection screen uses `TYPE_ACCESSIBILITY_OVERLAY`, which is supplied by the enabled AccessibilityService. The app does not request `SYSTEM_ALERT_WINDOW`; reintroducing that permission requires a new necessity and policy review.

The long-term product contains Personal and Managed modes under one application identity. To reduce first-review risk, the initial Play release should establish the transparent Personal mode first. Managed-device enrollment, remote guardian communication, and uninstall policy must be introduced only after their implementation, disclosures, target-audience answers, Data safety impact, and parental-control posture are independently reviewed.

References:

- https://support.google.com/googleplay/android-developer/answer/10964491
- https://support.google.com/googleplay/android-developer/answer/11150561
- https://support.google.com/googleplay/android-developer/answer/10787469
- https://support.google.com/googleplay/android-developer/answer/9867159

### Gate 4: technical preflight

The release candidate must pass:

1. Unit tests.
2. Android lint with no errors.
3. Release build and signing verification.
4. Manifest review for permissions, exported components, cleartext traffic, backup behavior, and package visibility.
5. Tests on every supported Android API tier.
6. Tests on representative Pixel/stock Android, Samsung, Xiaomi/HyperOS, Motorola, and Oppo/Realme devices when available.
7. Tests against the current supported YouTube, Instagram, and TikTok versions and supported locales.
8. Large-text, screen-reader, dark-mode, landscape, small-phone, and tablet checks.
9. Permission denial, permission revocation, service teardown, volume restoration, and false-positive checks.
10. Review that no accessibility text, account name, video metadata, or private content is logged or transmitted.

New apps and updates submitted after 2026-08-31 must target API 36 or higher. This project already targets API 36.

Reference: https://developer.android.com/google/play/requirements/target-sdk

### Gate 5: testing tracks

Recommended progression:

1. Local/debug verification.
2. Play internal testing for trusted maintainers and fast distribution.
3. Closed testing for broader device, locale, and social-app coverage.
4. Open testing only after the store listing and privacy posture are ready for public visibility.
5. Production after all Play requirements and project quality gates pass.

For personal developer accounts created after 2023-11-13, production access currently requires at least 12 testers continuously opted into a closed test for at least 14 days. This requirement must be checked again at release time because Play policies can change.

References:

- https://support.google.com/googleplay/android-developer/answer/14151465
- https://support.google.com/googleplay/android-developer/answer/9845334

### Gate 6: submission and rollout

For the first release:

1. Upload the signed AAB to internal testing.
2. Resolve Play Console errors and review warnings.
3. Complete the required closed-testing cycle if applicable.
4. Apply for production access if the account requires it.
5. Submit all app content and release changes for review.
6. Obtain explicit human approval before the first production rollout.

For updates:

1. Start with internal or closed testing.
2. Review Android vitals, crashes, ANRs, accessibility behavior, permission funnels, and tester reports.
3. Promote the exact tested bundle to production.
4. Prefer a staged rollout for material detector, permission, networking, guardian, or device-policy changes.
5. Pause rollout when health or false-positive thresholds are exceeded.
6. Expand gradually and record the release decision.

Reference: https://support.google.com/googleplay/android-developer/answer/9859348

### Gate 7: post-release operation

Monitor:

- crash and ANR rates;
- permission activation failures by Android/OEM version;
- false-positive and missed-detection reports by social-app version;
- Play policy messages and review status;
- security reports;
- staged rollout health;
- target-SDK and developer-verification deadlines.

Do not collect viewing history or accessibility contents to obtain these metrics. Prefer coarse, opt-in operational counters or user-submitted redacted diagnostics if telemetry is added later.

## Google Play Publishing API workflow

Once the developer account, Play app, policy declarations, app signing, and API access exist, an automated release can use the official edit transaction:

1. Authenticate through a least-privilege service account or user OAuth flow.
2. Insert an edit for `com.vallistruqui.brainrotshield`.
3. Upload the signed AAB to that edit.
4. Update the intended track, initially using a draft or internal release.
5. Validate the edit.
6. Present the full release diff and impact for human approval.
7. Commit the edit only after approval.
8. Poll the resulting release state with bounded retries.

The Android Publisher API uses an insert/change/validate/commit transaction model. API access requires a Google Cloud project, the Android Publisher API, and Play Console permissions for the chosen service account or OAuth identity.

References:

- https://developers.google.com/android-publisher/getting_started
- https://developers.google.com/android-publisher/api-ref/rest
- https://developers.google.com/android-publisher/api-ref/rest/v3/edits.bundles/upload

## Public GitHub releases

GitHub and Google Play releases can coexist, but they need an intentional signing strategy:

- Never publish debug-signed APKs as official releases.
- Sign every GitHub APK with a stable release key.
- Publish SHA-256 checksums and release notes.
- Register and prove ownership of the package/signing identity as required by Android developer verification.
- Ensure the GitHub APK and Play-distributed application can receive compatible updates before choosing Play's signing-key configuration.
- Treat GitHub source availability as transparency, not as a substitute for Android trust, signing, or Play policy compliance.

## Definition of release-ready

A version is release-ready only when all applicable gates above are recorded as passing, the tested artifact hash matches the artifact proposed for release, and a human with publication authority explicitly approves the destination track and rollout scope.
