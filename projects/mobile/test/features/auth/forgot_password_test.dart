import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/network/error_message.dart';
import 'package:mobile/core/routing/app_router.dart';
import 'package:mobile/core/storage/secure_token_storage.dart';
import 'package:mobile/features/auth/models/usuario_sesion.dart';
import 'package:mobile/features/auth/providers/auth_provider.dart';
import 'package:mobile/features/auth/screens/forgot_password_screen.dart';
import 'package:mobile/features/auth/screens/login_screen.dart';
import 'package:mobile/features/auth/services/auth_service.dart';

import '../../support/app_de_prueba.dart';
import '../../support/fake_adapter.dart';
import '../../support/memory_store.dart';

AuthService _servicio(FakeAdapter adapter) {
  final tokens = SecureTokenStorage(MemoryStore());
  final dio = ApiClient.create(baseUrl: 'http://api.test/api/v1', tokenStorage: tokens, adapter: adapter);
  return AuthService(dio, tokens);
}

FakeAdapter _ok() => FakeAdapter((_) async => FakeAdapter.jsonBody(envelopeOk(null), 200));

FakeAdapter _limitado() => FakeAdapter((_) async => FakeAdapter.jsonBody(
      {'status': 'failed', 'data': null, 'message': 'Demasiadas solicitudes', 'error': 'ERR_SYS_02'},
      429,
    ));

Future<void> _montar(WidgetTester tester, FakeAdapter adapter) async {
  await tester.pumpWidget(ProviderScope(
    overrides: [authServiceProvider.overrideWithValue(_servicio(adapter))],
    child: const AppDePrueba(home: ForgotPasswordScreen()),
  ));
}

Future<void> _enviar(WidgetTester tester, String correo) async {
  await tester.enterText(find.byType(TextFormField), correo);
  // * El adaptador falso usa futuros reales: se esperan fuera del reloj simulado.
  await tester.runAsync(() async {
    await tester.tap(find.text('Enviar enlace'));
    await Future<void>.delayed(const Duration(milliseconds: 100));
  });
  await tester.pump();
}

void main() {
  group('AuthService.solicitarRecuperacion', () {
    test('manda solo el correo al endpoint de recuperación', () async {
      final adapter = _ok();

      await _servicio(adapter).solicitarRecuperacion('a@b.co');

      final request = adapter.requests.single;
      expect(request.path, '/auth/password/forgot');
      expect(request.method, 'POST');
      expect(request.data, {'email': 'a@b.co'});
    });

    test('con 429 propaga el mensaje del backend', () async {
      await expectLater(
        _servicio(_limitado()).solicitarRecuperacion('a@b.co'),
        throwsA(predicate<Object>((e) => mensajeDeError(e) == 'Demasiadas solicitudes')),
      );
    });
  });

  group('ForgotPasswordScreen', () {
    testWidgets('con éxito muestra un mensaje que no afirma que la cuenta exista', (tester) async {
      final adapter = _ok();
      await _montar(tester, adapter);

      await _enviar(tester, ' a@b.co ');

      expect(find.textContaining('Si hay una cuenta con ese correo'), findsOneWidget);
      expect(find.text('Enviar enlace'), findsNothing);
      expect(adapter.requests.single.data, {'email': 'a@b.co'});
    });

    testWidgets('con 429 avisa y deja reintentar', (tester) async {
      await _montar(tester, _limitado());

      await _enviar(tester, 'a@b.co');

      expect(find.text('Demasiadas solicitudes'), findsOneWidget);
      expect(find.text('Enviar enlace'), findsOneWidget);
    });

    testWidgets('no llama a la API con un correo inválido', (tester) async {
      final adapter = _ok();
      await _montar(tester, adapter);

      await _enviar(tester, 'no-es-un-correo');

      expect(find.text('El correo no es válido'), findsOneWidget);
      expect(adapter.requests, isEmpty);
    });
  });

  group('redirectSegunSesion con /forgot-password', () {
    const usuario = UsuarioSesion(codigo: 'c', email: 'a@b.co', roles: ['USUARIO']);

    String? destino(AsyncValue<UsuarioSesion?> auth) =>
        redirectSegunSesion(auth: auth, location: rutaOlvidePassword);

    test('sin sesión se queda', () => expect(destino(const AsyncData(null)), isNull));
    test('con sesión va a inicio', () => expect(destino(const AsyncData(usuario)), rutaInicio));
    test('mientras carga va a la pantalla de carga', () => expect(destino(const AsyncLoading()), rutaCargando));
  });

  testWidgets('el login enlaza a la recuperación y se puede volver', (tester) async {
    await tester.pumpWidget(ProviderScope(
      overrides: [authServiceProvider.overrideWithValue(_servicio(_ok()))],
      child: Consumer(
        builder: (context, ref, _) => AppDePrueba(router: ref.watch(routerProvider)),
      ),
    ));
    await tester.runAsync(() async => Future<void>.delayed(const Duration(milliseconds: 100)));
    await tester.pumpAndSettle();
    expect(find.byType(LoginScreen), findsOneWidget);

    await tester.tap(find.text('¿Olvidaste tu contraseña?'));
    await tester.pumpAndSettle();
    expect(find.byType(ForgotPasswordScreen), findsOneWidget);

    await tester.tap(find.text('Volver a iniciar sesión'));
    await tester.pumpAndSettle();
    expect(find.byType(LoginScreen), findsOneWidget);
  });
}
