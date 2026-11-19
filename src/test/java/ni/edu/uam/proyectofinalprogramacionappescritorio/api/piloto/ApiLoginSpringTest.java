package ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ApiConfig;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ErrorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Json;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.LectorCuerpo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PasswordHasher;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.boot.tomcat.TomcatWebServer;

import java.io.ByteArrayInputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiLoginSpringTest {
    private static final String USUARIO = "piloto-sintetico";
    private static final String CLAVE = "Clave-Sintetica-Piloto-2026";
    private static final String LOGIN = "/api/v1/auth/login";

    private static final String ASISTENTE = "piloto-asistente";
    private static final String VALIDACIONES = "/api/v1/validaciones";
    private static final String LOGOUT = "/api/v1/auth/logout";

    private static HibernateTransactions tx;
    private static ServletWebServerApplicationContext servidor;
    private static HttpClient cliente;
    private static int puerto;
    private static Long usuarioId;

    @BeforeAll
    static void iniciar() {
        Properties propiedades = new Properties();
        propiedades.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        propiedades.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:piloto_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        propiedades.setProperty("hibernate.connection.username", "sa");
        propiedades.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        propiedades.setProperty("hibernate.jdbc.time_zone", "UTC");

        servidor = (ServletWebServerApplicationContext)
                PilotoLoginApplication.iniciar(ApiConfig.paraPruebas(0),
                        () -> HibernateUtil.construir(propiedades));
        puerto = servidor.getWebServer().getPort();
        cliente = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5)).build();
        tx = new HibernateTransactions(servidor.getBean(SessionFactory.class));
        String hash = new PasswordHasher().crear(CLAVE.toCharArray());
        usuarioId = tx.ejecutar(sesion -> {
            Usuario usuario = new Usuario(USUARIO, hash, Rol.PROPIETARIO);
            sesion.persist(usuario);
            sesion.persist(new Usuario(ASISTENTE, hash, Rol.ASISTENTE));
            return usuario.getId();
        });
    }

    @AfterAll
    static void cerrar() {
        if (cliente != null) cliente.close();
        if (servidor != null) servidor.close();
    }

    @Test
    void loginValidoEmiteTokenConElContratoVigente() throws Exception {
        Instant antes = Instant.now();
        var respuesta = enviar("application/json; charset=UTF-8",
                HttpRequest.BodyPublishers.ofString(Json.escribir(new LoginRequest(USUARIO, CLAVE))));
        assertEquals(200, respuesta.statusCode());
        verificarCabeceras(respuesta);
        var login = Json.leer(respuesta.body(), LoginResponse.class);
        assertTrue(login.token().matches("[A-Za-z0-9_-]{43}"));
        assertEquals(USUARIO, login.nombre());
        assertEquals(usuarioId, login.usuarioId());
        assertEquals(Rol.PROPIETARIO, login.rol());
        Instant expira = Instant.parse(login.expiraEn());
        assertFalse(expira.isBefore(antes.plusSeconds(900)));
        assertFalse(expira.isAfter(Instant.now().plusSeconds(900)));
        assertFalse(respuesta.body().contains(CLAVE));
        assertNull(SesionUsuario.getActual());
    }

    @Test
    void loginInvalidoMantieneElErrorYElRetoBearer() throws Exception {
        long antes = tx.ejecutar(sesion -> sesion.createQuery(
                "select count(a) from Auditoria a where a.accion = 'LOGIN_FALLIDO' and a.actor is null",
                Long.class).getSingleResult());
        var respuesta = enviar("application/json", HttpRequest.BodyPublishers.ofString(
                Json.escribir(new LoginRequest(USUARIO, "Clave-Sintetica-Incorrecta"))));
        var inexistente = enviar("application/json", HttpRequest.BodyPublishers.ofString(
                Json.escribir(new LoginRequest("usuario-inexistente", CLAVE))));
        verificarError(respuesta, 401, "CREDENCIALES_INVALIDAS");
        verificarError(inexistente, 401, "CREDENCIALES_INVALIDAS");
        assertEquals(respuesta.body(), inexistente.body());
        var referencias = tx.ejecutar(sesion -> sesion.createQuery(
                        "select a.referencia from Auditoria a where a.accion = 'LOGIN_FALLIDO' and a.actor is null order by a.id desc",
                        String.class).setMaxResults(2).getResultList());
        assertEquals(antes + 2, tx.ejecutar(sesion -> sesion.createQuery(
                "select count(a) from Auditoria a where a.accion = 'LOGIN_FALLIDO' and a.actor is null",
                Long.class).getSingleResult()).longValue());
        assertEquals(2, referencias.size());
        assertTrue(referencias.stream().allMatch(ref -> ref.startsWith("usuario_sha256:")
                && ref.endsWith(";origen:127.0.0.1")));
        assertFalse(referencias.stream().anyMatch(ref -> ref.contains(USUARIO)
                || ref.contains("usuario-inexistente") || ref.contains("Clave-Sintetica-Incorrecta")));
        assertEquals("Bearer", respuesta.headers().firstValue("WWW-Authenticate").orElseThrow());
    }

    @Test
    void loginInvalidoCierraLaConexionAlResponder() throws Exception {
        var cuerpo = Json.escribir(new LoginRequest("piloto-cierre-sintetico", CLAVE));
        var respuesta = enviar("application/json", HttpRequest.BodyPublishers.ofString(cuerpo));

        verificarError(respuesta, 401, "CREDENCIALES_INVALIDAS");
        assertEquals("close", respuesta.headers().firstValue("Connection").orElse("ausente"));
    }

    @Test
    void aplicaElBloqueoDespuesDeCincoFallosParaLaCuenta() throws Exception {
        String cuerpo = Json.escribir(new LoginRequest("piloto-limite-sintetico", CLAVE));
        for (int intento = 0; intento < 5; intento++) {
            verificarError(enviar("application/json", HttpRequest.BodyPublishers.ofString(cuerpo)),
                    401, "CREDENCIALES_INVALIDAS");
        }
        verificarError(enviar("application/json", HttpRequest.BodyPublishers.ofString(cuerpo)),
                429, "DEMASIADOS_INTENTOS");
    }

    @Test
    void controlaTipoJsonYLimiteTambienConEnvioPorFragmentos() throws Exception {
        verificarError(enviar(null, HttpRequest.BodyPublishers.ofString("{}")),
                415, "CONTENIDO_NO_SOPORTADO");
        verificarError(enviar("application/json", HttpRequest.BodyPublishers.ofString("{")),
                400, "JSON_INVALIDO");
        verificarError(enviar("application/json", HttpRequest.BodyPublishers.ofString("{}")),
                400, "SOLICITUD_INVALIDA");
        byte[] grande = new byte[LectorCuerpo.LIMITE_BYTES + 1];
        verificarError(enviar("application/json", HttpRequest.BodyPublishers.ofByteArray(grande)),
                413, "CUERPO_DEMASIADO_GRANDE");
        verificarError(enviar("application/json", HttpRequest.BodyPublishers.ofInputStream(
                () -> new ByteArrayInputStream(grande))), 413, "CUERPO_DEMASIADO_GRANDE");
    }

    @Test
    void rechazaHostAjenoSinProcesarElLogin() throws Exception {
        try (Socket socket = new Socket(ApiConfig.HOST_LOCAL, puerto)) {
            socket.setSoTimeout(5_000);
            String peticion = "POST " + LOGIN + " HTTP/1.1\r\nHost: example.test:" + puerto
                    + "\r\nContent-Length: 0\r\nConnection: close\r\n\r\n";
            socket.getOutputStream().write(peticion.getBytes(StandardCharsets.US_ASCII));
            String respuesta = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(respuesta.startsWith("HTTP/1.1 400"));
            assertTrue(respuesta.contains("La cabecera Host no es válida."));
        }
    }

    @Test
    void revocaSoloElTokenDeLogoutYConservaLasCabecerasDel204() throws Exception {
        String token = login(USUARIO);
        String otraSesion = login(USUARIO);
        assertEquals(200, validar(token).statusCode());
        var logout = enviar("POST", LOGOUT, token, null, HttpRequest.BodyPublishers.noBody());
        assertEquals(204, logout.statusCode());
        assertEquals("", logout.body());
        assertTrue(logout.headers().firstValue("Content-Type").isEmpty());
        verificarCabecerasSeguras(logout);
        verificarError(validar(token), 401, "NO_AUTENTICADO");
        verificarError(enviar("POST", LOGOUT, token, null, HttpRequest.BodyPublishers.noBody()),
                401, "NO_AUTENTICADO");
        assertEquals(200, validar(otraSesion).statusCode());
    }

    @Test
    void autenticaAntesDeRevelarUnaRutaOMetodoYNoUsaLaSesionGlobal() throws Exception {
        var escritorio = SesionUsuario.paraSesionAutenticada(usuarioId, USUARIO, Rol.PROPIETARIO);
        SesionUsuario.establecer(escritorio);
        try {
            for (String token : new String[]{null, "inventado", "x".repeat(600), login(USUARIO) + "x"}) {
                var fallo = validar(token);
                verificarError(fallo, 401, "NO_AUTENTICADO");
                assertEquals("Bearer", fallo.headers().firstValue("WWW-Authenticate").orElseThrow());
            }
            String asistente = login(ASISTENTE);
            assertEquals(200, validar(asistente).statusCode());
            assertEquals(escritorio, SesionUsuario.getActual());
            var vacio = HttpRequest.BodyPublishers.noBody();
            verificarError(enviar("GET", "/api/v1/no-existe", null, null, vacio), 401, "NO_AUTENTICADO");
            verificarError(enviar("DELETE", VALIDACIONES, null, null, vacio), 401, "NO_AUTENTICADO");
            verificarError(enviar("DELETE", VALIDACIONES, null, "application/x-www-form-urlencoded",
                    HttpRequest.BodyPublishers.ofString("valor=%zz")), 401, "NO_AUTENTICADO");
            verificarError(enviar("GET", LOGIN, null, null, vacio), 401, "NO_AUTENTICADO");
            verificarError(enviar("OPTIONS", VALIDACIONES, null, null, vacio), 401, "NO_AUTENTICADO");
            verificarError(enviar("GET", "/api/v1/no-existe", asistente, null, vacio), 404, "NO_ENCONTRADO");
            verificarError(enviar("DELETE", VALIDACIONES, asistente, null, vacio), 405, "METODO_NO_PERMITIDO");
            verificarError(enviar("OPTIONS", VALIDACIONES, asistente, null, vacio), 405, "METODO_NO_PERMITIDO");
        } finally {
            SesionUsuario.cerrarSesion();
        }
    }

    @Test
    void invalidaTokensTrasCambiarRolOEstadoYNoLosRehabilita() throws Exception {
        for (String cambio : List.of("rol = 'PROPIETARIO'", "activo = false")) {
            String token = login(ASISTENTE);
            assertEquals(200, validar(token).statusCode());
            try {
                modificarAsistente(cambio);
                verificarError(validar(token), 401, "NO_AUTENTICADO");
            } finally {
                modificarAsistente("rol = 'ASISTENTE', activo = true");
            }
            verificarError(validar(token), 401, "NO_AUTENTICADO");
            assertEquals(200, validar(login(ASISTENTE)).statusCode());
        }
    }

    @Test
    void validaContenidoEnLaRutaProtegidaTrasAutenticar() throws Exception {
        var cuerpo = HttpRequest.BodyPublishers.ofString("{}");
        verificarError(enviar("POST", VALIDACIONES, null, "text/plain", cuerpo), 401, "NO_AUTENTICADO");
        String token = login(ASISTENTE);
        verificarError(enviar("POST", VALIDACIONES, token, "text/plain", cuerpo),
                415, "CONTENIDO_NO_SOPORTADO");
        var grande = HttpRequest.BodyPublishers.ofByteArray(new byte[LectorCuerpo.LIMITE_BYTES + 1]);
        verificarError(enviar("POST", VALIDACIONES, token, "application/json", grande),
                413, "CUERPO_DEMASIADO_GRANDE");
    }

    @Test
    void sanitizaRechazosDelConectorYRechazaHostAntesDeLaRuta() throws Exception {
        String local = "Host: 127.0.0.1:" + puerto + "\r\n";
        assertEquals("127.0.0.1", ((TomcatWebServer) servidor.getWebServer())
                .getTomcat().getConnector().getProperty("address").toString().replace("/", ""));
        for (String cabeceras : List.of("", local + local, "Host: ajeno.test:" + puerto + "\r\n",
                local + "Cabecera-Muy-Grande: " + "x".repeat(9_000) + "\r\n")) {
            verificarRechazoCrudo("GET /api/v1/no-existe HTTP/1.1\r\n" + cabeceras);
        }
        verificarRechazoCrudo("GET /api/v1/%zz HTTP/1.1\r\n" + local);
        verificarRechazoCrudo("OPTIONS * HTTP/1.1\r\n" + local);
        verificarRechazoCrudo("GET http://127.0.0.1:" + puerto + "/api/v1/no-existe HTTP/1.0\r\n");
    }

    @Test
    void cierraSpringAlVencerElPlazoTotalAunqueSiganLlegandoBytes() throws Exception {
        try (var lectores = Executors.newVirtualThreadPerTaskExecutor();
             Socket cabeceras = conectar(puerto);
             Socket cuerpo = conectar(puerto)) {
            var sockets = List.of(cabeceras, cuerpo);
            var cierres = new ArrayList<Future<Long>>();
            Thread.sleep(Duration.ofSeconds(5));
            for (Socket socket : sockets) {
                long inicio = System.nanoTime();
                escribir(socket, "POST " + LOGIN + " HTTP/1.1\r\n");
                cierres.add(lectores.submit(() -> medirCierreSinRespuesta(socket, inicio)));
            }
            for (int envio = 1; envio <= 7; envio++) {
                Thread.sleep(Duration.ofSeconds(4));
                escribir(cabeceras, "X-Prueba-" + envio + ": parcial\r\n");
                if (envio < 3) {
                    escribir(cuerpo, "X-Prueba-" + envio + ": parcial\r\n");
                } else if (envio == 3) {
                    escribir(cuerpo, "Host: 127.0.0.1:" + cuerpo.getPort()
                            + "\r\nContent-Type: application/json\r\nContent-Length: 64\r\n\r\n{");
                } else {
                    escribir(cuerpo, " ");
                }
            }
            for (int i = 0; i < cierres.size(); i++) {
                long ms = cierres.get(i).get(10, TimeUnit.SECONDS);
                assertTrue(ms >= 29_500 && ms <= 35_000,
                        "Tiempo fuera de plazo en " + sockets.get(i) + ": " + ms + " ms");
                System.out.printf("Recepción Spring %s: cierre sin respuesta tras %d ms%n",
                        i == 0 ? "cabeceras" : "cuerpo", ms);
            }
        }
    }

    private static Socket conectar(int puerto) throws Exception {
        Socket socket = new Socket(ApiConfig.HOST_LOCAL, puerto);
        socket.setSoTimeout(39_000);
        return socket;
    }

    private static long medirCierreSinRespuesta(Socket socket, long inicio) throws Exception {
        try {
            assertEquals(-1, socket.getInputStream().read(), "El servidor respondió antes de cerrar");
        } catch (SocketException e) {
            // El SO puede señalar el cierre TCP con un reset en vez de EOF.
            assertTrue(e.getMessage().toLowerCase(Locale.ROOT).contains("reset"), e::getMessage);
        }
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
    }

    private static void escribir(Socket socket, String fragmento) throws Exception {
        socket.getOutputStream().write(fragmento.getBytes(StandardCharsets.US_ASCII));
    }


    private static void verificarRechazoCrudo(String inicio) throws Exception {
        try (Socket socket = new Socket(ApiConfig.HOST_LOCAL, puerto)) {
            socket.setSoTimeout(5_000);
            socket.getOutputStream().write((inicio + "Connection: close\r\n\r\n")
                    .getBytes(StandardCharsets.US_ASCII));
            String respuesta = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String[] partes = respuesta.split("\r\n\r\n", 2);
            String cabeceras = partes[0].toLowerCase(Locale.ROOT);
            assertTrue(cabeceras.startsWith("http/1.1 400"), respuesta);
            assertTrue(cabeceras.contains("content-type: application/json"), respuesta);
            assertTrue(cabeceras.contains("cache-control: no-store"), respuesta);
            assertTrue(cabeceras.contains("x-content-type-options: nosniff"), respuesta);
            assertFalse(cabeceras.contains("access-control-"), respuesta);
            var error = Json.leer(partes[1], ErrorApi.Cuerpo.class).error();
            assertEquals("SOLICITUD_INVALIDA", error.codigo());
            assertTrue(List.of("La solicitud HTTP no es válida.", "La cabecera Host no es válida.")
                    .contains(error.mensaje()), respuesta);
        }
    }

    private static String login(String usuario) throws Exception {
        var respuesta = enviar("application/json", HttpRequest.BodyPublishers.ofString(
                Json.escribir(new LoginRequest(usuario, CLAVE))));
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        return Json.leer(respuesta.body(), LoginResponse.class).token();
    }

    private static void modificarAsistente(String cambio) {
        tx.ejecutar(sesion -> sesion.createNativeMutationQuery(
                        "update usuario set " + cambio + ", version = version + 1 where nombre = :nombre")
                .setParameter("nombre", ASISTENTE).executeUpdate());
    }

    private static HttpResponse<String> validar(String token) throws Exception {
        return enviar("POST", VALIDACIONES, token, "application/json", HttpRequest.BodyPublishers.ofString(
                "{\"tipo\":\"correo\",\"valor\":\"alguien@dominio.com\"}"));
    }


    private static HttpResponse<String> enviar(String tipo, HttpRequest.BodyPublisher cuerpo)
            throws Exception {
        return enviar("POST", LOGIN, null, tipo, cuerpo);
    }

    private static HttpResponse<String> enviar(String metodo, String ruta, String token,
                                               String tipo, HttpRequest.BodyPublisher cuerpo)
            throws Exception {
        var peticion = HttpRequest.newBuilder(URI.create("http://" + ApiConfig.HOST_LOCAL + ":" + puerto + ruta))
                .timeout(Duration.ofSeconds(10))
                .header("Origin", "https://example.test")
                .method(metodo, cuerpo);
        if (tipo != null) peticion.header("Content-Type", tipo);
        if (token != null) peticion.header("Authorization", "Bearer " + token);
        return cliente.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void verificarError(HttpResponse<String> respuesta, int estado, String codigo) {
        assertEquals(estado, respuesta.statusCode());
        verificarCabeceras(respuesta);
        assertEquals(codigo, Json.leer(respuesta.body(), ErrorApi.Cuerpo.class).error().codigo());
    }

    private static void verificarCabeceras(HttpResponse<String> respuesta) {
        var tipo = MediaType.parseMediaType(respuesta.headers().firstValue("Content-Type").orElseThrow());
        assertTrue(MediaType.APPLICATION_JSON.equalsTypeAndSubtype(tipo));
        assertEquals(StandardCharsets.UTF_8, tipo.getCharset());
        verificarCabecerasSeguras(respuesta);
    }

    private static void verificarCabecerasSeguras(HttpResponse<String> respuesta) {
        assertEquals("no-store", respuesta.headers().firstValue("Cache-Control").orElseThrow());
        assertEquals("nosniff", respuesta.headers().firstValue("X-Content-Type-Options").orElseThrow());
        assertTrue(respuesta.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
        assertTrue(respuesta.headers().firstValue("Access-Control-Allow-Credentials").isEmpty());
    }
}
