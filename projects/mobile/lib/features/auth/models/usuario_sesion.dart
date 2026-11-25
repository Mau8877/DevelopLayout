/// Usuario de la sesión actual (respuesta de `GET /auth/me` y de login).
class UsuarioSesion {
  final String codigo;
  final String email;
  final List<String> roles;

  const UsuarioSesion({required this.codigo, required this.email, required this.roles});

  factory UsuarioSesion.fromJson(Map<String, dynamic> json) {
    return UsuarioSesion(
      codigo: json['codigo'] as String,
      email: json['email'] as String,
      roles: (json['roles'] as List<dynamic>? ?? const []).map((r) => r as String).toList(),
    );
  }

  bool tieneRol(String rol) => roles.contains(rol);
}
