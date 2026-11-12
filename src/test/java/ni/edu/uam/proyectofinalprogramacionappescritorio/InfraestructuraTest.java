package ni.edu.uam.proyectofinalprogramacionappescritorio;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.*;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.*;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.*;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PasswordHasher;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class InfraestructuraTest {
    private static SessionFactory factory;
    private static HibernateTransactions tx;
    private static String hash;
    private static final char[] PASSWORD = "ClaveSintetica-2026".toCharArray();

    @BeforeAll static void iniciar() {
        Properties p;
        if (Boolean.getBoolean("latch.test.postgres")) {
            // Usar exclusivamente una base desechable: las tablas se crean y eliminan en esta suite.
            p = DatabaseConfig.desdeEntorno(System.getenv());
        } else {
            p = new Properties();
            p.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
            p.setProperty("hibernate.connection.url", "jdbc:h2:mem:latch_" + UUID.randomUUID() + ";MODE=PostgreSQL");
            p.setProperty("hibernate.connection.username", "sa");
            p.setProperty("hibernate.connection.password", "");
        }
        p.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        p.setProperty("hibernate.jdbc.time_zone", "UTC");
        factory = HibernateUtil.construir(p);
        tx = new HibernateTransactions(factory);
        hash = new PasswordHasher().crear(PASSWORD);
    }

    @AfterAll static void cerrar() { if (factory != null) factory.close(); }

    private Usuario crearUsuario(String rol) {
        return tx.ejecutar(session -> {
            Usuario usuario = new Usuario("u-" + UUID.randomUUID(), hash, rol);
            session.persist(usuario);
            return usuario;
        });
    }

    private SesionUsuario login(Usuario usuario) {
        return new AutenticacionService(tx).autenticar(usuario.getNombre(), PASSWORD);
    }

    @Test void persisteUsuarioYAuditoriaConActorUtcYCorrelacion() {
        Usuario usuario = crearUsuario(Rol.PROPIETARIO);
        try (SesionUsuario identidad = login(usuario)) {
            assertEquals(usuario.getId(), identidad.getUsuarioId());
            tx.ejecutar(session -> {
                Auditoria registro = session.createQuery("from Auditoria a join fetch a.actor where a.actor.id = :id", Auditoria.class)
                        .setParameter("id", usuario.getId()).getSingleResult();
                assertEquals("INICIO_SESION", registro.getAccion());
                assertNotNull(registro.getInstante());
                assertNotNull(registro.getCorrelacion());
                assertEquals(usuario.getNombre(), registro.getActor().getNombre());
                return null;
            });
        }
    }

    @Test void falloRevierteDatosYAuditoriaYCierraSesion() {
        Usuario actor = crearUsuario(Rol.PROPIETARIO);
        String clave = UUID.randomUUID().toString();
        Session[] usada = new Session[1];
        assertThrows(IllegalArgumentException.class, () -> tx.ejecutar(session -> {
            usada[0] = session;
            session.persist(new Configuracion(clave, "valor"));
            new AuditoriaService().registrar(session, session.find(Usuario.class, actor.getId()), UUID.randomUUID(), "PRUEBA_ROLLBACK", clave);
            session.flush();
            throw new IllegalArgumentException("Fallo provocado");
        }));
        assertFalse(usada[0].isOpen());
        tx.ejecutar(session -> {
            assertNull(session.find(Configuracion.class, clave));
            assertEquals(0L, session.createQuery("select count(a) from Auditoria a where a.referencia = :ref", Long.class)
                    .setParameter("ref", clave).getSingleResult());
            return null;
        });
    }

    @Test void duplicadoRevierteTransaccionYPermiteReintentar() {
        Usuario existente = crearUsuario(Rol.ASISTENTE);
        String clave = UUID.randomUUID().toString();
        assertThrows(IllegalStateException.class, () -> tx.ejecutar(session -> {
            session.persist(new Configuracion(clave, "no debe persistirse"));
            session.persist(new Usuario(existente.getNombre(), hash, Rol.ASISTENTE));
            return null;
        }));
        assertNull(tx.ejecutar(session -> session.find(Configuracion.class, clave)));
        assertNotNull(crearUsuario(Rol.ASISTENTE).getId());
    }

    @Test void asistenteNoPuedeCrearUsuariosNiCambiarConfiguracion() {
        try (SesionUsuario asistente = login(crearUsuario(Rol.ASISTENTE))) {
            assertThrows(SecurityException.class, () -> new UsuarioService(tx).crear(asistente, "nuevo", PASSWORD, Rol.PROPIETARIO));
            assertThrows(SecurityException.class, () -> new ConfiguracionService(tx).guardar(asistente, "prueba", "valor", -1));
            tx.ejecutar(session -> {
                assertNotNull(new PermisoService().exigir(session, asistente, Permiso.CONSULTAR_REPORTES));
                assertThrows(IllegalArgumentException.class, () -> new PermisoService().exigir(session, asistente, "INEXISTENTE"));
                return null;
            });
        }
    }

    @Test void propietarioCreaUsuarioYAuditaEnLaMismaTransaccion() {
        try (SesionUsuario propietario = login(crearUsuario(Rol.PROPIETARIO))) {
            Long id = new UsuarioService(tx).crear(propietario, "nuevo-" + UUID.randomUUID(), PASSWORD, Rol.ASISTENTE);
            tx.ejecutar(session -> {
                assertNotEquals(new String(PASSWORD), session.find(Usuario.class, id).getPasswordHash());
                assertEquals(1L, session.createQuery("select count(a) from Auditoria a where a.accion = :accion and a.referencia = :ref", Long.class)
                        .setParameter("accion", "CREAR_USUARIO").setParameter("ref", "usuario:" + id).getSingleResult());
                return null;
            });
        }
    }

    @Test void usuarioInactivoYSesionCerradaPierdenAcceso() {
        Usuario usuario = crearUsuario(Rol.PROPIETARIO);
        SesionUsuario identidad = login(usuario);
        tx.ejecutar(session -> { session.find(Usuario.class, usuario.getId()).desactivar(); return null; });
        assertThrows(SecurityException.class, () -> new ConfiguracionService(tx).guardar(identidad, "revocado", "valor", -1));
        assertThrows(IllegalArgumentException.class, () -> login(usuario));
        identidad.close();
        assertFalse(identidad.isVigente());
        try (SesionUsuario otra = login(crearUsuario(Rol.PROPIETARIO))) {
            otra.close();
            assertThrows(SecurityException.class, () -> new ConfiguracionService(tx).guardar(otra, "cerrado", "valor", -1));
        }
    }

    @Test void loginIncorrectoNoGeneraAuditoriaDeExito() {
        Usuario usuario = crearUsuario(Rol.ASISTENTE);
        AutenticacionService autenticacion = new AutenticacionService(tx);
        String mensaje = assertThrows(IllegalArgumentException.class,
                () -> autenticacion.autenticar(usuario.getNombre(), "incorrecta".toCharArray())).getMessage();
        assertEquals(mensaje, assertThrows(IllegalArgumentException.class,
                () -> autenticacion.autenticar("no-existe", PASSWORD)).getMessage());
        long cantidad = tx.ejecutar(session -> session.createQuery("select count(a) from Auditoria a where a.actor.id = :id", Long.class)
                .setParameter("id", usuario.getId()).getSingleResult());
        assertEquals(0L, cantidad);
    }

    @Test void consultaContratosTraeRelacionesYSeguimientoDeGarantia() {
        assertDoesNotThrow(() -> tx.ejecutar(session -> session.createQuery(
                "from Contrato c join fetch c.unidad join fetch c.inquilino left join fetch c.garantiaSeguimiento order by c.inicio desc",
                Contrato.class).getResultList()));
    }

    @Test void versionImpideSobrescribirConfiguracionObsoleta() {
        try (SesionUsuario propietario = login(crearUsuario(Rol.PROPIETARIO))) {
            ConfiguracionService service = new ConfiguracionService(tx);
            String clave = UUID.randomUUID().toString();
            service.guardar(propietario, clave, "original", -1);
            long version = service.consultar(propietario, clave).getVersion();
            service.guardar(propietario, clave, "nuevo", version);
            assertThrows(IllegalArgumentException.class, () -> service.guardar(propietario, clave, "obsoleto", version));
            assertEquals("nuevo", service.consultar(propietario, clave).getValor());
        }
    }

    @Test void versionJpaRechazaDosEscriturasConcurrentes() {
        String clave = UUID.randomUUID().toString();
        tx.ejecutar(session -> { session.persist(new Configuracion(clave, "base")); return null; });
        try (Session primera = factory.openSession(); Session segunda = factory.openSession()) {
            primera.beginTransaction();
            segunda.beginTransaction();
            Configuracion a = primera.find(Configuracion.class, clave);
            Configuracion b = segunda.find(Configuracion.class, clave);
            a.cambiarValor("primero");
            primera.getTransaction().commit();
            b.cambiarValor("segundo");
            assertThrows(jakarta.persistence.OptimisticLockException.class, () -> segunda.getTransaction().commit());
            if (segunda.getTransaction().isActive()) segunda.getTransaction().rollback();
        }
        assertEquals("primero", tx.ejecutar(session -> session.find(Configuracion.class, clave).getValor()));
    }

    @Test void configuracionRechazaSecretosEnUrlYNoIncluyeValoresEnError() {
        Map<String, String> entorno = Map.of("LATCH_DB_URL", "jdbc:postgresql://localhost/latch?password=secreto");
        String error = assertThrows(IllegalArgumentException.class, () -> DatabaseConfig.desdeEntorno(entorno)).getMessage();
        assertFalse(error.contains("secreto"));
        Properties p = DatabaseConfig.desdeEntorno(Map.of("LATCH_DB_URL", "jdbc:postgresql://localhost:5432/latch",
                "LATCH_DB_USER", "local", "LATCH_DB_PASSWORD", "sintetica"));
        assertEquals("validate", p.getProperty("hibernate.hbm2ddl.auto"));
        assertThrows(IllegalArgumentException.class, () -> new Usuario("usuario", hash, "OTRO"));
    }
}
