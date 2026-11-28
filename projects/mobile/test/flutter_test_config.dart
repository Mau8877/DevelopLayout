import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/shared/language/app_texts.dart';

/// Los textos se cargan una vez antes de cada archivo de pruebas, como en `main`.
Future<void> testExecutable(FutureOr<void> Function() testMain) async {
  TestWidgetsFlutterBinding.ensureInitialized();
  await AppTexts.precargar();
  await testMain();
}
