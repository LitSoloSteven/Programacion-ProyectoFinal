package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.dto.ErrorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.SaludResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Cliente HTTP síncrono sobre java.net.http.HttpClient. Gestiona token Bearer,
 * expiración 401 (excepto en login), cuerpos JSON y binarios acotados.
 */
public final class ClienteApi {
    private static final Duration CONEXION = Duration.ofSeconds(5);
    private static final Duration TOTAL = Duration.ofSeconds(10);

    private final String baseUrl;
    private final HttpClient http;
    private final Gson gson = new Gson();
    private volatile String token;
    private volatile Runnable alExpirar = () -> { };

    public ClienteApi(String baseUrl) {
        this.baseUrl = ClienteConfig.normalizar(baseUrl);
        this.http = HttpClient.newBuilder().connectTimeout(CONEXION).build();
    }

    public String baseUrl() {
        return baseUrl;
    }

    void alExpirar(Runnable accion) {
        alExpirar = Objects.requireNonNull(accion);
    }

    public synchronized boolean sesionActiva() {
        return token != null && !token.isBlank();
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

    private synchronized boolean descartarToken(String tokenUsado) {
        if (token == null || !token.equals(tokenUsado)) {
            return false;
        }
        token = null;
        return true;
    }

    public SaludResponse getSalud() {
        return get("/api/v1/salud", SaludResponse.class);
    }

    public <T> T get(String ruta, Class<T> tipo) {
        return get(ruta, (Type) tipo);
    }

    public <T> T get(String ruta, Type tipo) {
        return enviar(construir(ruta).GET().build(), tipo);
    }

    public byte[] getBytes(String ruta) {
        HttpRequest peticion = construirConToken(ruta, token, "*/*", TOTAL).GET().build();
        return enviarBinario(peticion);
    }

    public <T> T post(String ruta, Object cuerpo, Class<T> tipo) {
        return post(ruta, cuerpo, tipo, TOTAL);
    }

    public <T> T post(String ruta, Object cuerpo, Class<T> tipo, Duration timeout) {
        HttpRequest peticion = construirConToken(ruta, token, "application/json", timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(cuerpo), StandardCharsets.UTF_8))
                .build();
        return enviar(peticion, tipo);
    }

    public <T> T postBytes(String ruta, byte[] bytes, Map<String, String> cabeceras, Class<T> tipo) {
        Objects.requireNonNull(bytes, "Falta el contenido binario.");
        HttpRequest.Builder builder = construirConToken(ruta, token, "application/json", Duration.ofSeconds(30))
                .header("Content-Type", "application/octet-stream");
        if (cabeceras != null) {
            cabeceras.forEach(builder::header);
        }
        HttpRequest peticion = builder.POST(HttpRequest.BodyPublishers.ofByteArray(bytes)).build();
        return enviar(peticion, tipo);
    }

    public <T> T postConCabecera(String ruta, Object cuerpo, Class<T> tipo,
                                 String nombreCabecera, String valorCabecera) {
        if (nombreCabecera == null || nombreCabecera.isBlank()
                || valorCabecera == null || valorCabecera.isBlank()) {
            throw new IllegalArgumentException("La cabecera de la petición es obligatoria.");
        }
        HttpRequest peticion = construir(ruta)
                .header("Content-Type", "application/json")
                .header(nombreCabecera, valorCabecera)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(cuerpo), StandardCharsets.UTF_8))
                .build();
        return enviar(peticion, tipo);
    }

    public void postSinContenido(String ruta, Object cuerpo) {
        post(ruta, cuerpo, Void.class);
    }

    public <T> T put(String ruta, Object cuerpo, Class<T> tipo) {
        HttpRequest peticion = construir(ruta)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(cuerpo), StandardCharsets.UTF_8))
                .build();
        return enviar(peticion, tipo);
    }

    public <T> T patch(String ruta, Object cuerpo, Class<T> tipo) {
        HttpRequest peticion = construir(ruta)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(gson.toJson(cuerpo), StandardCharsets.UTF_8))
                .build();
        return enviar(peticion, tipo);
    }

    public void delete(String ruta) {
        HttpRequest peticion = construir(ruta).DELETE().build();
        enviar(peticion, Void.class);
    }

    /**
     * Envía una petición con un token explícito sin tocar el token guardado.
     * Se usa para revocar una sesión ya descartada localmente.
     */
    public void postSinContenidoConToken(String ruta, Object cuerpo, String tokenExplicito) {
        HttpRequest peticion = construirConToken(ruta, tokenExplicito, "application/json", TOTAL)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(cuerpo), StandardCharsets.UTF_8))
                .build();
        enviar(peticion, Void.class);
    }

    private HttpRequest.Builder construir(String ruta) {
        return construirConToken(ruta, token, "application/json", TOTAL);
    }

    private HttpRequest.Builder construirConToken(String ruta, String tokenExplicito, String accept, Duration timeout) {
        HttpRequest.Builder constructor = HttpRequest.newBuilder(URI.create(baseUrl + ruta))
                .timeout(timeout == null ? TOTAL : timeout)
                .header("Accept", accept);
        if (tokenExplicito != null && !tokenExplicito.isBlank()) {
            constructor.header("Authorization", "Bearer " + tokenExplicito);
        }
        return constructor;
    }

    private <T> T enviar(HttpRequest peticion, Type tipo) {
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
            manejarExpiracion(peticion, estado);
            throw errorTexto(estado, respuesta.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErrorClienteException(0, "INTERRUMPIDO", "La operación fue cancelada.");
        } catch (IOException e) {
            throw new ErrorClienteException(0, "SIN_CONEXION",
                    "No se pudo conectar con el servidor Latch en " + baseUrl + ".");
        }
    }

    private byte[] enviarBinario(HttpRequest peticion) {
        try {
            HttpResponse<byte[]> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofByteArray());
            int estado = respuesta.statusCode();
            if (estado >= 200 && estado < 300) {
                return respuesta.body();
            }
            manejarExpiracion(peticion, estado);
            String cuerpoTexto = respuesta.body() == null ? ""
                    : new String(respuesta.body(), StandardCharsets.UTF_8);
            throw errorTexto(estado, cuerpoTexto);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErrorClienteException(0, "INTERRUMPIDO", "La descarga fue cancelada.");
        } catch (IOException e) {
            throw new ErrorClienteException(0, "SIN_CONEXION",
                    "No se pudo conectar con el servidor Latch en " + baseUrl + ".");
        }
    }

    private void manejarExpiracion(HttpRequest peticion, int estado) {
        String ruta = peticion.uri().getPath();
        if (estado == 401 && (ruta == null || !ruta.endsWith("/api/v1/auth/login"))) {
            boolean descartado = peticion.headers().firstValue("Authorization")
                    .filter(valor -> valor.startsWith("Bearer "))
                    .map(valor -> valor.substring("Bearer ".length()))
                    .filter(this::descartarToken)
                    .isPresent();
            if (descartado) {
                SesionCliente.cerrarSesion();
                alExpirar.run();
            }
        }
    }

    private ErrorClienteException errorTexto(int estado, String cuerpoTexto) {
        try {
            ErrorApi.Cuerpo cuerpo = gson.fromJson(cuerpoTexto, ErrorApi.Cuerpo.class);
            if (cuerpo != null && cuerpo.error() != null) {
                return new ErrorClienteException(estado, cuerpo.error().codigo(), cuerpo.error().mensaje());
            }
        } catch (JsonParseException ignored) {
            // Se usa el mensaje genérico de abajo.
        }
        return new ErrorClienteException(estado, "ERROR_API",
                "El servidor respondió con un error (" + estado + ").");
    }
}
