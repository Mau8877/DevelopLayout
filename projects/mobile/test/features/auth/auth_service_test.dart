import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/network/error_message.dart';
import 'package:mobile/core/storage/secure_token_storage.dart';
import 'package:mobile/features/auth/services/auth_service.dart';

import '../../support/fake_adapter.dart';
import '../../support/memory_store.dart';

const _usuarioJson = {'codigo': 'c1', 'email': 'a@b.co', 'roles': ['USUARIO']};

AuthService _service(FakeAdapter adapter, SecureTokenStorage tokens) {
  final dio = ApiClient.create(baseUrl: 'http://api.test/api/v1', tokenStorage: tokens, adapter: adapter);
  return AuthService(dio, tokens);
}

void main() {
  test('login exitoso devuelve el usuario y guarda los tokens', () async {
    final tokens = SecureTokenStorage(MemoryStore());
    final adapter = FakeAdapter.json(envelopeOk({
      'usuario': _usuarioJson,
      'accessToken': 'acc',
      'refreshToken': 'ref',
    }));

    final usuario = await _service(adapter, tokens).login('a@b.co', 'ClaveSegura123');

    expect(usuario.email, 'a@b.co');
    expect(await tokens.leerAccess(), 'acc');
    expect(await tokens.leerRefresh(), 'ref');
  });

  test('login fallido propaga ApiException y no guarda tokens', () async {
    final tokens = SecureTokenStorage(MemoryStore());
    final adapter = FakeAdapter.json(envelopeError('ERR_AUTH_01', 'Correo o contraseña incorrectos'), status: 401);

    await expectLater(
      _service(adapter, tokens).login('a@b.co', 'mala-clave-1'),
      throwsA(isA<DioException>().having((e) => apiExceptionDe(e)?.code, 'code', 'ERR_AUTH_01')),
    );
    expect(await tokens.leerAccess(), isNull);
  });

  test('me devuelve el usuario de la sesión', () async {
    final usuario = await _service(FakeAdapter.json(envelopeOk(_usuarioJson)), SecureTokenStorage(MemoryStore())).me();

    expect(usuario.codigo, 'c1');
  });
}
