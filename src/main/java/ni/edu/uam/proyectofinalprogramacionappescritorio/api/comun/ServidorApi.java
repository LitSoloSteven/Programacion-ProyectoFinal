package ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.RutasAuth;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AutenticacionService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servidor HTTP local de Latch. Escucha únicamente en 127.0.0.1, usa un pool
 * acotado y concentra el manejo uniforme de errores y de cabeceras.
 */
public final class ServidorApi implements AutoCloseable {
    private static final int HILOS_API = 4;
    private static final int COLA_API = 64;
    /** Consumo acotado del cuerpo rechazado para que el cliente pueda leer el 413. */
    private static final long LIMITE_DRENADO = 1024L * 1024;

    private final ApiConfig config;
    private final TokenService tokens;
    private final Router router;
    private final ThreadPoolExecutor ejecutor;
    private HttpServer servidor;

    public ServidorApi(ApiConfig config, AutenticacionService autenticacion) {
        this(config, autenticacion, new TokenService());
    }

    public ServidorApi(ApiConfig config, AutenticacionService autenticacion, TokenService tokens) {
        this(config, autenticacion, tokens, new Router());
    }

    public ServidorApi(ApiConfig config, AutenticacionService autenticacion,
                       TokenService tokens, Router router) {
        this.config = Objects.requireNonNull(config, "Falta la configuración de la API.");
        this.tokens = Objects.requireNonNull(tokens, "Falta el servicio de tokens.");
        this.router = Objects.requireNonNull(router, "Falta el router de la API.");
        Objects.requireNonNull(autenticacion, "Falta el servicio de autenticación.");
        new RutasAuth(autenticacion, tokens).registrar(router);

        AtomicInteger contador = new AtomicInteger();
        this.ejecutor = new ThreadPoolExecutor(
                HILOS_API, HILOS_API, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(COLA_API),
                runnable -> {
                    Thread hilo = new Thread(runnable, "latch-api-" + contador.incrementAndGet());
                    hilo.setDaemon(true);
                    return hilo;
                },
                new ThreadPoolExecutor.AbortPolicy());
    }

    public synchronized void iniciar() throws IOException {
        if (servidor != null) throw new IllegalStateException("La API ya está iniciada.");
        servidor = HttpServer.create(new InetSocketAddress(config.host(), config.puerto()), 0);
        servidor.setExecutor(ejecutor);
        servidor.createContext("/api/v1", this::manejar);
        servidor.createContext("/", this::manejar);
        servidor.start();
    }

    public synchronized int puerto() {
        return servidor == null ? config.puerto() : servidor.getAddress().getPort();
    }

    public String urlBase() {
        return "http://" + config.host() + ":" + puerto();
    }

    public Router router() {
        return router;
    }

    private void manejar(HttpExchange intercambio) {
        try {
            Router.Resultado resultado = router.resolver(
                    intercambio.getRequestMethod(), intercambio.getRequestURI().getPath());
            Map<String, String> consulta = Router.analizarConsulta(
                    intercambio.getRequestURI().getRawQuery());

            SesionApi sesion = null;
            String token = null;
            if (!resultado.ruta().publica()) {
                token = extraerToken(intercambio);
                sesion = tokens.validar(token).orElseThrow(ExcepcionApi::noAutorizado);
            }

            ContextoPeticion contexto = new ContextoPeticion(
                    intercambio, resultado.parametros(), consulta, sesion, token);
            escribir(intercambio, resultado.ruta().manejador().manejar(contexto));
        } catch (ExcepcionApi e) {
            if (e.getEstado() == 413) drenarCuerpo(intercambio);
            escribirError(intercambio, e);
        } catch (IllegalArgumentException e) {
            escribirError(intercambio, ExcepcionApi.solicitudInvalida(
                    "La solicitud contiene datos inválidos."));
        } catch (SecurityException e) {
            escribirError(intercambio, ExcepcionApi.sinPermiso());
        } catch (Exception e) {
            // No se registra el cuerpo, la cabecera Authorization ni datos sensibles.
            System.err.println("Error interno en API Latch: " + e.getClass().getSimpleName());
            escribirError(intercambio, ExcepcionApi.interno());
        } finally {
            intercambio.close();
        }
    }

    /**
     * Evita que el servidor cierre la conexión mientras el cliente aún envía
     * un cuerpo grande. Se descarta como máximo un MiB adicional.
     */
    private static void drenarCuerpo(HttpExchange intercambio) {
        try (InputStream entrada = intercambio.getRequestBody()) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int leidos;
            while ((leidos = entrada.read(buffer)) != -1) {
                total += leidos;
                if (total >= LIMITE_DRENADO) break;
            }
        } catch (IOException ignored) {
            // El cliente ya cerró la conexión; la respuesta de error sigue su curso.
        }
    }

    private static String extraerToken(HttpExchange intercambio) {
        List<String> valores = intercambio.getRequestHeaders().get("Authorization");
        if (valores == null || valores.isEmpty()) throw ExcepcionApi.noAutorizado();
        String valor = valores.get(0).strip();
        if (!valor.regionMatches(true, 0, "Bearer ", 0, 7)) throw ExcepcionApi.noAutorizado();
        String token = valor.substring(7).strip();
        if (token.isEmpty()) throw ExcepcionApi.noAutorizado();
        return token;
    }

    private static void escribir(HttpExchange intercambio, RespuestaJson respuesta) throws IOException {
        intercambio.getResponseHeaders().set("Cache-Control", "no-store");
        intercambio.getResponseHeaders().set("X-Content-Type-Options", "nosniff");

        if (respuesta.estado() == 204) {
            intercambio.sendResponseHeaders(204, -1);
            return;
        }

        byte[] cuerpo = Json.escribir(respuesta.cuerpo()).getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        intercambio.sendResponseHeaders(respuesta.estado(), cuerpo.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(cuerpo);
        }
    }

    private static void escribirError(HttpExchange intercambio, ExcepcionApi error) {
        try {
            if (error.getEstado() == 401) {
                intercambio.getResponseHeaders().set("WWW-Authenticate", "Bearer");
            }
            escribir(intercambio, new RespuestaJson(error.getEstado(),
                    new ErrorApi.Cuerpo(error.getCodigo(), error.getMessage())));
        } catch (IOException e) {
            // La conexión ya no está disponible; no hay nada más que enviar.
        }
    }

    public synchronized void detener() {
        if (servidor != null) {
            servidor.stop(0);
            servidor = null;
        }
        ejecutor.shutdownNow();
        try {
            ejecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        detener();
    }
}