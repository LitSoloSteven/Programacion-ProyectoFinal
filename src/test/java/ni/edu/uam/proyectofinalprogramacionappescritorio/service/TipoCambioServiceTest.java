package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PasswordHasher;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TipoCambioServiceTest {
    private static final char[] PASSWORD = "ClaveSintetica-2026".toCharArray();
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 10);

    private SessionFactory factory;
    private HibernateTransactions transactions;
    private TipoCambioService servicio;
    private AutenticacionService autenticacion;
    private String nombrePropietario;
    private String nombreAsistente;

    @BeforeEach
    void iniciar() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        properties.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:tasas_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        properties.setProperty("hibernate.connection.username", "sa");
        properties.setProperty("hibernate.connection.password", "");
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        properties.setProperty("hibernate.jdbc.time_zone", "UTC");

        factory = HibernateUtil.construir(properties);
        transactions = new HibernateTransactions(factory);
        servicio = new TipoCambioService(transactions);
        autenticacion = new AutenticacionService(transactions);

        String hash = new PasswordHasher().crear(PASSWORD);
        nombrePropietario = "prop-" + UUID.randomUUID().toString().substring(0, 8);
        nombreAsistente = "asis-" + UUID.randomUUID().toString().substring(0, 8);
        transactions.ejecutar(session -> {
            session.persist(new Usuario(nombrePropietario, hash, Rol.PROPIETARIO));
            session.persist(new Usuario(nombreAsistente, hash, Rol.ASISTENTE));
            return null;
        });
    }

    @AfterEach
    void cerrar() {
        SesionUsuario.cerrarSesion();
        if (factory != null) factory.close();
    }

    @Test
    void registraTasaInformativaConOchoDecimalesYAuditoria() {
        entrarComoPropietario();

        TipoCambio tasa = servicio.registrar(FECHA, new BigDecimal("36.7"), "BCN");

        assertFalse(tasa.isAutorizada());
        assertEquals(new BigDecimal("36.70000000"), tasa.getNioPorUsd());
        assertEquals(8, tasa.getNioPorUsd().scale());
        assertEquals("BCN", tasa.getFuente());
        assertEquals(1L, contarAuditoria("TASA_REGISTRADA"));
    }

    @Test
    void elAsistentePuedeRegistrarPeroNoAutorizar() {
        autenticacion.autenticar(nombreAsistente, PASSWORD);

        TipoCambio informativa = servicio.registrar(FECHA, new BigDecimal("36.75"), "BCN");
        assertFalse(informativa.isAutorizada());

        assertThrows(SecurityException.class,
                () -> servicio.autorizar(informativa.getId(), "Intento sin permiso"));
        assertEquals(0L, contarAuditoria("TASA_AUTORIZADA"));
    }

    @Test
    void autorizarCreaUnaNuevaInstantaneaYConservaLaInformativa() {
        entrarComoPropietario();
        TipoCambio informativa = servicio.registrar(FECHA, new BigDecimal("36.75"), "BCN");

        TipoCambio autorizada = servicio.autorizar(informativa.getId(), "Tasa oficial verificada")
                .orElseThrow();

        assertNotEquals(informativa.getId(), autorizada.getId());
        assertTrue(autorizada.isAutorizada());
        assertEquals(FECHA, autorizada.getFecha());
        assertEquals(new BigDecimal("36.75000000"), autorizada.getNioPorUsd());
        assertEquals("Tasa oficial verificada", autorizada.getMotivo());
        assertEquals(1L, contarAuditoria("TASA_AUTORIZADA"));

        TipoCambio original = servicio.obtener(informativa.getId()).orElseThrow();
        assertFalse(original.isAutorizada());

        TipoCambio vigente = servicio.vigente(FECHA).orElseThrow();
        assertEquals(autorizada.getId(), vigente.getId());
    }

    @Test
    void noPermiteDosTasasAutorizadasParaLaMismaFecha() {
        entrarComoPropietario();
        TipoCambio primera = servicio.registrar(FECHA, new BigDecimal("36.75"), "BCN");
        TipoCambio segunda = servicio.registrar(FECHA, new BigDecimal("37.10"), "BCN");
        servicio.autorizar(primera.getId(), "Primera autorización").orElseThrow();

        assertThrows(ConflictoDatosException.class,
                () -> servicio.autorizar(segunda.getId(), "Segunda autorización"));
        assertEquals(1, servicio.listar(FECHA, true, 50).size());
    }

    @Test
    void noAutorizaDosVecesLaMismaTasa() {
        entrarComoPropietario();
        TipoCambio informativa = servicio.registrar(FECHA, new BigDecimal("36.75"), "BCN");
        TipoCambio autorizada = servicio.autorizar(informativa.getId(), "Autorización").orElseThrow();

        assertThrows(ConflictoDatosException.class,
                () -> servicio.autorizar(autorizada.getId(), "Repetida"));
    }

    @Test
    void validaFechaTasaFuenteYLimite() {
        entrarComoPropietario();
        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(LocalDate.now().plusDays(1),
                        new BigDecimal("36.75"), "BCN"));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(FECHA, BigDecimal.ZERO, "BCN"));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(FECHA, new BigDecimal("36.75"), " "));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.listar(null, null, 0));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.listar(null, null, TipoCambioService.LIMITE_MAXIMO + 1));
    }

    @Test
    void requiereSesionActivaYPermisoDeConsulta() {
        SesionUsuario.cerrarSesion();
        assertThrows(SecurityException.class,
                () -> servicio.listar(null, null, 50));
        assertThrows(SecurityException.class,
                () -> servicio.registrar(FECHA, new BigDecimal("36.75"), "BCN"));
    }

    @Test
    void listaPorFechaYEstadoYDevuelveVacioSiNoExiste() {
        entrarComoPropietario();
        TipoCambio informativa = servicio.registrar(FECHA, new BigDecimal("36.75"), "BCN");
        servicio.registrar(FECHA.plusDays(1), new BigDecimal("37.00"), "BCN");
        servicio.autorizar(informativa.getId(), "Autorización").orElseThrow();

        List<TipoCambio> informativas = servicio.listar(FECHA, false, 50);
        assertEquals(1, informativas.size());
        assertFalse(informativas.get(0).isAutorizada());

        List<TipoCambio> autorizadas = servicio.listar(FECHA, true, 50);
        assertEquals(1, autorizadas.size());
        assertTrue(autorizadas.get(0).isAutorizada());

        assertEquals(3, servicio.listar(null, null, 50).size());
        assertTrue(servicio.obtener(UUID.randomUUID()).isEmpty());
        assertTrue(servicio.vigente(FECHA.plusDays(5)).isEmpty());
    }

    private void entrarComoPropietario() {
        autenticacion.autenticar(nombrePropietario, PASSWORD);
    }

    private long contarAuditoria(String accion) {
        return transactions.ejecutar(session -> session.createQuery(
                        "select count(a) from Auditoria a where a.accion = :accion", Long.class)
                .setParameter("accion", accion)
                .getSingleResult());
    }
}