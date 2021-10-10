


import 'app_localizations.dart';

/// The translations for Turkish (`tr`).
class AppLocalizationsTr extends AppLocalizations {
  AppLocalizationsTr([String locale = 'tr']) : super(locale);

  @override
  String get contacts => 'Rehber';

  @override
  String get recents => 'Çağrı Geçmişi';

  @override
  String get settings => 'Ayarlar';

  @override
  String get activateRecents => '\'Çağrı Geçmişi\' sekmesini etkinleştir.';

  @override
  String get permissonNeeded => 'İzin Gerekli';

  @override
  String get defaultPhoneAppWarning => '\'Simple Dialer\' uygulamasını varsayılan Telefon uygulaması olarak belirlemelisiniz.';

  @override
  String get cancel => 'Vazgeç';

  @override
  String get openSettings => 'Ayarları Aç';

  @override
  String get setAsDefaultPhoneApp => 'Varsayılan Telefon uygulaması olarak ayarla';

  @override
  String get makeDefault => 'Varsayılan Yap';

  @override
  String get makeDefaultMessage => '\'Simple Dialer\' uygulamasını varsayılan Telefon uygulamanız yapmak ister misiniz?';

  @override
  String get neverAsk => 'Tekrar sorma.';

  @override
  String get proximityAccept => 'Auto accept calls';

  @override
  String get proximityAcceptDescription => 'Allow proximity sensor to automatically accept calls when it detects an object(like user\'s ears)';

  @override
  String get proximityLoudspeaker => 'Auto switch loudspeaker';

  @override
  String get proximityLoudspeakerDescription => 'Allow proximity sensor to automatically switch to loudspeaker when there is no object in vicinity(like user\'s ears)';
}
