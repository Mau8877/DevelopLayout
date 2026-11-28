import 'package:flutter/material.dart';

import '../models/mi_sesion.dart';
import '../../../shared/language/app_texts.dart';

/// Una sesión de la lista. La actual no ofrece cierre: para eso está "Cerrar sesión".
class SesionTile extends StatelessWidget {
  final MiSesion sesion;
  final VoidCallback? onCerrar;

  const SesionTile({super.key, required this.sesion, required this.onCerrar});

  @override
  Widget build(BuildContext context) {
    final ultimoUso = sesion.ultimoUsoEn;
    return ListTile(
      title: Text(sesion.dispositivo(context.textos)),
      subtitle: Text([
        '${context.t(sesion.tipoCliente == 'MOBILE' ? 'auth:sesiones.movil' : 'auth:sesiones.web')}'
            ' · ${sesion.ip ?? context.t('auth:sesiones.ipDesconocida')}',
        context.t('auth:sesiones.iniciada', args: {'fecha': context.fechaYHora(sesion.creadaEn)}),
        if (ultimoUso != null)
          context.t('auth:sesiones.ultimoUso', args: {'fecha': context.fechaYHora(ultimoUso)}),
      ].join('\n')),
      isThreeLine: true,
      trailing: sesion.actual
          ? Chip(label: Text(context.t('auth:sesiones.actual')))
          : IconButton(
              icon: const Icon(Icons.logout),
              tooltip: context.t('auth:sesiones.cerrarEsta'),
              onPressed: onCerrar,
            ),
    );
  }
}
