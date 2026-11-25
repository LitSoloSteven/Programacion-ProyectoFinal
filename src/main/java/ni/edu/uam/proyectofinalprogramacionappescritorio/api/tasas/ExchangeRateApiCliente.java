package ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class ExchangeRateApiCliente implements AutoCloseable {
    static final int LIMITE_BYTES = 256 * 1024;
    private static final URI URL = URI.create("https://v6.exchangerate-api.com/v6/latest/USD");
    private static final Duration PLAZO = Duration.ofSeconds(10);
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();

    private final HttpClient http;
    private final URI url;
    private final Clock reloj;

    ExchangeRateApiCliente() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build(), URL, Clock.systemUTC());
    }

    ExchangeRateApiCliente(HttpClient http, URI url, Clock reloj) {
        this.http = http;
        this.url = url;
        this.reloj = reloj;
    }

    Referencia consultar(String llave) throws Fallo, InterruptedException {
        if (llave == null || llave.isBlank()) {
            throw new Fallo(TipoFallo.CONFIGURACION);
        }
        final HttpRequest solicitud;
        try {
            solicitud = HttpRequest.newBuilder(url).timeout(PLAZO)
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + llave).GET().build();
        } catch (IllegalArgumentException invalida) {
            throw new Fallo(TipoFallo.CONFIGURACION);
        }
        var futuro = http.sendAsync(solicitud, ExchangeRateApiCliente::receptor);
        final HttpResponse<byte[]> respuesta;
        try {
            respuesta = futuro.get(PLAZO.toSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException interrumpida) {
            futuro.cancel(true);
            throw interrumpida;
        } catch (TimeoutException vencido) {
            futuro.cancel(true);
            throw new Fallo(TipoFallo.TRANSITORIO);
        } catch (ExecutionException fallo) {
            if (fallo.getCause() instanceof Fallo respuestaInvalida) {
                throw respuestaInvalida;
            }
            throw new Fallo(TipoFallo.TRANSITORIO);
        }
        int estado = respuesta.statusCode();
        if (estado == 429) {
            throw new Fallo(TipoFallo.CUOTA_TEMPORAL,
                    respuesta.headers().firstValue("Retry-After").orElse(null));
        }
        if (estado == 401 || estado == 403) {
            throw new Fallo(TipoFallo.CONFIGURACION);
        }
        if (estado == 408 || estado >= 500) {
            throw new Fallo(TipoFallo.TRANSITORIO);
        }
        if (estado != 200) {
            throw new Fallo(TipoFallo.RESPUESTA_INVALIDA);
        }
        return interpretar(respuesta.body(), reloj.instant());
    }

    private static HttpResponse.BodySubscriber<byte[]> receptor(HttpResponse.ResponseInfo respuesta) {
        if (respuesta.statusCode() != 200) {
            return HttpResponse.BodySubscribers.replacing(new byte[0]);
        }
        String tipo = respuesta.headers().firstValue("Content-Type").orElse("").split(";", 2)[0].strip();
        long longitud = respuesta.headers().firstValueAsLong("Content-Length").orElse(-1);
        return new CuerpoLimitado(!tipo.equalsIgnoreCase("application/json") || longitud > LIMITE_BYTES);
    }

    private static Referencia interpretar(byte[] cuerpo, Instant consultada) throws Fallo {
        try {
            String contenido = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(cuerpo)).toString();
            JsonObject json = JSON.fromJson(contenido, JsonObject.class);
            if (json == null) {
                throw new Fallo(TipoFallo.RESPUESTA_INVALIDA);
            }
            if (textoEs(json, "result", "error")) {
                if (textoEs(json, "error-type", "quota-reached")) {
                    throw new Fallo(TipoFallo.CUOTA_AGOTADA);
                }
                if (textoEs(json, "error-type", "invalid-key")
                        || textoEs(json, "error-type", "inactive-account")) {
                    throw new Fallo(TipoFallo.CONFIGURACION);
                }
            }
            if (!textoEs(json, "result", "success") || !textoEs(json, "base_code", "USD")) {
                throw new Fallo(TipoFallo.RESPUESTA_INVALIDA);
            }
            Instant actualizada = Instant.ofEpochSecond(numero(json, "time_last_update_unix").longValueExact());
            Instant proxima = Instant.ofEpochSecond(numero(json, "time_next_update_unix").longValueExact());
            if (!actualizada.isAfter(Instant.EPOCH) || actualizada.isAfter(consultada.plusSeconds(300))
                    || !proxima.isAfter(actualizada)
                    || !coincideMarcaUtc(json, "time_last_update_utc", actualizada)
                    || !coincideMarcaUtc(json, "time_next_update_utc", proxima)) {
                throw new Fallo(TipoFallo.RESPUESTA_INVALIDA);
            }
            JsonObject conversiones = json.getAsJsonObject("conversion_rates");
            BigDecimal tasa = numero(conversiones, "NIO");
            if (conversiones.has("USD") && numero(conversiones, "USD").compareTo(BigDecimal.ONE) != 0) {
                throw new Fallo(TipoFallo.RESPUESTA_INVALIDA);
            }
            if (tasa.compareTo(BigDecimal.ONE) < 0 || tasa.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new Fallo(TipoFallo.FUERA_DE_BANDA, null, tasa, huella(cuerpo));
            }
            return new Referencia(tasa.setScale(8, RoundingMode.HALF_EVEN), consultada, actualizada,
                    proxima, huella(cuerpo));
        } catch (JsonParseException | IllegalStateException | ClassCastException
                 | ArithmeticException | NumberFormatException | DateTimeException
                 | CharacterCodingException invalida) {
            throw new Fallo(TipoFallo.RESPUESTA_INVALIDA);
        }
    }

    private static boolean coincideMarcaUtc(JsonObject json, String campo, Instant esperada) {
        var valor = json.get(campo);
        return valor == null || (valor.isJsonPrimitive() && valor.getAsJsonPrimitive().isString()
                && ZonedDateTime.parse(valor.getAsString(), DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toInstant().equals(esperada));
    }

    private static String huella(byte[] cuerpo) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(cuerpo));
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException(imposible);
        }
    }

    private static boolean textoEs(JsonObject json, String campo, String esperado) {
        var valor = json.get(campo);
        return valor != null && valor.isJsonPrimitive() && valor.getAsJsonPrimitive().isString()
                && esperado.equals(valor.getAsString());
    }

    private static BigDecimal numero(JsonObject json, String campo) throws Fallo {
        var valor = json == null ? null : json.get(campo);
        if (valor == null || !valor.isJsonPrimitive() || !valor.getAsJsonPrimitive().isNumber()) {
            throw new Fallo(TipoFallo.RESPUESTA_INVALIDA);
        }
        return valor.getAsBigDecimal();
    }

    record Referencia(BigDecimal tasa, Instant consultada, Instant actualizada, Instant proxima, String huella) {}

    @Override
    public void close() {
        http.shutdownNow();
    }

    enum TipoFallo {
        CONFIGURACION, TRANSITORIO, CUOTA_TEMPORAL, CUOTA_AGOTADA, RESPUESTA_INVALIDA, FUERA_DE_BANDA
    }

    static final class Fallo extends Exception {
        private final TipoFallo tipo;
        private final String retryAfter;
        private final BigDecimal tasaRechazada;
        private final String huellaRespuesta;

        Fallo(TipoFallo tipo) {
            this(tipo, null);
        }

        Fallo(TipoFallo tipo, String retryAfter) {
            this(tipo, retryAfter, null, null);
        }

        Fallo(TipoFallo tipo, String retryAfter, BigDecimal tasaRechazada, String huellaRespuesta) {
            super(tipo.name());
            this.tipo = tipo;
            this.retryAfter = retryAfter;
            this.tasaRechazada = tasaRechazada;
            this.huellaRespuesta = huellaRespuesta;
        }

        TipoFallo tipo() {
            return tipo;
        }

        String retryAfter() {
            return retryAfter;
        }

        String detalleCuarentena() {
            String valor = tasaRechazada.toString();
            return "origen:H3;valor:" + valor.substring(0, Math.min(valor.length(), 64))
                    + ";huella:" + huellaRespuesta;
        }
    }

    private static final class CuerpoLimitado implements HttpResponse.BodySubscriber<byte[]> {
        private final HttpResponse.BodySubscriber<byte[]> receptor = HttpResponse.BodySubscribers.ofByteArray();
        private final boolean rechazarCabeceras;
        private Flow.Subscription suscripcion;
        private long bytes;

        CuerpoLimitado(boolean rechazarCabeceras) {
            this.rechazarCabeceras = rechazarCabeceras;
        }

        @Override
        public CompletionStage<byte[]> getBody() {
            return receptor.getBody();
        }

        @Override
        public void onSubscribe(Flow.Subscription suscripcion) {
            this.suscripcion = suscripcion;
            if (rechazarCabeceras) {
                rechazar();
                return;
            }
            receptor.onSubscribe(suscripcion);
        }

        @Override
        public void onNext(List<ByteBuffer> partes) {
            for (ByteBuffer parte : partes) {
                bytes += parte.remaining();
                if (bytes > LIMITE_BYTES) {
                    rechazar();
                    return;
                }
            }
            receptor.onNext(partes);
        }

        private void rechazar() {
            receptor.onError(new Fallo(TipoFallo.RESPUESTA_INVALIDA));
            suscripcion.cancel();
        }

        @Override
        public void onError(Throwable error) {
            receptor.onError(error);
        }

        @Override
        public void onComplete() {
            receptor.onComplete();
        }
    }
}
