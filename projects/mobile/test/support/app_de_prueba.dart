import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mobile/shared/language/app_texts.dart';
import 'package:mobile/shared/language/idioma_provider.dart';

/// `MaterialApp` con los textos de la app, en el idioma de `idiomaProvider`
/// (español salvo que la prueba lo reemplace). Va dentro de un `ProviderScope`.
class AppDePrueba extends ConsumerWidget {
  final Widget? home;
  final RouterConfig<Object>? router;

  const AppDePrueba({super.key, this.home, this.router});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final locale = Locale(ref.watch(idiomaProvider));
    if (router != null) {
      return MaterialApp.router(
        locale: locale,
        supportedLocales: localesSoportados,
        localizationsDelegates: delegadosDeIdioma,
        routerConfig: router,
      );
    }
    return MaterialApp(
      locale: locale,
      supportedLocales: localesSoportados,
      localizationsDelegates: delegadosDeIdioma,
      home: home,
    );
  }
}
