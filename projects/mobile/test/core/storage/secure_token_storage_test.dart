import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/storage/secure_token_storage.dart';

import '../../support/memory_store.dart';

void main() {
  test('guarda, lee y borra los tokens', () async {
    final storage = SecureTokenStorage(MemoryStore());

    expect(await storage.leerAccess(), isNull);

    await storage.guardar(access: 'a', refresh: 'r');
    expect(await storage.leerAccess(), 'a');
    expect(await storage.leerRefresh(), 'r');

    await storage.borrar();
    expect(await storage.leerAccess(), isNull);
    expect(await storage.leerRefresh(), isNull);
  });
}
