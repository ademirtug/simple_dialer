# Architecture

| Module | Responsibility |
| --- | --- |
| `domain` | Plain Kotlin models, repository interfaces, number validation and automatic-call policy |
| `platform` | API 23+ default-phone/call-placement adapters, version profile and permission request history |
| `data` | Modern provider/DataStore adapters, native call collection, proximity observation and screen lock |
| `app` | Compose phone/admin/call screens, ViewModels, Telecom entry points and Koin composition root |

The domain depends on no Android or DI implementation. Call launching and active-call management have separate interfaces. All Android modules require API 23 or newer.

## Platform selection

`PlatformProfile.current()` contains the sole `Build.VERSION.SDK_INT` read. Factories and Koin select implementations of:

- `DefaultDialerAccess`: Telecom on 23–28, RoleManager on 29+.
- `CallPlacement`: Telecom on all supported Android versions.
- `SpeakerRouting`: audio routes on 23–33, call endpoints on 34+.
- `CallWindowPolicy`: window flags before 27, lock-screen methods thereafter.
- `NotificationAccess`: app notification state before 26, channel state on 26+, runtime notification consent on 33+.
- `FullScreenCallAccess`: Android 14 special access or the older unrestricted platform contract.

SDK differences do not spread through screens or ViewModels. Newer implementations are instantiated only on supported platforms.

## Observable state and cleanup

Contacts/history use `ContentObserver` inside `callbackFlow`, run queries on IO, and unregister in `awaitClose`. DataStore exposes immutable `DialerPreferences`.

Contacts retain their phone data-row ID, aggregate contact ID and stable contact lookup URI. The app opens Android's contact editor with `ACTION_EDIT` and the lookup URI. The editor owns address-book writes; the dialer needs no `WRITE_CONTACTS` permission. Provider observations refresh contact lists after edits.

Every native call has an independent callback and ID. `CallRepository` exposes calls, audio and add-call availability through read-only StateFlows. No Activity is kept in a static field. UI collectors are lifecycle-aware; service scopes and sensor listeners are cancelled at service destruction.

Provider failures are distinct from permission denial. The readiness repository is refreshed after permission/settings results, foreground entry and access failures. History observation stops when the default-phone role is lost.

## Admin configuration

`DialerPreferences` persists optional tab visibility, automatic modes, local favorites, exclusions for device-starred favorites and hidden optional call controls. All three tabs can be hidden independently; the dialpad, Settings and essential answer/decline/end actions remain available. Normal phone screens do not show configuration controls or setup dashboards.

Ocean is the default color palette; explicitly saved choices remain unchanged. The application's scope observes palette changes and selects one of five launcher activity aliases with a matching handset color on a fixed white background. All aliases target the permanently enabled `MainActivity`, preserving dial-intent handling. `PlatformProfile` selects atomic component updates on API 33+ and enable-before-disable updates on API 23–32. Icon updates do not retain an Activity.

`artwork/launcher/handset.svg` defines a single continuous receiver contour with matching ends. `scripts/generate-launcher-icons.ps1` produces the native vectors, adaptive icons, density PNGs and palette previews from this shape. Colored handsets use the app's light-mode primary accents for contrast against white.

Dark mode uses neutral charcoal background and surface roles across all palettes, with no elevation tint. Palette colors remain on accents and action containers. Monochrome also applies neutral roles to ordinary controls. Contact photos render in their original colors. Dialing actions use green accents, while the in-call answer and decline/end buttons retain green and red independently of the selected palette.

## Automatic call policy

`CallAssistancePolicy` is plain Kotlin. Each ringing call is armed after a far proximity reading and answered at most once on a near transition. Speaker decisions apply only to active calls and preserve headset routes. Missing sensor readings produce no automatic action. Both modes default off.

`CallAssistance` observes policy inputs in the system-bound call service. A separate `CallScreenLock` manages the proximity wake lock for earpiece calls. Call controls are gated by actual Telecom capabilities; DTMF invokes native call-tone APIs.

## Permission handling

Permission request history distinguishes an unrequested permission from a received denial. A first request is not classified as permanently denied just because Android has no rationale to show. Android grant state is always rechecked; stored history never substitutes for a grant.

Only affected features are unavailable on denial. Explanations can be cancelled. Settings recovery occurs only after an explicit user action. Permission and role callbacks never chain requests or resume a pending telephone call.
