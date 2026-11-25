import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/auth/models/usuario_sesion.dart';
import 'package:mobile/features/auth/providers/auth_provider.dart';

import 'fake_auth_service.dart';

const _usuario = UsuarioSesion(codigo: 'c1', email: 'a@b.co', roles: ['USUARIO']);

ProviderContainer _container(FakeAuthService service) {
  final container = ProviderContainer(overrides: [authServiceProvider.overrideWithValue(service)]);
  addTearDown(container.dispose);
  return container;
}

void main() {
  test('arranque sin token: sin sesión y sin llamar a /me', () async {
    final container = _container(FakeAuthService(token: false, errorEnMe: StateError('no debe llamarse')));

    expect(await container.read(authProvider.future), isNull);
  });

  test('arranque con token válido: carga el usuario desde /me', () async {
    final container = _container(FakeAuthService(token: true, usuarioDeMe: _usuario));

    expect(await container.read(authProvider.future), _usuario);
  });

  test('arranque con token rechazado (401): sin sesión y descarta los tokens', () async {
    final service = FakeAuthService(token: true, errorEnMe: ApiException(code: 'ERR_AUTH_07', message: 'x'));
    final container = _container(service);

    expect(await container.read(authProvider.future), isNull);
    expect(service.descartados, isTrue);
  });

  test('arranque con fallo de red: sin sesión pero conserva los tokens', () async {
    final service = FakeAuthService(token: true, errorEnMe: ApiException(code: 'CLIENT_NETWORK', message: 'x'));
    final container = _container(service);

    expect(await container.read(authProvider.future), isNull);
    expect(service.descartados, isFalse);
  });

  test('login exitoso actualiza la sesión', () async {
    final container = _container(FakeAuthService(token: false, usuarioDeMe: _usuario));
    await container.read(authProvider.future);

    await container.read(authProvider.notifier).login('a@b.co', 'ClaveSegura123');

    expect(container.read(authProvider).value, _usuario);
  });

  test('login fallido relanza el error y no cambia la sesión', () async {
    final container = _container(FakeAuthService(
      token: false,
      errorEnLogin: ApiException(code: 'ERR_AUTH_01', message: 'Correo o contraseña incorrectos'),
    ));
    await container.read(authProvider.future);

    await expectLater(
      container.read(authProvider.notifier).login('a@b.co', 'mala-clave-1'),
      throwsA(isA<ApiException>()),
    );
    expect(container.read(authProvider).value, isNull);
  });
}
