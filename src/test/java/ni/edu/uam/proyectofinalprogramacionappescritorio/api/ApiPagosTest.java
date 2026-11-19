package ni.edu.uam.proyectofinalprogramacionappescritorio.api;

import java.util.concurrent.atomic.AtomicBoolean;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import com.google.gson.Gson;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ErrorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto.ServidorSpringPrueba;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto.MetricasResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto.TotalMonedaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.ServicioPagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.DistribucionRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.EstadoCuentaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoDetalleResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoRegistroResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.ReciboResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.PostgresTestSupport;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propietario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SolicitudIdempotente;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RelojService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PasswordHasher;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiPagosTest {
    private static final char[] PASSWORD = "ClaveSintetica-2026".toCharArray();
    private static final Gson GSON = new Gson();
    private static final LocalDate FECHA_PAGO = LocalDate.of(2026, 11, 10);
    private static final LocalDate CORTE = LocalDate.of(2026, 11, 30);
    private static final BigDecimal TASA = new BigDecimal("36.6243");

    private static final AtomicBoolean DESFASAR_RELOJ = new AtomicBoolean();
    private static SessionFactory factory;
    private static HibernateTransactions tx;
    private static ServidorSpringPrueba servidor;
    private static HttpClient cliente;
    private static String base;
    private static String nombrePropietario;
    private static String nombreAsistente;
    private static Long idPropietario;

    private UUID contratoId;
    private UUID cargoRentaId;
    private UUID cargoServicioId;

    @BeforeAll
    static void iniciar() throws Exception {
        Properties p = new Properties();
        if (Boolean.getBoolean("latch.test.postgres")) {
            p = PostgresTestSupport.propiedadesHibernate();
        } else {
            p.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
            p.setProperty("hibernate.connection.url",
                    "jdbc:h2:mem:latch_api_pagos_" + UUID.randomUUID() + ";MODE=PostgreSQL");
            p.setProperty("hibernate.connection.username", "sa");
            p.setProperty("hibernate.connection.password", "");
        }
        p.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        p.setProperty("hibernate.jdbc.time_zone", "UTC");
        p.setProperty("hibernate.show_sql", "false");

        p.put("hibernate.session_factory.statement_inspector", (StatementInspector) sql ->
                DESFASAR_RELOJ.get() && sql.strip().equalsIgnoreCase("select current_timestamp")
                        ? (Boolean.getBoolean("latch.test.postgres")
                            ? "select current_timestamp - interval '10 minutes'"
                            : "select dateadd('MINUTE', -10, current_timestamp)")
                        : sql);
        factory = HibernateUtil.construir(p);
        if (Boolean.getBoolean("latch.test.postgres")) {
            PostgresTestSupport.aplicarMigraciones(p);
        }
        tx = new HibernateTransactions(factory);
        String hash = new PasswordHasher().crear(PASSWORD);
        nombrePropietario = "prop-" + UUID.randomUUID().toString().substring(0, 8);
        nombreAsistente = "asis-" + UUID.randomUUID().toString().substring(0, 8);
        tx.ejecutar(session -> {
            Usuario propietario = new Usuario(nombrePropietario, hash, Rol.PROPIETARIO);
            Usuario asistente = new Usuario(nombreAsistente, hash, Rol.ASISTENTE);
            session.persist(propietario);
            session.persist(asistente);
            session.flush();
            idPropietario = propietario.getId();
            session.persist(new Propietario(propietario, "Propietario de pagos",
                    "pagos@latch.test", null));
            return null;
        });
        SesionUsuario.cerrarSesion();

        servidor = new ServidorSpringPrueba(factory);
        base = servidor.urlBase();
        cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @AfterAll
    static void cerrar() {
        if (cliente != null) cliente.close();
        if (servidor != null) servidor.close();
        if (factory != null && !factory.isClosed()) factory.close();
        if (Boolean.getBoolean("latch.test.postgres")) {
            PostgresTestSupport.eliminarFuncionRecibo();
        }
        SesionUsuario.cerrarSesion();
    }

    @BeforeEach
    void limpiarYSembrar() {
        tx.ejecutar(session -> {
            session.createNativeMutationQuery("delete from aplicacion_pago").executeUpdate();
            session.createNativeMutationQuery("delete from solicitud_idempotente").executeUpdate();
            session.createNativeMutationQuery("delete from recibo").executeUpdate();
            session.createNativeMutationQuery("delete from auditoria").executeUpdate();
            session.createNativeMutationQuery("delete from pago").executeUpdate();
            session.createNativeMutationQuery("delete from cargo").executeUpdate();
            session.createNativeMutationQuery("delete from garantia").executeUpdate();
            session.createNativeMutationQuery("delete from contrato").executeUpdate();
            session.createNativeMutationQuery("delete from unidad").executeUpdate();
            session.createNativeMutationQuery("delete from inquilino").executeUpdate();
            session.createNativeMutationQuery("delete from propiedad").executeUpdate();
            session.createNativeMutationQuery("delete from tipo_cambio").executeUpdate();
            return null;
        });
        tx.ejecutar(session -> {
            Propietario propietario = session.find(Propietario.class, idPropietario);
            Propiedad propiedad = new Propiedad();
            propiedad.setNombre("Edificio de pagos");
            propiedad.setDireccion("Calle de pagos");
            propiedad.setPropietario(propietario);
            session.persist(propiedad);

            Unidad unidad = new Unidad();
            unidad.setPropiedadId(propiedad.getId());
            unidad.setCodigo("P-01");
            unidad.setTipo("Apartamento");
            unidad.setEstado("Disponible");
            unidad.setCanonReferencia(new BigDecimal("1000.00"));
            unidad.setMoneda("USD");
            session.persist(unidad);

            Inquilino inquilino = new Inquilino();
            inquilino.setNombre("Inquilino de pagos");
            inquilino.setIdentificacion("PAG-" + UUID.randomUUID());
            session.persist(inquilino);
            session.flush();

            Contrato contrato = new Contrato();
            contrato.setUnidadId(unidad.getId());
            contrato.setInquilinoId(inquilino.getId());
            contrato.setInicio(LocalDate.of(2026, 1, 1));
            contrato.setFin(LocalDate.of(2026, 12, 31));
            contrato.setCanon(new BigDecimal("1000.00"));
            contrato.setMoneda("USD");
            contrato.setDiaPago(5);
            contrato.setDepositoPactado(BigDecimal.ZERO);
            contrato.setEstado("Vigente");
            session.persist(contrato);
            session.flush();
            contratoId = contrato.getId();

            Cargo renta = new Cargo();
            renta.setContratoId(contratoId);
            renta.setTipo("Renta");
            renta.setPeriodo(LocalDate.of(2026, 11, 1));
            renta.setFechaEmision(LocalDate.of(2026, 11, 1));
            renta.setVenceEl(LocalDate.of(2026, 11, 5));
            renta.setImporte(new BigDecimal("400.00"));
            renta.setConcepto("Renta de noviembre");
            renta.setEstado("Pendiente");
            session.persist(renta);

            Cargo servicio = new Cargo();
            servicio.setContratoId(contratoId);
            servicio.setTipo("Servicio");
            servicio.setPeriodo(LocalDate.of(2026, 11, 1));
            servicio.setFechaEmision(LocalDate.of(2026, 11, 1));
            servicio.setVenceEl(LocalDate.of(2026, 11, 10));
            servicio.setImporte(new BigDecimal("600.00"));
            servicio.setConcepto("Servicios de noviembre");
            servicio.setEstado("Pendiente");
            session.persist(servicio);
            session.flush();
            cargoRentaId = renta.getId();
            cargoServicioId = servicio.getId();

            Usuario actor = session.find(Usuario.class, idPropietario);
            session.persist(TipoCambio.autorizada(FECHA_PAGO, TASA, "BCN",
                    Instant.now(), actor, "Tasa de prueba"));
            return null;
        });
    }

    @Test
    void requiereTokenYValidaCabeceraYCuerpo() throws Exception {
        PagoRequest solicitud = pago("1000.00", null);

        assertEquals(401, postJson("/api/v1/pagos", GSON.toJson(solicitud), null, null)
                .statusCode());

        String token = login(nombrePropietario);
        HttpResponse<String> sinClave = postJson("/api/v1/pagos",
                GSON.toJson(solicitud), token, null);
        assertEquals(400, sinClave.statusCode());
        assertEquals("SOLICITUD_INVALIDA", error(sinClave).error().codigo());

        HttpResponse<String> claveInvalida = postJson("/api/v1/pagos",
                GSON.toJson(solicitud), token, "no-es-uuid");
        assertEquals(400, claveInvalida.statusCode());

        HttpResponse<String> jsonInvalido = postJson("/api/v1/pagos",
                "{esto no es json", token, UUID.randomUUID().toString());
        assertEquals(400, jsonInvalido.statusCode());
        assertEquals("JSON_INVALIDO", error(jsonInvalido).error().codigo());

        String grande = "x".repeat(300 * 1024);
        HttpResponse<String> demasiadoGrande = postJson("/api/v1/pagos",
                "{\"referencia\":\"" + grande + "\"}", token,
                UUID.randomUUID().toString());
        assertEquals(413, demasiadoGrande.statusCode());
        assertEquals("CUERPO_DEMASIADO_GRANDE", error(demasiadoGrande).error().codigo());
    }

    @Test
    void paginaPagosYCargosConLimiteDeCincuentaYConsultasAcotadas() throws Exception {
        tx.ejecutar(session -> {
            for (int indice = 0; indice < 51; indice++) {
                BigDecimal importe = new BigDecimal("10.00");
                Pago pago = new Pago(null, contratoId, null, FECHA_PAGO.minusDays(indice),
                        "USD", importe, importe, "Transferencia", "REF-PAG-" + indice,
                        null, "Registrado");
                pago.aplicarConversion("USD", importe, null);
                session.persist(pago);
            }
            for (int indice = 0; indice < 49; indice++) {
                Cargo cargo = new Cargo();
                cargo.setContratoId(contratoId);
                cargo.setTipo("Servicio");
                cargo.setPeriodo(LocalDate.of(2026, 10, 1).minusMonths(indice));
                cargo.setFechaEmision(LocalDate.of(2026, 10, 1).minusMonths(indice));
                cargo.setVenceEl(LocalDate.of(2026, 10, 5).minusMonths(indice));
                cargo.setImporte(new BigDecimal("25.00"));
                cargo.setConcepto("Cargo de paginación " + indice);
                cargo.setEstado("Pendiente");
                session.persist(cargo);
            }
            return null;
        });

        String token = login(nombrePropietario);
        factory.getStatistics().setStatisticsEnabled(true);
        factory.getStatistics().clear();
        HttpResponse<String> pagosPrimera = get("/api/v1/pagos?tamano=50", token);
        assertEquals(200, pagosPrimera.statusCode(), pagosPrimera.body());
        var pagosPagina = GSON.fromJson(pagosPrimera.body(), com.google.gson.JsonObject.class);
        assertEquals(50, pagosPagina.get("tamano").getAsInt());
        assertEquals(51, pagosPagina.get("total").getAsInt());
        assertEquals(50, pagosPagina.getAsJsonArray("elementos").size());
        long consultasPagos = factory.getStatistics().getPrepareStatementCount();
        assertTrue(consultasPagos < 25,
                "la consulta de pagos no debe crecer con el número de filas de la página: " + consultasPagos);

        HttpResponse<String> pagosSegunda = get("/api/v1/pagos?pagina=2&tamano=50", token);
        assertEquals(200, pagosSegunda.statusCode(), pagosSegunda.body());
        var pagosPaginaDos = GSON.fromJson(pagosSegunda.body(), com.google.gson.JsonObject.class);
        assertEquals(1, pagosPaginaDos.getAsJsonArray("elementos").size());

        factory.getStatistics().clear();
        HttpResponse<String> cargosPrimera = get("/api/v1/cargos?tamano=50", token);
        assertEquals(200, cargosPrimera.statusCode(), cargosPrimera.body());
        var cargosPagina = GSON.fromJson(cargosPrimera.body(), com.google.gson.JsonObject.class);
        assertEquals(50, cargosPagina.get("tamano").getAsInt());
        assertEquals(51, cargosPagina.get("total").getAsInt());
        assertEquals(50, cargosPagina.getAsJsonArray("elementos").size());
        long consultasCargos = factory.getStatistics().getPrepareStatementCount();
        assertTrue(consultasCargos < 25,
                "la consulta de cargos no debe crecer con el número de filas de la página: " + consultasCargos);

        HttpResponse<String> cargosSegunda = get("/api/v1/cargos?pagina=2&tamano=50", token);
        assertEquals(200, cargosSegunda.statusCode(), cargosSegunda.body());
        var cargosPaginaDos = GSON.fromJson(cargosSegunda.body(), com.google.gson.JsonObject.class);
        assertEquals(1, cargosPaginaDos.getAsJsonArray("elementos").size());

        assertEquals(400, get("/api/v1/pagos?tamano=51", token).statusCode());
        assertEquals(400, get("/api/v1/cargos?tamano=51", token).statusCode());
    }

    @Test
    void busquedasDePagosYCargosTratanComodinesComoTextoLiteral() throws Exception {
        tx.ejecutar(session -> {
            Cargo cargo = session.find(Cargo.class, cargoRentaId);
            cargo.setConcepto("Cargo %_! literal");
            BigDecimal monto = new BigDecimal("10.00");
            Pago pago = new Pago(null, contratoId, null, FECHA_PAGO, "USD", monto, monto,
                    "Transferencia", "Referencia %_! literal", null, "Registrado");
            pago.aplicarConversion("USD", monto, null);
            session.persist(pago);
            return null;
        });

        String token = login(nombrePropietario);
        for (String consulta : new String[]{"%25", "%5F", "%21"}) {
            HttpResponse<String> pagos = get("/api/v1/pagos?texto=" + consulta, token);
            HttpResponse<String> cargos = get("/api/v1/cargos?texto=" + consulta, token);
            assertEquals(200, pagos.statusCode(), pagos.body());
            assertEquals(200, cargos.statusCode(), cargos.body());
            assertEquals(1, GSON.fromJson(pagos.body(), com.google.gson.JsonObject.class)
                    .get("total").getAsInt(), consulta);
            assertEquals(1, GSON.fromJson(cargos.body(), com.google.gson.JsonObject.class)
                    .get("total").getAsInt(), consulta);
        }
    }

    @Test
    void rechazaExponenteExtremoSinGuardarPago() throws Exception {
        String token = login(nombrePropietario);
        long pagosAntes = contar("select count(p) from Pago p");

        HttpResponse<String> respuesta = postJson("/api/v1/pagos",
                GSON.toJson(pago("1E+999999999", null)), token, UUID.randomUUID().toString());

        assertEquals(422, respuesta.statusCode(), respuesta.body());
        assertEquals(pagosAntes, contar("select count(p) from Pago p"));
    }

    @Test
    void registraUnaSolaVezYConsultaPagoYRecibo() throws Exception {
        String token = login(nombrePropietario);
        String clave = UUID.randomUUID().toString();
        PagoRequest solicitud = pago("1000.00", List.of(
                new DistribucionRequest(cargoRentaId.toString(), "400.00"),
                new DistribucionRequest(cargoServicioId.toString(), "600.00")));

        Instant inicioSolicitud = tx.ejecutar(s -> new RelojService().leer(s).instante());
        HttpResponse<String> primera = postJson("/api/v1/pagos",
                GSON.toJson(solicitud), token, clave);
        Instant finSolicitud = tx.ejecutar(s -> new RelojService().leer(s).instante());
        assertEquals(201, primera.statusCode(), primera.body());
        PagoRegistroResponse registro = GSON.fromJson(primera.body(), PagoRegistroResponse.class);
        assertNotNull(registro.pagoId());
        assertTrue(registro.numeroRecibo() > 0);
        assertEquals("1000.00", registro.montoContrato());
        assertTrue(com.google.gson.JsonParser.parseString(primera.body()).getAsJsonObject()
                .get("montoContrato").getAsJsonPrimitive().isString());
        assertEquals("0.00", registro.credito());
        Pago pagoGuardado = tx.ejecutar(session ->
                session.find(Pago.class, UUID.fromString(registro.pagoId())));
        assertNotNull(pagoGuardado.getCreadoEn());
        assertTrue(!pagoGuardado.getCreadoEn().isBefore(inicioSolicitud));
        assertTrue(!pagoGuardado.getCreadoEn().isAfter(finSolicitud));

        HttpResponse<String> repetida = postJson("/api/v1/pagos",
                GSON.toJson(solicitud), token, clave);
        assertEquals(201, repetida.statusCode(), repetida.body());
        PagoRegistroResponse otra = GSON.fromJson(repetida.body(), PagoRegistroResponse.class);
        assertEquals(registro.pagoId(), otra.pagoId());
        assertEquals(registro.numeroRecibo(), otra.numeroRecibo());
        assertEquals(primera.body(), repetida.body());
        assertEquals(1, contar("select count(p) from Pago p"));

        HttpResponse<String> distinta = postJson("/api/v1/pagos",
                GSON.toJson(pago("999.00", null)), token, clave);
        assertEquals(409, distinta.statusCode());
        assertEquals("CONFLICTO", error(distinta).error().codigo());

        HttpResponse<String> detalleRespuesta = get("/api/v1/pagos/" + registro.pagoId(), token);
        assertEquals(200, detalleRespuesta.statusCode(), detalleRespuesta.body());
        PagoDetalleResponse detalle = GSON.fromJson(detalleRespuesta.body(),
                PagoDetalleResponse.class);
        assertEquals("1000.00", detalle.montoContrato());
        assertEquals(2, detalle.aplicaciones().size());
        assertEquals(registro.numeroRecibo(), detalle.numeroRecibo());

        HttpResponse<String> reciboRespuesta = get(
                "/api/v1/recibos/" + registro.numeroRecibo(), token);
        assertEquals(200, reciboRespuesta.statusCode(), reciboRespuesta.body());
        ReciboResponse recibo = GSON.fromJson(reciboRespuesta.body(), ReciboResponse.class);
        assertEquals("1000.00", recibo.montoOriginal());
        assertTrue(com.google.gson.JsonParser.parseString(reciboRespuesta.body()).getAsJsonObject()
                .get("montoOriginal").getAsJsonPrimitive().isString());
        assertFalse(recibo.anulado());

        assertEquals(404, get("/api/v1/pagos/" + UUID.randomUUID(), token).statusCode());
        assertEquals(404, get("/api/v1/recibos/999999", token).statusCode());
    }

    @Test
    void claveDistintaRegistraUnPagoNuevo() throws Exception {
        String token = login(nombrePropietario);
        PagoRequest solicitud = pago("1000.00", null);

        HttpResponse<String> primera = postJson("/api/v1/pagos", GSON.toJson(solicitud),
                token, UUID.randomUUID().toString());
        HttpResponse<String> segunda = postJson("/api/v1/pagos", GSON.toJson(solicitud),
                token, UUID.randomUUID().toString());

        assertEquals(201, primera.statusCode(), primera.body());
        assertEquals(201, segunda.statusCode(), segunda.body());
        PagoRegistroResponse pagoUno = GSON.fromJson(primera.body(), PagoRegistroResponse.class);
        PagoRegistroResponse pagoDos = GSON.fromJson(segunda.body(), PagoRegistroResponse.class);
        assertNotEquals(pagoUno.pagoId(), pagoDos.pagoId());
        assertEquals(pagoUno.numeroRecibo() + 1, pagoDos.numeroRecibo());
        assertEquals(2, contar("select count(p) from Pago p"));
    }

    @Test
    void rollbackNoConsumeNumeroDeRecibo() throws Exception {
        long numeroEsperado = Boolean.getBoolean("latch.test.postgres")
                ? siguienteNumeroRecibo() : 1L;
        String token = login(nombrePropietario);
        PagoRequest solicitudInvalida = pago("1000.00", List.of(
                new DistribucionRequest(UUID.randomUUID().toString(), "100.00")));
        HttpResponse<String> rechazada = postJson("/api/v1/pagos", GSON.toJson(solicitudInvalida),
                token, UUID.randomUUID().toString());
        assertEquals(422, rechazada.statusCode(), rechazada.body());
        assertEquals(0, contar("select count(p) from Pago p"));
        assertEquals(0, contar("select count(r) from Recibo r"));

        String claveValida = UUID.randomUUID().toString();
        String cuerpoValido = GSON.toJson(pago("1000.00", null));
        if (Boolean.getBoolean("latch.test.postgres")) {
            PostgresTestSupport.impedirAuditoriaFinanciera(tx);
            try {
                var fallida = postJson("/api/v1/pagos", cuerpoValido, token, claveValida);
                assertEquals(500, fallida.statusCode(), fallida.body());
                assertEquals("ERROR_INTERNO", error(fallida).error().codigo());
                assertEquals(numeroEsperado, siguienteNumeroRecibo());
                assertEquals(0, contar("select count(p) from Pago p"));
                assertEquals(0, contar("select count(r) from Recibo r"));
                assertEquals(0, contar("select count(i) from SolicitudIdempotente i"));
                assertEquals(0, contar("select count(a) from Auditoria a where a.accion = 'PAGO_REGISTRADO'"));
            } finally {
                PostgresTestSupport.permitirAuditoriaFinanciera(tx);
            }
        }
        HttpResponse<String> confirmada = postJson("/api/v1/pagos", cuerpoValido, token, claveValida);
        assertEquals(201, confirmada.statusCode(), confirmada.body());
        PagoRegistroResponse resultado = GSON.fromJson(confirmada.body(),
                PagoRegistroResponse.class);
        assertEquals(numeroEsperado, resultado.numeroRecibo());
        assertEquals(1, contar("select count(r) from Recibo r"));
        assertEquals(1, contar("select count(i) from SolicitudIdempotente i"));
        assertEquals(1, contar("select count(a) from Auditoria a where a.accion = 'PAGO_REGISTRADO'"));
    }

    @Test
    void reciboConservaUnResumenDeContratoMayorA300Caracteres() throws Exception {
        tx.ejecutar(session -> {
            Contrato contrato = session.find(Contrato.class, contratoId);
            Inquilino inquilino = session.find(Inquilino.class, contrato.getInquilinoId());
            Unidad unidad = session.find(Unidad.class, contrato.getUnidadId());
            Propiedad propiedad = session.find(Propiedad.class, unidad.getPropiedadId());
            inquilino.setNombre("I".repeat(160));
            propiedad.setNombre("P".repeat(120));
            unidad.setCodigo("U".repeat(30));
            unidad.setTipo("T".repeat(24));
            return null;
        });

        String token = login(nombrePropietario);
        HttpResponse<String> respuesta = postJson("/api/v1/pagos",
                GSON.toJson(pago("1000.00", null)), token, UUID.randomUUID().toString());
        assertEquals(201, respuesta.statusCode(), respuesta.body());
        PagoRegistroResponse pago = GSON.fromJson(respuesta.body(), PagoRegistroResponse.class);
        HttpResponse<String> respuestaRecibo = get(
                "/api/v1/recibos/" + pago.numeroRecibo(), token);
        assertEquals(200, respuestaRecibo.statusCode(), respuestaRecibo.body());
        ReciboResponse recibo = GSON.fromJson(respuestaRecibo.body(), ReciboResponse.class);
        assertEquals(343, recibo.contratoInfo().length());
    }

    @Test
    void limpiezaRespetaRetencionYSoloEliminaSolicitudesCompletadas() {
        Instant ahora = Instant.parse("2026-11-14T12:00:00Z");
        String claveExpirada = UUID.randomUUID().toString();
        String claveReciente = UUID.randomUUID().toString();
        String claveEnProceso = UUID.randomUUID().toString();

        tx.ejecutar(session -> {
            SolicitudIdempotente expirada = new SolicitudIdempotente(claveExpirada,
                    idPropietario, "PAGOS", "a".repeat(64), ahora.minus(Duration.ofDays(45)));
            expirada.completar(201, "{}", ahora.minus(Duration.ofDays(31)));
            session.persist(expirada);

            SolicitudIdempotente reciente = new SolicitudIdempotente(claveReciente,
                    idPropietario, "PAGOS", "b".repeat(64), ahora.minus(Duration.ofDays(40)));
            reciente.completar(201, "{}", ahora.minus(Duration.ofDays(29)));
            session.persist(reciente);

            session.persist(new SolicitudIdempotente(claveEnProceso, idPropietario,
                    "PAGOS", "c".repeat(64), ahora.minus(Duration.ofDays(100))));
            return null;
        });

        ServicioPagos servicio = new ServicioPagos(tx, Clock.fixed(ahora, ZoneOffset.UTC));
        assertEquals(1, servicio.limpiarSolicitudesExpiradas());
        assertEquals(0, contar("select count(s) from SolicitudIdempotente s where s.clave = '"
                + claveExpirada + "'"));
        assertEquals(1, contar("select count(s) from SolicitudIdempotente s where s.clave = '"
                + claveReciente + "'"));
        assertEquals(1, contar("select count(s) from SolicitudIdempotente s where s.clave = '"
                + claveEnProceso + "'"));
    }

    @Test
    void convierteConTasaAutorizadaYFallaSinTasa() throws Exception {
        String token = login(nombrePropietario);
        PagoRequest solicitud = new PagoRequest(contratoId.toString(),
                FECHA_PAGO.toString(), "NIO", "36624.30", "Transferencia",
                "REF-NIO", null, null);

        HttpResponse<String> respuesta = postJson("/api/v1/pagos",
                GSON.toJson(solicitud), token, UUID.randomUUID().toString());
        assertEquals(201, respuesta.statusCode(), respuesta.body());
        PagoRegistroResponse registro = GSON.fromJson(respuesta.body(),
                PagoRegistroResponse.class);
        assertEquals("1000.00", registro.montoContrato());
        assertTrue(com.google.gson.JsonParser.parseString(respuesta.body()).getAsJsonObject()
                .get("montoContrato").getAsJsonPrimitive().isString());
        assertEquals("1000.00", registro.credito());
        assertEquals(1, contar("select count(p) from Pago p"));

        PagoRequest sinTasa = new PagoRequest(contratoId.toString(),
                FECHA_PAGO.plusDays(1).toString(), "NIO", "36624.30",
                "Transferencia", "REF-SIN-TASA", null, null);
        HttpResponse<String> fallo = postJson("/api/v1/pagos",
                GSON.toJson(sinTasa), token, UUID.randomUUID().toString());
        assertEquals(422, fallo.statusCode(), fallo.body());
        assertEquals("ENTIDAD_NO_PROCESABLE", error(fallo).error().codigo());
        assertTrue(error(fallo).error().mensaje().toLowerCase().contains("tasa"));
        assertEquals(1, contar("select count(p) from Pago p"));
        assertEquals(1, contar("select count(s) from SolicitudIdempotente s"));
    }

    @Test
    void distribucionInvalidaNoEscribeNada() throws Exception {
        String token = login(nombrePropietario);

        PagoRequest exceso = pago("1000.00", List.of(
                new DistribucionRequest(cargoRentaId.toString(), "500.00"),
                new DistribucionRequest(cargoServicioId.toString(), "600.00")));
        assertEquals(422, postJson("/api/v1/pagos", GSON.toJson(exceso), token,
                UUID.randomUUID().toString()).statusCode());

        PagoRequest repetido = pago("1000.00", List.of(
                new DistribucionRequest(cargoRentaId.toString(), "100.00"),
                new DistribucionRequest(cargoRentaId.toString(), "100.00")));
        assertEquals(422, postJson("/api/v1/pagos", GSON.toJson(repetido), token,
                UUID.randomUUID().toString()).statusCode());

        PagoRequest ajeno = pago("1000.00", List.of(
                new DistribucionRequest(UUID.randomUUID().toString(), "100.00")));
        assertEquals(422, postJson("/api/v1/pagos", GSON.toJson(ajeno), token,
                UUID.randomUUID().toString()).statusCode());

        assertEquals(0, contar("select count(p) from Pago p"));
        assertEquals(0, contar("select count(s) from SolicitudIdempotente s"));
    }

    @Test
    void soloElPropietarioAnulaYQuedaAuditado() throws Exception {
        String tokenPropietario = login(nombrePropietario);
        String tokenAsistente = login(nombreAsistente);
        HttpResponse<String> registro = postJson("/api/v1/pagos",
                GSON.toJson(pago("1000.00", null)), tokenPropietario,
                UUID.randomUUID().toString());
        assertEquals(201, registro.statusCode(), registro.body());
        PagoRegistroResponse pago = GSON.fromJson(registro.body(), PagoRegistroResponse.class);

        assertEquals(403, postJson("/api/v1/pagos/" + pago.pagoId() + "/anulacion",
                "{\"motivo\":\"Prueba\"}", tokenAsistente, null).statusCode());
        assertEquals(422, postJson("/api/v1/pagos/" + pago.pagoId() + "/anulacion",
                "{\"motivo\":\"\"}", tokenPropietario, null).statusCode());

        HttpResponse<String> anulacion = postJson("/api/v1/pagos/" + pago.pagoId()
                + "/anulacion", "{\"motivo\":\"Pago duplicado\"}", tokenPropietario, null);
        assertEquals(200, anulacion.statusCode(), anulacion.body());
        PagoDetalleResponse detalle = GSON.fromJson(anulacion.body(),
                PagoDetalleResponse.class);
        assertEquals("Anulado", detalle.estado());
        assertEquals("Pago duplicado", detalle.motivoAnulacion());
        assertEquals(idPropietario, detalle.anuladoPor());
        assertNotNull(detalle.anuladoEn());
        assertTrue(detalle.anuladoEn().endsWith("-06:00"));

        assertEquals(409, postJson("/api/v1/pagos/" + pago.pagoId() + "/anulacion",
                "{\"motivo\":\"Otra vez\"}", tokenPropietario, null).statusCode());
        assertEquals(1, contar("select count(a) from Auditoria a where a.accion = 'PAGO_ANULADO'"));

        HttpResponse<String> recibo = get("/api/v1/recibos/" + pago.numeroRecibo(),
                tokenPropietario);
        assertEquals(200, recibo.statusCode(), recibo.body());
        assertTrue(GSON.fromJson(recibo.body(), ReciboResponse.class).anulado());
    }

    @Test
    void estadoCuentaCalculaSaldoYCredito() throws Exception {
        String token = login(nombrePropietario);
        HttpResponse<String> registro = postJson("/api/v1/pagos",
                GSON.toJson(pago("1000.00", List.of(
                        new DistribucionRequest(cargoRentaId.toString(), "400.00")))),
                token, UUID.randomUUID().toString());
        assertEquals(201, registro.statusCode(), registro.body());

        HttpResponse<String> respuesta = get("/api/v1/contratos/" + contratoId
                + "/estado-cuenta?corte=" + CORTE, token);
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        EstadoCuentaResponse estado = GSON.fromJson(respuesta.body(),
                EstadoCuentaResponse.class);
        assertEquals(CORTE.toString(), estado.corte());
        assertEquals("USD", estado.moneda());
        assertEquals(2, estado.cargos().size());
        assertEquals(1, estado.pagos().size());
        assertEquals("1000.00", estado.totales().cargos());
        assertEquals("400.00", estado.totales().pagado());
        assertEquals("600.00", estado.totales().saldo());
        assertEquals("600.00", estado.totales().credito());
        assertEquals("Pagado", estado.cargos().get(0).estado());
        assertEquals("Vencido", estado.cargos().get(1).estado());
        assertEquals("400.00", estado.pagos().get(0).aplicado());
        assertEquals("600.00", estado.pagos().get(0).credito());

        assertEquals(404, get("/api/v1/contratos/" + UUID.randomUUID()
                + "/estado-cuenta?corte=" + CORTE, token).statusCode());
        assertEquals(400, get("/api/v1/contratos/" + contratoId
                + "/estado-cuenta?corte=no-es-fecha", token).statusCode());
    }

    @Test
    void estadoCuentaDerivaVencimientoYAnulacionAlCorte() throws Exception {
        String token = login(nombrePropietario);
        tx.ejecutar(session -> {
            session.find(Cargo.class, cargoServicioId).setEstado("Cancelado");
            return null;
        });

        HttpResponse<String> respuesta = get("/api/v1/contratos/" + contratoId
                + "/estado-cuenta?corte=" + CORTE, token);

        assertEquals(200, respuesta.statusCode(), respuesta.body());
        EstadoCuentaResponse estado = GSON.fromJson(respuesta.body(),
                EstadoCuentaResponse.class);
        assertEquals(2, estado.cargos().size());
        assertTrue(estado.cargos().stream().anyMatch(c -> "Vencido".equals(c.estado())));
        assertTrue(estado.cargos().stream().anyMatch(c -> "Anulado".equals(c.estado())));
        assertEquals("400.00", estado.totales().cargos());
        assertEquals("400.00", estado.totales().saldo());
    }

    @Test
    void metricasDevuelveTotalesComoTextoYSeparadosPorMoneda() throws Exception {
        String token = login(nombrePropietario);
        assertEquals(201, postJson("/api/v1/pagos",
                GSON.toJson(pago("1000.00", null)), token,
                UUID.randomUUID().toString()).statusCode());

        HttpResponse<String> respuesta = get("/api/v1/dashboard/metricas", token);
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        MetricasResponse metricas = GSON.fromJson(respuesta.body(), MetricasResponse.class);
        assertEquals(1, metricas.propiedades());
        assertEquals(1, metricas.unidades());
        assertEquals(1, metricas.inquilinos());
        assertEquals(1, metricas.contratosVigentes());
        assertEquals(List.of("USD", "NIO"),
                metricas.cargosPendientes().stream().map(TotalMonedaResponse::moneda).toList());
        assertEquals("1000.00", total(metricas.cargosPendientes(), "USD").total());
        assertEquals("0.00", total(metricas.cargosPendientes(), "NIO").total());
        assertEquals("1000.00", total(metricas.pagosRegistrados(), "USD").total());
        assertTrue(respuesta.body().contains("\"total\":\"1000.00\""));

        String tokenAsistente = login(nombreAsistente);
        assertEquals(200, get("/api/v1/dashboard/metricas", tokenAsistente).statusCode());
        assertEquals(401, get("/api/v1/dashboard/metricas", null).statusCode());
    }

    @Test
    void concurrenciaConLaMismaClaveRegistraUnSoloPago() throws Exception {
        String token = login(nombrePropietario);
        String clave = UUID.randomUUID().toString();
        String cuerpo = GSON.toJson(pago("1000.00", null));
        int hilos = 2;
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        try {
            List<Future<Integer>> futuros = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                futuros.add(pool.submit(() -> {
                    salida.await();
                    return postJson("/api/v1/pagos", cuerpo, token, clave).statusCode();
                }));
            }
            salida.countDown();
            List<Integer> estados = new ArrayList<>();
            for (Future<Integer> futuro : futuros) {
                estados.add(futuro.get(15, TimeUnit.SECONDS));
            }
            assertTrue(estados.stream().allMatch(e -> e == 201 || e == 409),
                    () -> "Estados inesperados: " + estados);
            assertTrue(estados.contains(201), () -> "Ninguna petición registró el pago: " + estados);
            assertEquals(1, contar("select count(p) from Pago p"));
            assertEquals(1, contar("select count(s) from SolicitudIdempotente s"));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void dosPagosConcurrentesNoSobreaplicanElMismoCargo() throws Exception {
        String token = login(nombrePropietario);
        String cuerpo = GSON.toJson(pago("300.00", List.of(
                new DistribucionRequest(cargoRentaId.toString(), "300.00"))));
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> primero = pool.submit(() -> {
                salida.await();
                return postJson("/api/v1/pagos", cuerpo, token,
                        UUID.randomUUID().toString()).statusCode();
            });
            Future<Integer> segundo = pool.submit(() -> {
                salida.await();
                return postJson("/api/v1/pagos", cuerpo, token,
                        UUID.randomUUID().toString()).statusCode();
            });
            salida.countDown();

            List<Integer> estados = List.of(primero.get(15, TimeUnit.SECONDS),
                    segundo.get(15, TimeUnit.SECONDS));

            assertEquals(1, estados.stream().filter(e -> e == 201).count(), estados.toString());
            assertEquals(1, estados.stream().filter(e -> e == 422).count(), estados.toString());
            assertEquals(1, contar("select count(p) from Pago p"));
            assertEquals(1, contar("select count(a) from AplicacionPago a"));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void unaClaveDeOtroUsuarioNoRevelaNiDuplicaElPago() throws Exception {
        String tokenPropietario = login(nombrePropietario);
        String tokenAsistente = login(nombreAsistente);
        String clave = UUID.randomUUID().toString();
        PagoRequest solicitud = pago("1000.00", null);

        assertEquals(201, postJson("/api/v1/pagos", GSON.toJson(solicitud),
                tokenPropietario, clave).statusCode());

        HttpResponse<String> ajena = postJson("/api/v1/pagos", GSON.toJson(solicitud),
                tokenAsistente, clave);
        assertEquals(409, ajena.statusCode(), ajena.body());
        assertEquals("CONFLICTO", error(ajena).error().codigo());
        assertEquals(1, contar("select count(p) from Pago p"));
    }

    @Test
    void desfaseDelRelojImpidePersistirPagoReciboEIdempotencia() throws Exception {
        String token = login(nombrePropietario);
        String clave = UUID.randomUUID().toString();
        String cuerpo = GSON.toJson(pago("1000.00", null));
        HttpResponse<String> respuesta;
        DESFASAR_RELOJ.set(true);
        try {
            respuesta = postJson("/api/v1/pagos", cuerpo, token, clave);
        } finally {
            DESFASAR_RELOJ.set(false);
        }

        assertEquals(503, respuesta.statusCode(), respuesta.body());
        assertEquals("ERR-057", error(respuesta).error().codigo());
        assertEquals(0, contar("select count(p) from Pago p"));
        assertEquals(0, contar("select count(r) from Recibo r"));
        assertEquals(0, contar("select count(i) from SolicitudIdempotente i"));
        assertEquals(201, postJson("/api/v1/pagos", cuerpo, token, clave).statusCode());
    }

    private PagoRequest pago(String monto, List<DistribucionRequest> distribucion) {
        return new PagoRequest(contratoId.toString(), FECHA_PAGO.toString(), "USD",
                monto, "Transferencia", "REF-" + UUID.randomUUID(), distribucion, null);
    }

    private static TotalMonedaResponse total(List<TotalMonedaResponse> totales, String moneda) {
        return totales.stream().filter(t -> moneda.equals(t.moneda())).findFirst().orElseThrow();
    }

    private static long contar(String hql) {
        return tx.ejecutar(session -> session.createQuery(hql, Long.class).getSingleResult());
    }

    private static long siguienteNumeroRecibo() {
        return tx.ejecutar(session -> session.createNativeQuery(
                "select siguiente_numero from consecutivo_recibo where id = 1", Long.class)
                .getSingleResult());
    }

    private static String login(String nombre) throws Exception {
        HttpResponse<String> respuesta = postJson("/api/v1/auth/login",
                GSON.toJson(new LoginRequest(nombre, new String(PASSWORD))), null, null);
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        LoginResponse sesion = GSON.fromJson(respuesta.body(), LoginResponse.class);
        assertNotNull(sesion.token());
        return sesion.token();
    }

    private static HttpResponse<String> get(String ruta, String token) throws Exception {
        HttpRequest.Builder constructor = HttpRequest.newBuilder(URI.create(base + ruta))
                .timeout(Duration.ofSeconds(10))
                .GET();
        if (token != null) constructor.header("Authorization", "Bearer " + token);
        return cliente.send(constructor.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> postJson(String ruta, String cuerpo, String token,
                                                 String idempotencyKey) throws Exception {
        HttpRequest.Builder constructor = HttpRequest.newBuilder(URI.create(base + ruta))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8));
        if (token != null) constructor.header("Authorization", "Bearer " + token);
        if (idempotencyKey != null) constructor.header("Idempotency-Key", idempotencyKey);
        return cliente.send(constructor.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static ErrorApi.Cuerpo error(HttpResponse<String> respuesta) {
        return GSON.fromJson(respuesta.body(), ErrorApi.Cuerpo.class);
    }
}
