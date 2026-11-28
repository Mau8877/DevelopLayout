package com.example.backend.common.idioma;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Las reglas de IDIOMAS_BACKEND.md que se pueden comprobar sin levantar la aplicación. */
class ArchivosDeIdiomaTest {

    private static final Path RECURSOS = Path.of("src", "main", "resources", "i18n");
    private static final Path CODIGO = Path.of("src", "main", "java");
    private static final Pattern CLAVE_USADA = Pattern.compile(
            "[\"{]((?:common|auth|usuarios|bitacora)\\.(?:error|exito|validacion)\\.[A-Za-z]+)[\"}]");
    private static final Pattern CORREO_USADO = Pattern.compile("\"(auth\\.correo\\.[a-z]+)\"");
    private static final Pattern MARCADOR = Pattern.compile("\\{\\d+}");
    private static final Pattern VOSEO = Pattern.compile(
            "(?<![\\p{L}])(vos|podés|tenés|querés|sos|iniciá|elegí|ingresá|revisá|abrí|probá|usá|pedí|pedile|creá|"
                    + "cambiá|confirmá|guardá|ignorá|activá|intentá|reenviá|reactivala|restablecé|fuiste vos)(?![\\p{L}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Una frase escrita donde debería ir una clave. */
    private static final List<Pattern> FRASES_FIJAS = List.of(
            Pattern.compile("new \\w*(?:Validation|Unauthorized|Forbidden|NotFound|Conflict|Locked)Exception\\("
                    + "\\s*[\\w.\"]*ERR_\\w+\"?\\s*,\\s*\"[^\"]* [^\"]*\""),
            Pattern.compile("message\\s*=\\s*\"(?!\\{)[^\"]*\""),
            Pattern.compile("ApiResponse\\.(?:success|successList)\\([^;]*,\\s*\"[^\"]* [^\"]*\"\\s*[,)]"),
            Pattern.compile("ApiResponse\\.error\\([^;]*,\\s*\"[^\"]* [^\"]*\"\\s*\\)"));

    private static Map<String, String> leer(String modulo, String archivo) {
        Properties propiedades = new Properties();
        try (Reader lector = Files.newBufferedReader(RECURSOS.resolve(modulo).resolve(archivo),
                StandardCharsets.UTF_8)) {
            propiedades.load(lector);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Map<String, String> mapa = new TreeMap<>();
        propiedades.stringPropertyNames().forEach(clave -> mapa.put(clave, propiedades.getProperty(clave)));
        return mapa;
    }

    private static List<String> modulos() {
        try (Stream<Path> carpetas = Files.list(RECURSOS)) {
            return carpetas.filter(Files::isDirectory).map(p -> p.getFileName().toString()).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Map<Path, String> fuentes() {
        try (Stream<Path> rutas = Files.walk(CODIGO)) {
            Map<Path, String> mapa = new TreeMap<>();
            for (Path ruta : rutas.filter(p -> p.toString().endsWith(".java")).toList()) {
                mapa.put(ruta, Files.readString(ruta, StandardCharsets.UTF_8));
            }
            return mapa;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> marcadores(String valor) {
        return MARCADOR.matcher(valor).results().map(r -> r.group()).sorted().toList();
    }

    @ParameterizedTest
    @ValueSource(strings = {"common", "auth", "usuarios", "bitacora"})
    void espanolEInglesTienenLasMismasClavesYMarcadores(String modulo) {
        Map<String, String> es = leer(modulo, "messages.properties");
        Map<String, String> en = leer(modulo, "messages_en.properties");

        assertThat(en.keySet()).isEqualTo(es.keySet());
        es.forEach((clave, valor) -> {
            assertThat(clave).startsWith(modulo + ".");
            assertThat(valor).as(clave).isNotBlank();
            assertThat(en.get(clave)).as(clave).isNotBlank();
            assertThat(marcadores(en.get(clave))).as(clave).isEqualTo(marcadores(valor));
            // * Con argumentos se usa MessageFormat, donde un apóstrofo suelto se come el texto.
            assertThat(valor + en.get(clave)).as(clave).doesNotContain("'");
        });
    }

    @Test
    void todosLosModulosEstanRegistrados() {
        assertThat(Mensajes.crearFuente().getBasenameSet())
                .containsExactlyInAnyOrderElementsOf(modulos().stream().map(m -> "i18n/" + m + "/messages").toList());
    }

    @Test
    void cadaClaveUsadaExiste_yCadaClaveSeUsa() {
        Set<String> definidas = new TreeSet<>();
        modulos().forEach(m -> definidas.addAll(leer(m, "messages.properties").keySet()));
        Set<String> usadas = new TreeSet<>();
        fuentes().values().forEach(texto -> {
            Matcher claves = CLAVE_USADA.matcher(texto);
            while (claves.find()) {
                usadas.add(claves.group(1));
            }
            Matcher correos = CORREO_USADO.matcher(texto);
            while (correos.find()) {
                usadas.add(correos.group(1) + ".asunto");
                usadas.add(correos.group(1) + ".cuerpo");
            }
        });

        assertThat(definidas).containsAll(usadas);
        assertThat(usadas).containsAll(definidas);
    }

    @Test
    void noHayFrasesFijasEnExcepcionesValidacionesNiRespuestas() {
        List<String> halladas = new ArrayList<>();
        fuentes().forEach((ruta, texto) -> FRASES_FIJAS.forEach(patron -> patron.matcher(texto).results()
                .forEach(r -> halladas.add(ruta.getFileName() + ": " + r.group().replaceAll("\\s+", " ")))));

        assertThat(halladas).isEmpty();
    }

    @Test
    void elEspanolNoTieneVoseo() {
        List<String> conVoseo = new ArrayList<>();
        modulos().forEach(m -> leer(m, "messages.properties").forEach((clave, valor) -> {
            if (VOSEO.matcher(valor).find()) {
                conVoseo.add(clave + ": " + valor);
            }
        }));

        assertThat(conVoseo).isEmpty();
    }
}
