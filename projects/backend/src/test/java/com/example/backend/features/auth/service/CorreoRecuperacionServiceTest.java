package com.example.backend.features.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class CorreoRecuperacionServiceTest {

    private final JavaMailSender mail = mock(JavaMailSender.class);

    private CorreoRecuperacionService servicio(String servidor, String remitente, String url) {
        return new CorreoRecuperacionService(mail, servidor, remitente, url);
    }

    @Test
    void enviarEnlace_poneElTokenEnElFragmentoDeUnCorreoDeTexto() {
        servicio("smtp.test", "no-reply@test.local", "https://app.test/")
                .enviarEnlace("persona@test.local", "abc123");

        ArgumentCaptor<SimpleMailMessage> enviado = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(enviado.capture());
        assertThat(enviado.getValue().getTo()).containsExactly("persona@test.local");
        assertThat(enviado.getValue().getFrom()).isEqualTo("no-reply@test.local");
        assertThat(enviado.getValue().getText()).contains("https://app.test/reset-password#token=abc123")
                .doesNotContain("?token=");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "''        | a@b.test | https://app.test | SPRING_MAIL_HOST",
        "smtp.test | ''       | https://app.test | MAIL_FROM",
        "smtp.test | a@b.test | ''               | APP_FRONTEND_BASE_URL",
    })
    void sinConfiguracion_noArrancaYDiceQueFalta(String servidor, String remitente, String url, String variable) {
        assertThatThrownBy(() -> servicio(servidor, remitente, url))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining(variable);
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://app.midominio.com", "ftp://app.test", "app.test", "https://",
        "https://app.test?x=1", "https://app.test#x", "https://usuario@app.test"})
    void urlInsegura_noArranca(String url) {
        assertThatThrownBy(() -> servicio("smtp.test", "a@b.test", url))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("APP_FRONTEND_BASE_URL");
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:5173", "http://127.0.0.1", "https://app.midominio.com"})
    void urlLocalSinTlsOHttps_arranca(String url) {
        assertThatCode(() -> servicio("smtp.test", "a@b.test", url)).doesNotThrowAnyException();
    }
}
