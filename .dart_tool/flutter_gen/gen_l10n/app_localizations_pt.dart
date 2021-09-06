


import 'app_localizations.dart';

/// The translations for Portuguese (`pt`).
class AppLocalizationsPt extends AppLocalizations {
  AppLocalizationsPt([String locale = 'pt']) : super(locale);

  @override
  String get contacts => 'Contactos';

  @override
  String get recents => 'Histórico';

  @override
  String get settings => 'Definições';

  @override
  String get activateRecents => 'Ative a guia \'Histórico\'';
}

/// The translations for Portuguese, as used in Brazil (`pt_BR`).
class AppLocalizationsPtBr extends AppLocalizationsPt {
  AppLocalizationsPtBr(): super('pt_BR');

  @override
  String get contacts => 'Contactos';

  @override
  String get recents => 'Histórico';

  @override
  String get settings => 'Definições';

  @override
  String get activateRecents => 'Ative a guia \'Histórico\'';
}
