package ni.edu.uam.proyectofinalprogramacionappescritorio.api;

import com.google.gson.Gson;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ApiConfig;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ErrorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.LectorCuerpo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ServidorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.TokenService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AutenticacionService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PasswordHasher;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiFase1Test {
    private static final char[] PASSWORD = "ClaveSintetica-2026".toCharArray();
    private static final Gson GSON = new Gson();

    private static SessionFactory factory;
    private static HibernateTransactions tx;
    private static ServidorApi servidor;
    private static HttpClient cliente;
    private static String base;
    private static String hash;

    @BeforeAll
    static void iniciar() throws Exception {
        Properties p = new Properties();
        p.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        p.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:latch_api_fase1_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        p.setProperty("hibernate.connection.username", "sa");
        p.setProperty("hibernate.connection.password", "");
        p.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        p.setProperty("hibernate.jdbc.time_zone", "UTC");
        p.setProperty("hibernate.show_sql", "false");

        factory = HibernateUtil.construir(p);
        tx = new HibernateTransactions(factory);
        hash = new PasswordHasher().crear(PASSWORD);
        SesionUsuario.cerrarSesion();

        servidor = new ServidorApi(ApiConfig.paraPruebas(0),
                new AutenticacionService(tx), new TokenService());
        servidor.iniciar();
        base = servidor.urlBase();
        cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterAll
    static void cerrar() {
        if (servidor != null) servidor.detener();
        if (factory != null) factory.close();
        SesionUsuario.cerrarSesion();
    }

    @Test
    void loginCorrectoEmiteTokenSinTocarSesionGlobal() throws Exception {
        Usuario usuario = crearUsuario(Rol.PROPIETARIO);
        LoginResponse respuesta = login(usuario);

        assertNotNull(respuesta.token());
        assertFalse(respuesta.token().isBlank());
        assertEquals(Rol.PROPIETARIO, respuesta.rol());
        assertEquals(usuario.getId(), respuesta.usuarioId());
        assertEquals(usuario.getNombre(), respuesta.nombre());
        assertTrue(Instant.parse(respuesta.expiraEn()).isAfter(Instant.now()));
        assertNull(SesionUsuario.getActual());
    }

    @Test
    void credencialesInvalidasDevuelvenMismoMensaje() throws Exception {
        Usuario usuario = crearUsuario(Rol.ASISTENTE);

        HttpResponse<String> claveMala = postJson("/api/v1/auth/login",
                GSON.toJson(new LoginRequest(usuario.getNombre(), "ClaveIncorrecta-2026")), null);
        HttpResponse<String> inexistente = postJson("/api/v1/auth/login",
                GSON.toJson(new LoginRequest("no-existe-" + UUID.randomUUID(), new String(PASSWORD))), null);

        assertEquals(401, claveMala.statusCode());
        assertEquals(401, inexistente.statusCode());
        ErrorApi.Cuerpo errorClave = error(claveMala);
        ErrorApi.Cuerpo errorInexistente = error(inexistente);
        assertEquals("CREDENCIALES_INVALIDAS", errorClave.error().codigo());
        assertEquals(errorClave.error().mensaje(), errorInexistente.error().mensaje());
    }

    @Test
    void rutaProtegidaSinTokenDevuelve401YRetoBearer() throws Exception {
        HttpResponse<String> respuesta = postJson("/api/v1/auth/logout", "{}", null);

        assertEquals(401, respuesta.statusCode());
        assertEquals("NO_AUTENTICADO", error(respuesta).error().codigo());
        assertTrue(respuesta.headers().firstValue("WWW-Authenticate")
                .orElse("").contains("Bearer"));
    }

    @Test
    void tokenInvalidoDevuelve401() throws Exception {
        HttpResponse<String> respuesta = postJson("/api/v1/auth/logout", "{}",
                "token-que-no-existe");

        assertEquals(401, respuesta.statusCode());
        assertEquals("NO_AUTENTICADO", error(respuesta).error().codigo());
    }

    @Test
    void logoutRevocaElToken() throws Exception {
        String token = login(crearUsuario(Rol.PROPIETARIO)).token();

        HttpResponse<String> salida = postJson("/api/v1/auth/logout", "{}", token);
        assertEquals(204, salida.statusCode());

        HttpResponse<String> repetido = postJson("/api/v1/auth/logout", "{}", token);
        assertEquals(401, repetido.statusCode());
    }

    @Test
    void jsonInvalidoDevuelve400() throws Exception {
        HttpResponse<String> respuesta = postJson("/api/v1/auth/login", "{", null);

        assertEquals(400, respuesta.statusCode());
        assertEquals("JSON_INVALIDO", error(respuesta).error().codigo());
    }

    @Test
    void contentTypeDistintoDevuelve415() throws Exception {
        HttpResponse<String> respuesta = post("/api/v1/auth/login",
                GSON.toJson(new LoginRequest("usuario", "ClaveSintetica-2026")),
                null, "text/plain");

        assertEquals(415, respuesta.statusCode());
        assertEquals("CONTENIDO_NO_SOPORTADO", error(respuesta).error().codigo());
    }

    @Test
    void contentTypeConCharsetEsAceptado() throws Exception {
        Usuario usuario = crearUsuario(Rol.PROPIETARIO);
        HttpResponse<String> respuesta = post("/api/v1/auth/login",
                GSON.toJson(new LoginRequest(usuario.getNombre(), new String(PASSWORD))),
                null, "application/json; charset=UTF-8");

        assertEquals(200, respuesta.statusCode(), respuesta.body());
    }

    @Test
    void cuerpoMayorAlLimiteDevuelve413() throws Exception {
        String grande = "{\"usuario\":\"u\",\"password\":\""
                + "x".repeat(LectorCuerpo.LIMITE_BYTES) + "\"}";

        HttpResponse<String> respuesta = post("/api/v1/auth/login", grande,
                null, "application/json");

        assertEquals(413, respuesta.statusCode());
        assertEquals("CUERPO_DEMASIADO_GRANDE", error(respuesta).error().codigo());
    }

    @Test
    void bloqueaTrasCincoFallos() throws Exception {
        Usuario usuario = crearUsuario(Rol.ASISTENTE);

        for (int intento = 0; intento < 5; intento++) {
            HttpResponse<String> respuesta = postJson("/api/v1/auth/login",
                    GSON.toJson(new LoginRequest(usuario.getNombre(), "ClaveIncorrecta-2026")), null);
            assertEquals(401, respuesta.statusCode());
        }

        HttpResponse<String> bloqueado = postJson("/api/v1/auth/login",
                GSON.toJson(new LoginRequest(usuario.getNombre(), new String(PASSWORD))), null);
        assertEquals(429, bloqueado.statusCode());
        assertEquals("DEMASIADOS_INTENTOS", error(bloqueado).error().codigo());
    }

    @Test
    void rutaInexistenteDevuelve404Json() throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(base + "/api/v1/no-existe"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> respuesta = cliente.send(peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, respuesta.statusCode());
        assertEquals("NO_ENCONTRADO", error(respuesta).error().codigo());
    }

    @Test
    void metodoNoPermitidoDevuelve405() throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(base + "/api/v1/auth/login"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> respuesta = cliente.send(peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(405, respuesta.statusCode());
        assertEquals("METODO_NO_PERMITIDO", error(respuesta).error().codigo());
    }

    private static Usuario crearUsuario(String rol) {
        return tx.ejecutar(session -> {
            Usuario usuario = new Usuario("u-" + UUID.randomUUID(), hash, rol);
            session.persist(usuario);
            return usuario;
        });
    }

    private static LoginResponse login(Usuario usuario) throws Exception {
        HttpResponse<String> respuesta = postJson("/api/v1/auth/login",
                GSON.toJson(new LoginRequest(usuario.getNombre(), new String(PASSWORD))), null);
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        return GSON.fromJson(respuesta.body(), LoginResponse.class);
    }

    private static HttpResponse<String> postJson(String ruta, String cuerpo, String token)
            throws Exception {
        return post(ruta, cuerpo, token, "application/json");
    }

    private static HttpResponse<String> post(String ruta, String cuerpo, String token,
                                             String contentType) throws Exception {
        HttpRequest.Builder constructor = HttpRequest.newBuilder(URI.create(base + ruta))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8));
        if (token != null) constructor.header("Authorization", "Bearer " + token);
        return cliente.send(constructor.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static ErrorApi.Cuerpo error(HttpResponse<String> respuesta) {
        return GSON.fromJson(respuesta.body(), ErrorApi.Cuerpo.class);
    }
}