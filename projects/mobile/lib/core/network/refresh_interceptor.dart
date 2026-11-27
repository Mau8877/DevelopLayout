import 'package:dio/dio.dart';

import '../logging/app_logger.dart';
import '../storage/secure_token_storage.dart';

/// Renueva la sesión ante un 401 y reintenta la request una sola vez.
///
/// * [QueuedInterceptor]: los errores se procesan de a uno, así varias requests
/// * con 401 producen un solo refresh (las siguientes ya ven el token nuevo).
/// * [_auxiliar] no lleva este interceptor: reintentar por el cliente principal
/// * bloquearía la cola.
/// * Solo un 401/403 del refresh cierra la sesión. Sin conexión, 429 o 500 los
/// * tokens se conservan.
/// ! Acá circulan tokens: solo se registra el tipo de un error inesperado.
class RefreshInterceptor extends QueuedInterceptor {
  RefreshInterceptor({
    required SecureTokenStorage tokenStorage,
    required this._auxiliar,
    this._onSesionExpirada,
  }) : _tokens = tokenStorage;

  /// Un 401 de estas rutas no se arregla renovando.
  static const _sinRenovacion = ['/auth/login', '/auth/refresh', '/auth/logout'];

  final SecureTokenStorage _tokens;
  final Dio _auxiliar;
  final void Function()? _onSesionExpirada;

  @override
  Future<void> onError(DioException err, ErrorInterceptorHandler handler) async {
    final request = err.requestOptions;
    if (err.response?.statusCode != 401 || _sinRenovacion.contains(request.path)) {
      return handler.next(err);
    }

    try {
      final access = await _tokens.leerAccess();
      final yaRenovada = access != null && request.headers['Authorization'] != 'Bearer $access';
      if (!yaRenovada && !await _renovar()) return handler.next(err);
      handler.resolve(await _reintentar(request));
    } on DioException catch (fallo) {
      handler.reject(fallo);
    } catch (error, stack) {
      // * Sin esto, un error inesperado (ej. cuerpo mal formado) dejaría la
      // * request sin respuesta para siempre.
      AppLogger.error('No se pudo renovar la sesión', error.runtimeType, stack);
      handler.next(err);
    }
  }

  /// `true` si quedaron tokens nuevos guardados. `false` si no hay sesión que
  /// renovar. Un fallo temporal se relanza.
  Future<bool> _renovar() async {
    final refresh = await _tokens.leerRefresh();
    if (refresh == null) return false;

    final Response<Map<String, dynamic>> response;
    try {
      response = await _auxiliar.post<Map<String, dynamic>>('/auth/refresh', data: {'refreshToken': refresh});
    } on DioException catch (fallo) {
      final status = fallo.response?.statusCode;
      if (status != 401 && status != 403) rethrow;
      await _tokens.borrar();
      _onSesionExpirada?.call();
      return false;
    }

    final nuevoAccess = response.data!['accessToken'] as String;
    final nuevoRefresh = response.data!['refreshToken'] as String;
    // * Si se cerró la sesión mientras se renovaba, no se la revive: se cierra
    // * también en el servidor la que se acaba de rotar.
    if (await _tokens.leerRefresh() != refresh) {
      await _auxiliar.post<dynamic>('/auth/logout', data: {'refreshToken': nuevoRefresh});
      return false;
    }
    await _tokens.guardar(access: nuevoAccess, refresh: nuevoRefresh);
    return true;
  }

  /// El [_auxiliar] vuelve a poner el `Authorization` con el token guardado.
  Future<Response<dynamic>> _reintentar(RequestOptions request) {
    final data = request.data;
    return _auxiliar.fetch<dynamic>(
      request.copyWith(data: data is FormData ? data.clone() : data),
    );
  }
}
