import 'dart:ui';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/storage/secure_token_storage.dart';
import 'package:mobile/shared/language/app_texts.dart';
import 'package:mobile/shared/language/idioma_provider.dart';
import 'package:mobile/shared/language/idiomas.dart';

import '../../support/memory_store.dart';

class _AlmacenRoto implements KeyValueStore {
  @override
  Future<void> write(String key, String value) async => throw StateError('sin almacenamiento');

  @override
  Future<String?> read(String key) async => throw StateError('sin almacenamiento');

  @override
  Future<void> delete(String key) async {}
}

void main() {
  tearDown(() => IdiomaActual.codigo = idiomaPorDefecto);

  group('detectarIdioma', () {
    test('prefiere la elección guardada', () {
      expect(detectarIdioma('es', const [Locale('en', 'US')]), 'es');
    });

    test('sin preferencia usa el primer idioma soportado del teléfono', () {
      expect(detectarIdioma(null, const [Locale('fr'), Locale('en', 'GB')]), 'en');
    });

    test('con un idioma no soportado o una preferencia inválida queda en español', () {
      expect(detectarIdioma(null, const [Locale('pt', 'BR')]), 'es');
      expect(detectarIdioma('xx', const []), 'es');
    });
  });

  group('AppTexts', () {
    test('usa el plural y reemplaza los marcadores', () {
      final en = AppTexts.de('en');

      expect(en.t('auth:sesiones.iniciada', args: {'fecha': 'hoy'}), 'Started: hoy');
      expect(
        AppTexts.de('es').t('shared:layout.ejemplo', args: {'seccion': 'Inicio'}),
        contains('«Inicio»'),
      );
    });

    test('una clave inexistente falla en debug y en las pruebas', () {
      expect(() => AppTexts.de('es').t('auth:no.existe'), throwsAssertionError);
    });

    test('traducir traduce una clave y deja igual el mensaje del servidor', () {
      final en = AppTexts.de('en');

      expect(en.traducir('shared:comun.reintentar'), 'Retry');
      expect(en.traducir('Demasiadas solicitudes'), 'Demasiadas solicitudes');
    });
  });

  group('idiomaProvider', () {
    test('cambiar guarda la elección y la usa el cliente HTTP', () async {
      final almacen = MemoryStore();
      final container = ProviderContainer(
        overrides: [preferenciaDeIdiomaProvider.overrideWithValue(PreferenciaDeIdioma(almacen))],
      );
      addTearDown(container.dispose);
      expect(container.read(idiomaProvider), 'es');

      await container.read(idiomaProvider.notifier).cambiar('en');

      expect(container.read(idiomaProvider), 'en');
      expect(IdiomaActual.codigo, 'en');
      expect(await PreferenciaDeIdioma(almacen).leer(), 'en');
    });

    test('ignora un idioma que no está soportado', () async {
      final container = ProviderContainer(
        overrides: [preferenciaDeIdiomaProvider.overrideWithValue(PreferenciaDeIdioma(MemoryStore()))],
      );
      addTearDown(container.dispose);

      await container.read(idiomaProvider.notifier).cambiar('xx');

      expect(container.read(idiomaProvider), 'es');
    });

    test('borrar los tokens no borra la preferencia', () async {
      final almacen = MemoryStore();
      final tokens = SecureTokenStorage(almacen);
      await tokens.guardar(access: 'a', refresh: 'r');
      await PreferenciaDeIdioma(almacen).guardar('en');

      await tokens.borrar();

      expect(almacen.data.values, ['en']);
    });

    test('con el almacenamiento roto no falla', () async {
      final preferencia = PreferenciaDeIdioma(_AlmacenRoto());

      expect(await preferencia.leer(), isNull);
      await expectLater(preferencia.guardar('en'), completes);
    });
  });
}
