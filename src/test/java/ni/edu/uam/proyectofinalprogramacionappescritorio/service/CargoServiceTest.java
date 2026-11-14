package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.AplicacionPago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Garantia;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CargoServiceTest {
    private SessionFactory factory;
    private HibernateTransactions tx;
    private CargoService servicio;
    private GarantiaService garantiaService;
    private UUID contratoId;
    private UUID garantiaId;
    private long propietarioId;
    private long asistenteId;
    private String propietarioNombre;
    private String asistenteNombre;
    private SesionUsuario propietario;
    private SesionUsuario asistente;

    @BeforeEach
    void iniciar() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        properties.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:cargos_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        properties.setProperty("hibernate.connection.username", "sa");
        properties.setProperty("hibernate.connection.password", "");
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        factory = HibernateUtil.construir(properties);
        tx = new HibernateTransactions(factory);
        servicio = new CargoService(tx);
        garantiaService = new GarantiaService(tx);

        propietarioNombre = "prop-" + UUID.randomUUID().toString().substring(0, 8);
        asistenteNombre = "asis-" + UUID.randomUUID().toString().substring(0, 8);
        contratoId = tx.ejecutar(session -> {
            Usuario propietario = new Usuario(propietarioNombre, "hash", Rol.PROPIETARIO);
            Usuario asistente = new Usuario(asistenteNombre, "hash", Rol.ASISTENTE);
            session.persist(propietario);
            session.persist(asistente);
            session.flush();
            propietarioId = propietario.getId();
            asistenteId = asistente.getId();

            Propiedad propiedad = new Propiedad();
            propiedad.setNombre("Casa de prueba");
            propiedad.setDireccion("Calle 1");
            session.persist(propiedad);
            session.flush();

            Unidad unidad = new Unidad();
            unidad.setPropiedadId(propiedad.getId());
            unidad.setCodigo("A-01");
            unidad.setTipo("Casa");
            unidad.setEstado("Ocupada");
            unidad.setMoneda("USD");
            unidad.setCanonReferencia(new BigDecimal("100.00"));
            session.persist(unidad);
            session.flush();

            Inquilino inquilino = new Inquilino();
            inquilino.setNombre("Inquilino de prueba");
            session.persist(inquilino);
            session.flush();

            Contrato contrato = new Contrato();
            contrato.setUnidadId(unidad.getId());
            contrato.setInquilinoId(inquilino.getId());
            contrato.setInicio(LocalDate.now().minusMonths(1));
            contrato.setFin(LocalDate.now().plusMonths(11));
            contrato.setCanon(new BigDecimal("100.00"));
            contrato.setMoneda("USD");
            contrato.setDepositoPactado(new BigDecimal("100.00"));
            contrato.setEstado("Vigente");
            contrato.inicializarGarantiaSeguimiento();
            session.persist(contrato);
            session.flush();
            garantiaId = contrato.getGarantiaSeguimiento().getId();
            return contrato.getId();
        });

        propietario = SesionUsuario.paraSesionAutenticada(
                propietarioId, propietarioNombre, Rol.PROPIETARIO);
        asistente = SesionUsuario.paraSesionAutenticada(
                asistenteId, asistenteNombre, Rol.ASISTENTE);
    }

    @AfterEach
    void cerrar() {
        SesionUsuario.cerrarSesion();
        if (factory != null) factory.close();
    }

    @Test
    void asistenteNoPuedeAnularYNoDejaCambiosNiAuditoria() throws Exception {
        UUID cargoId = guardarComo(propietario, solicitud(null, 0,
                new BigDecimal("100.00"), "Pendiente"));
        long auditoriasAntes = contar("CARGO_ANULADO");

        assertThrows(SecurityException.class, () -> anularComo(asistente, cargoId));

        assertEquals("Pendiente", leerCargo(cargoId).getEstado());
        assertEquals(auditoriasAntes, contar("CARGO_ANULADO"));
    }

    @Test
    void propietarioAnulaConAuditoriaEnLaMismaOperacion() throws Exception {
        UUID cargoId = guardarComo(propietario, solicitud(null, 0,
                new BigDecimal("100.00"), "Pendiente"));

        anularComo(propietario, cargoId);

        assertEquals("Cancelado", leerCargo(cargoId).getEstado());
        assertEquals(1L, contar("CARGO_ANULADO"));
    }

    @Test
    void noPermiteCambiarImporteNiEstadoConAplicacionActiva() throws Exception {
        UUID cargoId = guardarComo(propietario, solicitud(null, 0,
                new BigDecimal("100.00"), "Pendiente"));
        tx.ejecutar(session -> {
            Pago pago = new Pago(null, contratoId, null, LocalDate.now(), "USD",
                    new BigDecimal("50.00"), new BigDecimal("50.00"),
                    "Efectivo", null, null, "Registrado");
            pago.aplicarConversion("USD", new BigDecimal("50.00"), null);
            session.persist(pago);
            session.flush();
            session.persist(new AplicacionPago(pago.getId(), cargoId,
                    new BigDecimal("50.00")));
            return null;
        });
        Cargo actual = leerCargo(cargoId);
        long auditoriasAntes = contar("CARGO_ACTUALIZADO");

        assertThrows(IllegalStateException.class, () -> guardarComo(propietario,
                solicitud(cargoId, actual.getVersion(), new BigDecimal("120.00"), "Pendiente")));
        assertThrows(IllegalStateException.class, () -> guardarComo(propietario,
                solicitud(cargoId, actual.getVersion(), new BigDecimal("100.00"), "Pagado")));

        assertEquals(new BigDecimal("100.00"), leerCargo(cargoId).getImporte());
        assertEquals("Pendiente", leerCargo(cargoId).getEstado());
        assertEquals(auditoriasAntes, contar("CARGO_ACTUALIZADO"));
    }

    @Test
    void asistenteNoPuedeAjustarGarantiaYNoDejaCambiosNiAuditoria() throws Exception {
        long auditoriasAntes = contar("GARANTIA_SALDO_AJUSTADO");

        assertThrows(SecurityException.class,
                () -> ajustarGarantiaComo(asistente, new BigDecimal("50.00")));

        assertEquals(new BigDecimal("100.00"), leerGarantia().getSaldoActual());
        assertEquals(auditoriasAntes, contar("GARANTIA_SALDO_AJUSTADO"));
    }

    @Test
    void propietarioAjustaGarantiaConAuditoria() throws Exception {
        ajustarGarantiaComo(propietario, new BigDecimal("75.00"));

        assertEquals(new BigDecimal("75.00"), leerGarantia().getSaldoActual());
        assertEquals(1L, contar("GARANTIA_SALDO_AJUSTADO"));
    }

    private CargoService.Solicitud solicitud(UUID id, long version,
                                              BigDecimal importe, String estado) {
        return new CargoService.Solicitud(id, version, contratoId, "Renta",
                LocalDate.now().withDayOfMonth(1), LocalDate.now(),
                LocalDate.now().plusDays(5), importe, "Renta de prueba", estado);
    }

    private UUID guardarComo(SesionUsuario identidad, CargoService.Solicitud solicitud)
            throws SQLException {
        return ejecutarComo(identidad, () -> servicio.guardar(solicitud));
    }

    private void anularComo(SesionUsuario identidad, UUID id) throws SQLException {
        ejecutarComo(identidad, () -> {
            servicio.anular(id);
            return null;
        });
    }

    private void ajustarGarantiaComo(SesionUsuario identidad, BigDecimal saldo)
            throws SQLException {
        ejecutarComo(identidad, () -> {
            garantiaService.ajustarSaldo(garantiaId, saldo);
            return null;
        });
    }

    private <T> T ejecutarComo(SesionUsuario identidad, SqlSupplier<T> accion)
            throws SQLException {
        try {
            return SesionUsuario.ejecutarComo(identidad, () -> {
                try {
                    return accion.get();
                } catch (SQLException e) {
                    throw new WrappedSqlException(e);
                }
            });
        } catch (WrappedSqlException e) {
            throw e.sqlException;
        }
    }

    private Cargo leerCargo(UUID id) {
        return tx.ejecutar(session -> session.find(Cargo.class, id));
    }

    private Garantia leerGarantia() {
        return tx.ejecutar(session -> session.find(Garantia.class, garantiaId));
    }

    private long contar(String accion) {
        return tx.ejecutar(session -> session.createQuery(
                        "select count(a) from Auditoria a where a.accion = :accion", Long.class)
                .setParameter("accion", accion)
                .getSingleResult());
    }

    @FunctionalInterface
    private interface SqlSupplier<T> {
        T get() throws SQLException;
    }

    private static final class WrappedSqlException extends RuntimeException {
        private final SQLException sqlException;

        private WrappedSqlException(SQLException sqlException) {
            this.sqlException = sqlException;
        }
    }
}
