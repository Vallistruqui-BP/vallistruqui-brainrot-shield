# Dual-mode protection architecture

Status: accepted product direction

Date: 2026-09-03

## Decision

Brainrot Shield will ultimately expose both Personal and Managed protection modes under the application ID `com.vallistruqui.brainrotshield`. The modes must remain visibly distinct because Android grants them fundamentally different authority.

## Personal mode

Personal mode is installed normally from Google Play or a signed GitHub release.

Capabilities:

- Block configured short-form feeds through deterministic AccessibilityService rules.
- Protect in-app configuration with an optional local PIN.
- Permit an optional, explicitly paired remote accountability guardian to approve pauses or configuration changes.
- Continue operating locally when remote services are unavailable, according to the last valid policy.

Limits that must be shown before enabling a PIN:

- The PIN protects settings inside Brainrot Shield.
- It cannot stop the device owner from disabling the AccessibilityService in Android settings.
- It cannot stop the device owner from uninstalling Brainrot Shield.
- It must never be described as uninstall protection.

The local PIN exists to add intentional friction and support accountability, not to override ownership of a personal device.

## Managed mode

Managed mode is available only when Android reports that Brainrot Shield has been legitimately provisioned with the required device-policy authority.

Capabilities after explicit enrollment:

- Apply Android-supported uninstall blocking through `DevicePolicyManager`.
- Protect policy changes through the enrolled guardian or administrator.
- Receive signed remote policy updates when networking is later implemented.
- Show the identity of the guardian or administrator and the current managed state.

Enrollment requirements:

- Clear disclosure that the device will be managed.
- Confirmation by the device owner or an authorized parent/guardian.
- Android-supported provisioning, commonly during initial device setup.
- A documented recovery and unenrollment process appropriate to the ownership model.
- No covert activation, hidden icon, deceptive overlay, settings interception, or Accessibility-based prevention of removal.

Managed mode must not pretend that a normal installation has device-owner authority. If the package is not an authorized device or profile owner, the mode remains unavailable and explains the required setup.

Official Android references:

- https://developer.android.com/reference/android/app/admin/DevicePolicyManager
- https://developer.android.com/work/dpc/dedicated-devices
- https://developer.android.com/work/dpc/device-management

## Remote guardian model

Remote control will be consent-based and configuration-only.

The protected device:

- displays the guardian identity and connection state;
- creates or accepts a one-time pairing request;
- stores a guardian public key or equivalent verified identity;
- accepts only authenticated, replay-protected, expiring policy commands;
- retains an owner-appropriate recovery path;
- never uploads accessibility contents, screenshots, messages, usernames, viewed videos, or browsing history.

The guardian can:

- approve or reject a pause request;
- update protected apps and schedules;
- rotate configuration credentials;
- revoke a lost guardian session;
- view configuration health only when the protected user has consented to that reporting.

A raw PIN must not be transmitted or stored by the backend. A guardian approval or signed policy is preferable to synchronizing a reusable password.

## User experience

The app presents protection level as a capability choice, not as a vague strength slider:

```text
Personal
Protects Brainrot Shield settings. Android still allows the owner to disable or uninstall the app.

Managed
Can enforce uninstall policy only on an explicitly enrolled supervised device.
```

Each mode must include:

- a plain-language capability list;
- a plain-language limitations list;
- the current authority detected from Android;
- the guardian/administrator identity, when applicable;
- an accessible status using text and iconography rather than color alone;
- a recovery explanation before activation;
- a separate confirmation for security-impacting changes.

All touch targets must be at least 48dp, content must tolerate system text scaling, and the flow must remain usable in landscape, on small screens, and with TalkBack.

## Google Play rollout strategy

One application identity can evolve toward both modes, but release risk is staged:

1. Publish the smallest compliant Personal build with YouTube protection, prominent Accessibility disclosure, no networking, and no remote backend.
2. Establish internal and closed testing and obtain the initial policy review.
3. Add Instagram and TikTok detectors with explicit package scoping and a renewed Accessibility declaration.
4. Add optional local PIN configuration, still clearly labeled as in-app protection.
5. Add remote guardian pairing only after networking, privacy, account deletion, security, abuse prevention, Data safety, and consent reviews pass.
6. Add Managed mode only after device-policy provisioning and parental/administrative policy requirements pass an independent review.

Google Play publication cannot be guaranteed by implementation alone; Google makes the final review decision. This sequence minimizes avoidable rejection risk and keeps each permission expansion explainable and testable.

Official Play policy references:

- https://support.google.com/googleplay/android-developer/answer/10964491
- https://support.google.com/googleplay/android-developer/answer/11150561
- https://support.google.com/googleplay/android-developer/answer/16558241
- https://support.google.com/googleplay/android-developer/answer/9893335

## Rejected alternatives

- Blocking or covering Android settings through AccessibilityService.
- Automatically pressing Back on uninstall or service-disable screens.
- Hiding the app or managed state.
- Claiming that a local PIN prevents uninstall.
- Storing a guardian's raw password on a server.
- Sending social-app accessibility contents to a remote guardian.

These approaches are technically fragile, misleading, unsafe, or incompatible with the intended Google Play posture.
