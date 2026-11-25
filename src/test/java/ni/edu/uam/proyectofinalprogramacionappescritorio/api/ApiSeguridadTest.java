package ni.edu.uam.proyectofinalprogramacionappescritorio.api;

import java.util.concurrent.atomic.AtomicBoolean;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto.ServidorSpringPrueba;
import com.google.gson.Gson;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ErrorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.TasaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RelojDesfasadoException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RelojService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PasswordHasher;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.Socket;
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

    private static final AtomicBoolean DESFASAR_RELOJ = new AtomicBoolean();
    private static SessionFactory factory;
    private static HibernateTransactions tx;
    private static ServidorSpringPrueba servidor;
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

        p.put("hibernate.session_factory.statement_inspector", (StatementInspector) sql ->
                DESFASAR_RELOJ.get() && sql.strip().equalsIgnoreCase("select current_timestamp")
                        ? "select dateadd('MINUTE', -10, current_timestamp)"
                        : sql);
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

        servidor = new ServidorSpringPrueba(factory);
        base = servidor.urlBase();
        cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @AfterAll
    static void cerrar() {
        if (servidor != null) servidor.close();
        if (factory != null && !factory.isClosed()) factory.close();
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
    void traduceElDesfaseDelRelojAlCodigoErr057() throws Exception {
        String token = login(nombrePropietario);
        HttpResponse<String> respuesta;
        DESFASAR_RELOJ.set(true);
        try {
            String cuerpo = "{\"fecha\":\"" + LocalDate.now(RelojService.ZONA_OPERATIVA)
                    + "\",\"tasa\":\"36.65\",\"fuente\":\"BCN\"}";
            respuesta = postJson("/api/v1/tasas", cuerpo, token, null);
        } finally {
            DESFASAR_RELOJ.set(false);
        }

        assertEquals(503, respuesta.statusCode());
        assertEquals("ERR-057", error(respuesta).error().codigo());
        assertEquals(RelojDesfasadoException.MENSAJE, error(respuesta).error().mensaje());
    }

    @Test
    void presentaElInstanteDeTasaEnZonaManagua() throws Exception {
        TasaResponse tasa = registrarTasa(login(nombrePropietario),
                LocalDate.now(RelojService.ZONA_OPERATIVA).toString());

        assertTrue(tasa.registradoEn().endsWith("-06:00"));
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
        assertEquals(401, inexistente.statusCode());
        assertEquals("NO_AUTENTICADO", error(inexistente).error().codigo());

        String token = login(nombrePropietario);
        inexistente = get("/api/v1/no-existe", token);
        assertEquals(404, inexistente.statusCode());
        assertEquals("NO_ENCONTRADO", error(inexistente).error().codigo());

        HttpResponse<String> sinAutenticar = cliente.send(HttpRequest.newBuilder(
                        URI.create(base + "/api/v1/tasas"))
                .timeout(Duration.ofSeconds(10))
                .DELETE()
                .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(401, sinAutenticar.statusCode());

        HttpResponse<String> metodo = cliente.send(HttpRequest.newBuilder(
                        URI.create(base + "/api/v1/tasas"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(405, metodo.statusCode());
        assertEquals("METODO_NO_PERMITIDO", error(metodo).error().codigo());
    }

    @Test
    void rechazaHostAjenoAntesDeResolverLaRuta() throws Exception {
        String respuesta;
        try (Socket socket = new Socket("127.0.0.1", servidor.puerto())) {
            socket.setSoTimeout(5000);
            String peticion = "GET /api/v1/no-existe HTTP/1.1\r\n"
                    + "Host: atacante.example:" + servidor.puerto() + "\r\n"
                    + "Connection: close\r\n\r\n";
            socket.getOutputStream().write(peticion.getBytes(StandardCharsets.US_ASCII));
            respuesta = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(respuesta.startsWith("HTTP/1.1 400") || respuesta.startsWith("HTTP/1.1 421"), respuesta);
        assertFalse(respuesta.startsWith("HTTP/1.1 404"), respuesta);
    }

    @Test
    void configuraUnLimiteDeTreintaSegundosParaLeerLaPeticion() {
        assertEquals(30_000, servidor.plazoConexionMillis());
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
    void cambiarElRolRevocaElTokenHastaElSiguienteLogin() throws Exception {
        String token = login(nombrePropietario);
        assertEquals(200, get("/api/v1/dashboard/metricas", token).statusCode());

        cambiarRol(idPropietario, Rol.ASISTENTE);
        HttpResponse<String> invalidada = get("/api/v1/dashboard/metricas", token);
        assertEquals(401, invalidada.statusCode());
        assertEquals("NO_AUTENTICADO", error(invalidada).error().codigo());

        cambiarRol(idPropietario, Rol.PROPIETARIO);
        assertEquals(401, get("/api/v1/dashboard/metricas", token).statusCode());
        String tokenNuevo = login(nombrePropietario);
        assertEquals(200, get("/api/v1/dashboard/metricas", tokenNuevo).statusCode());
    }

    @Test
    void desactivarUnUsuarioRevocaSuTokenHastaElSiguienteLogin() throws Exception {
        String token = login(nombreAsistente);
        assertEquals(200, get("/api/v1/dashboard/metricas", token).statusCode());

        cambiarActivo(idAsistente, false);
        HttpResponse<String> respuesta = get("/api/v1/dashboard/metricas", token);
        assertEquals(401, respuesta.statusCode());
        assertEquals("NO_AUTENTICADO", error(respuesta).error().codigo());

        cambiarActivo(idAsistente, true);
        assertEquals(401, get("/api/v1/dashboard/metricas", token).statusCode());
        assertEquals(200, get("/api/v1/dashboard/metricas", login(nombreAsistente)).statusCode());
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
        assertFalse(cuerpo.contains("argon2"));
        assertFalse(cuerpo.contains("clavesintetica"));

        LoginResponse sesion = GSON.fromJson(respuesta.body(), LoginResponse.class);
        assertNotNull(sesion.expiraEn());
        assertTrue(sesion.expiraEn().contains("T"));
    }

    @Test
    void lasCredencialesInvalidasNoRevelanSiElUsuarioExiste() throws Exception {
        long auditoriasAntes = contarIntentosLoginFallidos();
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
        assertEquals(auditoriasAntes + 2, contarIntentosLoginFallidos());

        List<String> referencias = tx.ejecutar(session -> session.createQuery(
                        "select a.referencia from Auditoria a where a.accion = 'LOGIN_FALLIDO' order by a.id desc",
                        String.class)
                .setMaxResults(2).getResultList());
        assertEquals(2, referencias.size());
        assertTrue(referencias.stream().allMatch(ref -> ref.startsWith("usuario_sha256:")
                && ref.endsWith(";origen:127.0.0.1")));
        assertFalse(referencias.stream().anyMatch(ref -> ref.contains(nombrePropietario)
                || ref.contains("no.existe.") || ref.contains("ClaveEquivocada")));
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

    private static long contarIntentosLoginFallidos() {
        return tx.ejecutar(session -> session.createQuery(
                "select count(a) from Auditoria a where a.accion = 'LOGIN_FALLIDO' and a.actor is null",
                Long.class).getSingleResult());
    }

    private static void cambiarRol(Long id, String rol) {
        tx.ejecutar(session -> {
            session.createNativeMutationQuery("update usuario set rol = :rol, version = version + 1 where id = :id")
                    .setParameter("rol", rol)
                    .setParameter("id", id)
                    .executeUpdate();
            return null;
        });
    }

    private static void cambiarActivo(Long id, boolean activo) {
        tx.ejecutar(session -> {
            session.createNativeMutationQuery("update usuario set activo = :activo, version = version + 1 where id = :id")
                    .setParameter("activo", activo)
                    .setParameter("id", id)
                    .executeUpdate();
            return null;
        });
    }

    private static void restaurarUsuario(Long id, String rol, boolean activo) {
        tx.ejecutar(session -> {
            session.createNativeMutationQuery(
                            "update usuario set rol = :rol, activo = :activo, version = version + 1 where id = :id")
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
