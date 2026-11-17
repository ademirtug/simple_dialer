# Simple Dialer - Huawei AppGallery assets (English)

Prepared November 10, 2026. Upload individual files from this directory into the corresponding AppGallery Connect fields.

- `app-icon-1024.png`: requested 1024 x 1024 PNG, exported directly from the original handset vector, with the Ocean blue receiver on opaque white. No baked rounded corners or shadow.
- `app-icon-216.png`: 216 x 216 PNG alternative listed in Huawei's Android app file restrictions.
- `app-icon-512.png`: 512 x 512 PNG alternative using the same artwork as the existing Play store icon.
- `screenshots/`: five existing, unretouched English 1080 x 1920 screenshots: simplified contacts, contacts, dialpad, visibility settings, and appearance settings. All data is fictional. Captured from release 1.0.2 (27); see `artwork/store/en-US/README.md` in the project for original capture provenance. They have not been recaptured for 1.0.3 (28).
- `feature-graphic-1024x500.png`: existing English promotional illustration, provided for reuse if a suitable field is available. This is a Google Play feature-graphic size, not a required Huawei Android phone asset.
- `asset-manifest.json`: image dimensions, sizes and SHA-256 checksums.

## Which icon should I upload?

Use 1024 when your console requests 1024. Huawei publishes separate Android and HarmonyOS specifications: its Android API reference lists a 216 x 216 icon, whereas the newer HarmonyOS phone/tablet visual-asset guide accepts 216 or 1024. All three icon exports are included so the actual console field can determine the upload.

The app in this repository is an Android APK, not a native HarmonyOS NEXT app. These images do not convert the app to HarmonyOS.

Huawei's Android API table lists 3 to 5 phone screenshots under 2 MB each and a 450/800 resolution pair whose portrait/landscape labels appear reversed. The included originals are 9:16 portrait and each is under 2 MB; follow the dimensions shown by the Android app's live upload form if it requests a specific export size. No screenshots have been stretched or cropped to guess a requirement.

Official references checked November 10, 2026:

- Android file restrictions: https://developer.huawei.com/consumer/en/doc/AppGallery-connect-References/agcapi-file-requirement-0000001158365071/
- HarmonyOS visual assets: https://developer.huawei.com/consumer/en/doc/app/agc-help-app-visual-asset-spec-0000002277607976/

## Existing application packages

Release artifacts are built separately from the store images. Local versioned copies use these paths relative to the project root:

- APK: `app/build/outputs/apk/release/simple-dialer-1.0.3-28.apk`
- AAB: `app/build/outputs/bundle/release/simple-dialer-1.0.3-28.aab`

The release configuration uses application ID `info.pakkudev.simple_dialer`, version 1.0.3, code 28. Build outputs are excluded from Git. Huawei device compatibility and store acceptance need separate verification.

The console also needs store listing text, contact details, a hosted privacy policy and its other release declarations. This directory contains images and their documentation; it does not supply a privacy-policy URL, signing keys or passwords.

## Re-export icons

From the project root, using Windows PowerShell:

```powershell
./scripts/export-store-icon.ps1 -Size 1024 -Destination artwork/store/huawei/en-US/app-icon-1024.png
./scripts/export-store-icon.ps1 -Size 216 -Destination artwork/store/huawei/en-US/app-icon-216.png
```

Original assets remain in `artwork/store/en-US/`; the original vector is `artwork/launcher/handset.svg`.
