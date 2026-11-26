import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

/// El permiso de red de los manifiestos `debug`/`profile` no llega al build de
/// release: tiene que estar declarado en el manifiesto principal.
void main() {
  test('el manifiesto principal de Android declara el permiso INTERNET', () {
    final manifiesto = File('android/app/src/main/AndroidManifest.xml').readAsStringSync();

    expect(manifiesto, contains('<uses-permission android:name="android.permission.INTERNET"/>'));
  });
}
