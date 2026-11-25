package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.CarteraResumenDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.PrevisualizacionLoteDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ProgresoLoteDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ResultadoLoteDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ResultadoRevisionH2DTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.AplicacionPago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CobranzaH1H2ServiceTest {
    private SessionFactory factory;
    private HibernateTransactions tx;
    private CobranzaService cobranza;
    private CargoService cargoService;
    private ConfiguracionService configuracion;
    private SesionUsuario propietario;
    private SesionUsuario asistente;
    private UUID propiedadId;

    @BeforeEach
    void iniciar() {
        Properties props = new Properties();
        props.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        props.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:cobranza_p3_" + UUID.randomUUID() + ";MODE=PostgreSQL;LOCK_TIMEOUT=10000");
        props.setProperty("hibernate.connection.username", "sa");
        props.setProperty("hibernate.connection.password", "");
        props.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        props.setProperty("hibernate.jdbc.time_zone", "UTC");
        factory = HibernateUtil.construir(props);
        tx = new HibernateTransactions(factory);
        cobranza = new CobranzaService(tx);
        cargoService = new CargoService(tx);
        configuracion = new ConfiguracionService(tx);

        tx.ejecutar(session -> {
            Usuario prop = new Usuario("prop-" + UUID.randomUUID().toString().substring(0, 8), "hash", Rol.PROPIETARIO);
            Usuario asis = new Usuario("asis-" + UUID.randomUUID().toString().substring(0, 8), "hash", Rol.ASISTENTE);
            session.persist(prop);
            session.persist(asis);
            session.flush();
            propietario = SesionUsuario.paraSesionAutenticada(prop.getId(), prop.getNombre(), Rol.PROPIETARIO);
            asistente = SesionUsuario.paraSesionAutenticada(asis.getId(), asis.getNombre(), Rol.ASISTENTE);

            Propiedad p = new Propiedad();
            p.setNombre("Residencial Latch");
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
    void cp021PrevisualizaYGeneraRentasSinDuplicadosConAjusteFinDeMes() {
        UUID c1 = crearContrato("U-01", "USD", new BigDecimal("500.00"), 31,
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31), "Vigente");
        UUID c2 = crearContrato("U-02", "USD", new BigDecimal("600.00"), 15,
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31), "Por vencer");
        UUID c3 = crearContrato("U-03", "NIO", new BigDecimal("18000.00"), 5,
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31), "Vencido");
        crearContrato("U-04", "USD", new BigDecimal("400.00"), 10,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 31), "Terminado");

        YearMonth febrero2027 = YearMonth.of(2027, 2);
        PrevisualizacionLoteDTO previa = SesionUsuario.ejecutarComo(asistente,
                () -> cobranza.previsualizarGeneracionMes(febrero2027));
        assertEquals(3, previa.totalElegibles());
        assertEquals(3, previa.porGenerar());
        assertEquals(0, previa.omitidos());
        assertEquals(0L, contar("select count(c) from Cargo c"));
        LocalDate venceC1 = previa.items().stream()
                .filter(i -> i.contratoId().equals(c1))
                .findFirst().orElseThrow().venceEl();
        assertEquals(LocalDate.of(2027, 2, 28), venceC1);

        ResultadoLoteDTO primero = SesionUsuario.ejecutarComo(asistente,
                () -> cobranza.generarMes(febrero2027));
        assertEquals(3, primero.confirmados());
        assertEquals(0, primero.omitidos());
        assertEquals(3L, contar("select count(c) from Cargo c where c.tipo = 'Renta'"));

        ResultadoLoteDTO segundo = SesionUsuario.ejecutarComo(propietario,
                () -> cobranza.generarMes(febrero2027));
        assertEquals(0, segundo.confirmados());
        assertEquals(3, segundo.omitidos());
        assertEquals(3L, contar("select count(c) from Cargo c where c.tipo = 'Renta'"));
        assertTrue(c2 != null && c3 != null);
    }

    @Test
    void cp047YCp049CancelacionEntreContratosConservaCompletosYReanudaSinDuplicados() {
        List<UUID> contratos = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            contratos.add(crearContrato("LOT-" + i, "USD", new BigDecimal("300.00"), 5,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31), "Vigente"));
        }
        YearMonth mes = YearMonth.of(2026, 11);
        AtomicBoolean cancelar = new AtomicBoolean(false);
        AtomicInteger procesados = new AtomicInteger();
        List<ProgresoLoteDTO> eventosProgreso = new ArrayList<>();

        ResultadoLoteDTO parcial = SesionUsuario.ejecutarComo(propietario, () ->
                cobranza.generarMes(mes, cancelar::get, eventosProgreso::add, () -> {
                    if (procesados.incrementAndGet() == 2) {
                        cancelar.set(true);
                    }
                }));

        assertTrue(parcial.cancelado());
        assertEquals(CobranzaService.MSG_059, parcial.mensaje());
        assertEquals(2, parcial.confirmados());
        assertEquals(3, parcial.pendientes());
        assertEquals(2L, contar("select count(c) from Cargo c"));
        assertFalse(eventosProgreso.isEmpty());
        assertTrue(eventosProgreso.getLast().cancelado());

        ResultadoLoteDTO reanudado = SesionUsuario.ejecutarComo(propietario,
                () -> cobranza.generarMes(mes));
        assertFalse(reanudado.cancelado());
        assertEquals(3, reanudado.confirmados());
        assertEquals(2, reanudado.omitidos());
        assertEquals(0, reanudado.pendientes());
        assertEquals(5L, contar("select count(c) from Cargo c"));
        assertEquals(5, contratos.size());
    }

    @Test
    void cp045DosLotesConcurrentesNoDuplicanRentas() throws Exception {
        for (int i = 1; i <= 8; i++) {
            crearContrato("CON-" + i, "USD", new BigDecimal("250.00"), 5,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31), "Vigente");
        }
        YearMonth mes = YearMonth.of(2026, 11);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<ResultadoLoteDTO> f1 = pool.submit(() -> {
                salida.await();
                return SesionUsuario.ejecutarComo(propietario, () -> cobranza.generarMes(mes));
            });
            Future<ResultadoLoteDTO> f2 = pool.submit(() -> {
                salida.await();
                return SesionUsuario.ejecutarComo(asistente, () -> cobranza.generarMes(mes));
            });
            salida.countDown();
            ResultadoLoteDTO r1 = f1.get(15, TimeUnit.SECONDS);
            ResultadoLoteDTO r2 = f2.get(15, TimeUnit.SECONDS);
            assertEquals(8, r1.confirmados() + r2.confirmados());
            assertEquals(8L, contar("select count(c) from Cargo c where c.tipo = 'Renta'"));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void cp022H2GraciaMoraUnicaNoCapitalizaMoraYActualizaVencimientos() {
        configuracion.registrarParametro(propietario, "dias_gracia", "5", null, "Gracia 5 días");
        configuracion.registrarParametro(propietario, "mora_tipo", "Porcentaje", null, "Porcentaje");
        configuracion.registrarParametro(propietario, "mora_valor", "5", null, "5 por ciento");
        configuracion.registrarParametro(propietario, "dias_por_vencer", "30", null, "Umbral 30 días");

        UUID contratoPorVencer = crearContrato("MORA-1", "USD", new BigDecimal("100.00"), 5,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 11, 20), "Vigente");
        UUID rentaId = tx.ejecutar(session -> {
            Cargo renta = new Cargo();
            renta.setContratoId(contratoPorVencer);
            renta.setTipo("Renta");
            renta.setPeriodo(LocalDate.of(2026, 11, 1));
            renta.setFechaEmision(LocalDate.of(2026, 11, 1));
            renta.setVenceEl(LocalDate.of(2026, 11, 5));
            renta.setImporte(new BigDecimal("100.00"));
            renta.setConcepto("Renta noviembre");
            renta.setEstado("Pendiente");
            session.persist(renta);
            return renta.getId();
        });

        // Último día de gracia (vence 5 + 5 = 10 nov): cero mora, contrato pasa a Por vencer.
        ResultadoRevisionH2DTO enGracia = SesionUsuario.ejecutarComo(propietario,
                () -> cobranza.revisarMoraYVencimientos(LocalDate.of(2026, 11, 10)));
        assertEquals(0, enGracia.morasGeneradas());
        assertEquals(1, enGracia.marcadosPorVencer());
        assertEquals("Por vencer", leerContrato(contratoPorVencer).getEstado());

        // Día siguiente (11 nov): genera una mora de 5.00 USD.
        ResultadoRevisionH2DTO trasGracia = SesionUsuario.ejecutarComo(propietario,
                () -> cobranza.revisarMoraYVencimientos(LocalDate.of(2026, 11, 11)));
        assertEquals(1, trasGracia.morasGeneradas());
        Cargo mora = tx.ejecutar(session -> session.find(Cargo.class, trasGracia.cargosMoraGenerados().getFirst()));
        assertEquals("Mora", mora.getTipo());
        assertEquals(rentaId, mora.getCargoOrigenId());
        assertEquals(new BigDecimal("5.00"), mora.getImporte());
        assertEquals(1L, contar("select count(a) from Auditoria a where a.accion = 'MORA_AUTOMATICA'"));

        // Ejecutar H2 de nuevo (incluyendo después de fin del contrato): 0 moras adicionales, no mora sobre Mora, contrato Vencido.
        ResultadoRevisionH2DTO segunda = cobranza.revisarMoraYVencimientosSistema(LocalDate.of(2026, 11, 25));
        assertEquals(0, segunda.morasGeneradas());
        assertEquals(1, segunda.marcadosVencido());
        assertEquals("Vencido", leerContrato(contratoPorVencer).getEstado());
        assertEquals(1L, contar("select count(c) from Cargo c where c.tipo = 'Mora'"));
    }

    @Test
    void cp046H1ContraH2ConcurrentesConservanConsistencia() throws Exception {
        configuracion.registrarParametro(propietario, "dias_gracia", "0", null, "Sin gracia");
        configuracion.registrarParametro(propietario, "mora_tipo", "Fijo", null, "Monto fijo");
        configuracion.registrarParametro(propietario, "mora_valor", "15.00", null, "15 USD fijos");

        List<UUID> contratos = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            UUID cid = crearContrato("H1H2-" + i, "USD", new BigDecimal("200.00"), 5,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31), "Vigente");
            contratos.add(cid);
            tx.ejecutar(session -> {
                Cargo rentaOct = new Cargo();
                rentaOct.setContratoId(cid);
                rentaOct.setTipo("Renta");
                rentaOct.setPeriodo(LocalDate.of(2026, 10, 1));
                rentaOct.setFechaEmision(LocalDate.of(2026, 10, 1));
                rentaOct.setVenceEl(LocalDate.of(2026, 10, 5));
                rentaOct.setImporte(new BigDecimal("200.00"));
                rentaOct.setConcepto("Renta octubre");
                rentaOct.setEstado("Pendiente");
                session.persist(rentaOct);
                return null;
            });
        }

        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<ResultadoLoteDTO> fH1 = pool.submit(() -> {
                salida.await();
                return SesionUsuario.ejecutarComo(propietario,
                        () -> cobranza.generarMes(YearMonth.of(2026, 11)));
            });
            Future<ResultadoRevisionH2DTO> fH2 = pool.submit(() -> {
                salida.await();
                return cobranza.revisarMoraYVencimientosSistema(LocalDate.of(2026, 10, 10));
            });
            salida.countDown();
            ResultadoLoteDTO rH1 = fH1.get(15, TimeUnit.SECONDS);
            ResultadoRevisionH2DTO rH2 = fH2.get(15, TimeUnit.SECONDS);
            assertEquals(4, rH1.confirmados());
            assertEquals(4, rH2.morasGeneradas());
            assertEquals(4L, contar("select count(c) from Cargo c where c.tipo = 'Mora'"));
            long rentasNov = tx.ejecutar(s -> s.createQuery(
                            "select count(c) from Cargo c where c.tipo = 'Renta' and c.periodo = :p", Long.class)
                    .setParameter("p", LocalDate.of(2026, 11, 1))
                    .getSingleResult());
            assertEquals(4L, rentasNov);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void rfM0510CarteraAgrupaTramosYMonedasSeparadasAlCorte() {
        UUID cUsd = crearContrato("CAR-USD", "USD", new BigDecimal("100.00"), 5,
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31), "Vigente");
        UUID cNio = crearContrato("CAR-NIO", "NIO", new BigDecimal("500.00"), 5,
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31), "Vigente");

        LocalDate corte = LocalDate.of(2026, 11, 30);
        tx.ejecutar(session -> {
            crearCargo(session, cUsd, "Servicio", corte, corte.plusDays(2), "10.00"); // Al día
            crearCargo(session, cUsd, "Servicio", LocalDate.of(2026, 11, 1), corte.minusDays(15), "20.00"); // 1-30
            crearCargo(session, cUsd, "Servicio", LocalDate.of(2026, 10, 1), corte.minusDays(45), "30.00"); // 31-60
            crearCargo(session, cUsd, "Servicio", LocalDate.of(2026, 9, 1), corte.minusDays(75), "40.00"); // 61-90
            crearCargo(session, cUsd, "Servicio", LocalDate.of(2026, 7, 1), corte.minusDays(120), "50.00"); // >90
            crearCargo(session, cNio, "Renta", LocalDate.of(2026, 11, 1), corte.minusDays(10), "500.00"); // 1-30 NIO
            return null;
        });

        CarteraResumenDTO resumen = SesionUsuario.ejecutarComo(asistente,
                () -> cobranza.consultarCartera(corte, null, null, null, null));
        assertEquals(6, resumen.items().size());
        assertEquals(2, resumen.totalesPorMoneda().size());
        var usd = resumen.totalesPorMoneda().stream().filter(t -> "USD".equals(t.moneda())).findFirst().orElseThrow();
        var nio = resumen.totalesPorMoneda().stream().filter(t -> "NIO".equals(t.moneda())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("10.00"), usd.alDia());
        assertEquals(new BigDecimal("20.00"), usd.tramo1a30());
        assertEquals(new BigDecimal("30.00"), usd.tramo31a60());
        assertEquals(new BigDecimal("40.00"), usd.tramo61a90());
        assertEquals(new BigDecimal("50.00"), usd.tramoMasDe90());
        assertEquals(new BigDecimal("150.00"), usd.totalSaldo());
        assertEquals(new BigDecimal("500.00"), nio.tramo1a30());
        assertEquals(new BigDecimal("500.00"), nio.totalSaldo());
    }

    @Test
    void cp025AnulacionDeCargoConMotivoYBloqueoConAplicacionesActivas() {
        UUID cUsd = crearContrato("ANU-1", "USD", new BigDecimal("100.00"), 5,
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31), "Vigente");
        UUID cargoConAbono = SesionUsuario.ejecutarComo(propietario, () -> {
            try {
                return cargoService.guardar(new CargoService.Solicitud(null, 0, cUsd, "Servicio",
                        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 5), new BigDecimal("100.00"), "Agua", "Pendiente"));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        UUID cargoSinAbono = SesionUsuario.ejecutarComo(propietario, () -> {
            try {
                return cargoService.guardar(new CargoService.Solicitud(null, 0, cUsd, "Otro",
                        LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 11, 5), new BigDecimal("75.00"), "Multa", "Pendiente"));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        tx.ejecutar(session -> {
            Pago pago = new Pago(null, cUsd, null, LocalDate.of(2026, 10, 4), "USD",
                    new BigDecimal("40.00"), new BigDecimal("40.00"), "Efectivo", null, null, "Registrado");
            pago.aplicarConversion("USD", new BigDecimal("40.00"), null);
            session.persist(pago);
            session.flush();
            session.persist(new AplicacionPago(pago.getId(), cargoConAbono, new BigDecimal("40.00")));
            return null;
        });

        assertThrows(IllegalArgumentException.class, () -> anularCargo(propietario, cargoSinAbono, "   "));
        assertThrows(SecurityException.class, () -> anularCargo(asistente, cargoSinAbono, "Sin permiso"));
        assertThrows(IllegalStateException.class, () -> anularCargo(propietario, cargoConAbono, "Tiene abono"));

        anularCargo(propietario, cargoSinAbono, "Error de digitación");
        Cargo anulado = tx.ejecutar(session -> session.find(Cargo.class, cargoSinAbono));
        assertEquals("Cancelado", anulado.getEstadoPersistido());
        assertEquals(new BigDecimal("75.00"), anulado.getImporte());
        assertEquals("Error de digitación", anulado.getMotivoAnulacion());
        assertNotNull(anulado.getAnuladoEn());
    }

    private void anularCargo(SesionUsuario sesion, UUID cargoId, String motivo) {
        SesionUsuario.ejecutarComo(sesion, () -> {
            try {
                cargoService.anular(cargoId, motivo);
                return null;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private UUID crearContrato(String codigoUnidad, String moneda, BigDecimal canon, int diaPago,
                               LocalDate inicio, LocalDate fin, String estado) {
        return tx.ejecutar(session -> {
            Unidad u = new Unidad();
            u.setPropiedadId(propiedadId);
            u.setCodigo(codigoUnidad);
            u.setTipo("Apartamento");
            u.setEstado("Ocupada");
            u.setMoneda(moneda);
            u.setCanonReferencia(canon);
            session.persist(u);

            Inquilino i = new Inquilino();
            i.setNombre("Inquilino " + codigoUnidad);
            session.persist(i);
            session.flush();

            Contrato c = new Contrato();
            c.setUnidadId(u.getId());
            c.setInquilinoId(i.getId());
            c.setInicio(inicio);
            c.setFin(fin);
            c.setCanon(canon);
            c.setMoneda(moneda);
            c.setDiaPago(diaPago);
            c.setDepositoPactado(BigDecimal.ZERO);
            c.setEstado("Vigente");
            if ("Terminado".equals(estado)) {
                c.terminar(fin, "Finalizado en prueba");
            } else if (!"Vigente".equals(estado)) {
                c.setEstado(estado);
            }
            c.inicializarGarantiaSeguimiento();
            session.persist(c);
            session.flush();
            return c.getId();
        });
    }

    private void crearCargo(org.hibernate.Session session, UUID contratoId, String tipo,
                            LocalDate emision, LocalDate venceEl, String importe) {
        Cargo c = new Cargo();
        c.setContratoId(contratoId);
        c.setTipo(tipo);
        c.setPeriodo(emision.withDayOfMonth(1));
        c.setFechaEmision(emision);
        c.setVenceEl(venceEl);
        c.setImporte(new BigDecimal(importe));
        c.setConcepto("Cargo " + venceEl);
        c.setEstado("Pendiente");
        session.persist(c);
    }

    private Contrato leerContrato(UUID id) {
        return tx.ejecutar(session -> session.find(Contrato.class, id));
    }

    private long contar(String hql) {
        return tx.ejecutar(session -> session.createQuery(hql, Long.class).getSingleResult());
    }
}
