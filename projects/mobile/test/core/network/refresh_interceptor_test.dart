import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/network/error_message.dart';
import 'package:mobile/core/storage/secure_token_storage.dart';

import '../../support/fake_adapter.dart';
import '../../support/memory_store.dart';

/// Backend falso: las rutas de negocio solo aceptan el access token vigente.
class _Backend {
  _Backend({this.refreshStatus = 200, this.sinRed = false, this.rechazaSiempre = false, this.duranteElRefresh});

  final int refreshStatus;
  final bool sinRed;
  final bool rechazaSiempre;
  final Future<void> Function()? duranteElRefresh;

  late final adapter = FakeAdapter((options) async {
    if (options.path == '/auth/refresh') {
      await Future<void>.delayed(const Duration(milliseconds: 5));
      await duranteElRefresh?.call();
      if (sinRed) throw DioException(requestOptions: options, type: DioExceptionType.connectionError);
      if (refreshStatus != 200) {
        return FakeAdapter.jsonBody(envelopeError('ERR_AUTH_04', 'x'), refreshStatus);
      }
      return FakeAdapter.jsonBody(envelopeOk({'accessToken': 'acc2', 'refreshToken': 'ref2'}), 200);
    }
    if (options.path == '/auth/logout') return FakeAdapter.jsonBody(envelopeOk(null), 200);
    if (rechazaSiempre || options.headers['Authorization'] != 'Bearer acc2') {
      return FakeAdapter.jsonBody(envelopeError('ERR_AUTH_07', 'No autenticado'), 401);
    }
    return FakeAdapter.jsonBody(envelopeOk({'ruta': options.path}), 200);
  });

  int veces(String ruta) => adapter.requests.where((r) => r.path == ruta).length;
}

void main() {
  late SecureTokenStorage tokens;
  late int expiradas;

  setUp(() async {
    tokens = SecureTokenStorage(MemoryStore());
    await tokens.guardar(access: 'acc1', refresh: 'ref1');
    expiradas = 0;
  });

  Dio cliente(_Backend backend) => ApiClient.create(
        baseUrl: 'http://api.test/api/v1',
        tokenStorage: tokens,
        adapter: backend.adapter,
        onSesionExpirada: () => expiradas++,
      );

  Future<String?> codigoDe(Future<Object?> llamada) async {
    try {
      await llamada;
      return null;
    } on DioException catch (e) {
      return apiExceptionDe(e)?.code;
    }
  }

  test('tres 401 en paralelo hacen un solo refresh, rotan los tokens y reintentan', () async {
    final backend = _Backend();
    final dio = cliente(backend);

    final respuestas = await Future.wait(['/a', '/b', '/c'].map(dio.get<dynamic>));

    expect(respuestas.map((r) => r.data), [
      {'ruta': '/a'},
      {'ruta': '/b'},
      {'ruta': '/c'},
    ]);
    expect(backend.veces('/auth/refresh'), 1);
    expect(await tokens.leerAccess(), 'acc2');
    expect(await tokens.leerRefresh(), 'ref2');
    expect(expiradas, 0);
  });

  test('reintenta una sola vez aunque vuelva a dar 401', () async {
    final backend = _Backend(rechazaSiempre: true);

    expect(await codigoDe(cliente(backend).get<dynamic>('/a')), 'ERR_AUTH_07');

    expect(backend.veces('/a'), 2);
    expect(backend.veces('/auth/refresh'), 1);
  });

  for (final status in [401, 403]) {
    test('un $status del refresh borra los tokens y avisa una sola vez', () async {
      final backend = _Backend(refreshStatus: status);
      final dio = cliente(backend);

      final codigos = await Future.wait([
        codigoDe(dio.get<dynamic>('/a')),
        codigoDe(dio.get<dynamic>('/b')),
      ]);

      expect(codigos, ['ERR_AUTH_07', 'ERR_AUTH_07']);
      expect(await tokens.leerAccess(), isNull);
      expect(await tokens.leerRefresh(), isNull);
      expect(expiradas, 1);
    });
  }

  for (final caso in {
    'un 429': _Backend(refreshStatus: 429),
    'un 500': _Backend(refreshStatus: 500),
    'la falta de red': _Backend(sinRed: true),
  }.entries) {
    test('${caso.key} en el refresh conserva la sesión', () async {
      expect(await codigoDe(cliente(caso.value).get<dynamic>('/a')), isNot('ERR_AUTH_07'));

      expect(await tokens.leerAccess(), 'acc1');
      expect(await tokens.leerRefresh(), 'ref1');
      expect(expiradas, 0);
    });
  }

  test('si se cierra la sesión durante el refresh, no la revive y la cierra en el servidor', () async {
    final backend = _Backend(duranteElRefresh: () => tokens.borrar());

    expect(await codigoDe(cliente(backend).get<dynamic>('/a')), 'ERR_AUTH_07');

    expect(await tokens.leerRefresh(), isNull);
    expect(backend.veces('/auth/logout'), 1);
  });

  test('un refresh con cuerpo inesperado falla sin colgar la request ni cerrar la sesión', () async {
    final adapter = FakeAdapter((options) async => options.path == '/auth/refresh'
        ? FakeAdapter.jsonBody(envelopeOk({}), 200)
        : FakeAdapter.jsonBody(envelopeError('ERR_AUTH_07', 'No autenticado'), 401));
    final dio = ApiClient.create(baseUrl: 'http://api.test/api/v1', tokenStorage: tokens, adapter: adapter);

    final codigo = await codigoDe(dio.get<dynamic>('/a').timeout(const Duration(seconds: 2)));

    expect(codigo, 'ERR_AUTH_07');
    expect(await tokens.leerRefresh(), 'ref1');
  });

  test('un 401 del login no intenta renovar', () async {
    final backend = _Backend();

    await codigoDe(cliente(backend).post<dynamic>('/auth/login'));

    expect(backend.veces('/auth/refresh'), 0);
  });
}
