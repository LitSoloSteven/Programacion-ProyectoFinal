package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.MetricasDashboardDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.TotalMonedaDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propietario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PasswordHasher;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DashboardServiceTest {
    private static final char[] PASSWORD = "ClaveSintetica-2026".toCharArray();

    private SessionFactory factory;
    private HibernateTransactions transactions;
    private DashboardService dashboardService;
    private AutenticacionService autenticacionService;
    private static String passwordHash;

    private String usuarioNombre;
    private Long usuarioId;

    @BeforeAll
    static void crearHashDePrueba() {
        passwordHash = new PasswordHasher().crear(PASSWORD);
    }

    @BeforeEach
    void iniciarPersistenciaYSesion() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        properties.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:dashboard_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        properties.setProperty("hibernate.connection.username", "sa");
        properties.setProperty("hibernate.connection.password", "");
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        properties.setProperty("hibernate.jdbc.time_zone", "UTC");

        factory = HibernateUtil.construir(properties);
        transactions = new HibernateTransactions(factory);
        dashboardService = new DashboardService(transactions);
        autenticacionService = new AutenticacionService(transactions);
        usuarioNombre = "dashboard-" + UUID.randomUUID().toString().replace("-", "");
        usuarioId = transactions.ejecutar(session -> {
            Usuario usuario = new Usuario(usuarioNombre, passwordHash, Rol.PROPIETARIO);
            session.persist(usuario);
            session.flush();
            session.persist(new Propietario(usuario, "Propietario de prueba",
                    usuarioNombre + "@example.test", null));
            return usuario.getId();
        });
        autenticacionService.autenticar(usuarioNombre, PASSWORD);
    }

    @AfterEach
    void cerrarSesion() {
        SesionUsuario.cerrarSesion();
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    void devuelveCargosYPagosSeparadosPorMonedaYExcluyeOtrosEstados() {
        sembrarEscenario(true);

        MetricasDashboardDTO metricas = dashboardService.obtenerMetricas();

        assertEquals(1, metricas.propiedades());
        assertEquals(1, metricas.unidades());
        assertEquals(1, metricas.inquilinos());
        assertEquals(2, metricas.contratosVigentes());
        assertEquals(2L, metricas.cantidadCargosPendientes());
        assertEquals(2L, metricas.cantidadPagosRegistrados());
        assertEquals(List.of("USD", "NIO"), metricas.cargosPendientes().stream().map(TotalMonedaDTO::moneda).toList());
        assertEquals(List.of("USD", "NIO"), metricas.pagosRegistrados().stream().map(TotalMonedaDTO::moneda).toList());
        assertEquals(1L, total(metricas.cargosPendientes(), "USD").cantidad());
        assertEquals(new BigDecimal("450.25"), total(metricas.cargosPendientes(), "USD").total());
        assertEquals(1L, total(metricas.cargosPendientes(), "NIO").cantidad());
        assertEquals(new BigDecimal("35000.50"), total(metricas.cargosPendientes(), "NIO").total());
        assertEquals(1L, total(metricas.pagosRegistrados(), "USD").cantidad());
        assertEquals(new BigDecimal("300.10"), total(metricas.pagosRegistrados(), "USD").total());
        assertEquals(1L, total(metricas.pagosRegistrados(), "NIO").cantidad());
        assertEquals(new BigDecimal("20000.75"), total(metricas.pagosRegistrados(), "NIO").total());

        String importeFormateado = transactions.ejecutar(session -> session.createQuery(
                        "select c from Cargo c join fetch c.contrato ct where ct.moneda = 'USD' and c.estado = 'Pendiente'",
                        Cargo.class).getSingleResult().getImporteFormateado());
        assertEquals("450.25 USD", importeFormateado);
    }

    @Test
    void incluyeAmbasMonedasConCeroCuandoNoHayMovimientos() {
        sembrarEscenario(false);

        MetricasDashboardDTO metricas = dashboardService.obtenerMetricas();

        for (TotalMonedaDTO total : metricas.cargosPendientes()) {
            assertEquals(0L, total.cantidad());
            assertEquals(new BigDecimal("0.00"), total.total());
        }
        for (TotalMonedaDTO total : metricas.pagosRegistrados()) {
            assertEquals(0L, total.cantidad());
            assertEquals(new BigDecimal("0.00"), total.total());
        }
    }

    @Test
    void requiereSesionYUsuarioActivo() {
        SesionUsuario.cerrarSesion();
        assertThrows(SecurityException.class, dashboardService::obtenerMetricas);

        autenticacionService.autenticar(usuarioNombre, PASSWORD);
        transactions.ejecutar(session -> {
            session.find(Usuario.class, usuarioId).desactivar();
            return null;
        });

        assertThrows(SecurityException.class, dashboardService::obtenerMetricas);
    }

    private void sembrarEscenario(boolean incluirMovimientos) {
        transactions.ejecutar(session -> {
            Propiedad propiedad = new Propiedad();
            propiedad.setNombre("Propiedad de prueba");
            propiedad.setDireccion("Dirección de prueba");
            propiedad.setPropietario(session.find(Propietario.class, usuarioId));
            session.persist(propiedad);

            Unidad unidad = new Unidad();
            unidad.setPropiedadId(propiedad.getId());
            unidad.setCodigo("U-" + UUID.randomUUID().toString().substring(0, 8));
            unidad.setCanonReferencia(new BigDecimal("450.25"));
            unidad.setMoneda("USD");
            session.persist(unidad);

            Inquilino inquilino = new Inquilino();
            inquilino.setNombre("Inquilino de prueba");
            inquilino.setIdentificacion("DASH-" + UUID.randomUUID().toString().replace("-", ""));
            session.persist(inquilino);
            session.flush();

            UUID contratoUsd = insertarContrato(session, unidad.getId(), inquilino.getId(), "USD");
            UUID contratoNio = insertarContrato(session, unidad.getId(), inquilino.getId(), "NIO");

            if (incluirMovimientos) {
                insertarCargo(session, contratoUsd, new BigDecimal("450.25"), "Pendiente");
                insertarCargo(session, contratoUsd, new BigDecimal("999.99"), "Pagado");
                insertarCargo(session, contratoNio, new BigDecimal("35000.50"), "Pendiente");
                insertarCargo(session, contratoNio, new BigDecimal("9000.00"), "Pagado");

                insertarPago(session, contratoUsd, "USD", new BigDecimal("300.10"), "Registrado");
                insertarPago(session, contratoUsd, "USD", new BigDecimal("2000.00"), "Anulado");
                insertarPago(session, contratoNio, "NIO", new BigDecimal("20000.75"), "Registrado");
                insertarPago(session, contratoNio, "NIO", new BigDecimal("50000.00"), "Anulado");
            }
            return null;
        });
    }

    private UUID insertarContrato(Session session, UUID unidadId, UUID inquilinoId, String moneda) {
        UUID contratoId = UUID.randomUUID();
        // deposito_pactado es de solo lectura en el mapeo y debe incluirse en el seed.
        session.createNativeMutationQuery("""
                insert into contrato (id, unidad_id, inquilino_id, inicio, fin, canon, moneda,
                                      dia_pago, deposito_pactado, estado, version)
                values (:id, :unidadId, :inquilinoId, :inicio, :fin, :canon, :moneda,
                        :diaPago, :depositoPactado, :estado, :version)
                """)
                .setParameter("id", contratoId)
                .setParameter("unidadId", unidadId)
                .setParameter("inquilinoId", inquilinoId)
                .setParameter("inicio", LocalDate.of(2026, 11, 1))
                .setParameter("fin", LocalDate.of(2027, 11, 30))
                .setParameter("canon", new BigDecimal("450.25"))
                .setParameter("moneda", moneda)
                .setParameter("diaPago", 5)
                .setParameter("depositoPactado", BigDecimal.ZERO)
                .setParameter("estado", "Vigente")
                .setParameter("version", 0L)
                .executeUpdate();
        return contratoId;
    }

    private void insertarCargo(Session session, UUID contratoId, BigDecimal importe, String estado) {
        Cargo cargo = new Cargo();
        cargo.setContratoId(contratoId);
        cargo.setTipo("Renta");
        cargo.setPeriodo(LocalDate.of(2026, 11, 1));
        cargo.setFechaEmision(LocalDate.of(2026, 11, 1));
        cargo.setVenceEl(LocalDate.of(2026, 11, 5));
        cargo.setImporte(importe);
        cargo.setConcepto("Cargo de prueba");
        cargo.setEstado(estado);
        session.persist(cargo);
    }

    private void insertarPago(Session session, UUID contratoId, String moneda,
                              BigDecimal monto, String estado) {
        Pago pago = new Pago();
        pago.setContratoId(contratoId);
        pago.setFechaPago(LocalDate.of(2026, 11, 1));
        pago.setMonedaOriginal(moneda);
        pago.setMontoOriginal(monto);
        pago.setMontoContrato(monto);
        pago.setMetodo("Efectivo");
        pago.setReferencia("REF-" + UUID.randomUUID());
        pago.setEstado(estado);
        pago.aplicarConversion(moneda, monto, null);
        session.persist(pago);
    }

    private TotalMonedaDTO total(List<TotalMonedaDTO> totales, String moneda) {
        return totales.stream().filter(total -> moneda.equals(total.moneda())).findFirst().orElseThrow();
    }
}
