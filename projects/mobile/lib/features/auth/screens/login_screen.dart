import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/error_message.dart';
import '../../../shared/widgets/app_button.dart';
import '../../../shared/widgets/app_text_field.dart';
import '../providers/auth_provider.dart';

/// Validadores con las mismas reglas que el login web (loginSchema).
String? validarCorreo(String? value) {
  final texto = value?.trim() ?? '';
  if (texto.isEmpty) return 'El correo es obligatorio';
  if (!RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(texto)) return 'El correo no es válido';
  return null;
}

String? validarPassword(String? value) {
  final texto = value ?? '';
  if (texto.isEmpty) return 'La contraseña es obligatoria';
  if (texto.length < 8) return 'La contraseña debe tener al menos 8 caracteres';
  if (texto.codeUnits.length > 72) return 'La contraseña es demasiado larga';
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
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(mensajeDeError(error))));
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
              child: AutofillGroup(
                child: Form(
                  key: _formKey,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Text(
                        'Iniciar sesión',
                        style: Theme.of(context).textTheme.headlineMedium,
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 24),
                      AppTextField(
                        label: 'Correo',
                        controller: _emailController,
                        validator: validarCorreo,
                        keyboardType: TextInputType.emailAddress,
                        autofillHints: const [AutofillHints.username],
                        textInputAction: TextInputAction.next,
                        enabled: !_enviando,
                      ),
                      const SizedBox(height: 16),
                      AppTextField(
                        label: 'Contraseña',
                        controller: _passwordController,
                        validator: validarPassword,
                        obscureText: true,
                        autofillHints: const [AutofillHints.password],
                        textInputAction: TextInputAction.done,
                        enabled: !_enviando,
                        onFieldSubmitted: (_) => _enviar(),
                      ),
                      const SizedBox(height: 24),
                      AppButton(texto: 'Ingresar', cargando: _enviando, onPressed: _enviar),
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
