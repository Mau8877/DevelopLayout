import 'mi_sesion.dart';

/// Una página del listado: el interceptor entrega `{data, meta}`.
class PaginaSesiones {
  final List<MiSesion> sesiones;
  final int page;
  final int totalItems;
  final int totalPages;

  const PaginaSesiones({
    required this.sesiones,
    required this.page,
    required this.totalItems,
    required this.totalPages,
  });

  factory PaginaSesiones.fromJson(Map<String, dynamic> json) {
    final meta = json['meta'] as Map<String, dynamic>;
    return PaginaSesiones(
      sesiones: (json['data'] as List<dynamic>)
          .map((s) => MiSesion.fromJson(s as Map<String, dynamic>))
          .toList(),
      page: meta['page'] as int,
      totalItems: meta['totalItems'] as int,
      totalPages: meta['totalPages'] as int,
    );
  }

  bool get hayMas => page < totalPages;

  /// Suma la página siguiente sin repetir sesiones (el listado puede haber cambiado).
  PaginaSesiones seguidaDe(PaginaSesiones siguiente) {
    final vistas = sesiones.map((s) => s.codigo).toSet();
    return PaginaSesiones(
      sesiones: [...sesiones, ...siguiente.sesiones.where((s) => !vistas.contains(s.codigo))],
      page: siguiente.page,
      totalItems: siguiente.totalItems,
      totalPages: siguiente.totalPages,
    );
  }
}
