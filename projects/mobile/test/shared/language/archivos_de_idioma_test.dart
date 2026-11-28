import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/shared/language/app_texts.dart';
import 'package:mobile/shared/language/idiomas.dart';

// * Controles de IDIOMAS_MOBILE.md. Las pruebas corren en la raíz del paquete.

String _ruta(FileSystemEntity entidad) => entidad.path.replaceAll(r'\', '/');

final _archivos = Directory('lib').listSync(recursive: true).whereType<File>().toList();

final _carpetas = Directory('lib')
    .listSync(recursive: true)
    .whereType<Directory>()
    .map(_ruta)
    .where((ruta) => ruta.endsWith('/language') && File('$ruta/es.json').existsSync())
    .toList();

Map<String, String> _planos(Map<String, dynamic> textos, [String prefijo = '']) => {
      for (final MapEntry(:key, :value) in textos.entries)
        if (value is Map<String, dynamic>) ..._planos(value, '$prefijo$key.') else '$prefijo$key': value as String,
    };

Map<String, String> _leer(String carpeta, String idioma) =>
    _planos(jsonDecode(File('$carpeta/$idioma.json').readAsStringSync()) as Map<String, dynamic>);

Set<String> _marcadores(String texto) => RegExp(r'\{\{(\w+)\}\}').allMatches(texto).map((m) => m[1]!).toSet();

const _permitidas = {
  'más', 'además', 'atrás', 'demás', 'estás', 'después', 'través', 'inglés', 'interés', 'país',
  'está', 'así', 'ahí', 'aquí', 'allí', 'sí', 'qué', 'porqué', 'allá', 'quizá', 'mí', 'sé',
};
const _siempreMal = {
  'vos', 'sos', 'acá', 'mirá', 'esperá', 'cerrá', 'entrá', 'borrá', 'abrila', 'abrilo', 'cerralo', 'cerrala',
  'pedile', 'usted', 'ordenador', 'vale', 'ahorita',
};

List<String> _conVoseo(String texto) => RegExp(r'[a-záéíóúñü]+')
    .allMatches(texto.toLowerCase())
    .map((m) => m[0]!)
    .where((palabra) {
      if (_siempreMal.contains(palabra)) return true;
      if (_permitidas.contains(palabra)) return false;
      // * «tendrás» y «perderá» son futuro, no voseo.
      if (RegExp(r'r(á|ás|é)$').hasMatch(palabra)) return false;
      return RegExp(r'(ás|és|ís|á|é|í)$').hasMatch(palabra);
    })
    .toList();

/// Quita comentarios, logs y errores de programación: no se traducen.
String _visible(String codigo) => codigo
    .replaceAll(RegExp(r'//[^\n]*(?=\n)'), '')
    .replaceAll(RegExp(r'''(AppLogger\.\w+|StateError|assert)\([^;]*;'''), '');

/// Textos escritos a mano en una propiedad o un widget visible.
List<String> _fijos(String codigo) {
  final visibles = RegExp(
    r'''(?:\bText\(\s*|\b(?:labelText|hintText|helperText|tooltip|semanticLabel|semanticsLabel|title|label|titulo|mensaje|texto|confirmar|cancelar|message):\s*)(?:'([^'\n]*)'|"([^"\n]*)")''',
  );
  return visibles
      .allMatches(codigo)
      .map((m) => m[1] ?? m[2]!)
      // * Una clave de idioma no es texto fijo.
      .where((texto) => !RegExp('^(${carpetasDeTextos.keys.join('|')}):' r'[\w.]+$').hasMatch(texto))
      // * Con solo datos interpolados no hay nada que traducir.
      .where((texto) => RegExp(r'[A-Za-zÁÉÍÓÚáéíóúñÑ]').hasMatch(texto.replaceAll(RegExp(r'\$\{[^}]*\}|\$\w+'), '')))
      .toList();
}

Iterable<String> _enEspanol(String codigo) => RegExp(r'''['"]([^'"\n]*[áéíóúñ¿¡ÁÉÍÓÚÑ][^'"\n]*)['"]''')
    .allMatches(codigo)
    .map((m) => m[1]!);

void main() {
  final codigo = {
    for (final archivo in _archivos.where((a) => a.path.endsWith('.dart')))
      _ruta(archivo): _visible(archivo.readAsStringSync()),
  };

  test('cada carpeta language está declarada en pubspec.yaml y en app_texts.dart', () {
    final pubspec = File('pubspec.yaml').readAsStringSync();

    expect(_carpetas, hasLength(carpetasDeTextos.length));
    expect(carpetasDeTextos.values.toSet(), _carpetas.toSet());
    for (final carpeta in _carpetas) {
      for (final idioma in idiomas) {
        expect(pubspec, contains('- $carpeta/${idioma.codigo}.json'));
      }
    }
  });

  test('mismas claves y mismos marcadores en todos los idiomas, sin valores vacíos', () {
    for (final carpeta in _carpetas) {
      final es = _leer(carpeta, 'es');
      for (final idioma in idiomas) {
        final otro = _leer(carpeta, idioma.codigo);
        expect(otro.keys.toSet(), es.keys.toSet(), reason: '$carpeta/${idioma.codigo}');
        for (final clave in es.keys) {
          expect(otro[clave]!.trim(), isNotEmpty, reason: '$carpeta $clave');
          expect(_marcadores(otro[clave]!), _marcadores(es[clave]!), reason: '$carpeta $clave');
        }
      }
    }
  });

  test('cada clave usada en el código existe', () {
    final patron = RegExp('[\'"]((?:${carpetasDeTextos.keys.join('|')}):[\\w.]+)[\'"]');
    final usadas = [
      for (final MapEntry(key: ruta, value: texto) in codigo.entries)
        for (final m in patron.allMatches(texto)) (ruta, m[1]!),
    ];

    expect(usadas.length, greaterThan(30));
    for (final idioma in idiomas) {
      final textos = AppTexts.de(idioma.codigo);
      expect(usadas.where((u) => !textos.existe(u.$2) && !textos.existe('${u.$2}_other')), isEmpty);
    }
  });

  test('no hay texto fijo en el código', () {
    final faltas = [
      for (final MapEntry(key: ruta, value: texto) in codigo.entries) ...[
        for (final fijo in _fijos(texto)) '$ruta: $fijo',
        // * El nombre de un idioma va escrito en su propio idioma.
        for (final fijo in _enEspanol(texto).where((t) => t != 'Español')) '$ruta: $fijo',
      ],
    ];

    expect(faltas, isEmpty);
  });

  test('el español es neutro: sin voseo ni regionalismos', () {
    final faltas = [
      for (final carpeta in _carpetas)
        for (final MapEntry(:key, :value) in _leer(carpeta, 'es').entries)
          for (final palabra in _conVoseo(value)) '$carpeta $key: $palabra',
    ];

    expect(faltas, isEmpty);
  });

  test('los controles detectan lo que deben', () {
    expect(_conVoseo('Iniciá sesión y elegí: podés'), ['iniciá', 'elegí', 'podés']);
    expect(_conVoseo('Inicia sesión, tendrás más después'), isEmpty);
    expect(_fijos("Text('Hola'), tooltip: 'Salir', labelText: \"Correo\""), ['Hola', 'Salir', 'Correo']);
    expect(_fijos(r"Text('$total'), title: appName"), isEmpty);
    expect(_enEspanol("return 'Contraseña';"), ['Contraseña']);
  });
}
