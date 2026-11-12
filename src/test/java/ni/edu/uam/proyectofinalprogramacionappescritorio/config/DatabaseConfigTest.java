package ni.edu.uam.proyectofinalprogramacionappescritorio.config;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private Map<String, String> entorno() {
        return Map.of("LATCH_DB_URL", "jdbc:postgresql://localhost:5432/latch_db",
                "LATCH_DB_USER", "usuario", "LATCH_DB_PASSWORD", "sistema");
    }
}
