package ni.edu.uam.proyectofinalprogramacionappescritorio.config;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {
    @TempDir Path temporal;

    @Test void cargaArchivoLocalSinAlterarCaracteresDePassword() throws Exception {
        Path archivo = temporal.resolve(".env");
        Files.writeString(archivo, "\uFEFF# Configuración sintética\n\n"
                + "LATCH_DB_URL=jdbc:postgresql://localhost:5432/latch_db\n"
                + "LATCH_DB_USER='usuario_local'\n"
                + "LATCH_DB_PASSWORD=\"clave#con=signos\\literal\"\n"
                + "LATCH_DB_SCHEMA_MODE=update\n");
        Properties p = DatabaseConfig.desdeArchivo(archivo, Map.of());
        assertEquals("usuario_local", p.getProperty("hibernate.connection.username"));
        assertEquals("clave#con=signos\\literal", p.getProperty("hibernate.connection.password"));
        assertEquals("update", p.getProperty("hibernate.hbm2ddl.auto"));
    }

    @Test void variablesDelSistemaTienenPrioridad() throws Exception {
        Path archivo = temporal.resolve(".env");
        Files.writeString(archivo, "LATCH_DB_PASSWORD=archivo\n");
        Properties p = DatabaseConfig.desdeArchivo(archivo, entorno());
        assertEquals("sistema", p.getProperty("hibernate.connection.password"));
    }

    @Test void archivoAusentePermiteUsarSoloVariables() {
        Properties p = DatabaseConfig.desdeArchivo(temporal.resolve("ausente.env"), entorno());
        assertEquals("sistema", p.getProperty("hibernate.connection.password"));
        assertThrows(IllegalArgumentException.class,
                () -> DatabaseConfig.desdeArchivo(temporal.resolve("ausente.env"), Map.of()));
    }

    @Test void errorDeFormatoNoRevelaElContenido() throws Exception {
        Path archivo = temporal.resolve(".env");
        Files.writeString(archivo, "# comentario\nLATCH_DB_PASSWORD=\"secreto-sintetico\n");
        String error = assertThrows(IllegalArgumentException.class,
                () -> DatabaseConfig.desdeArchivo(archivo, entorno())).getMessage();
        assertTrue(error.contains("línea 2"));
        assertFalse(error.contains("secreto-sintetico"));
    }

    @Test void aceptaConexionLocalSinCambiarLaConfiguracionActual() {
        Properties p = DatabaseConfig.desdeEntorno(entorno());
        assertEquals("jdbc:postgresql://localhost:5432/latch_db", p.getProperty("hibernate.connection.url"));
    }

    @Test void limitaLosParametrosDeLaUrlYRechazaDuplicados() {
        assertThrows(IllegalArgumentException.class,
                () -> DatabaseConfig.desdeEntorno(entornoConUrl("jdbc:postgresql://localhost:5432/latch_db?currentSchema=public")));
        assertThrows(IllegalArgumentException.class,
                () -> DatabaseConfig.desdeEntorno(entornoConUrl("jdbc:postgresql://localhost:5432/latch_db?sslmode=disable&sslmode=require")));
        assertThrows(IllegalArgumentException.class,
                () -> DatabaseConfig.desdeEntorno(entornoConUrl("jdbc:postgresql://usuario@localhost:5432/latch_db")));
    }

    @Test void conexionesRemotasExigenModoConVerificacion() {
        assertThrows(IllegalArgumentException.class,
                () -> DatabaseConfig.desdeEntorno(entornoConUrl("jdbc:postgresql://db.example.com:5432/latch_db")));
        assertThrows(IllegalArgumentException.class,
                () -> DatabaseConfig.desdeEntorno(entornoConUrl("jdbc:postgresql://db.example.com:5432/latch_db?sslmode=require")));
        assertThrows(IllegalArgumentException.class,
                () -> DatabaseConfig.desdeEntorno(entornoConUrl("jdbc:postgresql://db.example.com:5432/latch_db?sslmode=disable")));
    }

    @Test void modoVerificadoRemotoExigeCertificadoRaizExistente() throws Exception {
        Path certificado = temporal.resolve("root.crt");
        Files.writeString(certificado, "certificado sintético");
        String ruta = URLEncoder.encode(certificado.toString(), StandardCharsets.UTF_8).replace("+", "%20");
        String url = "jdbc:postgresql://db.example.com:5432/latch_db?sslmode=verify-full&sslrootcert=" + ruta;

        Properties p = DatabaseConfig.desdeEntorno(entornoConUrl(url));
        String urlConfigurada = p.getProperty("hibernate.connection.url");
        assertTrue(urlConfigurada.startsWith("jdbc:postgresql://db.example.com:5432/latch_db?sslmode=verify-full&sslrootcert="));
        assertEquals(certificado.normalize().toString(), URLDecoder.decode(
                urlConfigurada.substring(urlConfigurada.indexOf("sslrootcert=") + "sslrootcert=".length()), StandardCharsets.UTF_8));
        Properties verificacionCa = DatabaseConfig.desdeEntorno(entornoConUrl(
                "jdbc:postgresql://db.example.com:5432/latch_db?sslmode=verify-ca&sslrootcert=" + ruta));
        assertTrue(verificacionCa.getProperty("hibernate.connection.url").contains("sslmode=verify-ca"));
        assertThrows(IllegalArgumentException.class,
                () -> DatabaseConfig.desdeEntorno(entornoConUrl("jdbc:postgresql://db.example.com:5432/latch_db?sslmode=verify-full")));
        assertThrows(IllegalArgumentException.class,
                () -> DatabaseConfig.desdeEntorno(entornoConUrl("jdbc:postgresql://db.example.com:5432/latch_db?sslmode=verify-full&sslrootcert=" + ruta + ".missing")));
    }

    @Test void requireSoloSePermiteEnHostLocalYElCertificadoSoloConVerificacion() {
        for (String modo : new String[] {"disable", "allow", "prefer", "require"}) {
            Properties p = DatabaseConfig.desdeEntorno(entornoConUrl(
                    "jdbc:postgresql://127.0.0.1:5432/latch_db?sslmode=" + modo));
            assertEquals("jdbc:postgresql://127.0.0.1:5432/latch_db?sslmode=" + modo,
                    p.getProperty("hibernate.connection.url"));
        }
        assertThrows(IllegalArgumentException.class,
                () -> DatabaseConfig.desdeEntorno(entornoConUrl("jdbc:postgresql://localhost:5432/latch_db?sslrootcert=C%3A%5Croot.crt")));
    }

    private Map<String, String> entorno() {
        return entornoConUrl("jdbc:postgresql://localhost:5432/latch_db");
    }

    private Map<String, String> entornoConUrl(String url) {
        return Map.of("LATCH_DB_URL", url,
                "LATCH_DB_USER", "usuario", "LATCH_DB_PASSWORD", "sistema");
    }
}
