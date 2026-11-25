import 'package:dio/dio.dart';

import '../../../core/storage/secure_token_storage.dart';
import '../models/login_response.dart';
import '../models/usuario_sesion.dart';

/// Llamadas de la feature de autenticación, vía el `Dio` de `ApiClient`.
class AuthService {
  final Dio _dio;
  final SecureTokenStorage _tokens;

  AuthService(this._dio, this._tokens);

  /// `POST /auth/login`. Al éxito guarda access y refresh en almacenamiento seguro.
  Future<UsuarioSesion> login(String email, String password) async {
    final response = await _dio.post<Map<String, dynamic>>(
      '/auth/login',
      data: {'email': email, 'password': password},
    );
    final login = LoginResponse.fromJson(response.data!);
    await _tokens.guardar(access: login.accessToken, refresh: login.refreshToken);
    return login.usuario;
  }

  /// `GET /auth/me`: el usuario de la sesión actual según el backend.
  Future<UsuarioSesion> me() async {
    final response = await _dio.get<Map<String, dynamic>>('/auth/me');
    return UsuarioSesion.fromJson(response.data!);
  }

  Future<bool> hayToken() async {
    final token = await _tokens.leerAccess();
    return token != null && token.isNotEmpty;
  }

  Future<void> descartarTokens() => _tokens.borrar();
}
