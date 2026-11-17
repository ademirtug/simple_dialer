# Simple Dialer — English Google Play assets

Upload these files in Play Console's main English store listing:

- `app-icon-512.png`: 512 × 512, 32-bit PNG. The actual Ocean launcher receiver on an opaque white square, exported from the existing vector artwork. No baked corner mask or shadow.
- `feature-graphic-1024x500.png`: 1024 × 500, 24-bit RGB PNG with no transparency. English title and tagline, matching the app's blue palette.
- `screenshots/`: six native 1080 × 1920 PNG screen captures. Suggested order: simplified contacts, dialpad, favorites, full contacts, visibility settings, appearance settings.

Requirements reference: https://support.google.com/googleplay/android-developer/answer/9866151?hl=en

## Screenshot authenticity

Captured directly from the real `MainActivity` using Android's `screencap`, from the same application source as release `1.0.2 (27)`. The images have no invented UI, composited content, marketing frames, or retouching. All shown settings and features exist in the app. Different screenshots intentionally show different supported visibility configurations.

The contact names are fictional. Numbers use the reserved North American 202-555-01xx fictional-number range. Contact photos are the app's real initial avatars. No real contacts, call logs, or conversations are used. No calls are placed. Android's real status-bar demo mode standardizes the clock and battery for the main capture sequence; the separately recaptured visibility screen uses the normal status bar. The emulator's real screen resolution is configured to 1080 × 1920 for capture.

## Artwork provenance

The icon is a deterministic export of `artwork/launcher/handset.svg`, generated with `scripts/export-store-icon.ps1`. It matches the installed app's handset rather than introducing a different logo.

The feature graphic is a promotional illustration, exported at 1024 x 500 pixels in RGB without alpha. It is separate from the app screenshots. Working files are kept outside version control.

## Reproduce screenshots

Start the disposable `Medium_Phone_API_36.1` emulator, then run:

```powershell
./scripts/capture-store-screenshots.ps1 -Serial emulator-5554
```

The script temporarily prepares fictional contacts, uses the real app and preferences, captures the six screens, then restores the emulator's display mode and the five normal debug contact fixtures. It does not operate on physical phones.
