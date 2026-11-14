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
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.RutasTasas;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.TasaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AutenticacionService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.DashboardService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.TipoCambioService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PasswordHasher;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Revisión de seguridad de la API local: sesión, tokens, permisos vigentes,
 * errores sin detalles técnicos y cabeceras de respuesta.
 */
class ApiSeguridadTest {
    private static final char[] PASSWORD = "ClaveSintetica-2026".toCharArray();
    private static final Gson GSON = new Gson();

    private static SessionFactory factory;
    private static HibernateTransactions tx;
    private static ServidorApi servidor;
    private static HttpClient cliente;
    private static String base;
    private static String nombrePropietario;
    private static String nombreAsistente;
    private static Long idPropietario;
    private static Long idAsistente;

    @BeforeAll
    static void iniciar() throws Exception {
        Properties p = new Properties();
        p.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        p.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:latch_api_seguridad_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        p.setProperty("hibernate.connection.username", "sa");
        p.setProperty("hibernate.connection.password", "");
        p.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        p.setProperty("hibernate.jdbc.time_zone", "UTC");
        p.setProperty("hibernate.show_sql", "false");

        factory = HibernateUtil.construir(p);
        tx = new HibernateTransactions(factory);
        String hash = new PasswordHasher().crear(PASSWORD);
        nombrePropietario = "seg-prop-" + UUID.randomUUID().toString().substring(0, 8);
        nombreAsistente = "seg-asis-" + UUID.randomUUID().toString().substring(0, 8);
        tx.ejecutar(session -> {
            Usuario propietario = new Usuario(nombrePropietario, hash, Rol.PROPIETARIO);
            Usuario asistente = new Usuario(nombreAsistente, hash, Rol.ASISTENTE);
            session.persist(propietario);
            session.persist(asistente);
            session.flush();
            idPropietario = propietario.getId();
            idAsistente = asistente.getId();
            return null;
        });
        SesionUsuario.cerrarSesion();

        Router router = new Router();
        new RutasTasas(new TipoCambioService(tx)).registrar(router);
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

    @AfterEach
    void limpiarSesionLocal() {
        SesionUsuario.cerrarSesion();
        restaurarUsuario(idPropietario, Rol.PROPIETARIO, true);
        restaurarUsuario(idAsistente, Rol.ASISTENTE, true);
    }

    @Test
    void logoutRevocaElTokenDeInmediato() throws Exception {
        String token = login(nombrePropietario);
        assertEquals(200, get("/api/v1/dashboard/metricas", token).statusCode());

        HttpResponse<String> salida = postJson("/api/v1/auth/logout", "", token, null);
        assertEquals(204, salida.statusCode());

        assertEquals(401, get("/api/v1/dashboard/metricas", token).statusCode());
    }

    @Test
    void unTokenAusenteFalsoOManipuladoDevuelve401() throws Exception {
        HttpResponse<String> sinToken = get("/api/v1/dashboard/metricas", null);
        assertEquals(401, sinToken.statusCode());
        assertEquals("NO_AUTENTICADO", error(sinToken).error().codigo());
        assertEquals("Bearer", sinToken.headers().firstValue("WWW-Authenticate").orElseThrow());

        assertEquals(401, get("/api/v1/dashboard/metricas", "token-inventado").statusCode());
        assertEquals(401, get("/api/v1/dashboard/metricas", "x".repeat(600)).statusCode());

        String token = login(nombrePropietario);
        assertEquals(401, get("/api/v1/dashboard/metricas", token + "x").statusCode());
    }

    @Test
    void respuestasIncluyenCabecerasSegurasYSinCors() throws Exception {
        String token = login(nombrePropietario);
        HttpResponse<String> respuesta = get("/api/v1/dashboard/metricas", token);

        assertEquals(200, respuesta.statusCode());
        assertEquals("no-store", respuesta.headers()
                .firstValue("Cache-Control").orElseThrow());
        assertEquals("nosniff", respuesta.headers()
                .firstValue("X-Content-Type-Options").orElseThrow());
        assertTrue(respuesta.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
        assertTrue(respuesta.headers().firstValue("Access-Control-Allow-Credentials").isEmpty());
    }

    @Test
    void exigeContentTypeJsonYLimitaElCuerpo() throws Exception {
        String token = login(nombrePropietario);

        HttpResponse<String> sinTipo = cliente.send(HttpRequest.newBuilder(
                        URI.create(base + "/api/v1/auth/login"))
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(415, sinTipo.statusCode());
        assertEquals("CONTENIDO_NO_SOPORTADO", error(sinTipo).error().codigo());

        HttpResponse<String> tipoTexto = cliente.send(HttpRequest.newBuilder(
                        URI.create(base + "/api/v1/tasas"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "text/plain")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(415, tipoTexto.statusCode());

        String grande = "{\"fuente\":\"" + "x".repeat(300 * 1024) + "\"}";
        HttpResponse<String> exceso = postJson("/api/v1/tasas", grande, token, null);
        assertEquals(413, exceso.statusCode());
        assertEquals("CUERPO_DEMASIADO_GRANDE", error(exceso).error().codigo());
    }

    @Test
    void rutasYMetodosInvalidosUsanElCuerpoUniforme() throws Exception {
        HttpResponse<String> inexistente = get("/api/v1/no-existe", null);
        assertEquals(404, inexistente.statusCode());
        assertEquals("NO_ENCONTRADO", error(inexistente).error().codigo());

        HttpResponse<String> metodo = cliente.send(HttpRequest.newBuilder(
                        URI.create(base + "/api/v1/tasas"))
                .timeout(Duration.ofSeconds(10))
                .DELETE()
                .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(405, metodo.statusCode());
        assertEquals("METODO_NO_PERMITIDO", error(metodo).error().codigo());
    }

    @Test
    void losErroresNoExponenDetallesTecnicos() throws Exception {
        String token = login(nombrePropietario);
        List<HttpResponse<String>> respuestas = List.of(
                postJson("/api/v1/auth/login", "{no-es-json", null, null),
                postJson("/api/v1/tasas", "{\"fecha\":\"2026-12-05\",\"tasa\":\"0\",\"fuente\":\"BCN\"}",
                        token, null),
                get("/api/v1/tasas/" + UUID.randomUUID(), token),
                get("/api/v1/no-existe", token));

        for (HttpResponse<String> respuesta : respuestas) {
            assertTrue(respuesta.statusCode() >= 400);
            String cuerpo = respuesta.body().toLowerCase();
            assertFalse(cuerpo.contains("sqlstate"), cuerpo);
            assertFalse(cuerpo.contains("exception"), cuerpo);
            assertFalse(cuerpo.contains("org.hibernate"), cuerpo);
            assertFalse(cuerpo.contains("at ni.edu"), cuerpo);
            assertFalse(cuerpo.contains("stacktrace"), cuerpo);
            assertFalse(cuerpo.contains("select "), cuerpo);
        }
    }

    @Test
    void elRolSeRevalidaEnCadaPeticion() throws Exception {
        String token = login(nombrePropietario);
        TasaResponse tasa = registrarTasa(token, LocalDate.now().minusDays(1).toString());

        cambiarRol(idPropietario, Rol.ASISTENTE);
        HttpResponse<String> denegada = postJson(
                "/api/v1/tasas/" + tasa.id() + "/autorizar",
                "{\"motivo\":\"Intento tras cambio de rol\"}", token, null);
        assertEquals(403, denegada.statusCode());
        assertEquals("SIN_PERMISO", error(denegada).error().codigo());

        // El asistente conserva las operaciones que sí le corresponden.
        assertEquals(201, postJson("/api/v1/tasas",
                "{\"fecha\":\"" + LocalDate.now().minusDays(2)
                        + "\",\"tasa\":\"36.70000000\",\"fuente\":\"BCN\"}",
                token, null).statusCode());

        cambiarRol(idPropietario, Rol.PROPIETARIO);
        HttpResponse<String> autorizada = postJson(
                "/api/v1/tasas/" + tasa.id() + "/autorizar",
                "{\"motivo\":\"Restablecido el rol\"}", token, null);
        assertEquals(200, autorizada.statusCode());
        assertTrue(GSON.fromJson(autorizada.body(), TasaResponse.class).autorizada());
    }

    @Test
    void unUsuarioDesactivadoPierdeElAccesoAunqueSuTokenSigaVigente() throws Exception {
        String token = login(nombreAsistente);
        assertEquals(200, get("/api/v1/dashboard/metricas", token).statusCode());

        cambiarActivo(idAsistente, false);
        HttpResponse<String> respuesta = get("/api/v1/dashboard/metricas", token);
        assertEquals(403, respuesta.statusCode());
        assertEquals("SIN_PERMISO", error(respuesta).error().codigo());
    }

    @Test
    void elLoginHttpNoModificaLaSesionGlobalDeJavaFx() throws Exception {
        SesionUsuario escritorio = SesionUsuario.paraSesionAutenticada(
                idAsistente, nombreAsistente, Rol.ASISTENTE);
        SesionUsuario.establecer(escritorio);

        LoginResponse sesionApi = loginCompleto(nombrePropietario);
        assertNotNull(sesionApi.token());
        assertEquals(Rol.PROPIETARIO, sesionApi.rol());

        SesionUsuario actual = SesionUsuario.getActual();
        assertNotNull(actual);
        assertEquals(idAsistente, actual.getUsuarioId());
        assertFalse(SesionUsuario.esPropietario());
    }

    @Test
    void elLoginNoExponeCredencialesNiHuellasDeClave() throws Exception {
        HttpResponse<String> respuesta = postJson("/api/v1/auth/login",
                GSON.toJson(new LoginRequest(nombrePropietario, new String(PASSWORD))),
                null, null);
        assertEquals(200, respuesta.statusCode());

        String cuerpo = respuesta.body().toLowerCase();
        assertFalse(cuerpo.contains("password"));
        assertFalse(cuerpo.contains("pbkdf2"));
        assertFalse(cuerpo.contains("clavesintetica"));

        LoginResponse sesion = GSON.fromJson(respuesta.body(), LoginResponse.class);
        assertNotNull(sesion.expiraEn());
        assertTrue(sesion.expiraEn().contains("T"));
    }

    @Test
    void lasCredencialesInvalidasNoRevelanSiElUsuarioExiste() throws Exception {
        HttpResponse<String> existente = postJson("/api/v1/auth/login",
                GSON.toJson(new LoginRequest(nombrePropietario, "ClaveEquivocada-2026")),
                null, null);
        HttpResponse<String> inexistente = postJson("/api/v1/auth/login",
                GSON.toJson(new LoginRequest("no.existe." + UUID.randomUUID(), "ClaveEquivocada-2026")),
                null, null);

        assertEquals(401, existente.statusCode());
        assertEquals(401, inexistente.statusCode());
        assertEquals(error(existente).error().mensaje(), error(inexistente).error().mensaje());
        assertEquals("CREDENCIALES_INVALIDAS", error(existente).error().codigo());
    }

    @Test
    void bloqueaTrasCincoIntentosFallidos() throws Exception {
        String usuario = "bloqueo." + UUID.randomUUID().toString().substring(0, 8);
        for (int intento = 0; intento < 5; intento++) {
            HttpResponse<String> fallo = postJson("/api/v1/auth/login",
                    GSON.toJson(new LoginRequest(usuario, "ClaveEquivocada-2026")), null, null);
            assertEquals(401, fallo.statusCode());
        }

        HttpResponse<String> bloqueado = postJson("/api/v1/auth/login",
                GSON.toJson(new LoginRequest(usuario, "ClaveEquivocada-2026")), null, null);
        assertEquals(429, bloqueado.statusCode());
        assertEquals("DEMASIADOS_INTENTOS", error(bloqueado).error().codigo());
    }

    private static TasaResponse registrarTasa(String token, String fecha) throws Exception {
        HttpResponse<String> respuesta = postJson("/api/v1/tasas",
                "{\"fecha\":\"" + fecha + "\",\"tasa\":\"36.50000000\",\"fuente\":\"BCN\"}",
                token, null);
        assertEquals(201, respuesta.statusCode(), respuesta.body());
        return GSON.fromJson(respuesta.body(), TasaResponse.class);
    }

    private static void cambiarRol(Long id, String rol) {
        tx.ejecutar(session -> {
            session.createNativeMutationQuery("update usuario set rol = :rol where id = :id")
                    .setParameter("rol", rol)
                    .setParameter("id", id)
                    .executeUpdate();
            return null;
        });
    }

    private static void cambiarActivo(Long id, boolean activo) {
        tx.ejecutar(session -> {
            session.createNativeMutationQuery("update usuario set activo = :activo where id = :id")
                    .setParameter("activo", activo)
                    .setParameter("id", id)
                    .executeUpdate();
            return null;
        });
    }

    private static void restaurarUsuario(Long id, String rol, boolean activo) {
        tx.ejecutar(session -> {
            session.createNativeMutationQuery(
                            "update usuario set rol = :rol, activo = :activo where id = :id")
                    .setParameter("rol", rol)
                    .setParameter("activo", activo)
                    .setParameter("id", id)
                    .executeUpdate();
            return null;
        });
    }

    private static LoginResponse loginCompleto(String nombre) throws Exception {
        HttpResponse<String> respuesta = postJson("/api/v1/auth/login",
                GSON.toJson(new LoginRequest(nombre, new String(PASSWORD))), null, null);
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        return GSON.fromJson(respuesta.body(), LoginResponse.class);
    }

    private static String login(String nombre) throws Exception {
        String token = loginCompleto(nombre).token();
        assertNotNull(token);
        return token;
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