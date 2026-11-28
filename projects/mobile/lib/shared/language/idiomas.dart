import 'dart:ui';

/// Un idioma soportado. El nombre va escrito en su propio idioma.
class Idioma {
  final String codigo;
  final String nombre;

  const Idioma(this.codigo, this.nombre);
}

const idiomas = [Idioma('es', 'Español'), Idioma('en', 'English')];

const idiomaPorDefecto = 'es';

bool esIdioma(String? codigo) => idiomas.any((idioma) => idioma.codigo == codigo);

/// La preferencia guardada; si no, el primer idioma soportado del teléfono; si no, español.
String detectarIdioma(String? guardado, List<Locale> delSistema) {
  if (guardado != null && esIdioma(guardado)) return guardado;
  for (final locale in delSistema) {
    if (esIdioma(locale.languageCode)) return locale.languageCode;
  }
  return idiomaPorDefecto;
}

/// Idioma vigente para lo que no tiene `BuildContext` (el cliente HTTP).
///
/// * Solo lo escribe `idiomaProvider`.
abstract final class IdiomaActual {
  static String codigo = idiomaPorDefecto;
}
