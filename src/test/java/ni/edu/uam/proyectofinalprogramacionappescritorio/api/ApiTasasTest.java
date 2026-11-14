package ni.edu.uam.proyectofinalprogramacionappescritorio.api;

import com.google.gson.Gson;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ApiConfig;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ErrorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Router;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ServidorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.TokenService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.RutasTasas;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.ListaTasasResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.TasaRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.TasaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AutenticacionService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.TipoCambioService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PasswordHasher;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
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
    private static ServidorApi servidor;
    private static HttpClient cliente;
    private static String base;
    private static String nombrePropietario;
    private static String nombreAsistente;

    @BeforeAll
    static void iniciar() throws Exception {
        Properties p = new Properties();
        p.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        p.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:latch_api_tasas_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        p.setProperty("hibernate.connection.username", "sa");
        p.setProperty("hibernate.connection.password", "");
        p.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        p.setProperty("hibernate.jdbc.time_zone", "UTC");
        p.setProperty("hibernate.show_sql", "false");

        factory = HibernateUtil.construir(p);
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

        Router router = new Router();
        new RutasTasas(new TipoCambioService(tx)).registrar(router);
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
    void registraTasaInformativaYLaDevuelveEnLaLista() throws Exception {
        String token = login(nombrePropietario);

        TasaResponse creada = registrar(token, FECHA, "36.7", "BCN");
        assertEquals(FECHA.toString(), creada.fecha());
        assertEquals("36.70000000", creada.nioPorUsd());
        assertEquals("BCN", creada.fuente());
        assertFalse(creada.autorizada());
        assertNull(creada.autorizadoPor());
        assertNull(creada.motivo());

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
        TasaResponse informativa = registrar(token, FECHA, "36.75", "BCN");

        HttpResponse<String> respuesta = postJson(
                "/api/v1/tasas/" + informativa.id() + "/autorizar",
                "{\"motivo\":\"Tasa oficial verificada\"}", token);

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
        assertEquals("CONFLICTO", error(respuesta).error().codigo());
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

    private static TasaResponse registrar(String token, LocalDate fecha, String tasa,
                                          String fuente) throws Exception {
        HttpResponse<String> respuesta = postJson("/api/v1/tasas",
                GSON.toJson(new TasaRequest(fecha.toString(), tasa, fuente)), token);
        assertEquals(201, respuesta.statusCode(), respuesta.body());
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