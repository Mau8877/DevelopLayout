import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/features/auth/models/login_response.dart';
import 'package:mobile/features/auth/models/usuario_sesion.dart';

void main() {
  test('UsuarioSesion.fromJson parsea codigo, email y roles', () {
    final u = UsuarioSesion.fromJson({'codigo': 'c1', 'email': 'a@b.co', 'roles': ['USUARIO']});

    expect(u.codigo, 'c1');
    expect(u.email, 'a@b.co');
    expect(u.tieneRol('USUARIO'), isTrue);
    expect(u.tieneRol('SUPER_ADMIN'), isFalse);
  });

  test('LoginResponse.fromJson parsea usuario y tokens', () {
    final r = LoginResponse.fromJson({
      'usuario': {'codigo': 'c1', 'email': 'a@b.co', 'roles': <String>[]},
      'accessToken': 'acc',
      'refreshToken': 'ref',
    });

    expect(r.usuario.email, 'a@b.co');
    expect(r.accessToken, 'acc');
    expect(r.refreshToken, 'ref');
  });
}
