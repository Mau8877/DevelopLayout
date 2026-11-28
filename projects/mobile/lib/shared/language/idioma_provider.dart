import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/storage/secure_token_storage.dart';
import 'idiomas.dart';

final preferenciaDeIdiomaProvider = Provider<PreferenciaDeIdioma>((ref) => PreferenciaDeIdioma());

/// Idioma con el que arranca la app; `main` lo reemplaza con el detectado.
final idiomaInicialProvider = Provider<String>((ref) => idiomaPorDefecto);

/// Única vía para leer y cambiar el idioma.
final idiomaProvider = NotifierProvider<IdiomaNotifier, String>(IdiomaNotifier.new);

class IdiomaNotifier extends Notifier<String> {
  @override
  String build() => IdiomaActual.codigo = ref.watch(idiomaInicialProvider);

  Future<void> cambiar(String codigo) async {
    if (!esIdioma(codigo) || codigo == state) return;
    // * Primero el cliente HTTP: lo que se vuelva a pedir ya sale en el idioma nuevo.
    IdiomaActual.codigo = codigo;
    state = codigo;
    await ref.read(preferenciaDeIdiomaProvider).guardar(codigo);
  }
}
