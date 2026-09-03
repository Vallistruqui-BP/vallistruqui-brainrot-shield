# Future Google Play release plugin blueprint

Status: research artifact only; no plugin has been created.

## Purpose

A future Codex plugin should turn the documented Google Play lifecycle into a guarded, repeatable release workflow for Android projects. Its job is to reduce omission and mechanical error, not to bypass Play Console review, developer identity checks, policy declarations, testing requirements, or human authorization.

Canonical lifecycle: [google-play-release-cycle.md](google-play-release-cycle.md)

## Proposed plugin name and scope

- Working name: `google-play-release-assistant`
- Primary skill: audit, prepare, validate, and release Android App Bundles to Google Play
- Optional MCP/service: a narrow wrapper around the Google Play Android Publisher API
- Default behavior: read-only audit and dry run
- Mutation boundary: uploading or committing an edit always requires an explicit release request
- Production boundary: production creation, promotion, or rollout expansion always requires a separate explicit confirmation showing package, version, artifact hash, track, countries, status, and rollout percentage

## Capabilities

### Read-only audit

- Detect Android Gradle projects.
- Read application ID, namespace, minimum SDK, target SDK, version code, and version name.
- Compare the package with the expected Play application.
- Run tests and lint.
- Inspect manifests and merged manifests.
- Detect sensitive permissions and required Play declarations.
- Verify prominent disclosure resources exist for AccessibilityService usage.
- Verify a separate prominent disclosure exists before opening Usage Access settings when `PACKAGE_USAGE_STATS` is declared.
- Inspect an AAB/APK package, version, signature certificate, and SHA-256.
- Check for accidentally tracked keystores, credentials, service-account files, or secret-bearing properties.
- Compare local `versionCode` with the latest uploaded Play bundle.
- Read tracks and release status through the Android Publisher API.
- Generate a human-readable readiness report with blocking errors, warnings, and manual gates.

### Local preparation

- Build a release AAB using project-provided Gradle tasks.
- Accept signing secrets only through runtime environment or an approved secret provider.
- Generate checksums, release notes, and a release manifest.
- Validate store-listing text and image inventory stored in the repository.
- Prepare, but never fabricate, Data safety and permission-declaration evidence for human review.

### Google Play mutation

- Insert an Android Publisher edit.
- Upload a signed AAB.
- Update an internal, closed, open, or production track.
- Create draft or in-progress staged releases.
- Validate the edit.
- Show the exact pending diff.
- Commit after explicit approval.
- Poll the committed release state and return Play Console URLs when available.

## Actions that remain human

- Create and pay for a Play Console account.
- Verify personal or organization identity.
- Choose legal account ownership and public developer identity.
- Create the initial Play Console application.
- Accept agreements and Play App Signing terms.
- Make the first signing-key ownership decision.
- Complete content rating, target audience, Data safety, ads, app access, and sensitive-permission declarations.
- Record or approve the AccessibilityService review video.
- Recruit legitimate testers and complete account-specific testing requirements.
- Apply for production access when Play requires it.
- Decide whether the Personal or Managed product is legally appropriate for the intended audience.
- Approve any first production release, material permission expansion, remote-control feature, or rollout expansion.

## API transaction

The plugin's write path should be stateful and resumable:

```text
preflight
  -> authenticate with least privilege
  -> edits.insert
  -> bundles.upload
  -> tracks.update (draft/internal by default)
  -> edits.validate
  -> render proposed release diff
  -> explicit approval
  -> edits.commit
  -> bounded status polling
  -> immutable local release receipt
```

An idempotency record should include:

- developer account ID;
- package name;
- local version code;
- AAB SHA-256;
- Play edit ID;
- uploaded bundle version code;
- destination track;
- release status and rollout fraction;
- approval timestamp and approving user;
- commit result.

If a run fails before commit, the plugin must report the existing edit and offer a safe resume or abandonment path. It must not silently create multiple uploads or widen a rollout.

## Credential model

- Prefer OAuth for an interactive, multi-user plugin.
- Permit a service account for CI or a dedicated secure release host.
- Grant the smallest Play Console permissions required for the chosen operation.
- Never accept credentials through chat text.
- Never store service-account JSON, refresh tokens, keystores, or passwords in the repository, plugin manifest, logs, reports, or generated artifacts.
- Redact tokens, emails where unnecessary, filesystem secret paths, and authorization headers from tool output.
- Support credential revocation and an authentication-only diagnostic command.

Official setup reference: https://developers.google.com/android-publisher/getting_started

## Policy-aware checks for Brainrot Shield

The first project profile should encode these checks:

- Expected package: `com.vallistruqui.brainrotshield`.
- Target SDK must satisfy the current Play requirement; API 36 as of 2026-09-03.
- Accessibility service must remain limited to explicitly supported social-app packages.
- Usage Access must remain optional, locally processed, and limited to aggregate supported-app timing plus foreground enforcement.
- The app must provide prominent disclosure and affirmative consent before opening Accessibility settings.
- The Play listing must disclose AccessibilityService use.
- The app must not declare itself an accessibility tool unless its primary purpose changes and legitimately qualifies.
- Rule-based detection may issue Back; open-ended autonomous behavior is out of scope.
- No viewing history, accessibility text, screenshots, or private social content may be uploaded.
- No detailed history of unrelated app usage may be retained or transmitted.
- Any guardian networking requires a new Data safety review.
- Any uninstall blocking requires a separately reviewed parental-control or enterprise-managed product mode.

## Suggested plugin commands

- `play-audit`: local project and policy readiness report; never mutates.
- `play-build`: run verified release build and produce artifact manifest.
- `play-tracks`: read current Play testing and production tracks.
- `play-prepare`: create an edit, upload the AAB, and leave a draft after validation.
- `play-release`: display the prepared diff and commit only after explicit approval.
- `play-promote`: promote an already tested version between tracks with explicit scope confirmation.
- `play-status`: read review, release, and rollout status.

## Reusable versus project-specific design

The plugin should be generic. Project-specific facts belong in a checked-in release profile, for example `.play/release-profile.yaml`, containing only non-secret configuration such as package name, Gradle task, artifact path, allowed tracks, required checks, and protected permissions.

Secrets remain external. Policy answers remain human-reviewed inputs. This division allows the same plugin to serve other Android projects without hard-coding Brainrot Shield's identity or privacy model.

## Build trigger for the future plugin

Create the actual plugin only after all of these are available:

1. A verified Play Console developer account.
2. The Play application for `com.vallistruqui.brainrotshield`.
3. A settled Personal/Managed product strategy.
4. A release signing and secret-storage decision.
5. At least one manually completed internal-track release whose steps can be observed and encoded.
6. Agreement on which Play actions may be automated and which approvals must remain interactive.

That manual first release is intentionally part of the design process: it supplies real account-specific screens, declarations, permissions, and failure modes instead of basing the plugin entirely on documentation.
