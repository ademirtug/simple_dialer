


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

  @override
  String get permissonNeeded => 'Permissão Necessária';

  @override
  String get defaultPhoneAppWarning => 'Você precisa definir \'Simple Dialer\' como seu aplicativo de Telefone padrão.';

  @override
  String get cancel => 'Cancelar';

  @override
  String get openSettings => 'Abrir Configurações';

  @override
  String get setAsDefaultPhoneApp => 'Definir como aplicativo de Telefone padrão';

  @override
  String get makeDefault => 'Tornar Padrão';

  @override
  String get makeDefaultMessage => 'Gostaria de definir \'Discador simples\' como seu aplicativo de Telefone padrão?';

  @override
  String get neverAsk => 'Nunca pergunte de novo.';
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

  @override
  String get permissonNeeded => 'Permissão Necessária';

  @override
  String get defaultPhoneAppWarning => 'Você precisa definir \'Simple Dialer\' como seu aplicativo de Telefone padrão.';

  @override
  String get cancel => 'Cancelar';

  @override
  String get openSettings => 'Abrir Configurações';

  @override
  String get setAsDefaultPhoneApp => 'Definir como aplicativo de Telefone padrão';

  @override
  String get makeDefault => 'Tornar Padrão';

  @override
  String get makeDefaultMessage => 'Gostaria de definir \'Discador simples\' como seu aplicativo de Telefone padrão?';

  @override
  String get neverAsk => 'Nunca pergunte de novo.';
}
