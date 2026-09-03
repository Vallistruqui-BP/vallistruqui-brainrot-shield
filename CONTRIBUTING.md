# Contributing

Thanks for helping make Brainrot Shield safer and more useful.

## Before you start

Open an issue before a large change, especially one involving additional apps, accessibility behavior, networking, remote guardians, authentication, or managed-device APIs. Small bug fixes and documentation improvements can go directly to a pull request.

## Local development

Requirements:

- JDK 17
- Android SDK Platform 36
- Android SDK Build Tools 36.1.0

On Windows:

    .\gradlew.bat test lint assembleDebug

On Linux or macOS:

    ./gradlew test lint assembleDebug

Before submitting a pull request, make sure tests and Android lint pass. Add focused tests when changing detection logic, and update the README and privacy policy when supported apps, permissions, setup, or data handling changes.

## Safety and privacy requirements

- Keep accessibility access scoped to the packages the feature explicitly supports.
- Keep Usage Access processing transient and limited to foreground detection plus aggregate time calculations for supported apps.
- Do not add analytics, advertising, screenshots, key logging, hidden persistence, or collection of accessibility content.
- Do not intercept or obstruct Android's uninstall or security interfaces through accessibility automation.
- Treat Personal and Managed modes as different security models. Only legitimate Android Device Owner or Profile Owner enrollment may claim managed uninstall restrictions.
- Restore media volume after every blocking attempt and service shutdown.
- Add regression fixtures for ordinary screens whenever changing a short-form detector; a navigation label by itself is not enough evidence.
- Never commit APKs, signing keys, keystores, service-account credentials, tokens, `local.properties`, or generated build directories.

## Pull requests

Explain the user-facing change, its privacy impact, supported Android and target-app versions, and how it was tested. Keep each pull request focused enough to review safely.

By contributing, you agree that your contributions are licensed under the Apache License 2.0 included in this repository.
