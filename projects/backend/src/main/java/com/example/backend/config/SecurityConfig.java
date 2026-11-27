package com.example.backend.config;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.security.JwtService;
import com.example.backend.security.RateLimitFilter;
import com.example.backend.security.RestAccessDeniedHandler;
import com.example.backend.security.RestAuthenticationEntryPoint;
import com.example.backend.security.SesionRevocadaRegistry;

@Configuration
@EnableWebSecurity
// ! Sin esto, todo @PreAuthorize de los services se ignora en silencio.
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private List<String> corsAllowedOrigins;

    private final RateLimitFilter rateLimitFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final JwtService jwtService;
    private final SesionRevocadaRegistry sesionRevocadaRegistry;

    // * CSRF solo aplica a clientes web (cookies). Un request con Bearer o con
    // * el header propio de mobile no depende de cookies del browser, y un
    // * header custom obliga a preflight CORS, que falla para orígenes no
    // * permitidos.
    private static boolean esClienteSinCookies(jakarta.servlet.http.HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        return (auth != null && auth.startsWith("Bearer "))
                || "mobile".equalsIgnoreCase(request.getHeader("X-Client-Type"));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                // ! Sin sesión de servidor, cada request autenticada con JWT cuenta como
                // ! "recién autenticada" y la estrategia por defecto borraba la cookie
                // ! XSRF-TOKEN: el siguiente refresh o logout recibía 403.
                .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                .ignoringRequestMatchers((RequestMatcher) SecurityConfig::esClienteSinCookies))
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Rate limit por IP sobre /api/v1/** -- ver SEGURIDAD_AUTH_BACKEND.md
            // y RateLimitFilter. Antes del filtro de autenticación: un cliente sin
            // token igual cuenta contra su límite, no es gratis intentar de más.
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            // Instancia propia (no @Component): ver nota en JwtAuthenticationFilter.
            .addFilterBefore(new JwtAuthenticationFilter(jwtService, sesionRevocadaRegistry),
                UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // 1. Endpoints públicos: health check, métricas de Prometheus y documentación Swagger
                .requestMatchers("/health", "/health/**", "/prometheus").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                // 2. Auth público por diseño: todavía no hay sesión al pedir el token
                //    CSRF ni al iniciar sesión. En refresh y logout la credencial es el
                //    propio refresh token (tienen que funcionar con el access token
                //    vencido) y en web el CSRF sigue activo. Solo estas rutas; todo lo
                //    demás de /api/v1/auth (incluido /me) exige autenticación.
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh",
                        "/api/v1/auth/logout").permitAll()
                // 3. Recuperar la contraseña: quien la olvidó no tiene sesión. La credencial
                //    para cambiarla es el token de un solo uso; en web el CSRF sigue activo.
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/password/forgot",
                        "/api/v1/auth/password/reset", "/api/v1/auth/invitation/accept").permitAll()
                // 4. Cualquier otra ruta es privada por defecto
                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsAllowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-XSRF-TOKEN", "X-Client-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}