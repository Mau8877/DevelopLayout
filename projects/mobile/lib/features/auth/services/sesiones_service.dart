import 'package:dio/dio.dart';

import '../models/pagina_sesiones.dart';

/// Sesiones del propio usuario, vía el `Dio` de `ApiClient`.
class SesionesService {
  final Dio _dio;

  SesionesService(this._dio);

  Future<PaginaSesiones> listar(int page) async {
    final response = await _dio.get<Map<String, dynamic>>(
      '/auth/sessions',
      queryParameters: {'page': page, 'pageSize': 20},
    );
    return PaginaSesiones.fromJson(response.data!);
  }

  Future<void> cerrar(String codigo) async {
    await _dio.delete<dynamic>('/auth/sessions/$codigo');
  }

  Future<void> cerrarOtras() async {
    await _dio.post<dynamic>('/auth/sessions/revoke-others');
  }
}
