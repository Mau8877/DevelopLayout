package com.example.backend.features.usuarios;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

/** Apoyo de las pruebas que envían correo: lo captura en una bandeja. */
abstract class InvitacionesIntegrationTest extends AdminUsuariosIntegrationTest {

    private static final Pattern ENLACE =
            Pattern.compile("http://localhost:5173/accept-invitation#token=([A-Za-z0-9_-]+)");

    @MockitoBean
    protected JavaMailSender mail;

    protected final List<SimpleMailMessage> bandeja = new CopyOnWriteArrayList<>();

    @BeforeEach
    void capturarCorreos() {
        bandeja.clear();
        doAnswer(llamada -> bandeja.add(llamada.getArgument(0))).when(mail).send(any(SimpleMailMessage.class));
    }

    protected ResultActions crear(Cuenta actor, String json) throws Exception {
        return mvc.perform(como(actor, conJson(post("/api/v1/usuarios"), json)));
    }

    protected ResultActions invitar(String email) throws Exception {
        return crear(admin, "{\"email\":\"" + email + "\",\"roles\":[\"USUARIO\"]}");
    }

    protected static UUID codigoCreado(ResultActions respuesta) throws Exception {
        return UUID.fromString(leer(respuesta, "$.data.usuario.codigo"));
    }

    /** El token del último correo de invitación. */
    protected String ultimoToken() {
        Matcher enlace = ENLACE.matcher(bandeja.getLast().getText());
        if (!enlace.find()) {
            throw new AssertionError("El último correo no trae un enlace de invitación");
        }
        return enlace.group(1);
    }
}
