import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/routing/app_router.dart';
import '../../features/auth/providers/auth_provider.dart';
import '../../core/marca.dart';
import '../../shared/language/app_texts.dart';
import '../../shared/widgets/selector_idioma.dart';

/// Shell del rol Usuario -- AppBar + `NavigationBar` (Material 3) inferior,
/// tres secciones. Equivalente mobile de `UsuarioLayout` en el frontend
/// web (ver ARQUITECTURA_FRONTEND.md#layouts-por-rol): una barra inferior es
/// el patrón mobile estándar para navegación de nivel superior con pocas
/// secciones, a diferencia del sidebar que usa el rol Admin.
class UsuarioShell extends ConsumerStatefulWidget {
  const UsuarioShell({super.key});

  @override
  ConsumerState<UsuarioShell> createState() => _UsuarioShellState();
}

class _UsuarioSeccion {
  /// Clave de idioma, nunca el texto.
  final String clave;
  final IconData icon;

  const _UsuarioSeccion(this.clave, this.icon);
}

const _secciones = [
  _UsuarioSeccion('shared:navegacion.inicio', Icons.home_outlined),
  _UsuarioSeccion('shared:navegacion.pedidos', Icons.list_alt_outlined),
  _UsuarioSeccion('shared:navegacion.perfil', Icons.person_outline),
];

class _UsuarioShellState extends ConsumerState<UsuarioShell> {
  int _selected = 0;

  @override
  Widget build(BuildContext context) {
    final seccionActual = _secciones[_selected];

    return Scaffold(
      appBar: AppBar(
        title: const Text(appName),
        actions: [
          const SelectorIdioma(),
          IconButton(
            icon: const Icon(Icons.devices_outlined),
            tooltip: context.t('shared:navegacion.misSesiones'),
            onPressed: () => context.push(rutaMisSesiones),
          ),
          IconButton(
            icon: const Icon(Icons.password),
            tooltip: context.t('shared:navegacion.cambiarPassword'),
            onPressed: () => context.push(rutaCambiarPassword),
          ),
          IconButton(
            icon: const Icon(Icons.logout),
            tooltip: context.t('shared:cabecera.cerrarSesion'),
            onPressed: () => ref.read(authProvider.notifier).logout(),
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(context.t(seccionActual.clave), style: Theme.of(context).textTheme.headlineSmall),
            const SizedBox(height: 8),
            Text(
              context.t('shared:layout.ejemplo', args: {'seccion': context.t(seccionActual.clave)}),
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ],
        ),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selected,
        onDestinationSelected: (index) => setState(() => _selected = index),
        destinations: [
          for (final seccion in _secciones)
            NavigationDestination(icon: Icon(seccion.icon), label: context.t(seccion.clave)),
        ],
      ),
    );
  }
}
