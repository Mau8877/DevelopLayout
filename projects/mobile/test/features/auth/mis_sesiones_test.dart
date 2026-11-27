import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/routing/app_router.dart';
import 'package:mobile/core/storage/secure_token_storage.dart';
import 'package:mobile/features/auth/models/mi_sesion.dart';
import 'package:mobile/features/auth/models/usuario_sesion.dart';
import 'package:mobile/features/auth/providers/auth_provider.dart';
import 'package:mobile/features/auth/providers/mis_sesiones_provider.dart';
import 'package:mobile/features/auth/screens/mis_sesiones_screen.dart';
import 'package:mobile/features/auth/services/sesiones_service.dart';

import '../../support/fake_adapter.dart';
import '../../support/memory_store.dart';
import 'fake_auth_service.dart';

const _usuario = UsuarioSesion(codigo: 'u1', email: 'a@b.co', roles: ['USUARIO']);

Map<String, dynamic> _sesion(String codigo, {bool actual = false, String tipo = 'WEB'}) => {
      'codigo': codigo,
      'tipoCliente': tipo,
      'userAgent': 'Mozilla/5.0 (Windows NT 10.0) Chrome/140.0 Safari/537.36',
      'creadaEn': '2026-03-10T10:00:00Z',
      'expiraEn': '2026-03-17T10:00:00Z',
      'actual': actual,
    };

Map<String, dynamic> _pagina(List<Map<String, dynamic>> sesiones, {int page = 1, int totalPages = 1, int? total}) => {
      ...envelopeOk(sesiones),
      'meta': {'page': page, 'pageSize': 20, 'totalItems': total ?? sesiones.length, 'totalPages': totalPages},
    };

SesionesService _servicio(FakeAdapter adapter) => SesionesService(ApiClient.create(
      baseUrl: 'http://api.test/api/v1',
      tokenStorage: SecureTokenStorage(MemoryStore()),
      adapter: adapter,
    ));

/// Backend con la sesión actual y otra; al cerrar, la otra desaparece del listado.
FakeAdapter _backend({int estadoDelCierre = 200}) {
  var cerrada = false;
  return FakeAdapter((options) async {
    if (options.method == 'GET') {
      return FakeAdapter.jsonBody(
        _pagina([_sesion('actual', actual: true), if (!cerrada) _sesion('otra', tipo: 'MOBILE')]),
        200,
      );
    }
    cerrada = true;
    return estadoDelCierre == 200
        ? FakeAdapter.jsonBody(envelopeOk(null), 200)
        : FakeAdapter.jsonBody(envelopeError('ERR_AUTH_09', 'La sesión no existe'), estadoDelCierre);
  });
}

/// Los futuros del adaptador falso son reales: se esperan fuera del reloj simulado.
Future<void> _dejarResponder(WidgetTester tester, [Future<void> Function()? accion]) async {
  await tester.runAsync(() async {
    await accion?.call();
    await Future<void>.delayed(const Duration(milliseconds: 150));
  });
  await tester.pump();
  await tester.pump(const Duration(milliseconds: 400));
}

Future<void> _montar(WidgetTester tester, FakeAdapter adapter) async {
  await tester.pumpWidget(ProviderScope(
    overrides: [
      authServiceProvider.overrideWithValue(FakeAuthService(token: true, usuarioDeMe: _usuario)),
      sesionesServiceProvider.overrideWithValue(_servicio(adapter)),
    ],
    child: const MaterialApp(home: MisSesionesScreen()),
  ));
  await _dejarResponder(tester);
  await _dejarResponder(tester);
}

List<RequestOptions> _cambios(FakeAdapter adapter) => adapter.requests.where((r) => r.method != 'GET').toList();

