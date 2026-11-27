<h1 align="center">Luncher</h1>

<p align="center">Minimal, lightweight, and clutter-free Android launcher.</p>

- Download the signed APK from [GitHub Releases](https://github.com/sudiplun/Luncher/releases).
- Based on [OlauncherCF](https://github.com/OlauncherCF/OlauncherCF) and [Olauncher](https://github.com/tanujnotes/Olauncher).

## Features

- **Home screen date modes (`AD` / `BS` / `Off`):** Supports Nepali Bikram Sambat (`BS`) dates (in English or Devanagari when the device language is Nepali), Gregorian (`AD`) dates, or hiding the date (`Off`).
- **Smaller APK:** Reduced the release APK from 1.61 MiB to 429 KB (about 74%) by filtering unused locales, removing unused UI dependencies, and excluding packaging metadata.
- **Gestures & shortcuts:** Configurable swipe up/down/left/right, double tap, and clock/date tap actions (open app, lock screen, notification drawer, quick settings, app list, or recents).
- **Customizable layout:** Independent alignment for clock/date, home apps, and app drawer, adjustable text size, app renaming, and hidden apps.
- **Zero network access:** No `INTERNET` permission—does not collect or transmit any data.

## Build & CI/CD

- Local release APK build:
  ```bash
  ./gradlew assembleRelease
  ```
- See [CI/CD Setup](docs/CICD.md) for GitHub Actions workflows, checks, signing secret setup, and release tagging instructions.

## License

**Luncher is licensed under GPLv3**, meaning you can use, study, modify, and share it freely under the same license terms. See [LICENSE](LICENSE) for details.

## Permissions

- `android.permission.EXPAND_STATUS_BAR` — Expand or collapse the notification shade and quick settings.
- `android.permission.QUERY_ALL_PACKAGES` — List installed apps in the app drawer.
- `android.alarm.permission.SET_ALARM` — Open the default alarm/clock app when tapping the clock (if no custom app is set).
- `android.permission.REQUEST_DELETE_PACKAGES` — Request uninstalling an app from the app info menu.
