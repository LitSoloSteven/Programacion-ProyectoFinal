package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ControlIntentosLogin;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.SesionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.UsuarioResumenDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Auditoria;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PasswordHasher;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
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

    @Test
    void autenticarConHashPbkdf2LegadoMigraAArgon2idYConservaSesionVigente() throws Exception {
        String clave = "ClaveLegadaLogin-2026";
        String nombre = "legado." + UUID.randomUUID().toString().substring(0, 8);
        Long usuarioId = crearUsuarioConHash(nombre, Rol.ASISTENTE, crearHashPbkdf2(clave.toCharArray()));
        long versionInicial = obtener(usuarioId).version();

        AutenticacionService auth = new AutenticacionService(transactions);
        SesionUsuario primera = auth.autenticar(nombre, clave.toCharArray(), false, "test");
        SesionApi sesionApi = new SesionApi(
                primera.getUsuarioId(), primera.getNombre(), primera.getRol(), primera.getVersion());

        assertTrue(auth.sesionVigente(sesionApi));
        EstadoUsuario migrado = obtener(usuarioId);
        assertTrue(migrado.passwordHash().startsWith("$argon2id$v=19$m=19456,t=2,p=1$"), migrado.passwordHash());
        assertTrue(migrado.version() > versionInicial);
        assertEquals(migrado.version(), primera.getVersion());

        long migraciones = contarAuditoria("MIGRAR_HASH_PASSWORD", "usuario:" + usuarioId);
        long inicios = contarAuditoria("INICIO_SESION", "usuario:" + usuarioId);
        assertEquals(1L, migraciones);
        assertEquals(1L, inicios);

        SesionUsuario segunda = auth.autenticar(nombre, clave.toCharArray(), false, "test");
        assertEquals(migrado.version(), segunda.getVersion());
        assertEquals(1L, contarAuditoria("MIGRAR_HASH_PASSWORD", "usuario:" + usuarioId));
        assertEquals(2L, contarAuditoria("INICIO_SESION", "usuario:" + usuarioId));
    }

    @Test
    void restablecerPasswordReemplazaHashLegadoAArgon2idEInvalidaSesionesPrevias() throws Exception {
        String claveVieja = "ClaveLegadaReset-2026";
        String claveNueva = "ClaveNuevaArgon2-2026";
        String nombre = "reset." + UUID.randomUUID().toString().substring(0, 8);
        Long asistenteId = crearUsuarioConHash(nombre, Rol.ASISTENTE, crearHashPbkdf2(claveVieja.toCharArray()));

        AutenticacionService auth = new AutenticacionService(transactions);
        EstadoUsuario inicial = obtener(asistenteId);
        SesionApi sesionPrevia = new SesionApi(asistenteId, nombre, Rol.ASISTENTE, inicial.version());
        assertTrue(auth.sesionVigente(sesionPrevia));

        UsuarioService usuarios = new UsuarioService(transactions);
        usuarios.restablecerPassword(actor(), asistenteId, claveNueva.toCharArray());

        EstadoUsuario actualizado = obtener(asistenteId);
        assertTrue(actualizado.passwordHash().startsWith("$argon2id$v=19$m=19456,t=2,p=1$"),
                actualizado.passwordHash());
        assertTrue(actualizado.debeCambiarPassword());
        assertTrue(actualizado.version() > inicial.version());
        assertFalse(auth.sesionVigente(sesionPrevia));

        assertThrows(IllegalArgumentException.class,
                () -> auth.autenticar(nombre, claveVieja.toCharArray(), false, "test"));
        assertThrows(AutenticacionService.CambioPasswordRequerido.class,
                () -> auth.autenticar(nombre, claveNueva.toCharArray(), false, "test"));
        assertThrows(AutenticacionService.CambioPasswordRequerido.class,
                () -> auth.autenticar(nombre, claveNueva.toCharArray(), false, "test", claveNueva.toCharArray()));
        assertTrue(obtener(asistenteId).debeCambiarPassword());
        SesionUsuario nuevaSesion = auth.autenticar(nombre, claveNueva.toCharArray(), false, "test",
                "ClavePersonalDefinitiva-2026".toCharArray());
        assertFalse(obtener(asistenteId).debeCambiarPassword());
        assertThrows(IllegalArgumentException.class,
                () -> auth.autenticar(nombre, claveNueva.toCharArray(), false, "test"));
        assertTrue(auth.sesionVigente(new SesionApi(
                nuevaSesion.getUsuarioId(), nuevaSesion.getNombre(),
                nuevaSesion.getRol(), nuevaSesion.getVersion())));

        List<String> referencias = transactions.ejecutar(session -> session.createQuery(
                        "select a.referencia from Auditoria a where a.accion = 'RESTABLECER_PASSWORD_USUARIO' and a.referencia = :ref",
                        String.class)
                .setParameter("ref", "usuario:" + asistenteId)
                .getResultList());
        assertEquals(1, referencias.size());
        assertFalse(referencias.get(0).contains(claveNueva));
        assertFalse(referencias.get(0).contains("argon2"));

        SesionUsuario asistenteActor = SesionUsuario.paraSesionAutenticada(
                asistenteId, nombre, Rol.ASISTENTE, actualizado.version());
        assertThrows(SecurityException.class,
                () -> usuarios.restablecerPassword(asistenteActor, propietarioId, claveNueva.toCharArray()));
        assertThrows(IllegalArgumentException.class,
                () -> usuarios.restablecerPassword(actor(), propietarioId, claveNueva.toCharArray()));
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class AdministracionYCuentas {
        private SessionFactory factoryAdmin;
        private HibernateTransactions txAdmin;
        private Long propietarioAdminId;
        private String nombrePropietarioAdmin;

        @BeforeAll
        void iniciarAdmin() {
            Properties propiedades = new Properties();
            propiedades.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
            propiedades.setProperty("hibernate.connection.url",
                    "jdbc:h2:mem:latch_usuario_admin_" + UUID.randomUUID() + ";MODE=PostgreSQL");
            propiedades.setProperty("hibernate.connection.username", "sa");
            propiedades.setProperty("hibernate.connection.password", "");
            propiedades.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            propiedades.setProperty("hibernate.jdbc.time_zone", "UTC");
            propiedades.setProperty("hibernate.show_sql", "false");
            factoryAdmin = HibernateUtil.construir(propiedades);
            txAdmin = new HibernateTransactions(factoryAdmin);

            nombrePropietarioAdmin = "admin." + UUID.randomUUID().toString().substring(0, 8);
            String hash = new PasswordHasher().crear("ClaveSintetica-2026".toCharArray());
            propietarioAdminId = txAdmin.ejecutar(session -> {
                Usuario propietario = new Usuario(nombrePropietarioAdmin, hash, Rol.PROPIETARIO);
                session.persist(propietario);
                return propietario.getId();
            });
        }

        @AfterAll
        void cerrarAdmin() {
            if (factoryAdmin != null && !factoryAdmin.isClosed()) factoryAdmin.close();
        }

        @Test
        void protegeUltimoPropietarioYPermiteListarActivarYDesbloquear() {
            UsuarioService usuarios = new UsuarioService(txAdmin);
            Long asistenteId = crearUsuarioAdmin(Rol.ASISTENTE);
            SesionUsuario actorAdmin = SesionUsuario.paraSesionAutenticada(
                    propietarioAdminId, nombrePropietarioAdmin, Rol.PROPIETARIO);
            List<UsuarioResumenDTO> lista = usuarios.listar(actorAdmin);
            assertTrue(lista.stream().anyMatch(u -> u.id().equals(asistenteId)));
            assertThrows(SecurityException.class, () -> usuarios.listar(
                    SesionUsuario.paraSesionAutenticada(asistenteId, "asistente", Rol.ASISTENTE)));

            usuarios.desactivar(actorAdmin, asistenteId);
            assertFalse(obtenerAdmin(asistenteId).activo());
            usuarios.activar(actorAdmin, asistenteId);
            assertTrue(obtenerAdmin(asistenteId).activo());

            Long segundoPropietario = crearUsuarioAdmin(Rol.PROPIETARIO);
            usuarios.cambiarRol(actorAdmin, segundoPropietario, Rol.ASISTENTE);
            assertEquals(Rol.ASISTENTE, obtenerAdmin(segundoPropietario).rol());

            usuarios.cambiarRol(actorAdmin, segundoPropietario, Rol.PROPIETARIO);
            SesionUsuario segundoActor = SesionUsuario.paraSesionAutenticada(
                    segundoPropietario, obtenerAdmin(segundoPropietario).nombre(),
                    Rol.PROPIETARIO, obtenerAdmin(segundoPropietario).version());
            usuarios.desactivar(segundoActor, propietarioAdminId);
            assertFalse(obtenerAdmin(propietarioAdminId).activo());
            usuarios.activar(segundoActor, propietarioAdminId);

            ControlIntentosLogin intentos = new ControlIntentosLogin(2,
                    java.time.Duration.ofMinutes(5), java.time.Duration.ofMinutes(5));
            String nombreAsistente = obtenerAdmin(asistenteId).nombre();
            intentos.registrarFallo(nombreAsistente);
            intentos.registrarFallo(nombreAsistente);
            assertTrue(intentos.estaBloqueado(nombreAsistente));

            usuarios.desbloquearCuenta(actorAdmin, asistenteId, intentos);
            assertFalse(intentos.estaBloqueado(nombreAsistente));
            assertEquals(1L, contarAuditoriaAdmin("DESBLOQUEAR_USUARIO", "usuario:" + asistenteId));
        }

        @Test
        void protegeUltimoPropietarioContraDesactivacionODegradacion() {
            UsuarioService usuarios = new UsuarioService(txAdmin);
            SesionUsuario actorAdmin = SesionUsuario.paraSesionAutenticada(
                    propietarioAdminId, nombrePropietarioAdmin, Rol.PROPIETARIO);
            for (UsuarioResumenDTO dto : usuarios.listar(actorAdmin)) {
                if (!dto.id().equals(propietarioAdminId) && Rol.PROPIETARIO.equals(dto.rol())) {
                    usuarios.cambiarRol(actorAdmin, dto.id(), Rol.ASISTENTE);
                }
            }
            Long otro = crearUsuarioAdmin(Rol.PROPIETARIO);
            SesionUsuario otroActor = SesionUsuario.paraSesionAutenticada(
                    otro, obtenerAdmin(otro).nombre(), Rol.PROPIETARIO, obtenerAdmin(otro).version());
            usuarios.cambiarRol(otroActor, propietarioAdminId, Rol.ASISTENTE);
            assertEquals(1L, usuarios.listar(otroActor).stream()
                    .filter(u -> u.activo() && Rol.PROPIETARIO.equals(u.rol())).count());
            usuarios.cambiarRol(otroActor, propietarioAdminId, Rol.PROPIETARIO);
            usuarios.desactivar(actorAdmin, otro);
            long restantes = txAdmin.ejecutar(s -> new ni.edu.uam.proyectofinalprogramacionappescritorio.dao.UsuarioDao()
                    .contarPropietariosActivosExcluyendo(s, propietarioAdminId));
            assertEquals(0L, restantes);
        }

        @Test
        void ambosRolesPuedenCambiarSuPropiaPasswordYLimpianPendiente() {
            UsuarioService usuarios = new UsuarioService(txAdmin);
            SesionUsuario actorAdmin = SesionUsuario.paraSesionAutenticada(
                    propietarioAdminId, nombrePropietarioAdmin, Rol.PROPIETARIO);
            String claveInicial = "ClaveInicialPropia-2026";
            String claveTemporal = "ClaveTemporalPropia-2026";
            String claveDefinitiva = "ClaveDefinitivaPropia-2026";
            String nombreAsistente = "propia." + UUID.randomUUID().toString().substring(0, 8);
            Long asistenteId = crearUsuarioAdminConHash(nombreAsistente, Rol.ASISTENTE,
                    new PasswordHasher().crear(claveInicial.toCharArray()));

            usuarios.restablecerPassword(actorAdmin, asistenteId, claveTemporal.toCharArray());
            assertTrue(obtenerAdmin(asistenteId).debeCambiarPassword());

            SesionUsuario sesionAsistente = SesionUsuario.paraSesionAutenticada(
                    asistenteId, nombreAsistente, Rol.ASISTENTE, obtenerAdmin(asistenteId).version());
            assertThrows(IllegalArgumentException.class, () -> usuarios.cambiarPasswordPropia(
                    sesionAsistente, "Equivocada-2026".toCharArray(), claveDefinitiva.toCharArray()));
            assertThrows(IllegalArgumentException.class, () -> usuarios.cambiarPasswordPropia(
                    sesionAsistente, claveTemporal.toCharArray(), claveTemporal.toCharArray()));

            long nuevaVersion = usuarios.cambiarPasswordPropia(
                    sesionAsistente, claveTemporal.toCharArray(), claveDefinitiva.toCharArray());

            EstadoUsuario actualizado = obtenerAdmin(asistenteId);
            assertFalse(actualizado.debeCambiarPassword());
            assertEquals(nuevaVersion, actualizado.version());
            assertEquals(1L, contarAuditoriaAdmin("CAMBIAR_PASSWORD_PROPIA", "usuario:" + asistenteId));

            AutenticacionService auth = new AutenticacionService(txAdmin);
            assertThrows(IllegalArgumentException.class,
                    () -> auth.autenticar(nombreAsistente, claveTemporal.toCharArray(), false, "test"));
            assertTrue(auth.autenticar(nombreAsistente, claveDefinitiva.toCharArray(), false, "test").isVigente());
        }

        private Long crearUsuarioAdmin(String rol) {
            String nombre = "usuario." + UUID.randomUUID().toString().substring(0, 8);
            String hash = new PasswordHasher().crear("ClaveSintetica-2026".toCharArray());
            return crearUsuarioAdminConHash(nombre, rol, hash);
        }

        private Long crearUsuarioAdminConHash(String nombre, String rol, String hash) {
            return txAdmin.ejecutar(session -> {
                Usuario usuario = new Usuario(nombre, hash, rol);
                session.persist(usuario);
                return usuario.getId();
            });
        }

        private long contarAuditoriaAdmin(String accion, String referencia) {
            return txAdmin.ejecutar(session -> session.createQuery(
                            "select count(a) from Auditoria a where a.accion = :accion and a.referencia = :referencia",
                            Long.class)
                    .setParameter("accion", accion)
                    .setParameter("referencia", referencia)
                    .getSingleResult());
        }

        private EstadoUsuario obtenerAdmin(Long id) {
            return txAdmin.ejecutar(session -> {
                Usuario usuario = session.find(Usuario.class, id);
                return new EstadoUsuario(usuario.getNombre(), usuario.getRol(), usuario.isActivo(),
                        usuario.isDebeCambiarPassword(), usuario.getVersion(), usuario.getPasswordHash());
            });
        }
    }

    private static SesionUsuario actor() {
        return SesionUsuario.paraSesionAutenticada(propietarioId, nombrePropietario, Rol.PROPIETARIO);
    }

    private static Long crearUsuario(String rol) {
        String nombre = "usuario." + UUID.randomUUID().toString().substring(0, 8);
        String hash = new PasswordHasher().crear("ClaveSintetica-2026".toCharArray());
        return crearUsuarioConHash(nombre, rol, hash);
    }

    private static Long crearUsuarioConHash(String nombre, String rol, String hash) {
        return transactions.ejecutar(session -> {
            Usuario usuario = new Usuario(nombre, hash, rol);
            session.persist(usuario);
            return usuario.getId();
        });
    }

    private static String crearHashPbkdf2(char[] password) throws Exception {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        PBEKeySpec spec = new PBEKeySpec(password, salt, 600_000, 256);
        byte[] derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        spec.clearPassword();
        return "pbkdf2-sha256$600000$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derived);
    }

    private static long contarAuditoria(String accion, String referencia) {
        return transactions.ejecutar(session -> session.createQuery(
                        "select count(a) from Auditoria a where a.accion = :accion and a.referencia = :referencia",
                        Long.class)
                .setParameter("accion", accion)
                .setParameter("referencia", referencia)
                .getSingleResult());
    }

    private static EstadoUsuario obtener(Long id) {
        return transactions.ejecutar(session -> {
            Usuario usuario = session.find(Usuario.class, id);
            return new EstadoUsuario(usuario.getNombre(), usuario.getRol(), usuario.isActivo(),
                    usuario.isDebeCambiarPassword(), usuario.getVersion(), usuario.getPasswordHash());
        });
    }

    private record EstadoUsuario(String nombre, String rol, boolean activo,
                                 boolean debeCambiarPassword, long version, String passwordHash) { }
}
