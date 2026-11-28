import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/storage/secure_token_storage.dart';
import 'package:mobile/features/auth/models/mi_sesion.dart';
import 'package:mobile/features/auth/providers/auth_provider.dart';
import 'package:mobile/features/auth/providers/mis_sesiones_provider.dart';
import 'package:mobile/features/auth/screens/forgot_password_screen.dart';
import 'package:mobile/features/auth/screens/login_screen.dart';
import 'package:mobile/features/auth/services/sesiones_service.dart';
import 'package:mobile/features/auth/widgets/sesion_tile.dart';
import 'package:mobile/features/health/providers/health_provider.dart';
import 'package:mobile/features/health/screens/health_check_screen.dart';
import 'package:mobile/layouts/admin/admin_shell.dart';
import 'package:mobile/layouts/usuario/usuario_shell.dart';
import 'package:mobile/shared/language/idioma_provider.dart';
import 'package:mobile/shared/language/idiomas.dart';

import '../../features/auth/fake_auth_service.dart';
import '../../support/app_de_prueba.dart';
import '../../support/fake_adapter.dart';
import '../../support/memory_store.dart';

final _almacen = MemoryStore();

Widget _app(Widget pantalla, {String idioma = 'es'}) => ProviderScope(
      overrides: [
        idiomaInicialProvider.overrideWithValue(idioma),
        preferenciaDeIdiomaProvider.overrideWithValue(PreferenciaDeIdioma(_almacen)),
        healthCheckProvider.overrideWith((ref) async => {'status': 'UP'}),
      ],
      child: AppDePrueba(home: pantalla),
    );

void main() {
  tearDown(() {
    _almacen.data.clear();
    IdiomaActual.codigo = idiomaPorDefecto;
  });

  testWidgets('el selector cambia el idioma sin reiniciar y recuerda la elección', (tester) async {
    await tester.pumpWidget(_app(const LoginScreen()));
    expect(find.text('Contraseña'), findsOneWidget);

    await tester.tap(find.byTooltip('Cambiar idioma'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('English'));
    await tester.pumpAndSettle();

    expect(find.text('Password'), findsOneWidget);
    expect(find.text('Forgot your password?'), findsOneWidget);
    expect(find.byTooltip('Change language'), findsOneWidget);
    expect(IdiomaActual.codigo, 'en');
    expect(_almacen.data.values, ['en']);
  });

  testWidgets('login en inglés, con sus validaciones', (tester) async {
    await tester.pumpWidget(_app(const LoginScreen(), idioma: 'en'));

    await tester.tap(find.widgetWithText(FilledButton, 'Sign in'));
    await tester.pump();

    expect(find.text('Email is required'), findsOneWidget);
    expect(find.text('Password is required'), findsOneWidget);
  });

  testWidgets('recuperar contraseña en inglés y con selector', (tester) async {
    await tester.pumpWidget(_app(const ForgotPasswordScreen(), idioma: 'en'));

    expect(find.text('Recover password'), findsOneWidget);
    expect(find.text('Send link'), findsOneWidget);
    expect(find.byTooltip('Change language'), findsOneWidget);
  });

  testWidgets('una sesión muestra textos y fecha según el idioma', (tester) async {
    final sesion = MiSesion(
      codigo: 'c',
      tipoCliente: 'MOBILE',
      creadaEn: DateTime(2026, 3, 25, 14, 5),
      actual: true,
    );

    await tester.pumpWidget(_app(Scaffold(body: SesionTile(sesion: sesion, onCerrar: null)), idioma: 'en'));
    expect(find.text('Mobile app'), findsOneWidget);
    expect(find.text('This session'), findsOneWidget);
    expect(find.textContaining('Started: 3/25/2026 14:05'), findsOneWidget);
    expect(find.textContaining('Unknown IP'), findsOneWidget);
  });

  testWidgets('la misma sesión en español usa el día primero', (tester) async {
    final sesion = MiSesion(codigo: 'c', tipoCliente: 'MOBILE', creadaEn: DateTime(2026, 3, 25, 14, 5), actual: false);

    await tester.pumpWidget(_app(Scaffold(body: SesionTile(sesion: sesion, onCerrar: () {}))));

    expect(find.textContaining('Iniciada: 25/3/2026 14:05'), findsOneWidget);
    expect(find.byTooltip('Cerrar esta otra sesión'), findsOneWidget);
  });

  testWidgets('diagnóstico en inglés', (tester) async {
    await tester.pumpWidget(_app(const HealthCheckScreen(), idioma: 'en'));
    await tester.pumpAndSettle();

    expect(find.text('Connection check'), findsOneWidget);
    expect(find.text('Server connected: UP'), findsOneWidget);
  });

  for (final shell in <String, Widget>{'AdminShell': const AdminShell(), 'UsuarioShell': const UsuarioShell()}.entries) {
    testWidgets('${shell.key} en inglés, con selector', (tester) async {
      await tester.pumpWidget(_app(shell.value, idioma: 'en'));

      expect(find.byTooltip('My sessions'), findsOneWidget);
      expect(find.byTooltip('Sign out'), findsOneWidget);
      expect(find.byTooltip('Change language'), findsOneWidget);
      expect(find.textContaining('Sample screen'), findsOneWidget);
    });
  }

  test('al cambiar de idioma, Mis sesiones se vuelve a pedir', () async {
    final adapter = FakeAdapter.json(envelopeOk([])..['meta'] = {'page': 1, 'totalPages': 0, 'totalItems': 0});
    final dio = ApiClient.create(
      baseUrl: 'http://api.test/api/v1',
      tokenStorage: SecureTokenStorage(MemoryStore()),
      adapter: adapter,
    );
    final container = ProviderContainer(
      overrides: [
        preferenciaDeIdiomaProvider.overrideWithValue(PreferenciaDeIdioma(_almacen)),
        authServiceProvider.overrideWithValue(FakeAuthService(token: false)),
        sesionesServiceProvider.overrideWithValue(SesionesService(dio)),
      ],
    );
    addTearDown(container.dispose);
    await container.read(authProvider.future);
    container.listen(misSesionesProvider, (_, _) {});
    await container.read(misSesionesProvider.future);

    await container.read(idiomaProvider.notifier).cambiar('en');
    await container.read(misSesionesProvider.future);

    expect(adapter.requests.map((r) => r.headers['Accept-Language']), ['es', 'en']);
  });
}
