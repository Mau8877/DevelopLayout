import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/network/error_message.dart';
import 'package:mobile/core/routing/app_router.dart';
import 'package:mobile/core/storage/secure_token_storage.dart';
import 'package:mobile/features/auth/models/usuario_sesion.dart';
import 'package:mobile/features/auth/providers/auth_provider.dart';
import 'package:mobile/features/auth/screens/cambiar_password_screen.dart';
import 'package:mobile/features/auth/screens/login_screen.dart';
import 'package:mobile/features/auth/services/auth_service.dart';
import 'package:mobile/shared/language/idioma_provider.dart';
import 'package:mobile/shared/language/idiomas.dart';

import '../../support/app_de_prueba.dart';
import '../../support/fake_adapter.dart';
import '../../support/memory_store.dart';

const _actual = 'ClaveActual123';
const _nueva = 'ClaveNueva456';

/// Backend falso. `rechazo` es lo que responde el cambio; `vencido` hace que el
/// primer intento dé 401 hasta que se renueve.
FakeAdapter _backend({(String, String, int)? rechazo, bool sinRed = false, bool vencido = false, String rol = 'USUARIO'}) {
  var renovada = false;
  return FakeAdapter((options) async {
    if (options.path == '/auth/me') {
      return FakeAdapter.jsonBody(envelopeOk({'codigo': 'c1', 'email': 'a@b.co', 'roles': [rol]}), 200);
    }
    if (options.path == '/auth/refresh') {
      renovada = true;
      return FakeAdapter.jsonBody(envelopeOk({'accessToken': 'acc2', 'refreshToken': 'ref2'}), 200);
    }
    if (sinRed) throw DioException(requestOptions: options, type: DioExceptionType.connectionError);
    if (vencido && !renovada) {
      return FakeAdapter.jsonBody(envelopeError('ERR_AUTH_07', 'No autenticado'), 401);
    }
    if (rechazo != null) {
      return FakeAdapter.jsonBody(envelopeError(rechazo.$1, rechazo.$2), rechazo.$3);
    }
    return FakeAdapter.jsonBody(envelopeOk(null), 200);
  });
}

Future<(AuthService, SecureTokenStorage)> _servicioConSesion(FakeAdapter adapter) async {
  final tokens = SecureTokenStorage(MemoryStore());
  await tokens.guardar(access: 'acc', refresh: 'ref');
  final dio = ApiClient.create(baseUrl: 'http://api.test/api/v1', tokenStorage: tokens, adapter: adapter);
  return (AuthService(dio, tokens), tokens);
}

Future<(ProviderContainer, SecureTokenStorage)> _contenedor(FakeAdapter adapter) async {
  final (servicio, tokens) = await _servicioConSesion(adapter);
  final container = ProviderContainer(overrides: [authServiceProvider.overrideWithValue(servicio)]);
  addTearDown(container.dispose);
  await container.read(authProvider.future);
  return (container, tokens);
}

Iterable<RequestOptions> _cambios(FakeAdapter adapter) =>
    adapter.requests.where((r) => r.path == '/auth/password/change');

Future<void> _dejarResponder(WidgetTester tester, [Future<void> Function()? accion]) async {
  await tester.runAsync(() async {
    await accion?.call();
    await Future<void>.delayed(const Duration(milliseconds: 150));
  });
  await tester.pumpAndSettle();
}

/// La app con el router real, ya con sesión y en la pantalla de cambio.
Future<SecureTokenStorage> _abrirPantalla(
  WidgetTester tester,
  FakeAdapter adapter, {
  String idioma = 'es',
}) async {
  final (servicio, tokens) = await _servicioConSesion(adapter);
  await tester.pumpWidget(ProviderScope(
    overrides: [
      authServiceProvider.overrideWithValue(servicio),
      idiomaInicialProvider.overrideWithValue(idioma),
    ],
    child: Consumer(builder: (context, ref, _) => AppDePrueba(router: ref.watch(routerProvider))),
  ));
  await _dejarResponder(tester);
  await tester.tap(find.byTooltip(idioma == 'es' ? 'Cambiar contraseña' : 'Change password'));
  await tester.pumpAndSettle();
  return tokens;
}

Future<void> _completar(WidgetTester tester, {String confirmacion = _nueva, String boton = 'Cambiar contraseña'}) async {
  final campos = find.byType(TextFormField);
  await tester.enterText(campos.at(0), _actual);
  await tester.enterText(campos.at(1), _nueva);
  await tester.enterText(campos.at(2), confirmacion);
  await _dejarResponder(tester, () => tester.tap(find.widgetWithText(FilledButton, boton)));
}

