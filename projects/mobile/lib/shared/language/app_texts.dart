import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

import 'idiomas.dart';

/// Carpetas con textos: el nombre es el espacio de nombres de sus claves.
///
/// * `shared/` no importa código de ninguna feature: solo conoce estas rutas.
/// * Cada archivo se declara también en `pubspec.yaml`.
const carpetasDeTextos = {
  'shared': 'lib/shared/language',
  'auth': 'lib/features/auth/language',
  'health': 'lib/features/health/language',
};

final _esClave = RegExp('^(${carpetasDeTextos.keys.join('|')}):\\w+(\\.\\w+)*\$');

/// Los textos de un idioma. Las claves se escriben `espacio:ruta.de.la.clave`.
class AppTexts {
  AppTexts._(this.idioma, this._textos);

  final String idioma;
  final Map<String, String> _textos;

  static final Map<String, AppTexts> _cargados = {};

  /// Carga todos los idiomas una vez, antes del primer fotograma.
  static Future<void> precargar([AssetBundle? bundle]) async {
    for (final idioma in idiomas) {
      final textos = <String, String>{};
      for (final MapEntry(key: espacio, value: carpeta) in carpetasDeTextos.entries) {
        final json = await (bundle ?? rootBundle).loadString('$carpeta/${idioma.codigo}.json');
        _aplanar(jsonDecode(json) as Map<String, dynamic>, '$espacio:', textos);
      }
      _cargados[idioma.codigo] = AppTexts._(idioma.codigo, textos);
    }
  }

  static AppTexts de(String idioma) {
    final textos = _cargados[idioma] ?? _cargados[idiomaPorDefecto];
    if (textos == null) throw StateError('Falta llamar a AppTexts.precargar()');
    return textos;
  }

  static void _aplanar(Map<String, dynamic> origen, String prefijo, Map<String, String> destino) {
    origen.forEach((clave, valor) {
      if (valor is Map<String, dynamic>) {
        _aplanar(valor, '$prefijo$clave.', destino);
      } else {
        destino['$prefijo$clave'] = valor as String;
      }
    });
  }

  bool existe(String clave) => _textos.containsKey(clave);

  /// Traduce `clave`. Con `count` usa la variante `_one` u `_other`; los datos
  /// van en `args` y reemplazan los marcadores `{{nombre}}`.
  ///
  /// * Una clave inexistente falla en debug y en las pruebas; en release se
  /// * muestra la clave, nunca una pantalla rota.
  String t(String clave, {int? count, Map<String, Object> args = const {}}) {
    final buscada = count == null ? clave : '${clave}_${count == 1 ? 'one' : 'other'}';
    var texto = _textos[buscada];
    if (texto == null) {
      assert(false, 'Falta la clave de idioma "$buscada"');
      return buscada;
    }
    final datos = {...args, 'count': ?count};
    datos.forEach((nombre, valor) => texto = texto!.replaceAll('{{$nombre}}', '$valor'));
    return texto!;
  }

  /// Para un mensaje que puede ser una clave (validaciones y errores propios de
  /// la app) o un texto ya listo (los del servidor), que se deja igual.
  String traducir(String mensaje) => _esClave.hasMatch(mensaje) && existe(mensaje) ? t(mensaje) : mensaje;
}

class _AppTextsDelegate extends LocalizationsDelegate<AppTexts> {
  const _AppTextsDelegate();

  @override
  bool isSupported(Locale locale) => esIdioma(locale.languageCode);

  @override
  Future<AppTexts> load(Locale locale) => SynchronousFuture(AppTexts.de(locale.languageCode));

  @override
  bool shouldReload(_AppTextsDelegate old) => false;
}

/// Para `MaterialApp`: textos propios y los de los widgets de Material.
const delegadosDeIdioma = <LocalizationsDelegate<dynamic>>[
  _AppTextsDelegate(),
  GlobalMaterialLocalizations.delegate,
  GlobalCupertinoLocalizations.delegate,
  GlobalWidgetsLocalizations.delegate,
];

final localesSoportados = [for (final idioma in idiomas) Locale(idioma.codigo)];

extension TextosDelContexto on BuildContext {
  AppTexts get textos => Localizations.of<AppTexts>(this, AppTexts)!;

  String t(String clave, {int? count, Map<String, Object> args = const {}}) =>
      textos.t(clave, count: count, args: args);

  String traducir(String mensaje) => textos.traducir(mensaje);

  /// Adapta un validador que devuelve claves para un `TextFormField`.
  String? Function(String?) validador(String? Function(String?) reglas) {
    final textos = this.textos;
    return (valor) {
      final clave = reglas(valor);
      return clave == null ? null : textos.traducir(clave);
    };
  }

  /// Fecha y hora locales con el formato del idioma vigente.
  String fechaYHora(DateTime instante) {
    final local = instante.toLocal();
    final formato = MaterialLocalizations.of(this);
    final hora = formato.formatTimeOfDay(TimeOfDay.fromDateTime(local), alwaysUse24HourFormat: true);
    return '${formato.formatCompactDate(local)} $hora';
  }
}
