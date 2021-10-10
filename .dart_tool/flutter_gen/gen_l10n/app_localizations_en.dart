


import 'app_localizations.dart';

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get contacts => 'Contacts';

  @override
  String get recents => 'Recents';

  @override
  String get settings => 'Settings';

  @override
  String get activateRecents => 'Activate \'Recents\' tab.';

  @override
  String get permissonNeeded => 'Permission Needed';

  @override
  String get defaultPhoneAppWarning => 'You need to set \'Simple Dialer\' as your default Phone app.';

  @override
  String get cancel => 'Cancel';

  @override
  String get openSettings => 'Open Settings';

  @override
  String get setAsDefaultPhoneApp => 'Set as default Phone app';

  @override
  String get makeDefault => 'Make Default';

  @override
  String get makeDefaultMessage => 'Would you like to set \'Simple Dialer\' as your default Phone app?';

  @override
  String get neverAsk => 'Never ask again.';

  @override
  String get proximityAccept => 'Auto accept calls';

  @override
  String get proximityAcceptDescription => 'Allow proximity sensor to automatically accept calls when it detects an object(like user\'s ears)';

  @override
  String get proximityLoudspeaker => 'Auto switch loudspeaker';

  @override
  String get proximityLoudspeakerDescription => 'Allow proximity sensor to automatically switch to loudspeaker when there is no object in vicinity(like user\'s ears)';
}
