import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/error_message.dart';
import '../../../shared/widgets/dialogo_confirmacion.dart';
import '../../../shared/widgets/error_snack_bar.dart';
import '../../../shared/widgets/estado_error.dart';
import '../models/pagina_sesiones.dart';
import '../providers/mis_sesiones_provider.dart';
import '../widgets/sesion_tile.dart';
import '../../../shared/language/app_texts.dart';
import '../../../shared/widgets/selector_idioma.dart';

/// Lugares donde la cuenta está abierta, con cierre de los demás.
class MisSesionesScreen extends ConsumerStatefulWidget {
  const MisSesionesScreen({super.key});

  @override
  ConsumerState<MisSesionesScreen> createState() => _MisSesionesScreenState();
}

class _MisSesionesScreenState extends ConsumerState<MisSesionesScreen> {
  bool _ocupado = false;

  MisSesionesNotifier get _sesiones => ref.read(misSesionesProvider.notifier);

  /// Ejecuta una acción sin permitir otra a la vez y muestra su fallo.
  Future<void> _ejecutar(Future<void> Function() accion) async {
    if (_ocupado) return;
    setState(() => _ocupado = true);
    try {
      await accion();
    } catch (error) {
      if (mounted) mostrarError(context, context.traducir(mensajeDeError(error)));
    } finally {
      if (mounted) setState(() => _ocupado = false);
    }
  }

  Future<void> _confirmarY(String titulo, String mensaje, Future<void> Function() accion) async {
    final confirmado = await pedirConfirmacion(
      context,
      titulo: titulo,
      mensaje: mensaje,
      confirmar: context.t('shared:comun.cerrar'),
    );
    if (confirmado && mounted) await _ejecutar(accion);
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(misSesionesProvider);
    return Scaffold(
      appBar: AppBar(title: Text(context.t('auth:sesiones.titulo')), actions: const [SelectorIdioma()]),
      body: SafeArea(
        child: estado.when(
          skipLoadingOnRefresh: true,
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (error, _) => EstadoError(
            mensaje: context.traducir(mensajeDeError(error)),
            onReintentar: () => ref.invalidate(misSesionesProvider),
          ),
          data: _lista,
        ),
      ),
    );
  }

  Widget _lista(PaginaSesiones pagina) {
    return ListView(
      children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: OutlinedButton(
            onPressed: _ocupado || pagina.totalItems <= 1
                ? null
                : () => _confirmarY(
                      context.t('auth:sesiones.cerrarDemas'),
                      context.t('auth:sesiones.avisoDemas'),
                      () => _sesiones.cerrarOtras(),
                    ),
            child: Text(context.t('auth:sesiones.cerrarDemas')),
          ),
        ),
        for (final sesion in pagina.sesiones)
          SesionTile(
            sesion: sesion,
            onCerrar: _ocupado
                ? null
                : () => _confirmarY(
                      context.t('auth:sesiones.tituloCerrar'),
                      context.t('auth:sesiones.avisoUna'),
                      () => _sesiones.cerrar(sesion.codigo),
                    ),
          ),
        if (pagina.hayMas)
          TextButton(
            onPressed: _ocupado ? null : () => _ejecutar(() => _sesiones.cargarMas()),
            child: Text(context.t('auth:sesiones.cargarMas')),
          ),
      ],
    );
  }
}
