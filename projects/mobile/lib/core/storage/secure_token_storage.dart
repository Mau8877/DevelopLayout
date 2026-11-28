import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Almacén clave-valor mínimo; existe para poder reemplazarlo por uno falso en tests.
abstract interface class KeyValueStore {
  Future<void> write(String key, String value);
  Future<String?> read(String key);
  Future<void> delete(String key);
}

/// Implementación real sobre el almacenamiento seguro del sistema
/// (Keychain en iOS, Keystore en Android).
class PlatformKeyValueStore implements KeyValueStore {
  final FlutterSecureStorage _storage;

  const PlatformKeyValueStore([this._storage = const FlutterSecureStorage()]);

  @override
  Future<void> write(String key, String value) => _storage.write(key: key, value: value);

  @override
  Future<String?> read(String key) => _storage.read(key: key);

  @override
  Future<void> delete(String key) => _storage.delete(key: key);
}

/// La elección de idioma. No es sensible y no comparte claves con los tokens:
/// cerrar la sesión no la borra.
class PreferenciaDeIdioma {
  static const _clave = 'preferencia.idioma';

  final KeyValueStore _store;

  PreferenciaDeIdioma([KeyValueStore? store]) : _store = store ?? const PlatformKeyValueStore();

  /// Si el almacenamiento falla, la app arranca igual con el idioma del teléfono.
  Future<String?> leer() async {
    try {
      return await _store.read(_clave);
    } catch (_) {
      return null;
    }
  }

  Future<void> guardar(String codigo) async {
    try {
      await _store.write(_clave, codigo);
    } catch (_) {
      // * La elección dura lo que dure la app abierta.
    }
  }
}

/// Guarda los tokens de sesión (access y refresh) del cliente mobile.
///
/// * Solo almacenamiento seguro del sistema: nunca `shared_preferences`.
/// ! Sin logs: ningún valor de token pasa por el logger.
class SecureTokenStorage {
  static const _accessKey = 'auth.access_token';
  static const _refreshKey = 'auth.refresh_token';

  final KeyValueStore _store;

  SecureTokenStorage([KeyValueStore? store]) : _store = store ?? const PlatformKeyValueStore();

  Future<void> guardar({required String access, required String refresh}) async {
    await _store.write(_accessKey, access);
    await _store.write(_refreshKey, refresh);
  }

  Future<String?> leerAccess() => _store.read(_accessKey);

  Future<String?> leerRefresh() => _store.read(_refreshKey);

  Future<void> borrar() async {
    await _store.delete(_accessKey);
    await _store.delete(_refreshKey);
  }
}
