import 'package:flutter/foundation.dart';

/// Único punto de log de la app (ver ERRORES_LOGGING_MOBILE.md).
///
/// * Solo emite en debug (`kDebugMode`); en release no escribe nada.
/// * Nunca se usa `print` directo en el resto del código.
/// ! Nunca pasar tokens, contraseñas ni bodies de `/auth/**`.
abstract final class AppLogger {
  static void debug(String message) {
    if (kDebugMode) debugPrint('[debug] $message');
  }

  static void error(String message, [Object? error, StackTrace? stackTrace]) {
    if (!kDebugMode) return;
    debugPrint('[error] $message${error != null ? ' (${error.runtimeType})' : ''}');
    if (stackTrace != null) debugPrint(stackTrace.toString());
  }
}
