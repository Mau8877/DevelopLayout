import 'package:dio/dio.dart';

import 'api_client.dart';

const _mensajeGenerico = 'Ocurrió un error inesperado. Intentá de nuevo.';

/// Extrae la [ApiException] de un error, venga sola o envuelta en un [DioException].
ApiException? apiExceptionDe(Object error) {
  if (error is ApiException) return error;
  if (error is DioException && error.error is ApiException) {
    return error.error as ApiException;
  }
  return null;
}

/// Convierte cualquier error en un texto apto para el usuario.
///
/// * Nunca devuelve `toString()` de una excepción ni el body de la respuesta.
String mensajeDeError(Object error) {
  return apiExceptionDe(error)?.message ?? _mensajeGenerico;
}
