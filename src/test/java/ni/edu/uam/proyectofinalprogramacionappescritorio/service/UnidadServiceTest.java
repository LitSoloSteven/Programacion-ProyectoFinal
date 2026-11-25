package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propietario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnidadServiceTest {
    @Test
    void asistenteEditaCondicionesSinModificarCamposProtegidos() throws Exception {
        Properties config = new Properties();
        config.setProperty("hibernate.connection.url", "jdbc:h2:mem:unidad_" + UUID.randomUUID()
                + ";MODE=PostgreSQL");
        config.setProperty("hibernate.connection.username", "sa");
        config.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        try (var factory = HibernateUtil.construir(config)) {
            HibernateTransactions tx = new HibernateTransactions(factory);
            Unidad unidad = tx.ejecutar(session -> {
                Usuario titular = new Usuario("titular-test", "hash", Rol.PROPIETARIO);
                Usuario asistente = new Usuario("asistente-test", "hash", Rol.ASISTENTE);
                session.persist(titular);
                session.persist(asistente);
                Propietario perfil = new Propietario(titular, "Titular", "titular@example.test", null);
                session.persist(perfil);
                Propiedad propiedad = new Propiedad();
                propiedad.setNombre("Propiedad de prueba");
                propiedad.setDireccion("Calle sintética");
                propiedad.setPropietario(perfil);
                session.persist(propiedad);
                Unidad u = new Unidad();
                u.setPropiedad(propiedad);
                u.setCodigo(" U-01 ");
                session.persist(u);
                SesionUsuario.establecer(SesionUsuario.paraSesionAutenticada(asistente.getId(),
                        asistente.getNombre(), asistente.getRol(), asistente.getVersion()));
                return u;
            });
            UnidadService servicio = new UnidadService(tx);
            BigDecimal canon = new BigDecimal("100.00");
            assertThrows(SecurityException.class, () -> servicio.actualizar(unidad.getId(),
                    unidad.getVersion(), unidad.getPropiedadId(), "U-02", "Cambio", "Apartamento",
                    "Disponible", canon, "NIO"));
            servicio.actualizar(unidad.getId(), unidad.getVersion(), unidad.getPropiedadId(),
                    "U-01", "Condiciones", "Apartamento", "Disponible", canon, "NIO");
            Unidad actual = tx.ejecutar(session -> session.find(Unidad.class, unidad.getId()));
            assertEquals(" U-01 ", actual.getCodigo());
            assertEquals("Condiciones", actual.getDescripcion());
            assertEquals("NIO", actual.getMoneda());
        } finally {
            SesionUsuario.cerrarSesion();
        }
    }
}
