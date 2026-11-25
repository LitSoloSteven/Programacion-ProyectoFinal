package ni.edu.uam.proyectofinalprogramacionappescritorio.api;

import com.google.gson.Gson;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ErrorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto.ServidorSpringPrueba;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.ListaTasasResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.ReferenciaTasaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.TasaRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.TasaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.ReferenciaTipoCambio;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.PostgresTestSupport;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RelojService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.TipoCambioService;
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
import java.time.LocalDate;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiTasasTest {
    private static final char[] PASSWORD = "ClaveSintetica-2026".toCharArray();
    private static final Gson GSON = new Gson();
    private static final LocalDate FECHA = LocalDate.now().minusDays(1);

    private static SessionFactory factory;
    private static ServidorSpringPrueba servidor;
    private static HttpClient cliente;
    private static String base;
    private static String nombrePropietario;
    private static String nombreAsistente;

    @BeforeAll
    static void iniciar() throws Exception {
        Properties p = new Properties();
        if (Boolean.getBoolean("latch.test.postgres")) {
            p = PostgresTestSupport.propiedadesHibernate();
        } else {
            p.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
            p.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:latch_api_tasas_" + UUID.randomUUID() + ";MODE=PostgreSQL");
            p.setProperty("hibernate.connection.username", "sa");
            p.setProperty("hibernate.connection.password", "");
            p.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            p.setProperty("hibernate.jdbc.time_zone", "UTC");
            p.setProperty("hibernate.show_sql", "false");
        }

        factory = HibernateUtil.construir(p);
        if (Boolean.getBoolean("latch.test.postgres")) PostgresTestSupport.aplicarMigraciones(p);
        HibernateTransactions tx = new HibernateTransactions(factory);
        String hash = new PasswordHasher().crear(PASSWORD);
        nombrePropietario = "prop-" + UUID.randomUUID().toString().substring(0, 8);
        nombreAsistente = "asis-" + UUID.randomUUID().toString().substring(0, 8);
        tx.ejecutar(session -> {
            session.persist(new Usuario(nombrePropietario, hash, Rol.PROPIETARIO));
            session.persist(new Usuario(nombreAsistente, hash, Rol.ASISTENTE));
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
        if (Boolean.getBoolean("latch.test.postgres")) PostgresTestSupport.eliminarFuncionRecibo();
        SesionUsuario.cerrarSesion();
    }

    @BeforeEach
    void limpiarTasas() {
        new HibernateTransactions(factory).ejecutar(session -> {
            session.createNativeMutationQuery("delete from tipo_cambio").executeUpdate();
            return null;
        });
    }

    @Test
    void listaVaciaSinTasas() throws Exception {
        HttpResponse<String> respuesta = get("/api/v1/tasas", login(nombrePropietario));

        assertEquals(200, respuesta.statusCode());
        assertTrue(GSON.fromJson(respuesta.body(), ListaTasasResponse.class).tasas().isEmpty());
    }

    @Test
    void requiereTokenBearer() throws Exception {
        HttpResponse<String> respuesta = get("/api/v1/tasas", null);

        assertEquals(401, respuesta.statusCode());
        assertEquals("NO_AUTENTICADO", error(respuesta).error().codigo());
    }

    @Test
    void consultaReferenciaComercialHistoricaSinAutorizarlaParaPagos() throws Exception {
        String ruta = "/api/v1/tasas/referencia";
        assertEquals(401, get(ruta, null).statusCode());
        String token = login(nombreAsistente);
        assertEquals("NO_DISPONIBLE",
                GSON.fromJson(get(ruta, token).body(), ReferenciaTasaResponse.class).estado());

        var tx = new HibernateTransactions(factory);
        Instant ayer = Instant.now().minusSeconds(86400);
        UUID id = tx.ejecutar(sesion -> {
            var referencia = new ReferenciaTipoCambio(new BigDecimal("36.62430000"), ayer,
                    ayer.plusSeconds(3600), ayer, "a".repeat(64));
            sesion.persist(referencia);
            return referencia.getId();
        });
        try {
            var consulta = get(ruta, token);
            assertEquals(200, consulta.statusCode());
            var referencia = GSON.fromJson(consulta.body(), ReferenciaTasaResponse.class);
            var efectiva = RelojService.fechaOperativa(ayer);
            assertEquals("HISTORICA", referencia.estado());
            assertEquals("36.62430000", referencia.nioPorUsd());
            assertEquals("ExchangeRate-API / AYR Tech", referencia.fuente());
            assertEquals(efectiva.toString(), referencia.fechaEfectiva());
            assertNotNull(referencia.consultadaEn());
            assertTrue(referencia.antiguedadSegundos() >= 86399);
            assertEquals(404, get("/api/v1/tasas/vigente?fecha=" + efectiva, token).statusCode());
            assertTrue(GSON.fromJson(get("/api/v1/tasas", token).body(), ListaTasasResponse.class)
                    .tasas().isEmpty());
        } finally {
            tx.ejecutar(sesion -> {
                sesion.remove(sesion.find(ReferenciaTipoCambio.class, id));
                return null;
            });
        }
    }

    @Test
    void registraTasaInformativaYLaDevuelveEnLaLista() throws Exception {
        String token = login(nombrePropietario);

        TasaResponse creada = registrar(token, FECHA, "36.7", "BCN");
        assertEquals(FECHA.toString(), creada.fecha());
        assertEquals("36.70000000", creada.nioPorUsd());
        assertEquals("BCN", creada.fuente());
        assertFalse(creada.autorizada());
        assertNull(creada.autorizadoPor());
        assertNull(creada.motivo());
        var detalle = get("/api/v1/tasas/" + creada.id(), token);
        assertEquals(200, detalle.statusCode());
        assertEquals(creada, GSON.fromJson(detalle.body(), TasaResponse.class));

        ListaTasasResponse lista = GSON.fromJson(
                get("/api/v1/tasas?fecha=" + FECHA, token).body(),
                ListaTasasResponse.class);
        assertEquals(1, lista.tasas().size());
        assertEquals(creada.id(), lista.tasas().get(0).id());
    }

    @Test
    void elAsistenteRegistraPeroNoAutoriza() throws Exception {
        String token = login(nombreAsistente);
        TasaResponse informativa = registrar(token, FECHA, "36.75", "BCN");

        HttpResponse<String> respuesta = postJson(
                "/api/v1/tasas/" + informativa.id() + "/autorizar",
                "{\"motivo\":\"Intento del asistente\"}", token);

        assertEquals(403, respuesta.statusCode());
        assertEquals("SIN_PERMISO", error(respuesta).error().codigo());
    }

    @Test
    void propietarioAutorizaYLaTasaQuedaVigente() throws Exception {
        String token = login(nombrePropietario);
        var tx = new HibernateTransactions(factory);
        Instant antes = instanteBd(tx);
        TasaResponse informativa = registrar(token, FECHA, "36.75", "BCN");
        String ruta = "/api/v1/tasas/" + informativa.id() + "/autorizar";
        String solicitud = "{\"motivo\":\"Tasa oficial verificada\"}";
        if (Boolean.getBoolean("latch.test.postgres")) {
            PostgresTestSupport.impedirAuditoriaFinanciera(tx);
            try {
                var fallida = postJson(ruta, solicitud, token);
                assertEquals(500, fallida.statusCode(), fallida.body());
                assertEquals("ERROR_INTERNO", error(fallida).error().codigo());
                assertEquals(0L, contar(tx, "select count(t) from TipoCambio t where t.autorizadoPor is not null"));
            } finally {
                PostgresTestSupport.permitirAuditoriaFinanciera(tx);
            }
        }
        HttpResponse<String> respuesta = postJson(ruta, solicitud, token);
        Instant despues = instanteBd(tx);

        assertEquals(200, respuesta.statusCode());
        TasaResponse autorizada = GSON.fromJson(respuesta.body(), TasaResponse.class);
        assertNotEquals(informativa.id(), autorizada.id());
        assertTrue(autorizada.autorizada());
        assertEquals(nombrePropietario, autorizada.autorizadoPorNombre());
        assertEquals("Tasa oficial verificada", autorizada.motivo());

        TasaResponse vigente = GSON.fromJson(
                get("/api/v1/tasas/vigente?fecha=" + FECHA, token).body(),
                TasaResponse.class);
        assertEquals(autorizada.id(), vigente.id());
        for (TasaResponse tasa : List.of(informativa, autorizada)) {
            var instante = OffsetDateTime.parse(tasa.registradoEn());
            assertEquals(RelojService.ZONA_OPERATIVA.getRules().getOffset(instante.toInstant()),
                    instante.getOffset());
            assertFalse(instante.toInstant().isBefore(antes));
            assertFalse(instante.toInstant().isAfter(despues));
            String accion = tasa.autorizada() ? "TASA_AUTORIZADA" : "TASA_REGISTRADA";
            assertEquals(1L, tx.ejecutar(sesion -> sesion.createQuery("""
                    select count(a) from Auditoria a
                    where a.accion = :accion and a.referencia = :referencia and a.actor.nombre = :actor
                    """, Long.class).setParameter("accion", accion)
                    .setParameter("referencia", "tasa:" + tasa.id())
                    .setParameter("actor", nombrePropietario).getSingleResult()).longValue());
        }
    }

    @Test
    void noPermiteDosAutorizadasParaLaMismaFecha() throws Exception {
        String token = login(nombrePropietario);
        TasaResponse primera = registrar(token, FECHA, "36.75", "BCN");
        TasaResponse segunda = registrar(token, FECHA, "37.10", "BCN");
        postJson("/api/v1/tasas/" + primera.id() + "/autorizar",
                "{\"motivo\":\"Primera\"}", token);

        HttpResponse<String> respuesta = postJson(
                "/api/v1/tasas/" + segunda.id() + "/autorizar",
                "{\"motivo\":\"Segunda\"}", token);

        assertEquals(409, respuesta.statusCode());
        assertEquals("ERR-060", error(respuesta).error().codigo());
        assertEquals(TipoCambioService.MENSAJE_TASA_DUPLICADA,
                error(respuesta).error().mensaje());
    }

    @Test
    void laBandaEsInclusivaYExigeConfirmacionFueraDeRango() throws Exception {
        String token = login(nombrePropietario);
        TasaResponse minimo = registrar(token, FECHA.minusDays(10), "1", "BCN");
        TasaResponse maximo = registrar(token, FECHA.minusDays(11), "100", "BCN");
        TasaResponse bajo = registrar(token, FECHA.minusDays(12), "0.99", "BCN");
        TasaResponse alto = registrar(token, FECHA.minusDays(13), "100.01", "BCN");

        assertEquals(200, postJson("/api/v1/tasas/" + minimo.id() + "/autorizar",
                "{\"motivo\":\"Límite inferior\"}", token).statusCode());
        assertEquals(200, postJson("/api/v1/tasas/" + maximo.id() + "/autorizar",
                "{\"motivo\":\"Límite superior\"}", token).statusCode());

        HttpResponse<String> rechazoBajo = postJson(
                "/api/v1/tasas/" + bajo.id() + "/autorizar",
                "{\"motivo\":\"Sin confirmar\"}", token);
        assertEquals(422, rechazoBajo.statusCode(), rechazoBajo.body());
        assertEquals("ENTIDAD_NO_PROCESABLE", error(rechazoBajo).error().codigo());
        assertEquals(TipoCambioService.MENSAJE_FUERA_DE_BANDA,
                error(rechazoBajo).error().mensaje());

        assertEquals(422, postJson("/api/v1/tasas/" + alto.id() + "/autorizar",
                "{\"motivo\":\"Sin confirmar\",\"confirmarFueraDeBanda\":false}",
                token).statusCode());

        HttpResponse<String> confirmada = postJson(
                "/api/v1/tasas/" + bajo.id() + "/autorizar",
                "{\"motivo\":\"Revisión confirmada\",\"confirmarFueraDeBanda\":true}",
                token);
        assertEquals(200, confirmada.statusCode(), confirmada.body());
        assertTrue(GSON.fromJson(confirmada.body(), TasaResponse.class).autorizada());
    }

    @Test
    void validaFechasTasasYFuentes() throws Exception {
        String token = login(nombrePropietario);

        assertEquals(400, postJson("/api/v1/tasas",
                "{\"fecha\":\"2026-13-01\",\"tasa\":\"36.75\",\"fuente\":\"BCN\"}",
                token).statusCode());
        assertEquals(422, postJson("/api/v1/tasas",
                "{\"fecha\":\"" + LocalDate.now().plusDays(1) + "\",\"tasa\":\"36.75\",\"fuente\":\"BCN\"}",
                token).statusCode());
        assertEquals(422, postJson("/api/v1/tasas",
                "{\"fecha\":\"" + FECHA + "\",\"tasa\":\"0\",\"fuente\":\"BCN\"}",
                token).statusCode());
        assertEquals(422, postJson("/api/v1/tasas",
                "{\"fecha\":\"" + FECHA + "\",\"tasa\":\"36.75\",\"fuente\":\" \"}",
                token).statusCode());
        assertEquals(400, postJson("/api/v1/tasas",
                "{\"fecha\":\"" + FECHA + "\",\"tasa\":\"abc\",\"fuente\":\"BCN\"}",
                token).statusCode());
        assertEquals(400, get("/api/v1/tasas?autorizada=si", token).statusCode());
        assertEquals(400, get("/api/v1/tasas?limite=0", token).statusCode());
    }

    @Test
    void devuelveNoEncontradoParaIdentificadoresDesconocidos() throws Exception {
        String token = login(nombrePropietario);

        assertEquals(404, get("/api/v1/tasas/" + UUID.randomUUID(), token).statusCode());
        assertEquals(404, postJson("/api/v1/tasas/" + UUID.randomUUID() + "/autorizar",
                "{\"motivo\":\"No existe\"}", token).statusCode());
        assertEquals(400, get("/api/v1/tasas/no-es-uuid", token).statusCode());
        assertEquals(400, get("/api/v1/tasas/vigente;dato=valor", token).statusCode());
        assertEquals(404, get("/api/v1/tasas/vigente?fecha=" + FECHA, token).statusCode());
    }

    @Test
    void filtraPorEstadoYFecha() throws Exception {
        String token = login(nombrePropietario);
        TasaResponse ayer = registrar(token, FECHA, "36.75", "BCN");
        registrar(token, FECHA.minusDays(1), "37.00", "BCN");
        postJson("/api/v1/tasas/" + ayer.id() + "/autorizar",
                "{\"motivo\":\"Autorizada\"}", token);

        List<TasaResponse> informativas = GSON.fromJson(
                get("/api/v1/tasas?autorizada=false", token).body(),
                ListaTasasResponse.class).tasas();
        List<TasaResponse> autorizadas = GSON.fromJson(
                get("/api/v1/tasas?autorizada=true", token).body(),
                ListaTasasResponse.class).tasas();

        assertEquals(2, informativas.size());
        assertEquals(1, autorizadas.size());
        assertEquals(ayer.fecha(), autorizadas.get(0).fecha());
    }

    private static Instant instanteBd(HibernateTransactions tx) {
        return tx.ejecutar(sesion -> new RelojService().leer(sesion).instante());
    }

    private static long contar(HibernateTransactions tx, String hql) {
        return tx.ejecutar(sesion -> sesion.createQuery(hql, Long.class).getSingleResult());
    }

    private static TasaResponse registrar(String token, LocalDate fecha, String tasa,
                                          String fuente) throws Exception {
        HttpResponse<String> respuesta = postJson("/api/v1/tasas",
                GSON.toJson(new TasaRequest(fecha.toString(), tasa, fuente)), token);
        assertEquals(201, respuesta.statusCode(), respuesta.body());
        assertTrue(com.google.gson.JsonParser.parseString(respuesta.body()).getAsJsonObject()
                .get("nioPorUsd").getAsJsonPrimitive().isString());
        return GSON.fromJson(respuesta.body(), TasaResponse.class);
    }

    private static String login(String nombre) throws Exception {
        HttpResponse<String> respuesta = postJson("/api/v1/auth/login",
                GSON.toJson(new LoginRequest(nombre, new String(PASSWORD))), null);
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

    private static HttpResponse<String> postJson(String ruta, String cuerpo, String token)
            throws Exception {
        HttpRequest.Builder constructor = HttpRequest.newBuilder(URI.create(base + ruta))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8));
        if (token != null) constructor.header("Authorization", "Bearer " + token);
        return cliente.send(constructor.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static ErrorApi.Cuerpo error(HttpResponse<String> respuesta) {
        return GSON.fromJson(respuesta.body(), ErrorApi.Cuerpo.class);
    }
}
