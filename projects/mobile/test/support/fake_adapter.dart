import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';

/// Adaptador de Dio falso: devuelve respuestas armadas por el test y guarda
/// las requests recibidas para poder inspeccionar headers y body.
class FakeAdapter implements HttpClientAdapter {
  final List<RequestOptions> requests = [];
  final Future<ResponseBody> Function(RequestOptions options) _handler;

  FakeAdapter(this._handler);

  /// Responde siempre con este JSON y status.
  factory FakeAdapter.json(Map<String, dynamic> body, {int status = 200}) {
    return FakeAdapter((_) async => jsonBody(body, status));
  }

  /// Falla siempre con un [DioException] del tipo dado.
  factory FakeAdapter.fails(DioExceptionType type) {
    return FakeAdapter((options) async {
      throw DioException(requestOptions: options, type: type);
    });
  }

  static ResponseBody jsonBody(Map<String, dynamic> body, int status) {
    return ResponseBody.fromString(
      jsonEncode(body),
      status,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) {
    requests.add(options);
    return _handler(options);
  }

  @override
  void close({bool force = false}) {}
}

Map<String, dynamic> envelopeOk(Object? data) => {
      'status': 'success',
      'data': data,
      'message': 'ok',
      'timestamp': '2026-01-01T00:00:00Z',
    };

Map<String, dynamic> envelopeError(String code, String message) => {
      'status': 'failed',
      'data': null,
      'message': message,
      'timestamp': '2026-01-01T00:00:00Z',
      'error': code,
    };
