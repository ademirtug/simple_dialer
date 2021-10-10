


import 'app_localizations.dart';

/// The translations for Japanese (`ja`).
class AppLocalizationsJa extends AppLocalizations {
  AppLocalizationsJa([String locale = 'ja']) : super(locale);

  @override
  String get contacts => '連絡先';

  @override
  String get recents => '通話履歴';

  @override
  String get settings => '設定';

  @override
  String get activateRecents => '通話履歴」タブを有効にする';

  @override
  String get permissonNeeded => '必要な許可 ';

  @override
  String get defaultPhoneAppWarning => 'デフォルトの電話アプリとして「Simple Dialer」を設定する必要があります。';

  @override
  String get cancel => 'キャンセル';

  @override
  String get openSettings => '設定を開く';

  @override
  String get setAsDefaultPhoneApp => 'デフォルトの電話アプリとして設定';

  @override
  String get makeDefault => 'デフォルトにする';

  @override
  String get makeDefaultMessage => 'デフォルトの電話アプリとして「シンプルダイヤラ」を設定しますか';

  @override
  String get neverAsk => '二度と聞かない';

  @override
  String get proximityAccept => 'Auto accept calls';

  @override
  String get proximityAcceptDescription => 'Allow proximity sensor to automatically accept calls when it detects an object(like user\'s ears)';

  @override
  String get proximityLoudspeaker => 'Auto switch loudspeaker';

  @override
  String get proximityLoudspeakerDescription => 'Allow proximity sensor to automatically switch to loudspeaker when there is no object in vicinity(like user\'s ears)';
}
