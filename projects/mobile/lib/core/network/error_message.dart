import 'package:dio/dio.dart';

import 'api_client.dart';

const _mensajeGenerico = 'shared:cliente.desconocido';

/// Extrae la [ApiException] de un error, venga sola o envuelta en un [DioException].
ApiException? apiExceptionDe(Object error) {
  if (error is ApiException) return error;
  if (error is DioException && error.error is ApiException) {
    return error.error as ApiException;
  }
  return null;
}

/// Convierte cualquier error en un mensaje apto para el usuario: el del servidor
/// o una clave de idioma. La pantalla lo muestra con `context.traducir`.
///
/// * Nunca devuelve `toString()` de una excepción ni el body de la respuesta.
String mensajeDeError(Object error) {
  return apiExceptionDe(error)?.message ?? _mensajeGenerico;
}
