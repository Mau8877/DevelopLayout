import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/network/error_message.dart';
import 'package:mobile/core/storage/secure_token_storage.dart';

import '../../support/fake_adapter.dart';
import '../../support/memory_store.dart';

Dio _client(FakeAdapter adapter, SecureTokenStorage tokens) =>
    ApiClient.create(baseUrl: 'http://api.test/api/v1', tokenStorage: tokens, adapter: adapter);

void main() {
  test('envía X-Client-Type mobile y, sin token, no manda Authorization', () async {
    final adapter = FakeAdapter.json(envelopeOk({'a': 1}));
    final dio = _client(adapter, SecureTokenStorage(MemoryStore()));

    final response = await dio.get<Map<String, dynamic>>('/algo');

    expect(adapter.requests.single.headers['X-Client-Type'], 'mobile');
    expect(adapter.requests.single.headers.containsKey('Authorization'), isFalse);
    expect(response.data, {'a': 1});
  });

  test('con token guardado envía Authorization Bearer', () async {
    final tokens = SecureTokenStorage(MemoryStore());
    await tokens.guardar(access: 'tok', refresh: 'ref');
    final adapter = FakeAdapter.json(envelopeOk({}));

    await _client(adapter, tokens).get<dynamic>('/algo');

    expect(adapter.requests.single.headers['Authorization'], 'Bearer tok');
  });

  test('un error con sobre llega como ApiException con el código del backend', () async {
    final adapter = FakeAdapter.json(envelopeError('ERR_AUTH_01', 'Correo o contraseña incorrectos'), status: 401);
    final dio = _client(adapter, SecureTokenStorage(MemoryStore()));

    try {
      await dio.post<dynamic>('/auth/login');
      fail('debía fallar');
    } on DioException catch (e) {
      final api = apiExceptionDe(e)!;
      expect(api.code, 'ERR_AUTH_01');
      expect(mensajeDeError(e), 'Correo o contraseña incorrectos');
    }
  });

  test('sin conexión es CLIENT_NETWORK', () async {
    final dio = _client(FakeAdapter.fails(DioExceptionType.connectionError), SecureTokenStorage(MemoryStore()));

    try {
      await dio.get<dynamic>('/algo');
      fail('debía fallar');
    } on DioException catch (e) {
      expect(apiExceptionDe(e)!.code, 'CLIENT_NETWORK');
    }
  });

  test('un timeout es CLIENT_TIMEOUT', () async {
    final dio = _client(FakeAdapter.fails(DioExceptionType.receiveTimeout), SecureTokenStorage(MemoryStore()));

    try {
      await dio.get<dynamic>('/algo');
      fail('debía fallar');
    } on DioException catch (e) {
      expect(apiExceptionDe(e)!.code, 'CLIENT_TIMEOUT');
    }
  });

  test('una respuesta sin sobre (502) es CLIENT_UNKNOWN', () async {
    final adapter = FakeAdapter((_) async => ResponseBody.fromString('<html>Bad Gateway</html>', 502));
    final dio = _client(adapter, SecureTokenStorage(MemoryStore()));

    try {
      await dio.get<dynamic>('/algo');
      fail('debía fallar');
    } on DioException catch (e) {
      expect(apiExceptionDe(e)!.code, 'CLIENT_UNKNOWN');
    }
  });

  group('mensajeDeError', () {
    test('usa el mensaje de una ApiException', () {
      expect(mensajeDeError(ApiException(code: 'X', message: 'Hola')), 'Hola');
    });

    test('para cualquier otro error devuelve un texto genérico, nunca toString()', () {
      final texto = mensajeDeError(StateError('detalle interno secreto'));

      expect(texto, isNot(contains('secreto')));
      expect(texto, isNotEmpty);
    });
  });
}
