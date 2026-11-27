/// Sesión propia (`GET /auth/sesiones`). `actual` es desde la que se consulta.
class MiSesion {
  final String codigo;
  final String tipoCliente;
  final String? ip;
  final String? userAgent;
  final DateTime creadaEn;
  final DateTime? ultimoUsoEn;
  final bool actual;

  const MiSesion({
    required this.codigo,
    required this.tipoCliente,
    required this.creadaEn,
    required this.actual,
    this.ip,
    this.userAgent,
    this.ultimoUsoEn,
  });

  factory MiSesion.fromJson(Map<String, dynamic> json) {
    final ultimoUso = json['ultimoUsoEn'] as String?;
    return MiSesion(
      codigo: json['codigo'] as String,
      tipoCliente: json['tipoCliente'] as String,
      ip: json['ip'] as String?,
      userAgent: json['userAgent'] as String?,
      creadaEn: DateTime.parse(json['creadaEn'] as String),
      ultimoUsoEn: ultimoUso == null ? null : DateTime.parse(ultimoUso),
      actual: json['actual'] as bool? ?? false,
    );
  }

  String get dispositivo => resumirDispositivo(userAgent, tipoCliente);
}

// * Mismas reglas que `resumirDispositivo` de la web. El orden importa: Edge y
// * Opera también dicen "Chrome"; iPhone dice "Mac OS X"; Android dice "Linux".
final _navegadores = <(RegExp, String)>[
  (RegExp(r'Edg(e|A|iOS)?/'), 'Edge'),
  (RegExp(r'OPR/|Opera'), 'Opera'),
  (RegExp(r'Firefox/|FxiOS/'), 'Firefox'),
  (RegExp(r'Chrome/|CriOS/'), 'Chrome'),
  (RegExp(r'Version/[\d.]+.*Safari/'), 'Safari'),
];

final _sistemas = <(RegExp, String)>[
  (RegExp('iPhone'), 'iPhone'),
  (RegExp('iPad'), 'iPad'),
  (RegExp('Android'), 'Android'),
  (RegExp('Windows'), 'Windows'),
  (RegExp('CrOS'), 'ChromeOS'),
  (RegExp('Mac OS X|Macintosh'), 'macOS'),
  (RegExp('Linux|X11'), 'Linux'),
];

String? _buscar(List<(RegExp, String)> reglas, String texto) {
  for (final (patron, nombre) in reglas) {
    if (patron.hasMatch(texto)) return nombre;
  }
  return null;
}

/// Nombre corto y orientativo del dispositivo. El user agent lo controla el
/// cliente: lo que no se reconoce no se adivina.
String resumirDispositivo(String? userAgent, String tipoCliente) {
  if (tipoCliente == 'MOBILE') return 'App móvil';
  final texto = userAgent ?? '';
  final navegador = _buscar(_navegadores, texto);
  final sistema = _buscar(_sistemas, texto);
  if (navegador != null && sistema != null) return '$navegador en $sistema';
  return navegador ?? sistema ?? 'Dispositivo desconocido';
}
