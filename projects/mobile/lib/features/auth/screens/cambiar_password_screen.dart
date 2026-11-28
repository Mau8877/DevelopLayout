import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/error_message.dart';
import '../../../shared/language/app_texts.dart';
import '../../../shared/widgets/app_button.dart';
import '../../../shared/widgets/app_text_field.dart';
import '../../../shared/widgets/error_snack_bar.dart';
import '../../../shared/widgets/selector_idioma.dart';
import '../providers/auth_provider.dart';
import 'login_screen.dart' show validarPassword;

/// La actual solo tiene que estar y caber en lo que admite el servidor.
String? validarPasswordActual(String? value) {
  final texto = value ?? '';
  if (texto.isEmpty) return 'auth:validacion.passwordActualObligatoria';
  if (utf8.encode(texto).length > 72) return 'auth:validacion.passwordLarga';
  return null;
}

String? validarConfirmacion(String? value, String nueva) {
  if ((value ?? '').isEmpty) return 'auth:validacion.repetirPassword';
  if (value != nueva) return 'auth:validacion.passwordsDistintas';
  return null;
}

/// Cambio de la propia contraseña. El servidor cierra todas las sesiones, así
/// que al lograrlo la app vuelve al login.
class CambiarPasswordScreen extends ConsumerStatefulWidget {
  const CambiarPasswordScreen({super.key});

  @override
  ConsumerState<CambiarPasswordScreen> createState() => _CambiarPasswordScreenState();
}

class _CambiarPasswordScreenState extends ConsumerState<CambiarPasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _actualController = TextEditingController();
  final _nuevaController = TextEditingController();
  final _confirmacionController = TextEditingController();
  bool _enviando = false;

  @override
  void dispose() {
    _actualController.dispose();
    _nuevaController.dispose();
    _confirmacionController.dispose();
    super.dispose();
  }

  Future<void> _enviar() async {
    if (_enviando || !(_formKey.currentState?.validate() ?? false)) return;
    setState(() => _enviando = true);
    // * Se toman antes: al cerrarse la sesión el router quita esta pantalla.
    final mensajero = ScaffoldMessenger.of(context);
    final aviso = context.t('auth:cambiarPassword.exito');
    try {
      await ref
          .read(authProvider.notifier)
          .cambiarPassword(_actualController.text, _nuevaController.text);
      mensajero
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(aviso)));
    } catch (error) {
      if (mounted) mostrarError(context, context.traducir(mensajeDeError(error)));
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(context.t('auth:cambiarPassword.titulo')),
        actions: const [SelectorIdioma()],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(context.t('auth:cambiarPassword.aviso')),
                const SizedBox(height: 24),
                AppTextField(
                  label: context.t('auth:cambiarPassword.actual'),
                  controller: _actualController,
                  validator: context.validador(validarPasswordActual),
                  obscureText: true,
                  autofillHints: const [AutofillHints.password],
                  textInputAction: TextInputAction.next,
                  enabled: !_enviando,
                ),
                const SizedBox(height: 16),
                AppTextField(
                  label: context.t('auth:cambiarPassword.nueva'),
                  controller: _nuevaController,
                  validator: context.validador(validarPassword),
                  obscureText: true,
                  autofillHints: const [AutofillHints.newPassword],
                  textInputAction: TextInputAction.next,
                  enabled: !_enviando,
                ),
                const SizedBox(height: 16),
                AppTextField(
                  label: context.t('auth:cambiarPassword.confirmacion'),
                  controller: _confirmacionController,
                  validator: context.validador((valor) => validarConfirmacion(valor, _nuevaController.text)),
                  obscureText: true,
                  autofillHints: const [AutofillHints.newPassword],
                  textInputAction: TextInputAction.done,
                  enabled: !_enviando,
                  onFieldSubmitted: (_) => _enviar(),
                ),
                const SizedBox(height: 24),
                AppButton(
                  texto: context.t('auth:cambiarPassword.boton'),
                  cargando: _enviando,
                  onPressed: _enviar,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
