import 'package:mobile/features/auth/models/usuario_sesion.dart';
import 'package:mobile/features/auth/services/auth_service.dart';

import '../../support/memory_store.dart';
import 'package:dio/dio.dart';
import 'package:mobile/core/storage/secure_token_storage.dart';

/// AuthService falso para tests de provider y router.
class FakeAuthService extends AuthService {
  bool token;
  UsuarioSesion? usuarioDeMe;
  Object? errorEnMe;
  Object? errorEnLogin;
  bool descartados = false;

  FakeAuthService({this.token = false, this.usuarioDeMe, this.errorEnMe, this.errorEnLogin})
      : super(Dio(), SecureTokenStorage(MemoryStore()));

  @override
  Future<bool> hayToken() async => token;

  @override
  Future<UsuarioSesion> me() async {
    if (errorEnMe != null) throw errorEnMe!;
    return usuarioDeMe!;
  }

  @override
  Future<UsuarioSesion> login(String email, String password) async {
    if (errorEnLogin != null) throw errorEnLogin!;
    return usuarioDeMe!;
  }

  @override
  Future<void> descartarTokens() async => descartados = true;
}
