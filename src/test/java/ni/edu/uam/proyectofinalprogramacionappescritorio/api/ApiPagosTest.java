package ni.edu.uam.proyectofinalprogramacionappescritorio.api;

import com.google.gson.Gson;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ApiConfig;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ErrorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Router;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ServidorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.TokenService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.RutasIndicadores;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto.MetricasResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto.TotalMonedaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.RutasPagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.ServicioPagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.DistribucionRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.EstadoCuentaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoDetalleResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoRegistroResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.ReciboResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propietario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AutenticacionService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.DashboardService;
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
import java.time.Instant;
import java.time.LocalDate;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiPagosTest {
    private static final char[] PASSWORD = "ClaveSintetica-2026".toCharArray();
    private static final Gson GSON = new Gson();
    private static final LocalDate FECHA_PAGO = LocalDate.of(2026, 11, 10);
    private static final LocalDate CORTE = LocalDate.of(2026, 11, 30);
    private static final BigDecimal TASA = new BigDecimal("36.6243");

    private static SessionFactory factory;
    private static HibernateTransactions tx;
    private static ServidorApi servidor;
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
        p.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        p.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:latch_api_pagos_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        p.setProperty("hibernate.connection.username", "sa");
        p.setProperty("hibernate.connection.password", "");
        p.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        p.setProperty("hibernate.jdbc.time_zone", "UTC");
        p.setProperty("hibernate.show_sql", "false");

        factory = HibernateUtil.construir(p);
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

        Router router = new Router();
        new RutasPagos(new ServicioPagos(tx)).registrar(router);
        new RutasIndicadores(new DashboardService(tx)).registrar(router);
        servidor = new ServidorApi(ApiConfig.paraPruebas(0),
                new AutenticacionService(tx), new TokenService(), router);
        servidor.iniciar();
        base = servidor.urlBase();
        cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @AfterAll
    static void cerrar() {
        if (servidor != null) servidor.detener();
        if (factory != null) factory.close();
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
    void registraUnaSolaVezYConsultaPagoYRecibo() throws Exception {
        String token = login(nombrePropietario);
        String clave = UUID.randomUUID().toString();
        PagoRequest solicitud = pago("1000.00", List.of(
                new DistribucionRequest(cargoRentaId.toString(), "400.00"),
                new DistribucionRequest(cargoServicioId.toString(), "600.00")));

        HttpResponse<String> primera = postJson("/api/v1/pagos",
                GSON.toJson(solicitud), token, clave);
        assertEquals(201, primera.statusCode(), primera.body());
        PagoRegistroResponse registro = GSON.fromJson(primera.body(), PagoRegistroResponse.class);
        assertNotNull(registro.pagoId());
        assertTrue(registro.numeroRecibo() > 0);
        assertEquals("1000.00", registro.montoContrato());
        assertEquals("0.00", registro.credito());

        HttpResponse<String> repetida = postJson("/api/v1/pagos",
                GSON.toJson(solicitud), token, clave);
        assertEquals(201, repetida.statusCode(), repetida.body());
        PagoRegistroResponse otra = GSON.fromJson(repetida.body(), PagoRegistroResponse.class);
        assertEquals(registro.pagoId(), otra.pagoId());
        assertEquals(registro.numeroRecibo(), otra.numeroRecibo());
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
        assertFalse(recibo.anulado());

        assertEquals(404, get("/api/v1/pagos/" + UUID.randomUUID(), token).statusCode());
        assertEquals(404, get("/api/v1/recibos/999999", token).statusCode());
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
        assertEquals("Pendiente", estado.cargos().get(1).estado());
        assertEquals("400.00", estado.pagos().get(0).aplicado());
        assertEquals("600.00", estado.pagos().get(0).credito());

        assertEquals(404, get("/api/v1/contratos/" + UUID.randomUUID()
                + "/estado-cuenta?corte=" + CORTE, token).statusCode());
        assertEquals(400, get("/api/v1/contratos/" + contratoId
                + "/estado-cuenta?corte=no-es-fecha", token).statusCode());
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