import 'package:flutter/material.dart';
import '../language/app_texts.dart';

/// Fallo al cargar datos, con opción de reintentar. El mensaje llega ya apto
/// para el usuario.
class EstadoError extends StatelessWidget {
  final String mensaje;
  final VoidCallback onReintentar;

  const EstadoError({super.key, required this.mensaje, required this.onReintentar});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(mensaje, textAlign: TextAlign.center),
            const SizedBox(height: 16),
            OutlinedButton(onPressed: onReintentar, child: Text(context.t('shared:comun.reintentar'))),
          ],
        ),
      ),
    );
  }
}
