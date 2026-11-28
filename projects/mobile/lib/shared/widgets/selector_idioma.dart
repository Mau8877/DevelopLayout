import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../language/app_texts.dart';
import '../language/idioma_provider.dart';
import '../language/idiomas.dart';

/// Cambia el idioma sin reiniciar. Va también en las pantallas sin sesión.
class SelectorIdioma extends ConsumerWidget {
  const SelectorIdioma({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final actual = ref.watch(idiomaProvider);
    return PopupMenuButton<String>(
      icon: const Icon(Icons.language),
      tooltip: context.t('shared:idioma.cambiar'),
      initialValue: actual,
      onSelected: (codigo) => ref.read(idiomaProvider.notifier).cambiar(codigo),
      itemBuilder: (_) => [
        for (final idioma in idiomas)
          CheckedPopupMenuItem(
            value: idioma.codigo,
            checked: idioma.codigo == actual,
            child: Text(idioma.nombre),
          ),
      ],
    );
  }
}
