# Localization

The app includes complete translations for 17 languages across 19 locale variants:

| Language / variant | Locale |
| --- | --- |
| English | en |
| Turkish | tr |
| Hindi | hi |
| Japanese | ja |
| Portuguese | pt |
| Brazilian Portuguese | pt-BR |
| Russian | ru |
| Spanish | es |
| Arabic | ar |
| French | fr |
| German | de |
| Indonesian | id |
| Simplified Chinese | zh-Hans |
| Traditional Chinese | zh-Hant |
| Korean | ko |
| Italian | it |
| Bengali | bn |
| Urdu | ur |
| Persian | fa |

Every locale variant contains the same string resource keys as the default English resources. English and Turkish resources remain the bases. Applicable original contact/settings labels and native launcher/call labels were retained for the restored languages. New features have matching translations.

Android selects resources from the app/device language. The app manifest declares `locales_config.xml`, enabling Android 13+ system app-language selection. There is no additional language picker in the dialer interface. Launcher aliases use the localized `app_name` resource (for example, **Phone** in English and **Telefon** in Turkish). The application and dial activities use the non-translatable `system_app_name`, **Simple Dialer**, for permission/default-phone dialogs and Android app settings. This identity is defined separately in `values/app_identity.xml`. Launcher labels normally follow the device language; some launchers cache labels until refreshed.

Chinese script qualifiers use API-21-qualified folders, available on all supported Android versions. Generic Simplified Chinese and Traditional Chinese region fallbacks for Taiwan/Hong Kong are retained. Indonesian uses Android's `values-in` qualifier.

Arabic, Persian and Urdu use RTL interfaces. Telephone keypad columns and telephone-number fields retain LTR order. Localized decimal digits, including Arabic, Persian, Devanagari, Bengali and full-width digits, are normalized to ASCII telephone digits before placing a call.

Run `python scripts/validate_localizations.py` to check complete key coverage, nonempty translations, unique keys, format arguments, declared locales and Chinese fallback parity. `scripts/verify.ps1` includes this check. Instrumentation tests cover resource selection for all 19 variants, Chinese regional fallback and keypad ordering inside an RTL interface.

The translations have not had native-speaker linguistic review. That review is recommended before a public release, alongside large-font and real-device checks.
