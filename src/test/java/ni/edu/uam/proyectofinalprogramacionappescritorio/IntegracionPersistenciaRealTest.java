package ni.edu.uam.proyectofinalprogramacionappescritorio;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.ConexionBD;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.DatabaseInitializer;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.PropiedadService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de verificación de persistencia real contra PostgreSQL 18.
 * Garantiza que no existen mocks, simulaciones ni datos volátiles en memoria,
 * verificando el almacenamiento directo en la base de datos 'latch'.
 */
public class IntegracionPersistenciaRealTest {

    private static PropiedadService propiedadService;

    @BeforeAll
    static void setup() {
        DatabaseInitializer.inicializarBaseDeDatos();
        propiedadService = new PropiedadService();

        Usuario admin = new Usuario(UUID.randomUUID(), "Propietario", "admin", "Admin Integración", true);
        SesionUsuario.setUsuarioActual(admin);
    }

    @Test
    @DisplayName("Verificar que la persistencia Service -> DAO escribe físicamente en PostgreSQL")
    void testPersistenciaFisicaEnPostgreSQL() throws SQLException {
        String nombreUnico = "Inmueble PG-" + System.currentTimeMillis();
        String direccion = "Carretera Norte Km 9, Managua";

        // 1. Guardar a través de la capa de Servicio
        Propiedad prop = new Propiedad();
        prop.setNombre(nombreUnico);
        prop.setDireccion(direccion);
        prop.setActiva(true);

        propiedadService.guardar(prop);
        assertNotNull(prop.getId());

        // 2. Comprobar consulta física directa por JDBC SQL nativo contra PostgreSQL
        String sql = "SELECT nombre, direccion, activa FROM latch.propiedad WHERE id = ?;";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, prop.getId());
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "El registro debe existir físicamente en la tabla latch.propiedad de PostgreSQL");
                assertEquals(nombreUnico, rs.getString("nombre"));
                assertEquals(direccion, rs.getString("direccion"));
                assertTrue(rs.getBoolean("activa"));
            }
        }

        // 3. Limpiar registro de prueba
        propiedadService.eliminar(prop.getId());

        // 4. Confirmar borrado físico en PostgreSQL
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, prop.getId());
            try (ResultSet rs = ps.executeQuery()) {
                assertFalse(rs.next(), "El registro no debe existir tras ser eliminado");
            }
        }
    }

    @Test
    @DisplayName("Verificar existencia de esquema 'latch' y tablas reales en information_schema")
    void testVerificarTablasEnInformationSchema() throws SQLException {
        String[] tablasEsperadas = {"rol", "usuario", "propiedad", "unidad", "inquilino", "contrato", "cargo", "pago"};

        String sql = "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'latch' AND table_name = ?;";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (String tabla : tablasEsperadas) {
                ps.setString(1, tabla);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1), "La tabla real latch." + tabla + " debe existir en PostgreSQL");
                }
            }
        }
    }
}
