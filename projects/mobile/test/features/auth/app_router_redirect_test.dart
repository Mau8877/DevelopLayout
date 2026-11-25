import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/routing/app_router.dart';
import 'package:mobile/features/auth/models/usuario_sesion.dart';

const _usuario = UsuarioSesion(codigo: 'c1', email: 'a@b.co', roles: ['USUARIO']);

void main() {
  test('cargando la sesión lleva a la pantalla de carga', () {
    expect(redirectSegunSesion(auth: const AsyncLoading(), location: '/'), rutaCargando);
    expect(redirectSegunSesion(auth: const AsyncLoading(), location: rutaCargando), isNull);
  });

  test('sin sesión lleva a /login (y no redirige si ya está ahí)', () {
    expect(redirectSegunSesion(auth: const AsyncData<UsuarioSesion?>(null), location: '/'), rutaLogin);
    expect(redirectSegunSesion(auth: const AsyncData<UsuarioSesion?>(null), location: rutaLogin), isNull);
  });

  test('un error al cargar la sesión se trata como sin sesión', () {
    final error = AsyncError<UsuarioSesion?>(StateError('x'), StackTrace.empty);

    expect(redirectSegunSesion(auth: error, location: '/'), rutaLogin);
  });

  test('con sesión, /login y /cargando llevan a inicio; el resto no redirige', () {
    const auth = AsyncData<UsuarioSesion?>(_usuario);

    expect(redirectSegunSesion(auth: auth, location: rutaLogin), rutaInicio);
    expect(redirectSegunSesion(auth: auth, location: rutaCargando), rutaInicio);
    expect(redirectSegunSesion(auth: auth, location: rutaInicio), isNull);
  });
}
