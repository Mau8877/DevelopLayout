import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/models/usuario_sesion.dart';
import '../../features/auth/providers/auth_provider.dart';
import '../../features/auth/screens/login_screen.dart';
import '../../layouts/admin/admin_shell.dart';
import '../../layouts/usuario/usuario_shell.dart';

const rutaLogin = '/login';
const rutaCargando = '/cargando';
const rutaInicio = '/';

/// Regla única de redirección según la sesión (función pura, testeable).
///
/// * Cargando la sesión => pantalla de carga. Sin sesión (o error) => login.
/// * Con sesión en login/carga => inicio.
String? redirectSegunSesion({
  required AsyncValue<UsuarioSesion?> auth,
  required String location,
}) {
  final cargando = auth.isLoading && !auth.hasValue && !auth.hasError;
  if (cargando) return location == rutaCargando ? null : rutaCargando;

  final usuario = auth.value;
  if (usuario == null) return location == rutaLogin ? null : rutaLogin;

  if (location == rutaLogin || location == rutaCargando) return rutaInicio;
  return null;
}

/// GoRouter único de la app. El router depende del provider de sesión, no al revés.
final routerProvider = Provider<GoRouter>((ref) {
  final refresh = ValueNotifier<int>(0);
  ref.listen(authProvider, (previous, next) => refresh.value++);
  ref.onDispose(refresh.dispose);

  final router = GoRouter(
    initialLocation: rutaInicio,
    refreshListenable: refresh,
    redirect: (context, state) =>
        redirectSegunSesion(auth: ref.read(authProvider), location: state.matchedLocation),
    routes: [
      GoRoute(path: rutaLogin, builder: (context, state) => const LoginScreen()),
      GoRoute(
        path: rutaCargando,
        builder: (context, state) => const Scaffold(body: Center(child: CircularProgressIndicator())),
      ),
      GoRoute(path: rutaInicio, builder: (context, state) => const _InicioPorRol()),
    ],
  );
  ref.onDispose(router.dispose);
  return router;
});

/// Pantalla inicial mínima según el rol, usando los shells existentes.
class _InicioPorRol extends ConsumerWidget {
  const _InicioPorRol();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final usuario = ref.watch(authProvider).value;
    if (usuario != null && usuario.tieneRol('SUPER_ADMIN')) {
      return const AdminShell();
    }
    return const UsuarioShell();
  }
}
