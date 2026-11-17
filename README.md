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

## Device setup

Select Simple Dialer as the default phone app and grant the permissions needed for the features you use. Incoming calls also require notifications and full-screen access on Android versions that provide those controls. Contacts and call-history access are optional; manual dialing remains available when either is denied.

On Xiaomi, Redmi, and Poco devices, check **Show on Lock screen** and **Open new windows while running in the background** in the app's MIUI permission settings. These permissions are separate from Android's notification and full-screen settings.

Auto answer requires an uncovered proximity sensor for 0.5 seconds followed by a nearby object for 0.8 seconds. Auto speaker switches to speaker after 1.5 seconds of an uncovered sensor during an active call. Wired and Bluetooth headset routing takes precedence.

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

## Verification

The Windows verification script checks translations, runs unit tests, builds the debug APK, runs Android lint, and checks the native libraries for 16 KB page compatibility:

```powershell
.\scripts\verify.ps1
```

Add `-IncludeRelease` to verify the release APK as well. The scripts require Python 3 in addition to the Android build tools.

To run unit tests and lint directly:

```sh
./gradlew :domain:test :platform:testDebugUnitTest :data:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug
python scripts/validate_localizations.py
```

Instrumentation tests run on a connected test device or emulator with `:app:connectedDebugAndroidTest`. The simulated-call tests require a disposable emulator:

```powershell
.\scripts\seed-emulator-contacts.ps1 -Serial emulator-5554
.\scripts\test-emulator-call.ps1 -Serial emulator-5554
.\scripts\test-emulator-call.ps1 -Serial emulator-5554 -AutoModes
.\scripts\test-emulator-call.ps1 -Serial emulator-5554 -LockedScreen
```

The fixtures use fictional contacts. Simulated calls do not exercise cellular audio or carrier services. Lock-screen behavior, call waiting, dual SIM, conference calls, and translations still need review on the intended devices and networks.

## Release signing

Release credentials are stored outside the repository at `~/.signing/simple-dialer/key.properties`. Copy [keystore.properties.example](keystore.properties.example) to that location and supply the registered upload keystore's details. Set `SIGNING_ROOT` to use another signing directory.

```sh
./gradlew :app:assembleRelease :app:bundleRelease
```

Outputs are written to `app/build/outputs/apk/release/` and `app/build/outputs/bundle/release/`. Without the release properties file, the build falls back to the local Android debug key. Configure the registered upload key before building a store update. See [Signing configuration](SIGNING.md) for verification and backup guidance.

## Data and permissions

Contacts and call history are read from Android's providers. Contact edits are handled by the system contact editor; local favorites and interface preferences are stored with DataStore. The app does not record call audio or send SMS messages. The incoming-call message action opens the system SMS composer.

Firebase Analytics collects its standard automatic events. App code does not log contact details or call data as custom analytics events. The Firebase configuration and all signing and service-account credentials are kept outside version control.

## Store assets

Google Play images are in [artwork/store/en-US](artwork/store/en-US/). Huawei AppGallery images and listing files are in [artwork/store/huawei](artwork/store/huawei/). Each image set includes upload notes and a checksum manifest. Screenshots show fictional data captured from the app.

See [Huawei publishing](docs/huawei-publishing.md) for the AppGallery helper and release-draft commands.
