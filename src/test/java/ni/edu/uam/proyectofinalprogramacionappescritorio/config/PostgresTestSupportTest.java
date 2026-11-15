package ni.edu.uam.proyectofinalprogramacionappescritorio.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresTestSupportTest {
    @Test
    void separaMigracionesSinPartirBloquesDollar() throws IOException {
        List<String> tasas = PostgresTestSupport.separarSentencias(
                Files.readString(Path.of("docs", "migrations", "V4__tasas_autorizadas.sql")));
        List<String> recibos = PostgresTestSupport.separarSentencias(
                Files.readString(Path.of("docs", "migrations", "V8__consecutivo_recibos_transaccional.sql")));

        assertEquals(2, tasas.size());
        assertEquals(5, recibos.size());
        assertTrue(recibos.get(4).contains("CREATE OR REPLACE FUNCTION public.fn_reservar_numero()"));
        assertTrue(recibos.get(4).contains("RETURN reservado;"), recibos.toString());
    }
}
