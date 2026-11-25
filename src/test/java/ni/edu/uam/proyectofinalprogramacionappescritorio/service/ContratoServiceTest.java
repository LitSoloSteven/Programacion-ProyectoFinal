package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void validaDuracionMinimaDeUnMesCalendarioYAjusteFinDeMes() throws Exception {
        // CP-015 / RN-02: 31/01 -> 28/02 válido; 31/01 -> 27/02 rechazado; 01/01 -> 31/01 rechazado; 01/01 -> 01/02 válido.
        Contrato invalidoFeb = nuevoContrato("Borrador");
        invalidoFeb.setInicio(LocalDate.of(2027, 1, 31));
        invalidoFeb.setFin(LocalDate.of(2027, 2, 27));
        assertThrows(IllegalArgumentException.class, () -> guardar(invalidoFeb));

        Contrato validoFeb = nuevoContrato("Borrador");
        validoFeb.setInicio(LocalDate.of(2027, 1, 31));
        validoFeb.setFin(LocalDate.of(2027, 2, 28));
        guardar(validoFeb);

        Contrato invalidoEnero = nuevoContrato("Borrador");
        invalidoEnero.setInicio(LocalDate.of(2027, 1, 1));
        invalidoEnero.setFin(LocalDate.of(2027, 1, 31));
        assertThrows(IllegalArgumentException.class, () -> guardar(invalidoEnero));

        Contrato validoMesNatural = nuevoContrato("Borrador");
        validoMesNatural.setInicio(LocalDate.of(2027, 1, 1));
        validoMesNatural.setFin(LocalDate.of(2027, 2, 1));
        guardar(validoMesNatural);
    }

    @Test
    void activarBorradorRechazaInicioFuturoYColisionConcurrente() throws Exception {
        Contrato futuro = nuevoContrato("Borrador");
        futuro.setInicio(LocalDate.now().plusDays(10));
        futuro.setFin(LocalDate.now().plusMonths(2));
        guardar(futuro);

        assertThrows(IllegalArgumentException.class, () -> ejecutarComoPropietario(() -> {
            servicio.activar(futuro.getId(), futuro.getVersion());
            return null;
        }));
        assertEquals("Borrador", leerContrato(futuro.getId()).getEstado());

        Contrato competidor = nuevoContrato("Borrador");
        guardar(competidor);

        CountDownLatch listo = new CountDownLatch(2);
        CountDownLatch arrancar = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> tarea1 = () -> intentarActivar(contratoId, leerContrato(contratoId).getVersion(), listo, arrancar);
            Callable<Boolean> tarea2 = () -> intentarActivar(competidor.getId(), leerContrato(competidor.getId()).getVersion(), listo, arrancar);
            List<Future<Boolean>> futuros = List.of(pool.submit(tarea1), pool.submit(tarea2));
            assertTrue(listo.await(5, TimeUnit.SECONDS));
            arrancar.countDown();

            int exitosos = 0;
            for (Future<Boolean> f : futuros) {
                if (f.get(10, TimeUnit.SECONDS)) exitosos++;
            }
            assertEquals(1, exitosos);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void cancelarBorradorExigeMotivoYNoGeneraMovimientos() throws Exception {
        Contrato borrador = leerContrato(contratoId);
        assertThrows(IllegalArgumentException.class, () -> ejecutarComoPropietario(() -> {
            servicio.cancelarBorrador(contratoId, borrador.getVersion(), "   ");
            return null;
        }));

        ejecutarComoPropietario(() -> {
            servicio.cancelarBorrador(contratoId, borrador.getVersion(), "Cliente desistió antes de firmar");
            return null;
        });

        Contrato cancelado = leerContrato(contratoId);
        assertEquals("Cancelado", cancelado.getEstado());
        assertEquals("Cliente desistió antes de firmar", cancelado.getMotivoCierre());
    }

    @Test
    void renovarSoloSeIniciaDesdeVigenteOPorVencerYCreaSucesorEnlazado() throws Exception {
        var req = new ContratoService.SolicitudRenovacion(
                LocalDate.now(), LocalDate.now().plusMonths(12),
                new BigDecimal("120.00"), "USD", 5, new BigDecimal("120.00"));

        assertThrows(IllegalArgumentException.class, () -> ejecutarComoPropietario(() ->
                servicio.renovar(contratoId, req)));

        ejecutarComoPropietario(() -> {
            servicio.activar(contratoId, leerContrato(contratoId).getVersion());
            return null;
        });

        UUID sucesorId = ejecutarComoPropietario(() -> servicio.renovar(contratoId, req));
        Contrato sucesor = leerContrato(sucesorId);
        assertEquals("Borrador", sucesor.getEstado());
        assertEquals(contratoId, sucesor.getPredecesorId());
        assertEquals(unidadId, sucesor.getUnidadId());
        assertEquals(inquilinoId, sucesor.getInquilinoId());

        List<Contrato> borradoresSucesores = ejecutarComoPropietario(() ->
                servicio.listarSucesoresBorrador(contratoId));
        assertEquals(1, borradoresSucesores.size());
        assertEquals(sucesorId, borradoresSucesores.getFirst().getId());
    }

    @Test
    void terminarExigeLiquidacionDeGarantiaYCierraAtomicamenteConSucesorORevierteSiFalla() throws Exception {
        ejecutarComoPropietario(() -> {
            servicio.activar(contratoId, leerContrato(contratoId).getVersion());
            return null;
        });

        // Sucesor con inicio futuro para provocar fallo tras liquidar y probar rollback completo.
        UUID sucesorFuturoId = ejecutarComoPropietario(() -> servicio.renovar(contratoId,
                new ContratoService.SolicitudRenovacion(
                        LocalDate.now().plusDays(15), LocalDate.now().plusMonths(13),
                        new BigDecimal("110.00"), "USD", 5, BigDecimal.ZERO)));

        Contrato activo = leerContrato(contratoId);
        // Bloquea terminación si garantía tiene saldo retenido (ERR-062).
        IllegalStateException err = assertThrows(IllegalStateException.class, () -> ejecutarComoPropietario(() -> {
            servicio.terminar(contratoId, activo.getVersion(), new ContratoService.SolicitudTerminacion(
                    LocalDate.now(), "Fin de vigencia", null, null));
            return null;
        }));
        assertTrue(err.getCause() != null && err.getCause().getMessage().contains("ERR-062"),
                String.valueOf(err.getCause()));

        // Si falla la activación del sucesor futuro, se revierte también la liquidación y el cierre.
        var liquidacionTotal = new GarantiaService.SolicitudLiquidacion(
                contratoId, UUID.randomUUID(),
                List.of(new GarantiaService.ItemMovimiento(
                        "Devolucion", null, new BigDecimal("100.00"),
                        LocalDate.now(), "Transferencia", "Devolución total al cierre")));
        assertThrows(IllegalArgumentException.class, () -> ejecutarComoPropietario(() -> {
            servicio.terminar(contratoId, activo.getVersion(), new ContratoService.SolicitudTerminacion(
                    LocalDate.now(), "Renovación atómica fallida", sucesorFuturoId, liquidacionTotal));
            return null;
        }));
        assertEquals("Vigente", leerContrato(contratoId).getEstado());
        assertEquals("Borrador", leerContrato(sucesorFuturoId).getEstado());
        assertEquals(0, new BigDecimal("100.00").compareTo(leerGarantia(contratoId).getSaldoActual()));

        // Ahora con un sucesor elegible hoy, cierra anterior, liquida garantía y activa sucesor en una transacción.
        UUID sucesorValidoId = ejecutarComoPropietario(() -> servicio.renovar(contratoId,
                new ContratoService.SolicitudRenovacion(
                        LocalDate.now(), LocalDate.now().plusMonths(12),
                        new BigDecimal("115.00"), "USD", 5, new BigDecimal("115.00"))));

        ejecutarComoPropietario(() -> {
            servicio.terminar(contratoId, leerContrato(contratoId).getVersion(),
                    new ContratoService.SolicitudTerminacion(
                            LocalDate.now(), "Cierre con renovación", sucesorValidoId, liquidacionTotal));
            return null;
        });

        Contrato cerrado = leerContrato(contratoId);
        Contrato nuevoActivo = leerContrato(sucesorValidoId);
        assertEquals("Terminado", cerrado.getEstado());
        assertEquals(LocalDate.now(), cerrado.getTerminadoEl());
        assertEquals("Cierre con renovación", cerrado.getMotivoCierre());
        assertEquals("Vigente", nuevoActivo.getEstado());
        assertEquals(0, BigDecimal.ZERO.compareTo(leerGarantia(contratoId).getSaldoActual()));
        assertEquals(0, new BigDecimal("115.00").compareTo(leerGarantia(sucesorValidoId).getSaldoActual()));

        List<Contrato> terminados = ejecutarComoPropietario(() ->
                servicio.listarFiltrado(unidadId, inquilinoId, "Terminado"));
        assertEquals(1, terminados.size());
        assertEquals(contratoId, terminados.getFirst().getId());
    }

    @Test
    void asistentePuedeTerminarContratoConGarantiaCeroSinMovimientos() throws Exception {
        Usuario asistenteUser = tx.ejecutar(session -> {
            Usuario u = new Usuario("asist-" + UUID.randomUUID().toString().substring(0, 8), "hash", Rol.ASISTENTE);
            session.persist(u);
            return u;
        });
        SesionUsuario asistente = SesionUsuario.paraSesionAutenticada(
                asistenteUser.getId(), asistenteUser.getNombre(), Rol.ASISTENTE);

        Contrato cero = nuevoContrato("Borrador");
        cero.getGarantiaSeguimiento().ajustarSaldo(BigDecimal.ZERO);
        cero.setDepositoPactado(BigDecimal.ZERO);
        guardar(cero);

        SesionUsuario.ejecutarComo(asistente, () -> {
            servicio.activar(cero.getId(), leerContrato(cero.getId()).getVersion());
            servicio.terminar(cero.getId(), leerContrato(cero.getId()).getVersion(),
                    new ContratoService.SolicitudTerminacion(
                            LocalDate.now(), "Cierre sin depósito", null, null));
            return null;
        });

        assertEquals("Terminado", leerContrato(cero.getId()).getEstado());
    }

    private boolean intentarActivar(UUID id, long version, CountDownLatch listo, CountDownLatch arrancar)
            throws Exception {
        listo.countDown();
        arrancar.await(5, TimeUnit.SECONDS);
        try {
            ejecutarComoPropietario(() -> {
                servicio.activar(id, version);
                return null;
            });
            return true;
        } catch (IllegalArgumentException | ConflictoDatosException e) {
            return false;
        }
    }

    private Garantia leerGarantia(UUID cId) {
        return tx.ejecutar(session -> session.createQuery(
                        "from Garantia g where g.contrato.id = :contratoId", Garantia.class)
                .setParameter("contratoId", cId)
                .getSingleResult());
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

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class WrappedSqlException extends RuntimeException {
        private final SQLException sqlException;

    }
}
