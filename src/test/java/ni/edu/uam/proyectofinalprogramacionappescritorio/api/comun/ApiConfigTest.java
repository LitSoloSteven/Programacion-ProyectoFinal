package ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiConfigTest {
    @TempDir Path temporal;

    @Test
    void leePuertoDelArchivoYCombinaEntorno() throws Exception {
        Path archivo = temporal.resolve(".env");
        Files.writeString(archivo, "LATCH_API_PORT=8090\n");

        ApiConfig config = ApiConfig.desdeArchivo(archivo, Map.of());

        assertEquals("127.0.0.1", config.host());
        assertEquals(8090, config.puerto());
    }

    @Test
    void elEntornoTienePrioridadSobreElArchivo() throws Exception {
        Path archivo = temporal.resolve(".env");
        Files.writeString(archivo, "LATCH_API_PORT=8090\n");

        ApiConfig config = ApiConfig.desdeArchivo(archivo,
                Map.of("LATCH_API_PORT", "9090"));

        assertEquals(9090, config.puerto());
    }

    @Test
    void exigePuertoConfigurado() {
        assertThrows(IllegalArgumentException.class,
                () -> ApiConfig.desdeArchivo(temporal.resolve("ausente.env"), Map.of()));
    }

    @Test
    void rechazaPuertoInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> ApiConfig.paraPruebas(70000));
        assertThrows(IllegalArgumentException.class,
                () -> new ApiConfig("0.0.0.0", 8090));
    }
}