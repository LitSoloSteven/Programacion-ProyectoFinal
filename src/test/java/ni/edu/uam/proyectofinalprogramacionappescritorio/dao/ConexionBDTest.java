package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class ConexionBDTest {

    @Test
    void testConexionYEsquema() {
        assertDoesNotThrow(() -> {
            try (Connection conn = ConexionBD.obtenerConexion()) {
                assertNotNull(conn, "La conexión no debe ser nula");
                assertFalse(conn.isClosed(), "La conexión debe estar abierta");
            }
        });
    }

    @Test
    void testInicializacionBaseDeDatos() {
        assertDoesNotThrow(() -> {
            DatabaseInitializer.inicializarBaseDeDatos();
            try (Connection conn = ConexionBD.obtenerConexion();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT count(*) FROM latch.propiedad;")) {
                assertTrue(rs.next());
                assertTrue(rs.getInt(1) >= 0);
            }
        });
    }
}
