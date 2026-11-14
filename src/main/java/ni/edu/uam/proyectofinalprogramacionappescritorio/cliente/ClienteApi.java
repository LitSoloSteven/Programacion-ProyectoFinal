package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ErrorApi;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

/**
 * Cliente HTTP compartido por la interfaz JavaFX. Usa tiempos acotados y
 * conserva el token Bearer en memoria; nunca lo escribe en logs.
 */
public final class ClienteApi {
    private static final Duration CONEXION = Duration.ofSeconds(5);
    private static final Duration TOTAL = Duration.ofSeconds(10);

    private final String baseUrl;
    private final HttpClient http;
    private final Gson gson = new Gson();
    private volatile String token;

    public ClienteApi(String baseUrl) {
        Objects.requireNonNull(baseUrl, "Falta la dirección del servicio local.");
        this.baseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        this.http = HttpClient.newBuilder().connectTimeout(CONEXION).build();
    }

    public synchronized void establecerToken(String token) {
        this.token = token;
    }

    public synchronized void limpiarToken() {
        this.token = null;
    }

    /** Extrae y borra el token actual de forma atómica. */
    public synchronized String tomarToken() {
        String actual = token;
        token = null;
        return actual;
    }

    public <T> T get(String ruta, Class<T> tipo) {
        return enviar(construir(ruta).GET().build(), tipo);
    }

    public <T> T post(String ruta, Object cuerpo, Class<T> tipo) {
        HttpRequest peticion = construir(ruta)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        gson.toJson(cuerpo), StandardCharsets.UTF_8))
                .build();
        return enviar(peticion, tipo);
    }

    public void postSinContenido(String ruta, Object cuerpo) {
        post(ruta, cuerpo, Void.class);
    }

    public <T> T put(String ruta, Object cuerpo, Class<T> tipo) {
        HttpRequest peticion = construir(ruta)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(
                        gson.toJson(cuerpo), StandardCharsets.UTF_8))
                .build();
        return enviar(peticion, tipo);
    }

    public <T> T patch(String ruta, Object cuerpo, Class<T> tipo) {
        HttpRequest peticion = construir(ruta)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(
                        gson.toJson(cuerpo), StandardCharsets.UTF_8))
                .build();
        return enviar(peticion, tipo);
    }

    /**
     * Envía una petición con un token explícito sin tocar el token guardado.
     * Se usa para revocar una sesión ya descartada localmente.
     */
    public void postSinContenidoConToken(String ruta, Object cuerpo, String tokenExplicito) {
        HttpRequest peticion = construirConToken(ruta, tokenExplicito)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        gson.toJson(cuerpo), StandardCharsets.UTF_8))
                .build();
        enviar(peticion, Void.class);
    }

    private HttpRequest.Builder construir(String ruta) {
        return construirConToken(ruta, token);
    }

    private HttpRequest.Builder construirConToken(String ruta, String tokenExplicito) {
        HttpRequest.Builder constructor = HttpRequest.newBuilder(
                        URI.create(baseUrl + ruta))
                .timeout(TOTAL)
                .header("Accept", "application/json");
        if (tokenExplicito != null && !tokenExplicito.isBlank()) {
            constructor.header("Authorization", "Bearer " + tokenExplicito);
        }
        return constructor;
    }

    private <T> T enviar(HttpRequest peticion, Class<T> tipo) {
        try {
            HttpResponse<String> respuesta = http.send(peticion,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int estado = respuesta.statusCode();
            if (estado >= 200 && estado < 300) {
                if (estado == 204 || tipo == Void.class || respuesta.body().isBlank()) {
                    return null;
                }
                return gson.fromJson(respuesta.body(), tipo);
            }
            throw error(respuesta);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErrorClienteException(0, "INTERRUMPIDO",
                    "La operación fue cancelada.");
        } catch (IOException e) {
            throw new ErrorClienteException(0, "SIN_CONEXION",
                    "No se pudo conectar con el servicio local de Latch.");
        }
    }

    private ErrorClienteException error(HttpResponse<String> respuesta) {
        try {
            ErrorApi.Cuerpo cuerpo = gson.fromJson(respuesta.body(), ErrorApi.Cuerpo.class);
            if (cuerpo != null && cuerpo.error() != null) {
                return new ErrorClienteException(respuesta.statusCode(),
                        cuerpo.error().codigo(), cuerpo.error().mensaje());
            }
        } catch (JsonParseException ignored) {
            // Se usa el mensaje genérico de abajo.
        }
        return new ErrorClienteException(respuesta.statusCode(), "ERROR_API",
                "El servicio local respondió con un error ("
                        + respuesta.statusCode() + ").");
    }
}