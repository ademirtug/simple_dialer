


import 'app_localizations.dart';

/// The translations for Hindi (`hi`).
class AppLocalizationsHi extends AppLocalizations {
  AppLocalizationsHi([String locale = 'hi']) : super(locale);

  @override
  String get contacts => 'संपर्क';

  @override
  String get recents => 'कॉल इतिहास';

  @override
  String get settings => 'समायोजन';

  @override
  String get activateRecents => '\'कॉल इतिहास\' टैब सक्रिय करें।';

  @override
  String get permissonNeeded => 'अनुमति की आवश्यकता';

  @override
  String get defaultPhoneAppWarning => 'अनुमति की आवश्यकता हैआपको \'Simple Dialer\' को अपने डिफ़ॉल्ट फ़ोन ऐप के रूप में सेट करने की आवश्यकता है।';

  @override
  String get cancel => 'रद्द करें';

  @override
  String get openSettings => 'सेटिंग्स खोलें';

  @override
  String get setAsDefaultPhoneApp => 'डिफ़ॉल्ट फ़ोन ऐप के रूप में सेट करें';

  @override
  String get makeDefault => 'डिफ़ॉल्ट बनाना';

  @override
  String get makeDefaultMessage => 'क्या आप \'साधारण डायलर\' को अपने डिफ़ॉल्ट फ़ोन ऐप के रूप में सेट करना चाहेंगे?';

  @override
  String get neverAsk => 'फिर कभी मत पूछो';

  @override
  String get proximityAccept => 'Auto accept calls';

  @override
  String get proximityAcceptDescription => 'Allow proximity sensor to automatically accept calls when it detects an object(like user\'s ears)';

  @override
  String get proximityLoudspeaker => 'Auto switch loudspeaker';

  @override
  String get proximityLoudspeakerDescription => 'Allow proximity sensor to automatically switch to loudspeaker when there is no object in vicinity(like user\'s ears)';
}
