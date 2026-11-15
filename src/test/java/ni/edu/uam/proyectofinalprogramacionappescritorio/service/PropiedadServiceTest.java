package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propietario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PropiedadServiceTest {
    private SessionFactory factory;
    private HibernateTransactions tx;
    private PropiedadService servicio;
    private UUID propiedadId;
    private long propietarioId;
    private long otroPropietarioId;
    private long asistenteId;
    private SesionUsuario propietario;
    private SesionUsuario asistente;

    @BeforeEach
    void iniciar() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        properties.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:propiedades_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        properties.setProperty("hibernate.connection.username", "sa");
        properties.setProperty("hibernate.connection.password", "");
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        factory = HibernateUtil.construir(properties);
        tx = new HibernateTransactions(factory);
        servicio = new PropiedadService(tx);

        tx.ejecutar(session -> {
            Usuario usuarioPropietario = new Usuario("owner-" + token(), "hash", Rol.PROPIETARIO);
            Usuario usuarioOtroPropietario = new Usuario("owner2-" + token(), "hash", Rol.PROPIETARIO);
            Usuario usuarioAsistente = new Usuario("assistant-" + token(), "hash", Rol.ASISTENTE);
            session.persist(usuarioPropietario);
            session.persist(usuarioOtroPropietario);
            session.persist(usuarioAsistente);
            session.flush();
            propietarioId = usuarioPropietario.getId();
            otroPropietarioId = usuarioOtroPropietario.getId();
            asistenteId = usuarioAsistente.getId();

            Propietario propietario = new Propietario(usuarioPropietario,
                    "Ana Propietaria", correo("ana"), "+505 8888-1111");
            Propietario otroPropietario = new Propietario(usuarioOtroPropietario,
                    "Luis Propietario", correo("luis"), "+505 8888-2222");
            session.persist(propietario);
            session.persist(otroPropietario);

            Propiedad propiedad = new Propiedad();
            propiedad.setNombre("Casa original");
            propiedad.setDireccion("Calle original");
            propiedad.setPropietario(propietario);
            session.persist(propiedad);
            session.flush();
            propiedadId = propiedad.getId();
            return null;
        });

        propietario = SesionUsuario.paraSesionAutenticada(
                propietarioId, "owner", Rol.PROPIETARIO);
        asistente = SesionUsuario.paraSesionAutenticada(
                asistenteId, "assistant", Rol.ASISTENTE);
    }

    @AfterEach
    void cerrar() {
        SesionUsuario.cerrarSesion();
        if (factory != null) factory.close();
    }

    @Test
    void edicionNormalConservaDuenioAunqueLaGuardeUnAsistente() throws Exception {
        guardarComo(asistente, edicion(null));

        assertEquals(propietarioId, leerPropietarioId());
        assertEquals("Casa actualizada", leerPropiedad().getNombre());
        assertNull(tx.ejecutar(session -> session.find(Propietario.class, asistenteId)));
    }

    @Test
    void propietarioPuedeCambiarDuenioConUnaSolicitudExplicita() throws Exception {
        Propietario destino = tx.ejecutar(
                session -> session.find(Propietario.class, otroPropietarioId));

        Propiedad edicion = edicion(destino);
        guardarComo(propietario, edicion);

        assertEquals(otroPropietarioId, leerPropietarioId());
    }

    @Test
    void asistenteNoPuedeCambiarDuenioYLaOperacionNoDejaCambios() throws Exception {
        Propietario destino = tx.ejecutar(
                session -> session.find(Propietario.class, otroPropietarioId));
        Propiedad edicion = edicion(destino);

        assertThrows(SecurityException.class, () -> guardarComo(asistente, edicion));

        assertEquals(propietarioId, leerPropietarioId());
        assertEquals("Casa original", leerPropiedad().getNombre());
    }

    @Test
    void noEliminaPropiedadMientrasTengaUnidadesAsociadas() {
        UUID unidadId = tx.ejecutar(session -> {
            Unidad unidad = new Unidad();
            unidad.setPropiedadId(propiedadId);
            unidad.setCodigo("U-01");
            unidad.setCanonReferencia(new BigDecimal("100.00"));
            session.persist(unidad);
            session.flush();
            return unidad.getId();
        });

        SesionUsuario.ejecutarComo(propietario, () -> assertThrows(
                ConflictoDatosException.class, () -> servicio.eliminar(propiedadId)));

        assertNotNull(tx.ejecutar(session -> session.find(Propiedad.class, propiedadId)));
        assertNotNull(tx.ejecutar(session -> session.find(Unidad.class, unidadId)));
    }

    @Test
    void eliminaPropiedadSinUnidadesAsociadas() {
        SesionUsuario.ejecutarComo(propietario, () -> assertDoesNotThrow(
                () -> servicio.eliminar(propiedadId)));

        assertNull(leerPropiedad());
    }

    private Propiedad edicion(Propietario propietarioDestino) {
        Propiedad actual = leerPropiedad();
        Propiedad propiedad = new Propiedad();
        propiedad.setId(propiedadId);
        propiedad.setVersion(actual.getVersion());
        propiedad.setNombre("Casa actualizada");
        propiedad.setDireccion("Calle actualizada");
        propiedad.setActiva(true);
        propiedad.setPropietario(propietarioDestino);
        return propiedad;
    }

    private void guardarComo(SesionUsuario identidad, Propiedad propiedad) throws SQLException {
        try {
            SesionUsuario.ejecutarComo(identidad, () -> {
                try {
                    servicio.guardar(propiedad, "Ana Propietaria", correo("ana"),
                            "+505 8888-1111");
                } catch (SQLException e) {
                    throw new WrappedSqlException(e);
                }
            });
        } catch (WrappedSqlException e) {
            throw e.sqlException;
        }
    }

    private long leerPropietarioId() {
        return tx.ejecutar(session -> session.createQuery(
                        "select p.propietario.id from Propiedad p where p.id = :id", Long.class)
                .setParameter("id", propiedadId)
                .getSingleResult());
    }

    private Propiedad leerPropiedad() {
        return tx.ejecutar(session -> session.find(Propiedad.class, propiedadId));
    }

    private static String token() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String correo(String nombre) {
        return nombre + "-" + token() + "@latch.test";
    }

    private static final class WrappedSqlException extends RuntimeException {
        private final SQLException sqlException;

        private WrappedSqlException(SQLException sqlException) {
            this.sqlException = sqlException;
        }
    }
}
