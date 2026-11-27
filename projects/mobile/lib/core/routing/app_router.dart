import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/models/usuario_sesion.dart';
import '../../features/auth/providers/auth_provider.dart';
import '../../features/auth/screens/forgot_password_screen.dart';
import '../../features/auth/screens/login_screen.dart';
import '../../features/auth/screens/mis_sesiones_screen.dart';
import '../../layouts/admin/admin_shell.dart';
import '../../layouts/usuario/usuario_shell.dart';

const rutaLogin = '/login';
const rutaCargando = '/cargando';
const rutaInicio = '/';
const rutaOlvidePassword = '/forgot-password';
const rutaMisSesiones = '/mis-sesiones';

/// Rutas a las que se entra sin sesión. Con sesión, llevan a inicio.
const _rutasSinSesion = {rutaLogin, rutaOlvidePassword};

/// Regla única de redirección según la sesión (función pura, testeable).
///
/// * Cargando la sesión => pantalla de carga. Sin sesión (o error) => login,
/// * salvo en las rutas sin sesión. Con sesión en una de esas o en carga => inicio.
String? redirectSegunSesion({
  required AsyncValue<UsuarioSesion?> auth,
  required String location,
}) {
  final cargando = auth.isLoading && !auth.hasValue && !auth.hasError;
  if (cargando) return location == rutaCargando ? null : rutaCargando;

  final usuario = auth.value;
  if (usuario == null) return _rutasSinSesion.contains(location) ? null : rutaLogin;

  if (_rutasSinSesion.contains(location) || location == rutaCargando) return rutaInicio;
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
      GoRoute(path: rutaOlvidePassword, builder: (context, state) => const ForgotPasswordScreen()),
      GoRoute(
        path: rutaCargando,
        builder: (context, state) => const Scaffold(body: Center(child: CircularProgressIndicator())),
      ),
      GoRoute(path: rutaInicio, builder: (context, state) => const _InicioPorRol()),
      GoRoute(path: rutaMisSesiones, builder: (context, state) => const MisSesionesScreen()),
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
