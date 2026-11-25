package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.ServicioPagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ResumenGarantiaDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Garantia;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.MovimientoGarantia;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GarantiaServiceTest {
    private SessionFactory factory;
    private HibernateTransactions tx;
    private GarantiaService garantiaService;
    private CargoService cargoService;
    private DashboardService dashboardService;
    private ServicioPagos servicioPagos;
    private SesionUsuario propietario;
    private SesionUsuario asistente;
    private UUID contratoId;
    private UUID otroContratoId;
    private UUID cargoId;
    private UUID cargoOtroContratoId;
    private UUID cargoCanceladoId;

    @BeforeEach
    void iniciar() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        properties.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:garantias_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        properties.setProperty("hibernate.connection.username", "sa");
        properties.setProperty("hibernate.connection.password", "");
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        factory = HibernateUtil.construir(properties);
        tx = new HibernateTransactions(factory);
        garantiaService = new GarantiaService(tx);
        cargoService = new CargoService(tx);
        dashboardService = new DashboardService(tx);
        servicioPagos = new ServicioPagos(tx);

        tx.ejecutar(session -> {
            Usuario propUser = new Usuario("prop-" + UUID.randomUUID().toString().substring(0, 8),
                    "hash", Rol.PROPIETARIO);
            Usuario asistUser = new Usuario("asist-" + UUID.randomUUID().toString().substring(0, 8),
                    "hash", Rol.ASISTENTE);
            session.persist(propUser);
            session.persist(asistUser);
            session.flush();
            propietario = SesionUsuario.paraSesionAutenticada(
                    propUser.getId(), propUser.getNombre(), Rol.PROPIETARIO);
            asistente = SesionUsuario.paraSesionAutenticada(
                    asistUser.getId(), asistUser.getNombre(), Rol.ASISTENTE);

            Propiedad prop = new Propiedad();
            prop.setNombre("Residencial Norte");
            prop.setDireccion("Km 4");
            session.persist(prop);

            Unidad u1 = crearUnidad(prop.getId(), "G-01");
            Unidad u2 = crearUnidad(prop.getId(), "G-02");
            session.persist(u1);
            session.persist(u2);

            Inquilino inq = new Inquilino();
            inq.setNombre("Carlos Garantía");
            session.persist(inq);
            session.flush();

            Contrato c1 = crearContrato(u1.getId(), inq.getId(), new BigDecimal("100.00"));
            Contrato c2 = crearContrato(u2.getId(), inq.getId(), new BigDecimal("200.00"));
            session.persist(c1);
            session.persist(c2);
            session.flush();
            contratoId = c1.getId();
            otroContratoId = c2.getId();

            Cargo cargo = new Cargo();
            cargo.setContratoId(contratoId);
            cargo.setTipo("Renta");
            cargo.setConcepto("Renta vencida");
            cargo.setPeriodo(LocalDate.now().withDayOfMonth(1));
            cargo.setFechaEmision(LocalDate.now().minusDays(10));
            cargo.setVenceEl(LocalDate.now().minusDays(2));
            cargo.setImporte(new BigDecimal("80.00"));
            cargo.setEstado("Pendiente");
            session.persist(cargo);

            Cargo cancelado = new Cargo();
            cancelado.setContratoId(contratoId);
            cancelado.setTipo("Otro");
            cancelado.setConcepto("Cargo cancelado");
            cancelado.setPeriodo(LocalDate.now().withDayOfMonth(1));
            cancelado.setFechaEmision(LocalDate.now().minusDays(5));
            cancelado.setVenceEl(LocalDate.now().plusDays(5));
            cancelado.setImporte(new BigDecimal("50.00"));
            cancelado.setEstado("Cancelado");
            session.persist(cancelado);

            Cargo ajeno = new Cargo();
            ajeno.setContratoId(otroContratoId);
            ajeno.setTipo("Renta");
            ajeno.setConcepto("Renta de otro contrato");
            ajeno.setPeriodo(LocalDate.now().withDayOfMonth(1));
            ajeno.setFechaEmision(LocalDate.now().minusDays(5));
            ajeno.setVenceEl(LocalDate.now().plusDays(5));
            ajeno.setImporte(new BigDecimal("100.00"));
            ajeno.setEstado("Pendiente");
            session.persist(ajeno);
            session.flush();

            cargoId = cargo.getId();
            cargoCanceladoId = cancelado.getId();
            cargoOtroContratoId = ajeno.getId();
            return null;
        });
    }

    @AfterEach
    void cerrar() {
        SesionUsuario.cerrarSesion();
        if (factory != null) factory.close();
    }

    @Test
    void conservaEcuacionRn26YDerivaEstadosCorrectamente() {
        assertEquals("Retenido", Garantia.derivarEstado(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        ResumenGarantiaDTO inicial = SesionUsuario.ejecutarComo(asistente,
                () -> garantiaService.consultarPorContrato(contratoId));
        assertEquals("Retenido", inicial.estadoDerivado());
        assertTrue(inicial.conciliado());

        // Devolución parcial -> Devuelto parcial
        ResumenGarantiaDTO parcial = SesionUsuario.ejecutarComo(propietario,
                () -> garantiaService.liquidar(new GarantiaService.SolicitudLiquidacion(
                        contratoId, UUID.randomUUID(),
                        List.of(new GarantiaService.ItemMovimiento(
                                MovimientoGarantia.TIPO_DEVOLUCION, null, new BigDecimal("25.00"),
                                LocalDate.now(), "Efectivo", "Devolución parcial")))));
        assertEquals("Devuelto parcial", parcial.estadoDerivado());
        assertTrue(parcial.conciliado());

        // Aplicación a deuda + deducción hasta agotar -> habiendo devolución previa, estado es Devuelto
        ResumenGarantiaDTO agotadaConDevolucion = SesionUsuario.ejecutarComo(propietario,
                () -> garantiaService.liquidar(new GarantiaService.SolicitudLiquidacion(
                        contratoId, UUID.randomUUID(),
                        List.of(
                                new GarantiaService.ItemMovimiento(
                                        MovimientoGarantia.TIPO_APLICACION, cargoId, new BigDecimal("50.00"),
                                        LocalDate.now(), null, "Cubre parte de renta"),
                                new GarantiaService.ItemMovimiento(
                                        MovimientoGarantia.TIPO_DEDUCCION, null, new BigDecimal("25.00"),
                                        LocalDate.now(), null, "Pintura de pared")))));
        assertEquals("Devuelto", agotadaConDevolucion.estadoDerivado());
        assertEquals(0, BigDecimal.ZERO.compareTo(agotadaConDevolucion.saldoActual()));
        assertTrue(agotadaConDevolucion.conciliado());

        // Otro contrato agotado solo con aplicaciones/deducciones -> Aplicado
        ResumenGarantiaDTO aplicado = SesionUsuario.ejecutarComo(propietario,
                () -> garantiaService.liquidar(new GarantiaService.SolicitudLiquidacion(
                        otroContratoId, UUID.randomUUID(),
                        List.of(
                                new GarantiaService.ItemMovimiento(
                                        MovimientoGarantia.TIPO_APLICACION, cargoOtroContratoId, new BigDecimal("100.00"),
                                        LocalDate.now(), null, "Cubre renta total"),
                                new GarantiaService.ItemMovimiento(
                                        MovimientoGarantia.TIPO_DEDUCCION, null, new BigDecimal("100.00"),
                                        LocalDate.now(), null, "Limpieza profunda")))));
        assertEquals("Aplicado", aplicado.estadoDerivado());
        assertTrue(aplicado.conciliado());
    }

    @Test
    void rechazaDeduccionSinConceptoYValidaCargosCompatibles() {
        // CP-035: deducción sin concepto rechazada
        assertThrows(IllegalArgumentException.class, () -> SesionUsuario.ejecutarComo(propietario,
                () -> garantiaService.liquidar(new GarantiaService.SolicitudLiquidacion(
                        contratoId, UUID.randomUUID(),
                        List.of(new GarantiaService.ItemMovimiento(
                                MovimientoGarantia.TIPO_DEDUCCION, null, new BigDecimal("20.00"),
                                LocalDate.now(), null, "   "))))));

        // ERR-061: cargo de otro contrato y cargo cancelado
        IllegalArgumentException ajenoErr = assertThrows(IllegalArgumentException.class,
                () -> SesionUsuario.ejecutarComo(propietario,
                        () -> garantiaService.liquidar(new GarantiaService.SolicitudLiquidacion(
                                contratoId, UUID.randomUUID(),
                                List.of(new GarantiaService.ItemMovimiento(
                                        MovimientoGarantia.TIPO_APLICACION, cargoOtroContratoId, new BigDecimal("20.00"),
                                        LocalDate.now(), null, "Intento ajeno"))))));
        assertTrue(ajenoErr.getMessage().contains("ERR-061"));

        IllegalArgumentException canceladoErr = assertThrows(IllegalArgumentException.class,
                () -> SesionUsuario.ejecutarComo(propietario,
                        () -> garantiaService.liquidar(new GarantiaService.SolicitudLiquidacion(
                                contratoId, UUID.randomUUID(),
                                List.of(new GarantiaService.ItemMovimiento(
                                        MovimientoGarantia.TIPO_APLICACION, cargoCanceladoId, new BigDecimal("20.00"),
                                        LocalDate.now(), null, "Intento cancelado"))))));
        assertTrue(canceladoErr.getMessage().contains("ERR-061"));

        // Exceso sobre saldo del cargo (80.00) y sobre saldo de garantía (100.00)
        assertThrows(IllegalArgumentException.class, () -> SesionUsuario.ejecutarComo(propietario,
                () -> garantiaService.liquidar(new GarantiaService.SolicitudLiquidacion(
                        contratoId, UUID.randomUUID(),
                        List.of(new GarantiaService.ItemMovimiento(
                                MovimientoGarantia.TIPO_APLICACION, cargoId, new BigDecimal("90.00"),
                                LocalDate.now(), null, "Excede cargo"))))));

        assertThrows(IllegalArgumentException.class, () -> SesionUsuario.ejecutarComo(propietario,
                () -> garantiaService.liquidar(new GarantiaService.SolicitudLiquidacion(
                        contratoId, UUID.randomUUID(),
                        List.of(new GarantiaService.ItemMovimiento(
                                MovimientoGarantia.TIPO_DEVOLUCION, null, new BigDecimal("150.00"),
                                LocalDate.now(), "Efectivo", "Excede garantía"))))));
    }

    @Test
    void aplicacionDesdeGarantiaActualizaSaldoDeCargoEstadoCuentaYDashboardSinEmitirRecibo() {
        // CP-036 + RN-10: aplicar 80.00 al cargo y devolver 20.00
        UUID liqId = UUID.randomUUID();
        var solicitud = new GarantiaService.SolicitudLiquidacion(
                contratoId, liqId,
                List.of(
                        new GarantiaService.ItemMovimiento(
                                MovimientoGarantia.TIPO_APLICACION, cargoId, new BigDecimal("80.00"),
                                LocalDate.now(), null, "Aplicación a renta vencida"),
                        new GarantiaService.ItemMovimiento(
                                MovimientoGarantia.TIPO_DEVOLUCION, null, new BigDecimal("20.00"),
                                LocalDate.now(), "Transferencia", "Devolución remanente")));

        ResumenGarantiaDTO resumen = SesionUsuario.ejecutarComo(propietario,
                () -> garantiaService.liquidar(solicitud));
        assertEquals("Devuelto", resumen.estadoDerivado());
        assertEquals(0, BigDecimal.ZERO.compareTo(resumen.saldoActual()));

        // Idempotencia: repetir misma liquidacionId con igual contenido devuelve mismo resumen sin duplicar.
        ResumenGarantiaDTO repetido = SesionUsuario.ejecutarComo(propietario,
                () -> garantiaService.liquidar(solicitud));
        assertEquals(2, repetido.movimientos().size());

        // Misma liquidacionId con distinto contenido -> ConflictoDatosException
        assertThrows(ConflictoDatosException.class, () -> SesionUsuario.ejecutarComo(propietario,
                () -> garantiaService.liquidar(new GarantiaService.SolicitudLiquidacion(
                        contratoId, liqId,
                        List.of(new GarantiaService.ItemMovimiento(
                                MovimientoGarantia.TIPO_DEVOLUCION, null, new BigDecimal("10.00"),
                                LocalDate.now(), "Efectivo", "Contenido alterado"))))));

        // El cargo quedó Pagado en CargoService, en estadoCuenta y fuera de cargosPendientes en DashboardService,
        // sin crear recibos ni pagos de renta.
        var cargos = SesionUsuario.ejecutarComo(propietario, () -> cargoService.listarTodos());
        Cargo actualizado = cargos.stream().filter(c -> c.getId().equals(cargoId)).findFirst().orElseThrow();
        assertEquals("Pagado", actualizado.getEstado());

        var estadoCuenta = SesionUsuario.ejecutarComo(propietario,
                () -> servicioPagos.estadoCuenta(contratoId, LocalDate.now()));
        var cargoCuenta = estadoCuenta.cargos().stream()
                .filter(c -> c.cargoId().equals(cargoId.toString())).findFirst().orElseThrow();
        assertEquals("80.00", cargoCuenta.aplicado());
        assertEquals("0.00", cargoCuenta.saldo());
        assertEquals("Pagado", cargoCuenta.estado());
        assertEquals("0.00", estadoCuenta.totales().credito());
        assertTrue(estadoCuenta.pagos().isEmpty());

        var metricas = SesionUsuario.ejecutarComo(propietario, () -> dashboardService.obtenerMetricas());
        var pendienteUsd = metricas.cargosPendientes().stream()
                .filter(t -> "USD".equals(t.moneda())).findFirst().orElseThrow();
        // Solo queda el cargo del otro contrato (100.00)
        assertEquals(0, new BigDecimal("100.00").compareTo(pendienteUsd.total()));

        long recibos = tx.ejecutar(s -> s.createQuery("select count(r) from Recibo r", Long.class).getSingleResult());
        assertEquals(0L, recibos);
    }

    @Test
    void asistenteNoPuedeLiquidarGarantiaYNoDejaRegistrosNiAuditoria() {
        long audAntes = tx.ejecutar(s -> s.createQuery(
                        "select count(a) from Auditoria a where a.accion = 'GARANTIA_LIQUIDADA'", Long.class)
                .getSingleResult());

        assertThrows(SecurityException.class, () -> SesionUsuario.ejecutarComo(asistente,
                () -> garantiaService.liquidar(new GarantiaService.SolicitudLiquidacion(
                        contratoId, UUID.randomUUID(),
                        List.of(new GarantiaService.ItemMovimiento(
                                MovimientoGarantia.TIPO_DEVOLUCION, null, new BigDecimal("25.00"),
                                LocalDate.now(), "Efectivo", "Intento asistente"))))));

        ResumenGarantiaDTO intacta = SesionUsuario.ejecutarComo(asistente,
                () -> garantiaService.consultarPorContrato(contratoId));
        assertEquals(0, new BigDecimal("100.00").compareTo(intacta.saldoActual()));
        assertTrue(intacta.movimientos().isEmpty());
        long audDespues = tx.ejecutar(s -> s.createQuery(
                        "select count(a) from Auditoria a where a.accion = 'GARANTIA_LIQUIDADA'", Long.class)
                .getSingleResult());
        assertEquals(audAntes, audDespues);
    }

    private static Unidad crearUnidad(UUID propiedadId, String codigo) {
        Unidad u = new Unidad();
        u.setPropiedadId(propiedadId);
        u.setCodigo(codigo);
        u.setTipo("Apartamento");
        u.setEstado("Disponible");
        u.setMoneda("USD");
        u.setCanonReferencia(new BigDecimal("100.00"));
        return u;
    }

    private static Contrato crearContrato(UUID unidadId, UUID inquilinoId, BigDecimal deposito) {
        Contrato c = new Contrato();
        c.setUnidadId(unidadId);
        c.setInquilinoId(inquilinoId);
        c.setInicio(LocalDate.now().minusMonths(1));
        c.setFin(LocalDate.now().plusMonths(11));
        c.setCanon(new BigDecimal("100.00"));
        c.setMoneda("USD");
        c.setDiaPago(5);
        c.setDepositoPactado(deposito);
        c.setEstado("Vigente");
        c.inicializarGarantiaSeguimiento();
        return c;
    }
}
