import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../providers/health_provider.dart';
import '../../../shared/language/app_texts.dart';

/// Pantalla de diagnóstico -- prueba de punta a punta el camino completo
/// `service -> provider -> screen` contra el backend real: `ApiClient`
/// (Dio + interceptor), Riverpod (`FutureProvider` + `AsyncValue`) y el
/// desempaquetado de un error catalogado real. Sirve como referencia de
/// cómo se ve ese camino antes de que exista la primera feature de negocio.
class HealthCheckScreen extends ConsumerWidget {
  const HealthCheckScreen({super.key});

  Future<void> _probarRutaInexistente(BuildContext context, WidgetRef ref) async {
    final messenger = ScaffoldMessenger.of(context);
    final textos = context.textos;
    try {
      await ref.read(healthServiceProvider).pingRutaInexistente();
      messenger.showSnackBar(SnackBar(content: Text(textos.t('health:respuestaInesperada'))));
    } on DioException catch (error) {
      final apiException = error.error;
      final mensaje = apiException is ApiException
          ? '[${apiException.code}] ${textos.traducir(apiException.message)}'
          : textos.t('health:sinApiException', args: {
              'estado': '${error.response?.statusCode}',
              'cuerpo': '${error.response?.data}',
            });
      messenger.showSnackBar(SnackBar(content: Text(mensaje)));
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final health = ref.watch(healthCheckProvider);

    return Scaffold(
      appBar: AppBar(title: Text(context.t('health:titulo'))),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(context.t('health:salud'), style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 8),
            health.when(
              loading: () => Row(
                children: [
                  const SizedBox(height: 16, width: 16, child: CircularProgressIndicator(strokeWidth: 2)),
                  const SizedBox(width: 12),
                  Text(context.t('health:consultando')),
                ],
              ),
              error: (error, stackTrace) => Text(
                context.t('health:sinConexion', args: {'error': error}),
                style: TextStyle(color: Theme.of(context).colorScheme.error),
              ),
              data: (data) => Text(
                context.t('health:conectado', args: {'estado': '${data['status']}'}),
                style: TextStyle(color: Theme.of(context).colorScheme.primary),
              ),
            ),
            const SizedBox(height: 8),
            OutlinedButton(
              onPressed: () => ref.invalidate(healthCheckProvider),
              child: Text(context.t('shared:comun.reintentar')),
            ),
            const Divider(height: 40),
            Text(
              context.t('health:rutaInexistente'),
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 4),
            Text(
              context.t('health:explicacion'),
              style: Theme.of(context).textTheme.bodySmall,
            ),
            const SizedBox(height: 12),
            FilledButton(
              onPressed: () => _probarRutaInexistente(context, ref),
              child: Text(context.t('health:probar')),
            ),
          ],
        ),
      ),
    );
  }
}
