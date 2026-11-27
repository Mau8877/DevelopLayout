package com.example.backend.features.usuarios.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.backend.features.usuarios.RolNombres;

/**
 * Crea el SUPER_ADMIN inicial si todavía no existe ninguno (idempotente).
 *
 * * Usa la conexión dueña (`spring.flyway.*`), no la de `app_runtime`: en el
 * * arranque no hay usuario autenticado y RLS está forzado. Para que también
 * * funcione con un dueño que no ignore RLS, fija dentro de cada transacción
 * * `app.alta_inicial` para buscar al administrador y `app.current_user` (el
 * * código del usuario nuevo) para crearlo. Es la única escritura fuera de
 * * `app_runtime` (ver docs/seguridad/RLS_EXCEPCIONES.md).
 * ! La contraseña solo se hashea con BCrypt: nunca se loguea ni se guarda en claro.
 */
@Component
public class AdminInicialInitializer implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdminInicialInitializer.class);

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int PASSWORD_MIN_BYTES = 8;
    private static final int PASSWORD_MAX_BYTES = 72;

    private final String url;
    private final String usuarioDueno;
    private final String passwordDueno;
    private final String email;
    private final String password;

    public AdminInicialInitializer(
            @Value("${spring.flyway.url:${spring.datasource.url}}") String url,
            @Value("${spring.flyway.user}") String usuarioDueno,
            @Value("${spring.flyway.password}") String passwordDueno,
            @Value("${app.initial-admin.email:}") String email,
            @Value("${app.initial-admin.password:}") String password) {
        this.url = url;
        this.usuarioDueno = usuarioDueno;
        this.passwordDueno = passwordDueno;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        MDC.put("module", "usuarios");
        try {
            DriverManagerDataSource dataSource = new DriverManagerDataSource(url, usuarioDueno, passwordDueno);
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            TransactionTemplate transaccion = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

            if (existeSuperAdmin(jdbc, transaccion)) {
                LOG.debug("Ya existe un {}; no se crea el administrador inicial", RolNombres.SUPER_ADMIN);
                return;
            }
            validar(email, password);
            crear(jdbc, transaccion);
        } finally {
            MDC.remove("module");
        }
    }

    private static boolean existeSuperAdmin(JdbcTemplate jdbc, TransactionTemplate transaccion) {
        // Sin este contexto, un dueño que no ignore RLS contaría 0 sin ningún error.
        Integer total = transaccion.execute(status -> {
            jdbc.queryForObject("SELECT set_config('app.alta_inicial', 'on', true)", String.class);
            return jdbc.queryForObject(
                    "SELECT count(*) FROM authentication.usuario_rol ur "
                            + "JOIN authentication.rol r ON r.id = ur.rol_id "
                            + "WHERE r.nombre = ? AND ur.is_active AND ur.deleted_at IS NULL",
                    Integer.class, RolNombres.SUPER_ADMIN);
        });
        return total != null && total > 0;
    }

    /**
     * Falla con un mensaje claro (sin incluir la contraseña) si faltan o son
     * inválidas las credenciales del administrador inicial.
     */
    static void validar(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("No existe ningún " + RolNombres.SUPER_ADMIN
                    + ": definí INITIAL_ADMIN_EMAIL e INITIAL_ADMIN_PASSWORD para crear el administrador inicial");
        }
        if (!EMAIL.matcher(email.trim()).matches()) {
            throw new IllegalStateException("INITIAL_ADMIN_EMAIL no tiene un formato de correo válido");
        }
        int bytes = password.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < PASSWORD_MIN_BYTES || bytes > PASSWORD_MAX_BYTES) {
            throw new IllegalStateException("INITIAL_ADMIN_PASSWORD debe tener entre " + PASSWORD_MIN_BYTES
                    + " y " + PASSWORD_MAX_BYTES + " bytes");
        }
    }

    private void crear(JdbcTemplate jdbc, TransactionTemplate transaccion) {
        String correo = email.trim().toLowerCase(Locale.ROOT);
        String hash = new BCryptPasswordEncoder().encode(password);
        UUID codigoUsuario = UUID.randomUUID();
        try {
            transaccion.executeWithoutResult(status -> {
                jdbc.queryForObject("SELECT set_config('app.current_user', ?, true)", String.class,
                        codigoUsuario.toString());
                jdbc.update("INSERT INTO authentication.usuario (codigo_usuario, email, password_hash, estado) "
                        + "VALUES (?, ?, ?, 'ACTIVO')", codigoUsuario, correo, hash);
                jdbc.update("INSERT INTO authentication.usuario_rol (codigo_usuario_rol, usuario_id, rol_id, is_active) "
                        + "SELECT ?, u.id, r.id, true FROM authentication.usuario u, authentication.rol r "
                        + "WHERE u.codigo_usuario = ? AND r.nombre = ?",
                        UUID.randomUUID(), codigoUsuario, RolNombres.SUPER_ADMIN);
            });
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Ya existe un usuario con INITIAL_ADMIN_EMAIL pero ninguno es "
                    + RolNombres.SUPER_ADMIN + ": resolvelo a mano o usá otro correo", e);
        }
        LOG.info("Administrador inicial creado: {} ({})", correo, RolNombres.SUPER_ADMIN);
    }
}
