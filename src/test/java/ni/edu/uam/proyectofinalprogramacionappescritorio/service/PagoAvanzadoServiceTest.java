package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.AplicacionPago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Recibo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PagoAvanzadoServiceTest {
    private SessionFactory factory;
    private HibernateTransactions tx;
    private PagoService pagoService;
    private CobranzaService cobranzaService;
    private SesionUsuario propietario;
    private SesionUsuario asistente;
    private UUID propiedadId;

    @BeforeEach
    void iniciar() {
        Properties props = new Properties();
        props.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        props.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:pagos_avanzados_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        props.setProperty("hibernate.connection.username", "sa");
        props.setProperty("hibernate.connection.password", "");
        props.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        props.setProperty("hibernate.jdbc.time_zone", "UTC");
        factory = HibernateUtil.construir(props);
        tx = new HibernateTransactions(factory);
        pagoService = new PagoService(tx);
        cobranzaService = new CobranzaService(tx);

        tx.ejecutar(session -> {
            Usuario prop = new Usuario("prop-" + UUID.randomUUID().toString().substring(0, 8), "hash", Rol.PROPIETARIO);
            Usuario asis = new Usuario("asis-" + UUID.randomUUID().toString().substring(0, 8), "hash", Rol.ASISTENTE);
            session.persist(prop);
            session.persist(asis);
            session.flush();
            propietario = SesionUsuario.paraSesionAutenticada(prop.getId(), prop.getNombre(), Rol.PROPIETARIO);
            asistente = SesionUsuario.paraSesionAutenticada(asis.getId(), asis.getNombre(), Rol.ASISTENTE);

            Propiedad p = new Propiedad();
            p.setNombre("Edificio Central");
            p.setDireccion("Managua");
            session.persist(p);
            session.flush();
            propiedadId = p.getId();
            return null;
        });
    }

    @AfterEach
    void cerrar() {
        SesionUsuario.cerrarSesion();
        if (factory != null && !factory.isClosed()) factory.close();
    }

    @Test
    void cp026AplicacionAutomaticaParcialPriorizaCargoVencidoMasAntiguo() {
        UUID contratoId = crearContrato("AUTO-1", "USD", new BigDecimal("100.00"));
        UUID oct = crearCargoVencido(contratoId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), "100.00");
        UUID nov = crearCargoVencido(contratoId, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5), "100.00");

        Pago pago = nuevoPago(contratoId, LocalDate.of(2026, 11, 15), "USD", "40.00");
        PagoService.RegistroPago registro = SesionUsuario.ejecutarComo(asistente,
                () -> pagoService.registrarPagoAutomatico(pago));

        assertEquals(new BigDecimal("0.00"), registro.credito());
        assertEquals(new BigDecimal("60.00"), saldoCargo(oct));
        assertEquals(new BigDecimal("100.00"), saldoCargo(nov));
        assertEquals(1, SesionUsuario.ejecutarComo(asistente,
                () -> pagoService.listarAplicaciones(registro.pago().getId())).size());
    }

    @Test
    void cp027SeleccionManualAbonaSegundoCargoYRechazaOtroContrato() {
        UUID c1 = crearContrato("MAN-1", "USD", new BigDecimal("100.00"));
        UUID c2 = crearContrato("MAN-2", "USD", new BigDecimal("100.00"));
        UUID primero = crearCargoVencido(c1, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), "100.00");
        UUID segundo = crearCargoVencido(c1, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5), "100.00");
        UUID ajeno = crearCargoVencido(c2, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5), "100.00");

        Pago pagoManual = nuevoPago(c1, LocalDate.of(2026, 11, 12), "USD", "40.00");
        SesionUsuario.ejecutarComo(asistente, () -> pagoService.registrarPago(
                pagoManual, List.of(new AplicacionPago(null, segundo, new BigDecimal("40.00")))));
        assertEquals(new BigDecimal("100.00"), saldoCargo(primero));
        assertEquals(new BigDecimal("60.00"), saldoCargo(segundo));

        Pago pagoAjeno = nuevoPago(c1, LocalDate.of(2026, 11, 12), "USD", "40.00");
        assertThrows(IllegalArgumentException.class, () ->
                SesionUsuario.ejecutarComo(asistente, () -> pagoService.registrarPago(
                        pagoAjeno, List.of(new AplicacionPago(null, ajeno, new BigDecimal("40.00"))))));
    }

    @Test
    void cp028YCp033ExcedenteYAplicacionEnSiguienteRentaConReversionRn31() {
        UUID contratoId = crearContrato("CRED-1", "USD", new BigDecimal("100.00"));
        crearCargoVencido(contratoId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), "100.00");
        crearCargoVencido(contratoId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), "100.00");

        Pago pagoExcedente = nuevoPago(contratoId, LocalDate.of(2026, 10, 10), "USD", "230.00");
        PagoService.RegistroPago registro = SesionUsuario.ejecutarComo(asistente,
                () -> pagoService.registrarPagoAutomatico(pagoExcedente));
        assertEquals(new BigDecimal("30.00"), registro.credito());
        assertEquals(1L, registro.recibo().getNumero());

        var lote = SesionUsuario.ejecutarComo(asistente,
                () -> cobranzaService.generarMes(YearMonth.of(2026, 11)));
        assertEquals(1, lote.confirmados());
        UUID rentaNov = lote.cargosGenerados().getFirst();
        assertEquals(new BigDecimal("70.00"), saldoCargo(rentaNov));
        assertEquals(1L, contar("select count(r) from Recibo r"));

        // RN-31: no se puede anular pago origen mientras tenga aplicación posterior activa de crédito.
        assertThrows(ConflictoDatosException.class, () ->
                SesionUsuario.ejecutarComo(propietario,
                        () -> pagoService.anular(registro.pago().getId(), "Intento prematuro")));

        AplicacionPago appCredito = SesionUsuario.ejecutarComo(propietario,
                        () -> pagoService.listarAplicaciones(registro.pago().getId())).stream()
                .filter(a -> AplicacionPago.ORIGEN_CREDITO.equals(a.getOrigen()))
                .findFirst().orElseThrow();

        // Asistente no puede revertir aplicación de crédito.
        assertThrows(SecurityException.class, () ->
                SesionUsuario.ejecutarComo(asistente,
                        () -> pagoService.revertirAplicacionCredito(appCredito.getId(), "Intento asistente")));

        // Propietario revierte la aplicación de crédito con motivo -> saldo de renta vuelve a 100.00 y queda auditado.
        SesionUsuario.ejecutarComo(propietario,
                () -> pagoService.revertirAplicacionCredito(appCredito.getId(), "Deshacer crédito antes de anular"));
        assertEquals(new BigDecimal("100.00"), saldoCargo(rentaNov));
        assertEquals(1L, contar("select count(a) from Auditoria a where a.accion = 'APLICACION_CREDITO_REVERTIDA'"));

        // Ahora el Propietario puede anular el pago origen, restaurando las aplicaciones iniciales y conservando el recibo Anulado.
        SesionUsuario.ejecutarComo(propietario,
                () -> pagoService.anular(registro.pago().getId(), "Pago devuelto por banco"));
        Recibo recibo = SesionUsuario.ejecutarComo(propietario,
                () -> pagoService.obtenerRecibo(1L)).orElseThrow();
        assertTrue(recibo.presentar().isAnulado());
        assertEquals(1L, recibo.getNumero());
    }

    @Test
    void cp029Cp030Cp031ConversionBidireccionalYRedondeoHalfEvenConProcedencia() {
        LocalDate fecha = LocalDate.of(2026, 11, 10);
        tx.ejecutar(session -> {
            Usuario prop = session.find(Usuario.class, propietario.getUsuarioId());
            session.persist(TipoCambio.autorizada(
                    fecha, new BigDecimal("36.62430000"), "BCN", Instant.now(), prop, "Oficial BCN"));
            session.persist(TipoCambio.autorizada(
                    fecha.minusDays(1), new BigDecimal("2.00000000"), "Sintética", Instant.now(), prop, "Half even"));
            return null;
        });

        UUID cUsd = crearContrato("CONV-USD", "USD", new BigDecimal("100.00"));
        UUID cNio = crearContrato("CONV-NIO", "NIO", new BigDecimal("3662.43"));
        UUID cargoUsd = crearCargoVencido(cUsd, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5), "100.00");
        UUID cargoNio = crearCargoVencido(cNio, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5), "3662.43");

        // CP-029: 3662.43 NIO / 36.6243 = 100.00 USD
        var p1 = SesionUsuario.ejecutarComo(asistente, () ->
                pagoService.registrarPagoAutomatico(nuevoPago(cUsd, fecha, "NIO", "3662.43")));
        assertEquals(new BigDecimal("100.00"), p1.pago().getMontoContrato());
        assertEquals(new BigDecimal("0.00"), saldoCargo(cargoUsd));
        assertNotNull(p1.pago().getTipoCambio());
        assertEquals("BCN", p1.pago().getTipoCambio().getFuente());

        // CP-030: 100.00 USD * 36.6243 = 3662.43 NIO
        var p2 = SesionUsuario.ejecutarComo(asistente, () ->
                pagoService.registrarPagoAutomatico(nuevoPago(cNio, fecha, "USD", "100.00")));
        assertEquals(new BigDecimal("3662.43"), p2.pago().getMontoContrato());
        assertEquals(new BigDecimal("0.00"), saldoCargo(cargoNio));

        // CP-031: 2.01 NIO / 2.00000000 = 1.005 -> HALF_EVEN = 1.00 USD
        UUID cHalf = crearContrato("HALF-1", "USD", new BigDecimal("10.00"));
        UUID ch1 = crearCargoVencido(cHalf, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5), "0.60");
        UUID ch2 = crearCargoVencido(cHalf, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6), "0.60");
        var p3 = SesionUsuario.ejecutarComo(asistente, () ->
                pagoService.registrarPago(
                        nuevoPago(cHalf, fecha.minusDays(1), "NIO", "2.01"),
                        List.of(new AplicacionPago(null, ch1, new BigDecimal("0.60")),
                                new AplicacionPago(null, ch2, new BigDecimal("0.40")))));
        assertEquals(new BigDecimal("1.00"), p3.pago().getMontoContrato());
        assertEquals(new BigDecimal("0.00"), p3.credito());
        assertEquals(new BigDecimal("0.20"), saldoCargo(ch2));
    }

    private UUID crearContrato(String codigo, String moneda, BigDecimal canon) {
        return tx.ejecutar(session -> {
            Unidad u = new Unidad();
            u.setPropiedadId(propiedadId);
            u.setCodigo(codigo);
            u.setTipo("Local");
            u.setEstado("Ocupada");
            u.setMoneda(moneda);
            u.setCanonReferencia(canon);
            session.persist(u);

            Inquilino i = new Inquilino();
            i.setNombre("Inquilino " + codigo);
            session.persist(i);
            session.flush();

            Contrato c = new Contrato();
            c.setUnidadId(u.getId());
            c.setInquilinoId(i.getId());
            c.setInicio(LocalDate.of(2026, 1, 1));
            c.setFin(LocalDate.of(2027, 12, 31));
            c.setCanon(canon);
            c.setMoneda(moneda);
            c.setDiaPago(5);
            c.setDepositoPactado(BigDecimal.ZERO);
            c.setEstado("Vigente");
            c.inicializarGarantiaSeguimiento();
            session.persist(c);
            session.flush();
            return c.getId();
        });
    }

    private UUID crearCargoVencido(UUID contratoId, LocalDate periodo, LocalDate venceEl, String importe) {
        return tx.ejecutar(session -> {
            Cargo c = new Cargo();
            c.setContratoId(contratoId);
            c.setTipo("Renta");
            c.setPeriodo(periodo);
            c.setFechaEmision(periodo);
            c.setVenceEl(venceEl);
            c.setImporte(new BigDecimal(importe));
            c.setConcepto("Cargo " + periodo);
            c.setEstado("Pendiente");
            session.persist(c);
            session.flush();
            return c.getId();
        });
    }

    private Pago nuevoPago(UUID contratoId, LocalDate fecha, String moneda, String monto) {
        Pago p = new Pago();
        p.setContratoId(contratoId);
        p.setFechaPago(fecha);
        p.setMonedaOriginal(moneda);
        p.setMontoOriginal(new BigDecimal(monto));
        p.setMetodo("Efectivo");
        return p;
    }

    private BigDecimal saldoCargo(UUID cargoId) {
        return tx.ejecutar(session -> {
            Cargo c = session.find(Cargo.class, cargoId);
            BigDecimal aplicado = session.createQuery("""
                            select sum(a.monto) from AplicacionPago a left join Pago p on a.pagoId = p.id
                            where a.cargoId = :id
                              and a.revertidaEn is null
                              and (a.pagoId is null or p.estado <> 'Anulado')
                            """, BigDecimal.class)
                    .setParameter("id", cargoId)
                    .getSingleResult();
            return c.saldoPendiente(aplicado);
        });
    }

    private long contar(String hql) {
        return tx.ejecutar(session -> session.createQuery(hql, Long.class).getSingleResult());
    }
}
