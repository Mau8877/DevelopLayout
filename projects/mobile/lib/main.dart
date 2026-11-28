import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/logging/app_logger.dart';
import 'core/routing/app_router.dart';
import 'core/theme/app_theme.dart';
import 'core/marca.dart';
import 'core/storage/secure_token_storage.dart';
import 'shared/language/app_texts.dart';
import 'shared/language/idioma_provider.dart';
import 'shared/language/idiomas.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // * Errores que nadie atrapó: se registran por el logger, sin mostrar detalles al usuario.
  FlutterError.onError = (details) {
    AppLogger.error('Error de Flutter', details.exception, details.stack);
  };
  PlatformDispatcher.instance.onError = (error, stack) {
    AppLogger.error('Error no controlado', error, stack);
    return true;
  };
  if (kReleaseMode) {
    ErrorWidget.builder = (details) => Material(
          child: Center(child: Text(AppTexts.de(IdiomaActual.codigo).t('shared:error.inesperado'))),
        );
  }

  await dotenv.load();
  await AppTexts.precargar();
  final idioma = detectarIdioma(await PreferenciaDeIdioma().leer(), PlatformDispatcher.instance.locales);
  IdiomaActual.codigo = idioma;
  runApp(ProviderScope(
    overrides: [idiomaInicialProvider.overrideWithValue(idioma)],
    child: const MyApp(),
  ));
}

class MyApp extends ConsumerWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return MaterialApp.router(
      title: appName,
      locale: Locale(ref.watch(idiomaProvider)),
      supportedLocales: localesSoportados,
      localizationsDelegates: delegadosDeIdioma,
      theme: AppTheme.light,
      darkTheme: AppTheme.dark,
      routerConfig: ref.watch(routerProvider),
    );
  }
}
