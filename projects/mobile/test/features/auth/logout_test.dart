import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/storage/secure_token_storage.dart';
import 'package:mobile/features/auth/providers/auth_provider.dart';
import 'package:mobile/features/auth/services/auth_service.dart';
import 'package:mobile/layouts/admin/admin_shell.dart';
import 'package:mobile/layouts/usuario/usuario_shell.dart';

import '../../support/fake_adapter.dart';
import '../../support/memory_store.dart';

const _usuario = {'codigo': 'c1', 'email': 'a@b.co', 'roles': ['USUARIO']};

FakeAdapter _backend({bool sinRed = false}) => FakeAdapter((options) async {
      if (options.path == '/auth/me') return FakeAdapter.jsonBody(envelopeOk(_usuario), 200);
      if (sinRed) throw DioException(requestOptions: options, type: DioExceptionType.connectionError);
      return FakeAdapter.jsonBody(envelopeOk(null), 200);
    });

Future<(ProviderContainer, SecureTokenStorage)> _conSesion(FakeAdapter adapter) async {
  final tokens = SecureTokenStorage(MemoryStore());
  await tokens.guardar(access: 'acc', refresh: 'ref');
  final dio = ApiClient.create(baseUrl: 'http://api.test/api/v1', tokenStorage: tokens, adapter: adapter);
  final container = ProviderContainer(
    overrides: [authServiceProvider.overrideWithValue(AuthService(dio, tokens))],
  );
  addTearDown(container.dispose);
  await container.read(authProvider.future);
  return (container, tokens);
}

void main() {
  for (final sinRed in [false, true]) {
    test('logout ${sinRed ? 'sin red' : 'con red'} borra los tokens y deja sin sesión', () async {
      final adapter = _backend(sinRed: sinRed);
      final (container, tokens) = await _conSesion(adapter);

      await container.read(authProvider.notifier).logout();

      final request = adapter.requests.singleWhere((r) => r.path == '/auth/logout');
      expect(request.data, {'refreshToken': 'ref'});
      expect(await tokens.leerAccess(), isNull);
      expect(await tokens.leerRefresh(), isNull);
      expect(container.read(authProvider).value, isNull);
    });
  }

  test('cuando la sesión expira, el provider queda sin sesión', () async {
    final (container, _) = await _conSesion(_backend());

    ApiClient.onSesionExpirada!();

    expect(container.read(authProvider).value, isNull);
  });

  for (final shell in <String, Widget>{'AdminShell': const AdminShell(), 'UsuarioShell': const UsuarioShell()}.entries) {
    testWidgets('${shell.key} ofrece "Cerrar sesión" y la ejecuta', (tester) async {
      final tokens = SecureTokenStorage(MemoryStore());
      await tokens.guardar(access: 'acc', refresh: 'ref');
      final adapter = _backend();
      final dio = ApiClient.create(baseUrl: 'http://api.test/api/v1', tokenStorage: tokens, adapter: adapter);

      await tester.pumpWidget(ProviderScope(
        overrides: [authServiceProvider.overrideWithValue(AuthService(dio, tokens))],
        child: MaterialApp(home: shell.value),
      ));
      await tester.runAsync(() async {
        await tester.tap(find.byTooltip('Cerrar sesión'));
        await Future<void>.delayed(const Duration(milliseconds: 100));
      });

      expect(adapter.requests.where((r) => r.path == '/auth/logout'), hasLength(1));
      expect(await tokens.leerRefresh(), isNull);
    });
  }
}
