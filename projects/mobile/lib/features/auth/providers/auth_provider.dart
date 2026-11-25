import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/error_message.dart';
import '../../../core/storage/secure_token_storage.dart';
import '../models/usuario_sesion.dart';
import '../services/auth_service.dart';

final secureTokenStorageProvider = Provider<SecureTokenStorage>((ref) => SecureTokenStorage());

final authServiceProvider = Provider<AuthService>((ref) {
  return AuthService(ApiClient.instance, ref.watch(secureTokenStorageProvider));
});

/// Sesión actual: `null` = sin sesión. La decide siempre el backend (`/auth/me`).
final authProvider = AsyncNotifierProvider<AuthNotifier, UsuarioSesion?>(AuthNotifier.new);

class AuthNotifier extends AsyncNotifier<UsuarioSesion?> {
  @override
  Future<UsuarioSesion?> build() async {
    final service = ref.read(authServiceProvider);
    if (!await service.hayToken()) return null;
    try {
      return await service.me();
    } catch (error) {
      // * Solo un 401 invalida los tokens guardados; un fallo de red no cierra la sesión.
      if (apiExceptionDe(error)?.code == 'ERR_AUTH_07') {
        await service.descartarTokens();
      }
      return null;
    }
  }

  /// Inicia sesión. Si falla, relanza el error (la pantalla lo muestra) y el
  /// estado de sesión no cambia.
  Future<void> login(String email, String password) async {
    final usuario = await ref.read(authServiceProvider).login(email, password);
    state = AsyncData(usuario);
  }
}
