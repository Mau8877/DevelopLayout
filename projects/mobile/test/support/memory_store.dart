import 'package:mobile/core/storage/secure_token_storage.dart';

/// Almacén en memoria para tests (reemplaza al almacenamiento seguro real).
class MemoryStore implements KeyValueStore {
  final Map<String, String> data = {};

  @override
  Future<void> write(String key, String value) async => data[key] = value;

  @override
  Future<String?> read(String key) async => data[key];

  @override
  Future<void> delete(String key) async => data.remove(key);
}
