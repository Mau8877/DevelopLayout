import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/layouts/usuario/usuario_shell.dart';

import 'support/app_de_prueba.dart';

// * No se testea `MyApp` completo -- `main()` depende de `dotenv.load()`
// * (lee un archivo real), que no tiene sentido resolver en un widget test.
// * `UsuarioShell` no depende de ningún router (core/routing/ todavía no
// * existe, ver ARQUITECTURA_MOBILE.md), así que se monta directo en un
// * MaterialApp para verificar que el shell de un rol renderiza bien.
void main() {
  testWidgets('muestra la sección inicial del shell de usuario', (tester) async {
    await tester.pumpWidget(
      const ProviderScope(child: AppDePrueba(home: UsuarioShell())),
    );

    expect(find.text('Inicio'), findsWidgets);
    expect(find.byIcon(Icons.person_outline), findsOneWidget);
  });
}
