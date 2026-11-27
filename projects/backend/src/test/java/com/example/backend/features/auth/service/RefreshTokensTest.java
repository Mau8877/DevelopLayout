package com.example.backend.features.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class RefreshTokensTest {

    private static final String CLAVE = "clave-de-refresh-para-tests-123456789";
    private static final String CLAVE_JWT = "clave-de-jwt-para-tests-1234567890123";

    private final RefreshTokens tokens = new RefreshTokens(CLAVE, CLAVE_JWT);

    @Test
    void generar_incluyeLaSesionYCadaTokenEsDistinto() {
        UUID sesion = UUID.randomUUID();
        String uno = tokens.generar(sesion);

        assertThat(tokens.sesionDe(uno)).contains(sesion);
        assertThat(tokens.generar(sesion)).isNotEqualTo(uno);
        assertThat(uno).hasSizeLessThanOrEqualTo(200);
    }

    @Test
    void alterarCualquierCaracter_invalidaElToken() {
        String token = tokens.generar(UUID.randomUUID());

        for (int i = 0; i < token.length(); i++) {
            char otro = token.charAt(i) == 'A' ? 'B' : 'A';
            assertThat(tokens.sesionDe(token.substring(0, i) + otro + token.substring(i + 1)))
                    .as("posición %d", i).isEmpty();
        }
    }

    @Test
    void firmadoConOtraClave_oBasura_noVale() {
        String ajeno = new RefreshTokens("otra-clave-distinta-para-tests-123456", CLAVE_JWT)
                .generar(UUID.randomUUID());

        for (String falso : new String[] {ajeno, null, "", ".", "a.b.c", "sin-puntos", "x".repeat(10_000)}) {
            assertThat(tokens.sesionDe(falso)).isEmpty();
        }
    }

    @Test
    void claveCortaDeEjemploOIgualALaDeJwt_noArranca() {
        for (String mala : new String[] {null, "corta", "CAMBIAME_MINIMO_32_CARACTERES_ALEATORIOS", CLAVE_JWT}) {
            assertThatThrownBy(() -> new RefreshTokens(mala, CLAVE_JWT)).isInstanceOf(IllegalStateException.class);
        }
    }
}
