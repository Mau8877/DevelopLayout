import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/routing/app_router.dart';
import '../../features/auth/providers/auth_provider.dart';
import '../../core/marca.dart';
import '../../shared/language/app_texts.dart';
import '../../shared/widgets/selector_idioma.dart';

/// Shell del rol Admin -- AppBar + `NavigationDrawer` (Material 3, tres
/// secciones). Equivalente mobile de `AdminLayout` en el frontend web (ver
/// ARQUITECTURA_FRONTEND.md#layouts-por-rol): un Drawer calza mejor que un
/// sidebar fijo en una pantalla angosta, pero cumple el mismo rol -- la
/// navegación de nivel superior del rol admin.
class AdminShell extends ConsumerStatefulWidget {
  const AdminShell({super.key});

  @override
  ConsumerState<AdminShell> createState() => _AdminShellState();
}

class _AdminSeccion {
  /// Clave de idioma, nunca el texto.
  final String clave;
  final IconData icon;

  const _AdminSeccion(this.clave, this.icon);
}

const _secciones = [
  _AdminSeccion('shared:navegacion.panel', Icons.dashboard_outlined),
  _AdminSeccion('shared:navegacion.usuarios', Icons.people_outline),
  _AdminSeccion('shared:navegacion.pedidos', Icons.list_alt_outlined),
];

class _AdminShellState extends ConsumerState<AdminShell> {
  int _selected = 0;

  @override
  Widget build(BuildContext context) {
    final seccionActual = _secciones[_selected];

    return Scaffold(
      appBar: AppBar(
        title: const Text(appNameAdmin),
        actions: [
          const SelectorIdioma(),
          IconButton(
            icon: const Icon(Icons.devices_outlined),
            tooltip: context.t('shared:navegacion.misSesiones'),
            onPressed: () => context.push(rutaMisSesiones),
          ),
          IconButton(
            icon: const Icon(Icons.logout),
            tooltip: context.t('shared:cabecera.cerrarSesion'),
            onPressed: () => ref.read(authProvider.notifier).logout(),
          ),
        ],
      ),
      drawer: NavigationDrawer(
        selectedIndex: _selected,
        onDestinationSelected: (index) {
          setState(() => _selected = index);
          Navigator.pop(context);
        },
        children: [
          const Padding(
            padding: EdgeInsets.fromLTRB(16, 16, 16, 8),
            child: Text(appNameAdmin, style: TextStyle(fontWeight: FontWeight.w600)),
          ),
          for (final seccion in _secciones)
            NavigationDrawerDestination(
              icon: Icon(seccion.icon),
              label: Text(context.t(seccion.clave)),
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
    );
  }
}
