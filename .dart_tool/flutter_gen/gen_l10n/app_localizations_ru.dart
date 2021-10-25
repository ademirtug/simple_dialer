


import 'app_localizations.dart';

/// The translations for Russian (`ru`).
class AppLocalizationsRu extends AppLocalizations {
  AppLocalizationsRu([String locale = 'ru']) : super(locale);

  @override
  String get contacts => 'Контакты';

  @override
  String get recents => 'Недавний';

  @override
  String get settings => 'Настройки';

  @override
  String get activateRecents => 'Активируйте вкладку «Недавние».';

  @override
  String get permissonNeeded => 'Требуется разрешение';

  @override
  String get defaultPhoneAppWarning => 'Вам необходимо установить «Простой номеронабиратель» в качестве телефонного приложения по умолчанию.';

  @override
  String get cancel => 'Отмена';

  @override
  String get openSettings => 'Открыть настройки';

  @override
  String get setAsDefaultPhoneApp => 'Установить как приложение \'Телефон по умолчанию\'';

  @override
  String get makeDefault => 'Использовать по умолчанию';

  @override
  String get makeDefaultMessage => 'Хотите установить «Простой номеронабиратель» в качестве телефонного приложения по умолчанию?';

  @override
  String get neverAsk => 'Никогда больше не спрашивай.';

  @override
  String get proximityAccept => 'Автоматический прием звонков';

  @override
  String get proximityAcceptDescription => 'Разрешить датчику приближения автоматически принимать вызовы при обнаружении объекта (например, ушей пользователя).';

  @override
  String get proximityLoudspeaker => 'Автоматически активировать громкоговоритель';

  @override
  String get proximityLoudspeakerDescription => 'Разрешить датчику приближения автоматически переключаться на громкоговоритель, когда поблизости нет никаких предметов (например, ушей пользователя).';

  @override
  String get systemRequirements => 'Системные Требования';

  @override
  String get android6RequirementDescription => 'Для этого варианта требуется Android 6.0 или новее.';

  @override
  String get okay => 'Ладно';

  @override
  String get unexpectedError => 'Неожиданная ошибка';

  @override
  String get details => 'Детали';
}