void main() {
  group('MiSesion', () {
    test('fromJson tolera ip, userAgent y último uso ausentes', () {
      final sesion = MiSesion.fromJson({'codigo': 'c', 'tipoCliente': 'WEB', 'creadaEn': '2026-03-10T10:00:00Z'});

      expect(sesion.ip, isNull);
      expect(sesion.ultimoUsoEn, isNull);
      expect(sesion.actual, isFalse);
      expect(sesion.dispositivo, 'Dispositivo desconocido');
    });

    test('el resumen sigue las mismas reglas que la web', () {
      const win = 'Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 Chrome/140.0 Safari/537.36';
      expect(resumirDispositivo(win, 'WEB'), 'Chrome en Windows');
      expect(resumirDispositivo('$win Edg/140.0', 'WEB'), 'Edge en Windows');
      expect(resumirDispositivo('(iPhone; like Mac OS X) Version/18.0 Safari/604.1', 'WEB'), 'Safari en iPhone');
      expect(resumirDispositivo('(Linux; Android 15) Chrome/140.0 Safari/537', 'WEB'), 'Chrome en Android');
      expect(resumirDispositivo('Dart/3.12 (dart:io)', 'MOBILE'), 'App móvil');
      expect(resumirDispositivo(null, 'WEB'), 'Dispositivo desconocido');
    });
  });

  test('SesionesService usa los tres endpoints', () async {
    final adapter = _backend();
    final servicio = _servicio(adapter);

    final pagina = await servicio.listar(2);
    await servicio.cerrar('abc');
    await servicio.cerrarOtras();

    expect(pagina.sesiones, hasLength(2));
    expect(adapter.requests.map((r) => '${r.method} ${r.path}'),
        ['GET /auth/sesiones', 'DELETE /auth/sesiones/abc', 'POST /auth/sesiones/cerrar-otras']);
    expect(adapter.requests.first.queryParameters, {'page': 2, 'pageSize': 20});
  });

  testWidgets('marca la actual y no le ofrece cierre', (tester) async {
    await _montar(tester, _backend());

    expect(find.text('Esta sesión'), findsOneWidget);
    expect(find.text('App móvil'), findsOneWidget);
    expect(find.byTooltip('Cerrar esta otra sesión'), findsOneWidget);
  });

  testWidgets('no cierra nada sin confirmar', (tester) async {
    final adapter = _backend();
    await _montar(tester, adapter);

    await tester.tap(find.byTooltip('Cerrar esta otra sesión'));
    await tester.pumpAndSettle();
    expect(_cambios(adapter), isEmpty);
    await tester.tap(find.text('Cancelar'));
    await tester.pumpAndSettle();

    expect(_cambios(adapter), isEmpty);
  });

  testWidgets('al confirmar cierra esa sesión y vuelve a cargar', (tester) async {
    final adapter = _backend();
    await _montar(tester, adapter);

    await tester.tap(find.byTooltip('Cerrar esta otra sesión'));
    await tester.pumpAndSettle();
    await _dejarResponder(tester, () => tester.tap(find.text('Cerrar')));
    await _dejarResponder(tester);

    expect(_cambios(adapter).single.path, '/auth/sesiones/otra');
    expect(find.text('App móvil'), findsNothing);
  });

  testWidgets('si ya no existe avisa y refresca el listado', (tester) async {
    final adapter = _backend(estadoDelCierre: 404);
    await _montar(tester, adapter);

    await tester.tap(find.byTooltip('Cerrar esta otra sesión'));
    await tester.pumpAndSettle();
    await _dejarResponder(tester, () => tester.tap(find.text('Cerrar')));
    await _dejarResponder(tester);

    expect(find.text('La sesión no existe'), findsOneWidget);
    expect(find.text('App móvil'), findsNothing);
  });

  testWidgets('con una sola sesión no se pueden cerrar las demás', (tester) async {
    await _montar(tester, FakeAdapter.json(_pagina([_sesion('actual', actual: true)])));

    expect(tester.widget<OutlinedButton>(find.byType(OutlinedButton)).onPressed, isNull);
  });

  testWidgets('"Cargar más" agrega la página siguiente sin repetir', (tester) async {
    final adapter = FakeAdapter((options) async {
      final page = options.queryParameters['page'] as int;
      return FakeAdapter.jsonBody(
        _pagina(
          page == 1 ? [_sesion('actual', actual: true)] : [_sesion('actual', actual: true), _sesion('b', tipo: 'MOBILE')],
          page: page,
          totalPages: 2,
          total: 2,
        ),
        200,
      );
    });
    await _montar(tester, adapter);

    await _dejarResponder(tester, () => tester.tap(find.text('Cargar más')));

    expect(find.text('Esta sesión'), findsOneWidget);
    expect(find.text('App móvil'), findsOneWidget);
    expect(find.text('Cargar más'), findsNothing);
  });

  testWidgets('un fallo al cargar ofrece reintentar', (tester) async {
    // * Falla hasta que se pida reintentar (la primera carga puede repetirse al resolver la sesión).
    var sinRed = true;
    final adapter = FakeAdapter((options) async {
      if (sinRed) {
        throw DioException(requestOptions: options, type: DioExceptionType.connectionError);
      }
      return FakeAdapter.jsonBody(_pagina([_sesion('actual', actual: true)]), 200);
    });
    await _montar(tester, adapter);
    expect(find.text('Reintentar'), findsOneWidget);
    expect(find.text('No pudimos conectarnos. Revisá tu conexión e intentá de nuevo.'), findsOneWidget);

    sinRed = false;
    await _dejarResponder(tester, () => tester.tap(find.text('Reintentar')));
    await _dejarResponder(tester);

    expect(find.text('Esta sesión'), findsOneWidget);
  });

  group('la ruta exige sesión', () {
    String? destino(AsyncValue<UsuarioSesion?> auth) => redirectSegunSesion(auth: auth, location: rutaMisSesiones);

    test('sin sesión va al login', () => expect(destino(const AsyncData(null)), rutaLogin));
    test('con sesión se queda', () => expect(destino(const AsyncData(_usuario)), isNull));
  });
}
