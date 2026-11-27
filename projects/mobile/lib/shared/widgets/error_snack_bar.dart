import 'package:flutter/material.dart';

/// Muestra el fallo de una acción en un `SnackBar`, reemplazando el anterior.
/// El texto llega ya apto para el usuario (ver `mensajeDeError`).
void mostrarError(BuildContext context, String mensaje) {
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(content: Text(mensaje)));
}
