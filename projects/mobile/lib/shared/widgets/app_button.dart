import 'package:flutter/material.dart';

/// Botón genérico con estado de carga: mientras `cargando` está activo se
/// deshabilita (evita el doble envío) y muestra un indicador.
class AppButton extends StatelessWidget {
  final String texto;
  final VoidCallback? onPressed;
  final bool cargando;

  const AppButton({super.key, required this.texto, this.onPressed, this.cargando = false});

  @override
  Widget build(BuildContext context) {
    return FilledButton(
      onPressed: cargando ? null : onPressed,
      child: cargando
          ? const SizedBox(
              height: 20,
              width: 20,
              child: CircularProgressIndicator(strokeWidth: 2),
            )
          : Text(texto),
    );
  }
}
