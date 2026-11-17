# Simple Dialer

A native Android phone app with contacts, recent calls, favorites, and a configurable call screen. The interface can be simplified by hiding tabs and optional controls in Settings.

Install the published app from Google Play:

<a href="https://play.google.com/store/apps/details?id=info.pakkudev.simple_dialer">
  <img src="artwork/store/google-play-badge.png" alt="Get it on Google Play" width="200">
</a>

## Build

Requirements:

- Android Studio with Android SDK 36 installed.
- JDK 17 or newer compatible with the included Gradle wrapper. Android Studio's bundled JDK can be used.
- A Firebase Android configuration for `info.pakkudev.simple_dialer`, saved as `app/google-services.json`. This file is excluded from Git.

Open the project in Android Studio, add the Firebase configuration, sync Gradle, and run the `app` configuration. To build from a terminal:

```sh
./gradlew :app:assembleDebug
```

On Windows:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Project structure

| Directory | Purpose |
| --- | --- |
| `app` | Compose screens, ViewModels, Telecom service, and dependency wiring |
| `domain` | Kotlin models, repository interfaces, and call-handling policies |
| `data` | Contacts, call history, preferences, active calls, and proximity sensing |
| `platform` | Android version handling, default-phone access, and call placement |
| `scripts` | Build verification, emulator fixtures, and artwork exports |
| `artwork` | Launcher artwork, store images, and listing files |
| `docs` | Architecture, localization, and publishing notes |

See [Architecture](docs/architecture.md) for module boundaries and state management, and [Localization](docs/localization.md) for supported locales and translation checks.
