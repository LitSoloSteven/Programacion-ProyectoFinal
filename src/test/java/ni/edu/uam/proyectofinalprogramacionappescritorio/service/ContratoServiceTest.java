package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
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

class ContratoServiceTest {
    private SessionFactory factory;
    private HibernateTransactions tx;
    private ContratoService servicio;
    private UUID contratoId;
    private UUID unidadId;
    private UUID inquilinoId;
    private long propietarioId;
    private String propietarioNombre;
    private SesionUsuario propietario;

    @BeforeEach
    void iniciar() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        properties.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:contratos_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        properties.setProperty("hibernate.connection.username", "sa");
        properties.setProperty("hibernate.connection.password", "");
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        factory = HibernateUtil.construir(properties);
        tx = new HibernateTransactions(factory);
        servicio = new ContratoService(tx);

        propietarioNombre = "prop-" + UUID.randomUUID().toString().substring(0, 8);
        tx.ejecutar(session -> {
            Usuario usuario = new Usuario(propietarioNombre, "hash", Rol.PROPIETARIO);
            session.persist(usuario);
            session.flush();
            propietarioId = usuario.getId();

            Propiedad propiedad = new Propiedad();
            propiedad.setNombre("Casa de prueba");
            propiedad.setDireccion("Calle 1");
            session.persist(propiedad);
            session.flush();

            Unidad unidad = new Unidad();
            unidad.setPropiedadId(propiedad.getId());
            unidad.setCodigo("A-01");
            unidad.setTipo("Casa");
            unidad.setEstado("Disponible");
            unidad.setMoneda("USD");
            unidad.setCanonReferencia(new BigDecimal("100.00"));
            session.persist(unidad);
            session.flush();
            unidadId = unidad.getId();

            Inquilino inquilino = new Inquilino();
            inquilino.setNombre("Inquilino de prueba");
            session.persist(inquilino);
            session.flush();
            inquilinoId = inquilino.getId();

            Contrato contrato = nuevoContrato("Borrador");
            session.persist(contrato);
            session.flush();
            contratoId = contrato.getId();
            return null;
        });
        propietario = SesionUsuario.paraSesionAutenticada(
                propietarioId, propietarioNombre, Rol.PROPIETARIO);
    }

    @AfterEach
    void cerrar() {
        SesionUsuario.cerrarSesion();
        if (factory != null) factory.close();
    }

    @Test
    void permiteTransicionesValidasYRechazaReabrirUnContratoTerminado() throws Exception {
        cambiarEstado(contratoId, "Vigente");
        cambiarEstado(contratoId, "Por vencer");
        cambiarEstado(contratoId, "Vencido");
        cambiarEstado(contratoId, "Terminado");

        assertEquals("Terminado", leerContrato(contratoId).getEstado());
        assertThrows(IllegalArgumentException.class,
                () -> cambiarEstado(contratoId, "Vigente"));
    }

    @Test
    void contratoCanceladoNoPuedeActivarse() throws Exception {
        cambiarEstado(contratoId, "Cancelado");

        assertThrows(IllegalArgumentException.class,
                () -> cambiarEstado(contratoId, "Vigente"));
        assertEquals("Cancelado", leerContrato(contratoId).getEstado());
    }

    @Test
    void impideActivarOGuardarUnSegundoContratoActivoParaLaUnidad() throws Exception {
        tx.ejecutar(session -> {
            session.persist(nuevoContrato("Vigente"));
            return null;
        });

        assertThrows(IllegalArgumentException.class,
                () -> cambiarEstado(contratoId, "Vigente"));
        assertEquals("Borrador", leerContrato(contratoId).getEstado());

        Contrato duplicado = nuevoContrato("Vigente");
        assertThrows(IllegalArgumentException.class, () -> guardar(duplicado));
        assertEquals(2L, contarContratos());
    }

    @Test
    void cargosImpidenCambiarMonedaCanonYDeposito() throws Exception {
        tx.ejecutar(session -> {
            Cargo cargo = new Cargo();
            cargo.setContratoId(contratoId);
            cargo.setConcepto("Renta de prueba");
            session.persist(cargo);
            return null;
        });

        assertThrows(IllegalStateException.class, () -> actualizarTerminos(
                contratoId, new BigDecimal("125.00"), "NIO", new BigDecimal("125.00")));

        Contrato contrato = leerContrato(contratoId);
        assertEquals("USD", contrato.getMoneda());
        assertEquals(0, new BigDecimal("100.00").compareTo(contrato.getCanon()));
        assertEquals(0, new BigDecimal("100.00").compareTo(contrato.getDepositoPactado()));
    }

    @Test
    void pagosImpidenCambiarMonedaCanonYDeposito() throws Exception {
        tx.ejecutar(session -> {
            Pago pago = new Pago();
            pago.setContratoId(contratoId);
            pago.aplicarConversion("USD", new BigDecimal("50.00"), null);
            session.persist(pago);
            return null;
        });

        assertThrows(IllegalStateException.class, () -> actualizarTerminos(
                contratoId, new BigDecimal("125.00"), "NIO", new BigDecimal("125.00")));

        Contrato contrato = leerContrato(contratoId);
        assertEquals("USD", contrato.getMoneda());
        assertEquals(0, new BigDecimal("100.00").compareTo(contrato.getCanon()));
        assertEquals(0, new BigDecimal("100.00").compareTo(contrato.getDepositoPactado()));
    }

    @Test
    void mantieneMontoInicialDeGarantiaSinCambiarElSaldoActual() throws Exception {
        actualizarTerminos(contratoId, new BigDecimal("150.00"), "USD",
                new BigDecimal("150.00"));

        Garantia garantia = tx.ejecutar(session -> session.createQuery(
                        "from Garantia g where g.contrato.id = :contratoId", Garantia.class)
                .setParameter("contratoId", contratoId)
                .getSingleResult());
        assertEquals(0, new BigDecimal("150.00").compareTo(garantia.getMontoInicial()));
        assertEquals(0, new BigDecimal("100.00").compareTo(garantia.getSaldoActual()));
    }

    private Contrato nuevoContrato(String estado) {
        Contrato contrato = new Contrato();
        contrato.setUnidadId(unidadId);
        contrato.setInquilinoId(inquilinoId);
        contrato.setInicio(LocalDate.now().minusMonths(1));
        contrato.setFin(LocalDate.now().plusMonths(11));
        contrato.setCanon(new BigDecimal("100.00"));
        contrato.setMoneda("USD");
        contrato.setDiaPago(5);
        contrato.setDepositoPactado(new BigDecimal("100.00"));
        contrato.setEstado(estado);
        contrato.inicializarGarantiaSeguimiento();
        return contrato;
    }

    private void guardar(Contrato contrato) throws SQLException {
        ejecutarComoPropietario(() -> {
            servicio.guardar(contrato);
            return null;
        });
    }

    private void cambiarEstado(UUID id, String estado) throws SQLException {
        ejecutarComoPropietario(() -> {
            servicio.cambiarEstado(id, estado);
            return null;
        });
    }

    private void actualizarTerminos(UUID id, BigDecimal canon, String moneda,
                                    BigDecimal deposito) throws SQLException {
        Contrato contrato = leerContrato(id);
        ejecutarComoPropietario(() -> {
            servicio.actualizar(id, contrato.getVersion(), contrato.getUnidadId(),
                    contrato.getInquilinoId(), contrato.getInicio(), contrato.getFin(), canon,
                    moneda, contrato.getDiaPago(), deposito, contrato.getEstado());
            return null;
        });
    }

    private <T> T ejecutarComoPropietario(SqlSupplier<T> accion) throws SQLException {
        try {
            return SesionUsuario.ejecutarComo(propietario, () -> {
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

    private Contrato leerContrato(UUID id) {
        return tx.ejecutar(session -> session.find(Contrato.class, id));
    }

    private long contarContratos() {
        return tx.ejecutar(session -> session.createQuery(
                        "select count(c) from Contrato c", Long.class)
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
