package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

import com.sun.net.httpserver.HttpServer;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.TasaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.ConstantesLatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas HTTP puras del cliente sin Spring, Hibernate ni base de datos en classpath. */
class ClienteApiTest {
    private static final LocalDate FECHA = LocalDate.now().minusDays(1);

    @AfterEach
    void limpiarSesion() {
        SesionCliente.cerrarSesion();
        System.clearProperty(ClienteConfig.PROPIEDAD_BASE_URL);
    }

    @Test
    void loginRegistroAutorizacionYLogout() throws Exception {
        AtomicBoolean sesionValida = new AtomicBoolean(false);
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/v1/auth/login", ex -> responder(ex, 200,
                "{\"token\":\"tok-1\",\"expiraEn\":\"2026-11-26T00:00:00Z\",\"rol\":\"PROPIETARIO\",\"usuarioId\":1,"
                        + "\"nombre\":\"prop-cli\"}"));
        servidor.createContext("/api/v1/auth/logout", ex -> {
            sesionValida.set(false);
            responderVacio(ex, 204);
        });
        servidor.createContext("/api/v1/tasas", ex -> {
            if (!"Bearer tok-1".equals(ex.getRequestHeaders().getFirst("Authorization")) || !sesionValida.get()) {
                responder(ex, 401, "{\"error\":{\"codigo\":\"NO_AUTORIZADO\",\"mensaje\":\"Sin sesión\"}}");
                return;
            }
            String path = ex.getRequestURI().getPath();
            if ("POST".equals(ex.getRequestMethod()) && path.endsWith("/autorizar")) {
                responder(ex, 200, "{\"id\":\"tasa-2\",\"fecha\":\"" + FECHA
                        + "\",\"nioPorUsd\":\"36.70000000\",\"origen\":\"BCN\","
                        + "\"autorizada\":true,\"version\":1}");
            } else if ("POST".equals(ex.getRequestMethod())) {
                responder(ex, 201, "{\"id\":\"tasa-1\",\"fecha\":\"" + FECHA
                        + "\",\"nioPorUsd\":\"36.70000000\",\"origen\":\"BCN\","
                        + "\"autorizada\":false,\"version\":0}");
            } else if (path.endsWith("/vigente")) {
                responder(ex, 200, "{\"id\":\"tasa-2\",\"fecha\":\"" + FECHA
                        + "\",\"nioPorUsd\":\"36.70000000\",\"origen\":\"BCN\","
                        + "\"autorizada\":true,\"version\":1}");
            } else {
                responder(ex, 200, "{\"tasas\":[{\"id\":\"tasa-1\",\"fecha\":\"" + FECHA
                        + "\",\"nioPorUsd\":\"36.70000000\",\"origen\":\"BCN\","
                        + "\"autorizada\":false,\"version\":0}]}");
            }
        });
        servidor.start();

        try {
            ClienteApi api = new ClienteApi("http://127.0.0.1:" + servidor.getAddress().getPort());
            ClienteAuth auth = new ClienteAuth(api);
            ClienteTasas tasas = new ClienteTasas(api);

            sesionValida.set(true);
            LoginResponse sesion = auth.login("prop-cli", "ClaveSintetica-2026");
            assertEquals("tok-1", sesion.token());
            assertEquals(ConstantesLatch.ROL_PROPIETARIO, sesion.rol());
            assertTrue(SesionCliente.haySesionActiva());

            TasaResponse informativa = tasas.registrar(FECHA, new BigDecimal("36.7"), "BCN");
            assertEquals("36.70000000", informativa.nioPorUsd());
            assertFalse(informativa.autorizada());

            List<TasaResponse> lista = tasas.listar(FECHA, null, 50);
            assertEquals(1, lista.size());

            TasaResponse autorizada = tasas.autorizar(informativa.id(), "Verificada");
            assertNotEquals(informativa.id(), autorizada.id());
            assertTrue(autorizada.autorizada());
            assertEquals(autorizada.id(), tasas.vigente(FECHA).orElseThrow().id());

            auth.logout();
            assertFalse(SesionCliente.haySesionActiva());
            ErrorClienteException error = assertThrows(ErrorClienteException.class,
                    () -> tasas.listar(FECHA, null, 50));
            assertEquals(401, error.getEstado());
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void unTokenRevocadoDescartaLaSesionYNotificaSoloUnaVez() throws Exception {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/v1/auth/login", ex -> responder(ex, 200,
                "{\"token\":\"tok-rev\",\"expiraEn\":\"2026-11-26T00:00:00Z\",\"rol\":\"PROPIETARIO\",\"usuarioId\":2,"
                        + "\"nombre\":\"prop-cli\"}"));
        servidor.createContext("/api/v1/auth/logout", ex -> responderVacio(ex, 204));
        servidor.createContext("/api/v1/dashboard/metricas", ex -> responder(ex, 401,
                "{\"error\":{\"codigo\":\"NO_AUTORIZADO\",\"mensaje\":\"Token revocado\"}}"));
        servidor.start();

        try {
            ClienteApi api = new ClienteApi("http://127.0.0.1:" + servidor.getAddress().getPort());
            ClienteAuth auth = new ClienteAuth(api);
            var avisos = new AtomicInteger();
            api.alExpirar(avisos::incrementAndGet);

            LoginResponse sesion = auth.login("prop-cli", "ClaveSintetica-2026");
            auth.logout(sesion.token());
            for (int intento = 0; intento < 2; intento++) {
                var error = assertThrows(ErrorClienteException.class,
                        () -> api.get("/api/v1/dashboard/metricas", Void.class));
                assertEquals(401, error.getEstado());
            }
            assertFalse(api.sesionActiva());
            assertFalse(SesionCliente.haySesionActiva());
            assertEquals(1, avisos.get());
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void traduceCredencialesInvalidasYFaltaDeTasaYNormalizaUrl() throws Exception {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/v1/auth/login", ex -> responder(ex, 401,
                "{\"error\":{\"codigo\":\"CREDENCIALES_INVALIDAS\",\"mensaje\":\"Usuario o clave incorrectos\"}}"));
        servidor.createContext("/api/v1/tasas/vigente", ex -> responder(ex, 404,
                "{\"error\":{\"codigo\":\"NO_ENCONTRADO\",\"mensaje\":\"Sin tasa\"}}"));
        servidor.start();

        try {
            ClienteApi api = new ClienteApi("http://127.0.0.1:" + servidor.getAddress().getPort());
            ClienteAuth auth = new ClienteAuth(api);

            ErrorClienteException error = assertThrows(ErrorClienteException.class,
                    () -> auth.login("prop-cli", "ClaveIncorrecta-2026"));
            assertEquals(401, error.getEstado());
            assertEquals("CREDENCIALES_INVALIDAS", error.getCodigo());

            assertTrue(new ClienteTasas(api).vigente(FECHA).isEmpty());

            System.setProperty(ClienteConfig.PROPIEDAD_BASE_URL, "http://127.0.0.1:8080/latch-api///");
            assertEquals("http://127.0.0.1:8080/latch-api", ClienteConfig.resolverUrlBase());
            assertThrows(IllegalArgumentException.class, () -> ClienteConfig.normalizar("ftp://127.0.0.1:8080"));
        } finally {
            servidor.stop(0);
        }
    }

    private static void responder(com.sun.net.httpserver.HttpExchange ex, int estado, String cuerpo)
            throws java.io.IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(estado, bytes.length);
        try (var salida = ex.getResponseBody()) {
            salida.write(bytes);
        }
    }

    private static void responderVacio(com.sun.net.httpserver.HttpExchange ex, int estado) throws java.io.IOException {
        ex.sendResponseHeaders(estado, -1);
        ex.close();
    }
}
