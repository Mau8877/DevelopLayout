import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/error_message.dart';
import '../../../core/routing/app_router.dart';
import '../../../shared/widgets/app_button.dart';
import '../../../shared/widgets/app_text_field.dart';
import '../../../shared/widgets/error_snack_bar.dart';
import '../providers/auth_provider.dart';
import '../../../shared/language/app_texts.dart';
import '../../../shared/widgets/selector_idioma.dart';

/// Validadores con las mismas reglas que el login web (loginSchema). Devuelven
/// claves de idioma.
String? validarCorreo(String? value) {
  final texto = value?.trim() ?? '';
  if (texto.isEmpty) return 'auth:validacion.correoObligatorio';
  if (!RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(texto)) return 'auth:validacion.correoInvalido';
  return null;
}

String? validarPassword(String? value) {
  final texto = value ?? '';
  if (texto.isEmpty) return 'auth:validacion.passwordObligatoria';
  if (texto.length < 8) return 'auth:validacion.passwordCorta';
  if (texto.codeUnits.length > 72) return 'auth:validacion.passwordLarga';
  return null;
}

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _enviando = false;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _enviar() async {
    if (_enviando || !(_formKey.currentState?.validate() ?? false)) return;
    setState(() => _enviando = true);
    try {
      await ref
          .read(authProvider.notifier)
          .login(_emailController.text.trim(), _passwordController.text);
      // * El router redirige solo al cambiar la sesión.
    } catch (error) {
      if (mounted) mostrarError(context, context.traducir(mensajeDeError(error)));
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(actions: const [SelectorIdioma()]),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 400),
              child: AutofillGroup(
                child: Form(
                  key: _formKey,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Text(
                        context.t('auth:login.titulo'),
                        style: Theme.of(context).textTheme.headlineMedium,
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 24),
                      AppTextField(
                        label: context.t('auth:comun.correo'),
                        controller: _emailController,
                        validator: context.validador(validarCorreo),
                        keyboardType: TextInputType.emailAddress,
                        autofillHints: const [AutofillHints.username],
                        textInputAction: TextInputAction.next,
                        enabled: !_enviando,
                      ),
                      const SizedBox(height: 16),
                      AppTextField(
                        label: context.t('auth:comun.password'),
                        controller: _passwordController,
                        validator: context.validador(validarPassword),
                        obscureText: true,
                        autofillHints: const [AutofillHints.password],
                        textInputAction: TextInputAction.done,
                        enabled: !_enviando,
                        onFieldSubmitted: (_) => _enviar(),
                      ),
                      const SizedBox(height: 24),
                      AppButton(texto: context.t('auth:login.entrar'), cargando: _enviando, onPressed: _enviar),
                      const SizedBox(height: 8),
                      TextButton(
                        onPressed: _enviando ? null : () => context.go(rutaOlvidePassword),
                        child: Text(context.t('auth:login.olvidaste')),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
