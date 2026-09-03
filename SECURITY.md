# Security policy

## Supported versions

Security fixes are applied to the latest code on the `main` branch. The project is currently an early Android release and does not promise maintenance for older APKs or forks.

## Reporting a vulnerability

Please report vulnerabilities privately through GitHub's **Report a vulnerability** flow in the Security tab of this repository. Do not open a public issue for a vulnerability that could expose user data, bypass intended restrictions, or enable abuse of Android accessibility or device-management APIs.

Include:

- the affected commit or version;
- the Android version and device family;
- concise reproduction steps;
- the expected and actual behavior; and
- a description of the possible impact.

Remove or redact usernames, videos, notifications, accessibility-node text, device identifiers, tokens, keys, and any other personal or secret information. Screenshots and logs should be shared only when necessary and sanitized first.

## Security boundaries

The current app has no networking or account system. Its accessibility service must remain package-scoped and must not be repurposed to capture content, credentials, or user input. A contribution that adds networking, persistence, remote control, broader package access, or Android device-management privileges requires explicit design and privacy review before it can be merged.
