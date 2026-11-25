import 'usuario_sesion.dart';

/// `data` de `POST /auth/login` para el cliente mobile (tokens en el body).
class LoginResponse {
  final UsuarioSesion usuario;
  final String accessToken;
  final String refreshToken;

  const LoginResponse({
    required this.usuario,
    required this.accessToken,
    required this.refreshToken,
  });

  factory LoginResponse.fromJson(Map<String, dynamic> json) {
    return LoginResponse(
      usuario: UsuarioSesion.fromJson(json['usuario'] as Map<String, dynamic>),
      accessToken: json['accessToken'] as String,
      refreshToken: json['refreshToken'] as String,
    );
  }
}
