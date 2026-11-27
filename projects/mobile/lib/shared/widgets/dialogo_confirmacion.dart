import 'package:flutter/material.dart';

/// Pide confirmación antes de una acción. `true` solo si se confirmó (cerrar
/// el diálogo de cualquier otra forma es cancelar).
Future<bool> pedirConfirmacion(
  BuildContext context, {
  required String titulo,
  required String mensaje,
  required String confirmar,
  String cancelar = 'Cancelar',
}) async {
  final respuesta = await showDialog<bool>(
    context: context,
    builder: (dialogo) => AlertDialog(
      title: Text(titulo),
      content: Text(mensaje),
      actions: [
        TextButton(onPressed: () => Navigator.of(dialogo).pop(false), child: Text(cancelar)),
        FilledButton(onPressed: () => Navigator.of(dialogo).pop(true), child: Text(confirmar)),
      ],
    ),
  );
  return respuesta ?? false;
}
