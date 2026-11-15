package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PasswordHasher;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioServiceTest {
    private static SessionFactory factory;
    private static HibernateTransactions transactions;
    private static Long propietarioId;
    private static String nombrePropietario;

    @BeforeAll
    static void iniciar() {
        Properties propiedades = new Properties();
        propiedades.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        propiedades.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:latch_usuario_service_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        propiedades.setProperty("hibernate.connection.username", "sa");
        propiedades.setProperty("hibernate.connection.password", "");
        propiedades.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        propiedades.setProperty("hibernate.jdbc.time_zone", "UTC");
        propiedades.setProperty("hibernate.show_sql", "false");
        factory = HibernateUtil.construir(propiedades);
        transactions = new HibernateTransactions(factory);

        nombrePropietario = "gestion." + UUID.randomUUID().toString().substring(0, 8);
        String hash = new PasswordHasher().crear("ClaveSintetica-2026".toCharArray());
        propietarioId = transactions.ejecutar(session -> {
            Usuario propietario = new Usuario(nombrePropietario, hash, Rol.PROPIETARIO);
            session.persist(propietario);
            return propietario.getId();
        });
    }

    @AfterAll
    static void cerrar() {
        if (factory != null && !factory.isClosed()) factory.close();
    }

    @Test
    void cambiarRolIncrementaVersionYRegistraAuditoria() {
        Long asistenteId = crearUsuario(Rol.ASISTENTE);
        long versionAnterior = obtener(asistenteId).version();

        new UsuarioService(transactions).cambiarRol(actor(), asistenteId, Rol.PROPIETARIO);

        EstadoUsuario actualizado = obtener(asistenteId);
        assertEquals(Rol.PROPIETARIO, actualizado.rol());
        assertTrue(actualizado.version() > versionAnterior);
        long cantidadAuditoria = transactions.ejecutar(session -> session.createQuery(
                "select count(a) from Auditoria a where a.accion = 'CAMBIAR_ROL_USUARIO'", Long.class)
                .getSingleResult());
        assertEquals(1L, cantidadAuditoria);
    }

    @Test
    void desactivarIncrementaVersion() {
        Long asistenteId = crearUsuario(Rol.ASISTENTE);
        long versionAnterior = obtener(asistenteId).version();

        new UsuarioService(transactions).desactivar(actor(), asistenteId);

        EstadoUsuario desactivado = obtener(asistenteId);
        assertFalse(desactivado.activo());
        assertTrue(desactivado.version() > versionAnterior);
    }

    @Test
    void rechazaCambioDeRolPropioORolInvalido() {
        UsuarioService usuarios = new UsuarioService(transactions);
        assertThrows(IllegalArgumentException.class,
                () -> usuarios.cambiarRol(actor(), propietarioId, Rol.ASISTENTE));

        Long asistenteId = crearUsuario(Rol.ASISTENTE);
        assertThrows(IllegalArgumentException.class,
                () -> usuarios.cambiarRol(actor(), asistenteId, "DESCONOCIDO"));
        assertEquals(Rol.ASISTENTE, obtener(asistenteId).rol());
    }

    private static SesionUsuario actor() {
        return SesionUsuario.paraSesionAutenticada(propietarioId, nombrePropietario, Rol.PROPIETARIO);
    }

    private static Long crearUsuario(String rol) {
        String nombre = "usuario." + UUID.randomUUID().toString().substring(0, 8);
        String hash = new PasswordHasher().crear("ClaveSintetica-2026".toCharArray());
        return transactions.ejecutar(session -> {
            Usuario usuario = new Usuario(nombre, hash, rol);
            session.persist(usuario);
            return usuario.getId();
        });
    }

    private static EstadoUsuario obtener(Long id) {
        return transactions.ejecutar(session -> {
            Usuario usuario = session.find(Usuario.class, id);
            return new EstadoUsuario(usuario.getRol(), usuario.isActivo(), usuario.getVersion());
        });
    }

    private record EstadoUsuario(String rol, boolean activo, long version) { }
}
