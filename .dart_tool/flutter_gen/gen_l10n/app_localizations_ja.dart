


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
  String get proximityAccept => '通話を自動的に受け入れる';

  @override
  String get proximityAcceptDescription => '近接センサーがオブジェクト（ユーザーの耳など）を検出すると、呼び出しを自動的に受け入れるようにします。';

  @override
  String get proximityLoudspeaker => 'スピーカーの自動起動';

  @override
  String get proximityLoudspeakerDescription => '近くに物体がない場合（ユーザーの耳など）、近接センサーが自動的にスピーカーに切り替わるようにします';

  @override
  String get systemRequirements => 'システム要求';

  @override
  String get android6RequirementDescription => 'このオプションには、Android6.0以降のバージョンが必要です。';

  @override
  String get okay => 'わかった';
}
