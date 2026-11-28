package com.example.backend.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ContrasenasComunesTest {

    @Test
    void laLista_estaCompleta() {
        // * Un recurso truncado dejaría pasar casi todo sin que nada fallara.
        assertThat(ContrasenasComunes.cantidad()).isGreaterThan(46_000);
    }

    @Test
    void reconoceLasMuyUsadas_sinDistinguirMayusculas() {
        assertThat(ContrasenasComunes.contiene("password")).isTrue(); // gitleaks:allow
        assertThat(ContrasenasComunes.contiene("PassWord")).isTrue(); // gitleaks:allow
        assertThat(ContrasenasComunes.contiene("12345678")).isTrue(); // gitleaks:allow
        assertThat(ContrasenasComunes.contiene("qwertyuiop")).isTrue(); // gitleaks:allow
    }

    @Test
    void dejaPasarUnaFraseQueNoEstaEnLaLista() {
        assertThat(ContrasenasComunes.contiene("rio-lampara-93-nube")).isFalse(); // gitleaks:allow
    }
}
