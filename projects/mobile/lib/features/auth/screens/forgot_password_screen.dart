import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/error_message.dart';
import '../../../core/routing/app_router.dart';
import '../../../shared/widgets/app_button.dart';
import '../../../shared/widgets/app_text_field.dart';
import '../../../shared/widgets/error_snack_bar.dart';
import '../providers/auth_provider.dart';
import 'login_screen.dart' show validarCorreo;

/// No afirma que se envió: la app no sabe (ni debe saber) si la cuenta existe.
const mensajeEnlacePedido =
    'Si hay una cuenta con ese correo, te enviamos un enlace para restablecer la contraseña. '
    'Abrilo desde el correo: se abre en el navegador. Revisá también la carpeta de spam.';

/// Pide el enlace de recuperación. El restablecimiento se completa en la web.
class ForgotPasswordScreen extends ConsumerStatefulWidget {
  const ForgotPasswordScreen({super.key});

  @override
  ConsumerState<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends ConsumerState<ForgotPasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  bool _enviando = false;
  bool _pedido = false;

  @override
  void dispose() {
    _emailController.dispose();
    super.dispose();
  }

  Future<void> _enviar() async {
    if (_enviando || !(_formKey.currentState?.validate() ?? false)) return;
    setState(() => _enviando = true);
    try {
      await ref.read(authServiceProvider).solicitarRecuperacion(_emailController.text.trim());
      if (mounted) setState(() => _pedido = true);
    } catch (error) {
      if (mounted) mostrarError(context, mensajeDeError(error));
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 400),
              child: Form(
                key: _formKey,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Text(
                      'Recuperar contraseña',
                      style: Theme.of(context).textTheme.headlineMedium,
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 24),
                    if (_pedido)
                      const Text(mensajeEnlacePedido, textAlign: TextAlign.center)
                    else ...[
                      AppTextField(
                        label: 'Correo',
                        controller: _emailController,
                        validator: validarCorreo,
                        keyboardType: TextInputType.emailAddress,
                        autofillHints: const [AutofillHints.username],
                        textInputAction: TextInputAction.done,
                        enabled: !_enviando,
                        onFieldSubmitted: (_) => _enviar(),
                      ),
                      const SizedBox(height: 24),
                      AppButton(texto: 'Enviar enlace', cargando: _enviando, onPressed: _enviar),
                    ],
                    const SizedBox(height: 16),
                    TextButton(
                      onPressed: () => context.go(rutaLogin),
                      child: const Text('Volver a iniciar sesión'),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
