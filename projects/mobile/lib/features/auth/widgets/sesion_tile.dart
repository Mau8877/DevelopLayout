import 'package:flutter/material.dart';

import '../models/mi_sesion.dart';

String _fecha(DateTime fecha) {
  final f = fecha.toLocal();
  String dos(int n) => n.toString().padLeft(2, '0');
  return '${dos(f.day)}/${dos(f.month)}/${f.year} ${dos(f.hour)}:${dos(f.minute)}';
}

/// Una sesión de la lista. La actual no ofrece cierre: para eso está "Cerrar sesión".
class SesionTile extends StatelessWidget {
  final MiSesion sesion;
  final VoidCallback? onCerrar;

  const SesionTile({super.key, required this.sesion, required this.onCerrar});

  @override
  Widget build(BuildContext context) {
    final ultimoUso = sesion.ultimoUsoEn;
    return ListTile(
      title: Text(sesion.dispositivo),
      subtitle: Text([
        '${sesion.tipoCliente == 'MOBILE' ? 'Móvil' : 'Web'} · ${sesion.ip ?? 'IP desconocida'}',
        'Iniciada: ${_fecha(sesion.creadaEn)}',
        if (ultimoUso != null) 'Último uso: ${_fecha(ultimoUso)}',
      ].join('\n')),
      isThreeLine: true,
      trailing: sesion.actual
          ? const Chip(label: Text('Esta sesión'))
          : IconButton(
              icon: const Icon(Icons.logout),
              tooltip: 'Cerrar esta otra sesión',
              onPressed: onCerrar,
            ),
    );
  }
}