void main() {
  tearDown(() => IdiomaActual.codigo = idiomaPorDefecto);

  group('AuthNotifier.cambiarPassword', () {
    test('al tener éxito envía solo las dos contraseñas, borra los tokens y deja sin sesión', () async {
      final adapter = _backend();
      final (container, tokens) = await _contenedor(adapter);

      await container.read(authProvider.notifier).cambiarPassword(_actual, _nueva);

      expect(_cambios(adapter).single.data, {'passwordActual': _actual, 'passwordNueva': _nueva});
      expect(await tokens.leerAccess(), isNull);
      expect(await tokens.leerRefresh(), isNull);
      expect(container.read(authProvider).value, isNull);
      expect(adapter.requests.where((r) => r.path == '/auth/logout'), isEmpty);
    });

    for (final caso in <String, FakeAdapter Function()>{
      '400': () => _backend(rechazo: ('ERR_AUTH_12', 'La contraseña actual no es correcta', 400)),
      '423': () => _backend(rechazo: ('ERR_AUTH_13', 'Demasiados intentos', 423)),
      '429': () => _backend(rechazo: ('ERR_SYS_02', 'Demasiadas solicitudes', 429)),
      'sin red': () => _backend(sinRed: true),
    }.entries) {
      test('con ${caso.key} conserva los tokens y la sesión', () async {
        final (container, tokens) = await _contenedor(caso.value());

        await expectLater(
          container.read(authProvider.notifier).cambiarPassword(_actual, _nueva),
          throwsA(predicate<Object>((e) => apiExceptionDe(e) != null)),
        );

        expect(await tokens.leerRefresh(), 'ref');
        expect(container.read(authProvider).value, isA<UsuarioSesion>());
      });
    }

    test('con el access token vencido renueva una vez y completa el cambio', () async {
      final adapter = _backend(vencido: true);
      final (container, tokens) = await _contenedor(adapter);

      await container.read(authProvider.notifier).cambiarPassword(_actual, _nueva);

      expect(adapter.requests.where((r) => r.path == '/auth/refresh'), hasLength(1));
      expect(_cambios(adapter), hasLength(2));
      expect(await tokens.leerRefresh(), isNull);
    });
  });

  group('validaciones', () {
    test('la actual es obligatoria y se mide en bytes', () {
      expect(validarPasswordActual(''), 'auth:validacion.passwordActualObligatoria');
      expect(validarPasswordActual('ñ' * 37), 'auth:validacion.passwordLarga');
      expect(validarPasswordActual('ñ' * 36), isNull);
    });

    test('la nueva usa la política del login, también en bytes', () {
      expect(validarPassword('corta'), 'auth:validacion.passwordCorta');
      expect(validarPassword('ñ' * 37), 'auth:validacion.passwordLarga');
      expect(validarPassword('ñ' * 36), isNull);
    });

    test('la confirmación debe coincidir', () {
      expect(validarConfirmacion('', _nueva), 'auth:validacion.repetirPassword');
      expect(validarConfirmacion('otra', _nueva), 'auth:validacion.passwordsDistintas');
      expect(validarConfirmacion(_nueva, _nueva), isNull);
    });
  });

  group('CambiarPasswordScreen', () {
    testWidgets('al cambiarla termina en el login con el aviso', (tester) async {
      final adapter = _backend();
      final tokens = await _abrirPantalla(tester, adapter);

      await _completar(tester);

      expect(find.byType(LoginScreen), findsOneWidget);
      expect(find.text('Contraseña cambiada. Inicia sesión con la nueva.'), findsOneWidget);
      expect(await tokens.leerRefresh(), isNull);
    });

    testWidgets('un error del servidor se muestra y la persona sigue en la pantalla', (tester) async {
      final tokens = await _abrirPantalla(
        tester,
        _backend(rechazo: ('ERR_AUTH_12', 'La contraseña actual no es correcta', 400)),
      );

      await _completar(tester);

      expect(find.byType(CambiarPasswordScreen), findsOneWidget);
      expect(find.text('La contraseña actual no es correcta'), findsOneWidget);
      expect(await tokens.leerRefresh(), 'ref');
    });

    testWidgets('no envía si las contraseñas no coinciden', (tester) async {
      final adapter = _backend();
      await _abrirPantalla(tester, adapter);

      await _completar(tester, confirmacion: 'ClaveNueva457');

      expect(find.text('Las contraseñas no coinciden'), findsOneWidget);
      expect(_cambios(adapter), isEmpty);
    });

    testWidgets('mientras envía no se puede enviar otra vez', (tester) async {
      final adapter = FakeAdapter((options) async {
        if (options.path == '/auth/me') {
          return FakeAdapter.jsonBody(envelopeOk({'codigo': 'c1', 'email': 'a@b.co', 'roles': ['USUARIO']}), 200);
        }
        await Future<void>.delayed(const Duration(days: 1));
        return FakeAdapter.jsonBody(envelopeOk(null), 200);
      });
      await _abrirPantalla(tester, adapter);
      final campos = find.byType(TextFormField);
      await tester.enterText(campos.at(0), _actual);
      await tester.enterText(campos.at(1), _nueva);
      await tester.enterText(campos.at(2), _nueva);

      await tester.runAsync(() async {
        await tester.tap(find.byType(FilledButton));
        await Future<void>.delayed(const Duration(milliseconds: 100));
      });
      await tester.pump();

      expect(tester.widget<FilledButton>(find.byType(FilledButton)).onPressed, isNull);
      expect(_cambios(adapter), hasLength(1));
    });

    testWidgets('los tres campos ocultan el texto y no sugieren palabras', (tester) async {
      await _abrirPantalla(tester, _backend());

      final campos = tester.widgetList<TextField>(find.byType(TextField)).toList();

      expect(campos, hasLength(3));
      for (final campo in campos) {
        expect(campo.obscureText, isTrue);
        expect(campo.enableSuggestions, isFalse);
        expect(campo.autocorrect, isFalse);
      }
    });

    testWidgets('en inglés, desde el shell de administrador', (tester) async {
      await _abrirPantalla(tester, _backend(rol: 'SUPER_ADMIN'), idioma: 'en');

      expect(find.text('Current password'), findsOneWidget);
      expect(find.text('Repeat the new password'), findsOneWidget);

      await _completar(tester, boton: 'Change password');

      expect(find.text('Password changed. Sign in with the new one.'), findsOneWidget);
    });
  });

  group('la ruta exige sesión', () {
    String? destino(AsyncValue<UsuarioSesion?> auth) =>
        redirectSegunSesion(auth: auth, location: rutaCambiarPassword);

    test('sin sesión va al login', () => expect(destino(const AsyncData(null)), rutaLogin));
    test('con sesión se queda', () {
      expect(destino(const AsyncData(UsuarioSesion(codigo: 'c', email: 'a@b.co', roles: []))), isNull);
    });
  });
}
