import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../models/pagina_sesiones.dart';
import '../services/sesiones_service.dart';
import 'auth_provider.dart';

final sesionesServiceProvider = Provider<SesionesService>((ref) => SesionesService(ApiClient.instance));

/// Mis sesiones, acumulando páginas.
///
/// * `autoDispose`: se descarta al salir de la pantalla. Depende del usuario de la
/// * sesión, así que al cambiar de cuenta no queda nada de la anterior.
/// * Sin reintento automático: el error se muestra con "Reintentar".
final misSesionesProvider = AsyncNotifierProvider.autoDispose<MisSesionesNotifier, PaginaSesiones>(
  MisSesionesNotifier.new,
  retry: (_, _) => null,
);

class MisSesionesNotifier extends AsyncNotifier<PaginaSesiones> {
  bool _cargandoMas = false;

  SesionesService get _servicio => ref.read(sesionesServiceProvider);

  @override
  Future<PaginaSesiones> build() {
    ref.watch(authProvider.select((auth) => auth.value?.codigo));
    return ref.watch(sesionesServiceProvider).listar(1);
  }

  /// Agrega la página siguiente. Si falla relanza y conserva lo ya cargado.
  Future<void> cargarMas() async {
    final actual = state.value;
    if (actual == null || !actual.hayMas || _cargandoMas) return;
    _cargandoMas = true;
    try {
      final siguiente = await _servicio.listar(actual.page + 1);
      if (ref.mounted) state = AsyncData(actual.seguidaDe(siguiente));
    } finally {
      _cargandoMas = false;
    }
  }

  Future<void> cerrar(String codigo) => _yRecargar(() => _servicio.cerrar(codigo));

  Future<void> cerrarOtras() => _yRecargar(_servicio.cerrarOtras);

  // * Se recarga también si falla: un 404 significa que el listado quedó viejo.
  Future<void> _yRecargar(Future<void> Function() accion) async {
    try {
      await accion();
    } finally {
      if (ref.mounted) ref.invalidateSelf();
    }
  }
}
