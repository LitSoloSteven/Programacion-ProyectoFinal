package ni.edu.uam.proyectofinalprogramacionappescritorio.api;

import com.google.gson.Gson;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ApiConfig;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ErrorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Router;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ServidorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.TokenService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.validaciones.RutasValidaciones;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.validaciones.dto.ValidacionRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.validaciones.dto.ValidacionResponse;
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
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiValidacionesTest {
    private static final char[] PASSWORD = "ClaveSintetica-2026".toCharArray();
    private static final Gson GSON = new Gson();

    private static SessionFactory factory;
    private static ServidorApi servidor;
    private static HttpClient cliente;
    private static String base;
    private static String nombreUsuario;

    @BeforeAll
    static void iniciar() throws Exception {
        Properties p = new Properties();
        p.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        p.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:latch_api_validaciones_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        p.setProperty("hibernate.connection.username", "sa");
        p.setProperty("hibernate.connection.password", "");
        p.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        p.setProperty("hibernate.jdbc.time_zone", "UTC");
        p.setProperty("hibernate.show_sql", "false");

        factory = HibernateUtil.construir(p);
        HibernateTransactions tx = new HibernateTransactions(factory);
        String hash = new PasswordHasher().crear(PASSWORD);
        nombreUsuario = "val-" + UUID.randomUUID().toString().substring(0, 8);
        tx.ejecutar(session -> {
            session.persist(new Usuario(nombreUsuario, hash, Rol.PROPIETARIO));
            return null;
        });
        SesionUsuario.cerrarSesion();

        Router router = new Router();
        new RutasValidaciones(tx).registrar(router);
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

    @Test
    void requiereTokenBearer() throws Exception {
        HttpResponse<String> respuesta = postJson(
                new ValidacionRequest("correo", "a@b.com", null, null, null), null);

        assertEquals(401, respuesta.statusCode());
        assertEquals("NO_AUTENTICADO", error(respuesta).error().codigo());
    }

    @Test
    void aceptaEntradasValidas() throws Exception {
        String token = login();

        assertTrue(validar(token, new ValidacionRequest(
                "correo", "usuario@empresa.com", null, null, null)).valido());
        assertTrue(validar(token, new ValidacionRequest(
                "telefono", "+505 8899-0011", null, null, null)).valido());
        assertTrue(validar(token, new ValidacionRequest(
                "cedula", "001-010190-0001A", null, null, null)).valido());
        assertTrue(validar(token, new ValidacionRequest(
                "monto", "150.75", null, null, null)).valido());
        assertTrue(validar(token, new ValidacionRequest(
                "rangoFechas", null, "2026-01-01", "2026-06-01", null)).valido());
    }

    @Test
    void rechazaEntradasInvalidasCon200() throws Exception {
        String token = login();

        assertFalse(validar(token, new ValidacionRequest(
                "correo", "invalido@", null, null, null)).valido());
        assertFalse(validar(token, new ValidacionRequest(
                "telefono", "12", null, null, null)).valido());
        assertFalse(validar(token, new ValidacionRequest(
                "cedula", "abc", null, null, null)).valido());
        assertFalse(validar(token, new ValidacionRequest(
                "monto", "0", null, null, null)).valido());
        assertFalse(validar(token, new ValidacionRequest(
                "monto", "10.123", null, null, null)).valido());
        assertFalse(validar(token, new ValidacionRequest(
                "rangoFechas", null, "2026-06-01", "2026-01-01", null)).valido());
        assertFalse(validar(token, new ValidacionRequest(
                "rangoFechas", null, "2026-01-01", "2026-01-02", 1)).valido());
        assertFalse(validar(token, new ValidacionRequest(
                "correo", "  ", null, null, null)).valido());
    }

    @Test
    void tipoDesconocidoDevuelve400() throws Exception {
        HttpResponse<String> respuesta = postJson(
                new ValidacionRequest("desconocido", "x", null, null, null), login());

        assertEquals(400, respuesta.statusCode());
        assertEquals("SOLICITUD_INVALIDA", error(respuesta).error().codigo());
    }

    @Test
    void jsonMalformadoDevuelve400() throws Exception {
        HttpResponse<String> respuesta = enviar("/api/v1/validaciones", "{", login());

        assertEquals(400, respuesta.statusCode());
        assertEquals("JSON_INVALIDO", error(respuesta).error().codigo());
    }

    private static ValidacionResponse validar(String token, ValidacionRequest solicitud)
            throws Exception {
        HttpResponse<String> respuesta = postJson(solicitud, token);
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        return GSON.fromJson(respuesta.body(), ValidacionResponse.class);
    }

    private static String login() throws Exception {
        HttpResponse<String> respuesta = enviar("/api/v1/auth/login", GSON.toJson(
                new LoginRequest(nombreUsuario, new String(PASSWORD))), null);
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        LoginResponse sesion = GSON.fromJson(respuesta.body(), LoginResponse.class);
        assertNotNull(sesion.token());
        return sesion.token();
    }

    private static HttpResponse<String> postJson(ValidacionRequest solicitud, String token)
            throws Exception {
        return enviar("/api/v1/validaciones", GSON.toJson(solicitud), token);
    }

    private static HttpResponse<String> enviar(String ruta, String cuerpo, String token)
            throws Exception {
        HttpRequest.Builder constructor = HttpRequest.newBuilder(
                        URI.create(base + ruta))
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