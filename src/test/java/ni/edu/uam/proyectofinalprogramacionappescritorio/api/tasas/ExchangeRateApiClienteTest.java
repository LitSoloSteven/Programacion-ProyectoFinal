package ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Auditoria;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.ControlFx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.ReferenciaTipoCambio;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.ExchangeRateApiCliente.TipoFallo.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.web.servlet.function.RouterFunctions.route;

class ExchangeRateApiClienteTest {
    private static final String LLAVE = "llave-sintetica-s1";
    private static final Instant AHORA = Instant.parse("2026-11-25T12:00:00Z");
    private static final long ACTUAL = AHORA.minusSeconds(3600).getEpochSecond();
    private static final long PROXIMA = AHORA.plusSeconds(86400).getEpochSecond();
    private static ServletWebServerApplicationContext s1;
    private static HttpClient http;
    private static final AtomicInteger RESPUESTA_H3 = new AtomicInteger();
    private static final AtomicInteger PETICIONES_H3 = new AtomicInteger();
    private static final AtomicReference<CountDownLatch> ESPERA_H3 = new AtomicReference<>();
    private static final CountDownLatch ENTRO_H3 = new CountDownLatch(1);

    @BeforeAll
    static void iniciarS1() {
        var app = new SpringApplication(S1.class);
        app.setRegisterShutdownHook(false);
        app.setDefaultProperties(Map.of("server.address", "127.0.0.1", "server.port", "0",
                "spring.main.banner-mode", "off"));
        s1 = (ServletWebServerApplicationContext) app.run();
        http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @AfterAll
    static void cerrarS1() {
        if (http != null) http.close();
        if (s1 != null) s1.close();
    }

    @Test
    void consultaUsaBearerYDevuelveTasaDecimalConFechas() throws Exception {
        var referencia = cliente("latest/USD").consultar(LLAVE);
        assertEquals(new BigDecimal("36.62430000"), referencia.tasa());
        assertEquals(AHORA, referencia.consultada());
        assertEquals(Instant.ofEpochSecond(ACTUAL), referencia.actualizada());
        assertEquals(Instant.ofEpochSecond(PROXIMA), referencia.proxima());
        assertTrue(referencia.huella().matches("[0-9a-f]{64}"));
    }

    @Test
    void clasificaCaidaCuotaTemporalCuotaAgotadaYAcreditacion() {
        assertEquals(TRANSITORIO, fallo("caida").tipo());
        assertEquals(TRANSITORIO, fallo("timeout").tipo());
        var cuota = fallo("cuota");
        assertEquals(CUOTA_TEMPORAL, cuota.tipo());
        assertEquals("120", cuota.retryAfter());
        assertEquals(CUOTA_AGOTADA, fallo("cuota-agotada").tipo());
        assertEquals(CONFIGURACION, fallo("credencial").tipo());
        assertEquals(CONFIGURACION, assertThrows(ExchangeRateApiCliente.Fallo.class,
                () -> cliente("latest/USD").consultar("incorrecta")).tipo());
    }

    @Test
    void rechazaBandaBaseFechasEsquemaYContenidoInvalido() {
        assertEquals(FUERA_DE_BANDA, fallo("fuera-de-banda").tipo());
        for (String escenario : new String[]{
                "base", "unidad", "fecha", "orden", "utc", "texto", "json", "tipo", "grande", "redirect"}) {
            assertEquals(RESPUESTA_INVALIDA, fallo(escenario).tipo(), escenario);
        }
    }

    @Test
    void h3ConservaCacheYComparteConcesionCalendarioYCuota() throws Exception {
        var propiedades = new Properties();
        propiedades.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        propiedades.setProperty("hibernate.connection.url", "jdbc:h2:mem:fx_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        propiedades.setProperty("hibernate.connection.username", "sa");
        propiedades.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        try (var factory = HibernateUtil.construir(propiedades); var ejecutor = Executors.newSingleThreadExecutor()) {
            var tx = new HibernateTransactions(factory);
            tx.ejecutar(sesion -> {
                sesion.persist(new ControlFx(AHORA, LocalDate.of(2026, 11, 1)));
                return null;
            });
            var reloj = new AtomicReference<>(AHORA);
            var h3 = new ActualizacionTasaH3(tx, cliente("h3"), LLAVE, sesion -> reloj.get());
            var otraInstancia = new ActualizacionTasaH3(new HibernateTransactions(factory),
                    cliente("h3"), LLAVE, sesion -> reloj.get());
            RESPUESTA_H3.set(200);
            var liberar = new CountDownLatch(1);
            ESPERA_H3.set(liberar);
            var primera = ejecutor.submit(h3::ejecutarSiPendiente);
            try {
                assertTrue(ENTRO_H3.await(5, TimeUnit.SECONDS));
                assertFalse(otraInstancia.ejecutarSiPendiente());
                assertEquals(1, PETICIONES_H3.get());
            } finally {
                ESPERA_H3.set(null);
                liberar.countDown();
            }
            assertTrue(primera.get(5, TimeUnit.SECONDS));
            assertEquals(1L, referencias(tx));
            assertEquals(Instant.ofEpochSecond(PROXIMA), control(tx).getProximoIntento());

            reloj.set(control(tx).getProximoIntento());
            RESPUESTA_H3.set(503);
            assertTrue(h3.ejecutarSiPendiente());
            assertEquals(reloj.get().plusSeconds(60), control(tx).getProximoIntento());
            assertFalse(otraInstancia.ejecutarSiPendiente());
            reloj.set(reloj.get().plusSeconds(60));
            assertTrue(otraInstancia.ejecutarSiPendiente());
            assertEquals(reloj.get().plusSeconds(86400), control(tx).getProximoIntento());
            assertFalse(control(tx).isReintento());

            reloj.set(control(tx).getProximoIntento());
            RESPUESTA_H3.set(429);
            assertTrue(h3.ejecutarSiPendiente());
            assertEquals(reloj.get().plusSeconds(120), control(tx).getProximoIntento());
            reloj.set(control(tx).getProximoIntento());
            RESPUESTA_H3.set(422);
            assertTrue(h3.ejecutarSiPendiente());
            assertEquals(1L, referencias(tx));
            String auditoria = tx.ejecutar(sesion -> sesion.createQuery("""
                    select a.referencia from Auditoria a where a.accion = 'REFERENCIA_EN_CUARENTENA'
                    """, String.class).getSingleResult());
            assertTrue(auditoria.matches("origen:H3;valor:101;huella:[0-9a-f]{64}"), auditoria);
            assertEquals(5, control(tx).getIntentosMes());

            reloj.set(control(tx).getProximoIntento());
            RESPUESTA_H3.set(401);
            assertTrue(h3.ejecutarSiPendiente());
            reloj.set(reloj.get().plusSeconds(86400));
            assertFalse(otraInstancia.ejecutarSiPendiente());
            var corregida = new ActualizacionTasaH3(tx, cliente("h3"), "corregida", sesion -> reloj.get());
            RESPUESTA_H3.set(429);
            assertTrue(corregida.ejecutarSiPendiente());

            reloj.set(control(tx).getProximoIntento());
            tx.ejecutar(sesion -> {
                var control = sesion.find(ControlFx.class, ControlFx.ID);
                while (control.getIntentosMes() < 100) {
                    control.reservar(UUID.randomUUID(), reloj.get());
                }
                control.liberar();
                return null;
            });
            assertFalse(h3.ejecutarSiPendiente());
            assertEquals("LIMITE_MENSUAL", control(tx).getAviso());
            reloj.set(control(tx).getProximoIntento());
            assertTrue(otraInstancia.ejecutarSiPendiente());
            assertEquals(1, control(tx).getIntentosMes());
            assertEquals(1L, referencias(tx));
        }
    }

    private static ControlFx control(HibernateTransactions tx) {
        return tx.ejecutar(sesion -> sesion.find(ControlFx.class, ControlFx.ID));
    }

    private static long referencias(HibernateTransactions tx) {
        return tx.ejecutar(sesion -> sesion.createQuery(
                "select count(r) from ReferenciaTipoCambio r", Long.class).getSingleResult());
    }

    private static ExchangeRateApiCliente cliente(String escenario) {
        var url = URI.create("http://127.0.0.1:" + s1.getWebServer().getPort() + "/v6/" + escenario);
        return new ExchangeRateApiCliente(http, url, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static ExchangeRateApiCliente.Fallo fallo(String escenario) {
        return assertThrows(ExchangeRateApiCliente.Fallo.class, () -> cliente(escenario).consultar(LLAVE));
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    public static class S1 {
        @Bean
        RouterFunction<ServerResponse> escenariosS1() {
            return route().GET("/v6/latest/USD", req -> {
                if (!("Bearer " + LLAVE).equals(req.headers().firstHeader("Authorization"))
                        || !"application/json".equals(req.headers().firstHeader("Accept"))
                        || req.uri().getQuery() != null) {
                    return ServerResponse.status(401).build();
                }
                return json(referencia("USD", ACTUAL, PROXIMA, "36.6243"));
            }).GET("/v6/{escenario}", req -> switch (req.pathVariable("escenario")) {
                case "h3" -> responderH3();
                case "caida" -> ServerResponse.status(503).build();
                case "timeout" -> ServerResponse.status(408).build();
                case "cuota" -> ServerResponse.status(429).header("Retry-After", "120").build();
                case "cuota-agotada" -> json("{\"result\":\"error\",\"error-type\":\"quota-reached\"}");
                case "credencial" -> json("{\"result\":\"error\",\"error-type\":\"invalid-key\"}");
                case "fuera-de-banda" -> json(referencia("USD", ACTUAL, PROXIMA, "100.00000001"));
                case "base" -> json(referencia("NIO", ACTUAL, PROXIMA, "36.6243"));
                case "unidad" -> json(referencia("USD", ACTUAL, PROXIMA, "36.6243")
                        .replace("\"NIO\":", "\"USD\":2,\"NIO\":"));
                case "fecha" -> json(referencia("USD", AHORA.plusSeconds(301).getEpochSecond(), PROXIMA, "36.6243"));
                case "orden" -> json(referencia("USD", ACTUAL, ACTUAL, "36.6243"));
                case "utc" -> json(referencia("USD", ACTUAL, PROXIMA, "36.6243")
                        .replace("\"base_code\"", "\"time_last_update_utc\":\"Wed, 25 Nov 2026 12:00:00 +0000\",\"base_code\""));
                case "texto" -> json(referencia("USD", ACTUAL, PROXIMA, "\"36.6243\""));
                case "json" -> json("{result:'success'}");
                case "tipo" -> ServerResponse.ok().contentType(MediaType.TEXT_PLAIN).body("{}");
                case "grande" -> json(" ".repeat(ExchangeRateApiCliente.LIMITE_BYTES + 1));
                case "redirect" -> ServerResponse.temporaryRedirect(URI.create("/v6/latest/USD")).build();
                default -> ServerResponse.notFound().build();
            }).build();
        }

        private static ServerResponse responderH3() {
            PETICIONES_H3.incrementAndGet();
            var espera = ESPERA_H3.get();
            if (espera != null) {
                ENTRO_H3.countDown();
                try {
                    if (!espera.await(5, TimeUnit.SECONDS)) return ServerResponse.status(503).build();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return ServerResponse.status(503).build();
                }
            }
            return switch (RESPUESTA_H3.get()) {
                case 200 -> json(referencia("USD", ACTUAL, PROXIMA, "36.6243"));
                case 422 -> json(referencia("USD", ACTUAL, PROXIMA, "101"));
                case 429 -> ServerResponse.status(429).header("Retry-After", "120").build();
                default -> ServerResponse.status(RESPUESTA_H3.get()).build();
            };
        }

        private static ServerResponse json(String cuerpo) {
            return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(cuerpo);
        }

        private static String referencia(String base, long actual, long proxima, String tasa) {
            return """
                    {"result":"success","base_code":"%s","time_last_update_unix":%d,
                     "time_next_update_unix":%d,"conversion_rates":{"NIO":%s}}
                    """.formatted(base, actual, proxima, tasa);
        }
    }
}
