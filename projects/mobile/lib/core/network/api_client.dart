import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

import '../logging/app_logger.dart';
import '../storage/secure_token_storage.dart';

/// Excepción tipada para cualquier fallo de una llamada a la API -- nunca se
/// deja propagar un [DioException] crudo, sin código, hasta la UI.
///
/// * Errores de negocio del backend (`status: "failed"`, ver
/// * RESPONSES_BACKEND.md): `code` es el `ERR_*` del backend.
/// * Fallos sin sobre: `CLIENT_NETWORK`, `CLIENT_TIMEOUT` o `CLIENT_UNKNOWN`.
class ApiException implements Exception {
  final String code;
  final String message;

  ApiException({required this.code, required this.message});

  @override
  String toString() => 'ApiException($code): $message';
}

/// Cliente HTTP único de la app -- todo `service` de cada feature llama a
/// través de acá, nunca crea su propia instancia de [Dio] (ver
/// CONSUMO_API_MOBILE.md).
///
/// * Envía `X-Client-Type: mobile` (el backend lo usa para responder con los
/// * tokens en el body y no con cookies) y el access token como Bearer.
/// * El interceptor desempaqueta el sobre estándar del backend: en éxito deja
/// * pasar `data` (y `meta` si vino); en error, lanza [ApiException].
class ApiClient {
  static Dio? _instance;

  static Dio get instance {
    _instance ??= create(
      baseUrl: _baseUrl(),
      tokenStorage: SecureTokenStorage(),
      logs: dotenv.env['ENABLE_API_LOGS'] == 'true',
    );
    return _instance!;
  }

  /// URL base de la API, leída de `.env`.
  ///
  /// * El default hacia el emulador (HTTP) existe solo en debug. En release
  /// * falta la variable => se falla al arrancar en vez de caer a una URL
  /// * insegura, y la URL tiene que ser HTTPS (ver SEGURIDAD_OWASP.md, MASVS-NETWORK).
  static String _baseUrl() {
    final configured = dotenv.env['API_BASE_URL'];
    if (kReleaseMode) {
      if (configured == null || !configured.startsWith('https://')) {
        throw StateError('API_BASE_URL debe estar definida y ser https:// en release');
      }
      return configured;
    }
    return configured ?? 'http://10.0.2.2:8080/api/v1';
  }

  /// Construye un cliente. Separado de [instance] para poder probarlo con un
  /// adaptador falso de Dio.
  static Dio create({
    required String baseUrl,
    required SecureTokenStorage tokenStorage,
    HttpClientAdapter? adapter,
    bool logs = false,
  }) {
    final dio = Dio(
      BaseOptions(
        baseUrl: baseUrl,
        connectTimeout: const Duration(seconds: 10),
        receiveTimeout: const Duration(seconds: 15),
        headers: {'X-Client-Type': 'mobile'},
      ),
    );
    if (adapter != null) dio.httpClientAdapter = adapter;

    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          final token = await tokenStorage.leerAccess();
          if (token != null && token.isNotEmpty) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          handler.next(options);
        },
      ),
    );

    dio.interceptors.add(
      InterceptorsWrapper(
        onResponse: (response, handler) {
          final envelope = response.data;
          if (envelope is Map<String, dynamic> && envelope.containsKey('status')) {
            response.data = envelope['meta'] != null
                ? {'data': envelope['data'], 'meta': envelope['meta']}
                : envelope['data'];
          }
          handler.next(response);
        },
        onError: (error, handler) {
          handler.reject(
            DioException(
              requestOptions: error.requestOptions,
              error: normalizarError(error),
              response: error.response,
              type: error.type,
            ),
          );
        },
      ),
    );

    // * Solo método, ruta y status: nunca headers ni body (puede haber tokens
    // * o credenciales, sobre todo en /auth/**), y solo en debug.
    if (logs) {
      dio.interceptors.add(
        InterceptorsWrapper(
          onResponse: (response, handler) {
            AppLogger.debug('${response.requestOptions.method} ${response.requestOptions.path} '
                '-> ${response.statusCode}');
            handler.next(response);
          },
          onError: (error, handler) {
            AppLogger.debug('${error.requestOptions.method} ${error.requestOptions.path} '
                '-> ${error.response?.statusCode ?? error.type.name}');
            handler.next(error);
          },
        ),
      );
    }

    return dio;
  }

  /// Convierte un [DioException] en [ApiException]: con sobre (código de
  /// negocio) o sin sobre (red, timeout, respuesta no JSON, 502/504).
  static ApiException normalizarError(DioException error) {
    final envelope = error.response?.data;
    if (envelope is Map<String, dynamic> && envelope['error'] != null) {
      return ApiException(
        code: envelope['error'] as String,
        message: envelope['message'] as String? ?? 'Ocurrió un error inesperado',
      );
    }
    switch (error.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
        return ApiException(
          code: 'CLIENT_TIMEOUT',
          message: 'La solicitud tardó demasiado. Intentá de nuevo.',
        );
      case DioExceptionType.connectionError:
        return ApiException(
          code: 'CLIENT_NETWORK',
          message: 'No pudimos conectarnos. Revisá tu conexión e intentá de nuevo.',
        );
      default:
        if (error.error is SocketException) {
          return ApiException(
            code: 'CLIENT_NETWORK',
            message: 'No pudimos conectarnos. Revisá tu conexión e intentá de nuevo.',
          );
        }
        return ApiException(
          code: 'CLIENT_UNKNOWN',
          message: 'Ocurrió un error inesperado. Intentá de nuevo.',
        );
    }
  }
}
