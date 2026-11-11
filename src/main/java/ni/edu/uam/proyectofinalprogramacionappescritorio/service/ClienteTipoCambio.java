package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import java.io.*;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** Referencia comercial informativa; nunca autoriza una tasa para pagos. */
public final class ClienteTipoCambio {
    public static final URI ENDPOINT = URI.create("https://open.er-api.com/v6/latest/USD");
    public record Respuesta(int estado, String cuerpo, String reintentarTras) { }
    @FunctionalInterface public interface Transporte {
        Respuesta obtener() throws IOException, InterruptedException;
    }
    private final Transporte transporte;
    private final Clock reloj;
    private TipoCambio cache;
    private Instant venceCache = Instant.MIN;
    private Instant bloqueadoHasta = Instant.MIN;

    public ClienteTipoCambio() {
        this(transporteHttp(), Clock.systemUTC());
    }
    public ClienteTipoCambio(Transporte transporte, Clock reloj) {
        this.transporte = Objects.requireNonNull(transporte);
        this.reloj = Objects.requireNonNull(reloj);
    }
    private static Transporte transporteHttp() {
        HttpClient cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        return () -> {
            HttpRequest solicitud = HttpRequest.newBuilder(ENDPOINT)
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json").GET().build();
            HttpResponse<String> respuesta = cliente.send(solicitud,
                    HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            return new Respuesta(respuesta.statusCode(), respuesta.body(),
                    respuesta.headers().firstValue("Retry-After").orElse(""));
        };
    }
    /** Serializa llamadas para no iniciar dos actualizaciones simultaneas. */
    public synchronized TipoCambio consultar() throws IOException, InterruptedException {
        Instant ahora = reloj.instant();
        if (cache != null && ahora.isBefore(venceCache)) return cache;
        if (ahora.isBefore(bloqueadoHasta)) {
            throw new IOException("Consulta limitada hasta " + bloqueadoHasta);
        }
        for (int intento = 0; intento < 2; intento++) {
            Respuesta respuesta;
            try { respuesta = transporte.obtener(); }
            catch (HttpTimeoutException timeout) {
                if (intento == 0) continue;
                throw timeout;
            } catch (InterruptedException cancelacion) {
                Thread.currentThread().interrupt();
                throw cancelacion;
            }
            if (respuesta.estado() == 429) {
                bloqueadoHasta = calcularEspera(respuesta.reintentarTras());
                throw new IOException("Proveedor limitado; reintentar despues de " + bloqueadoHasta);
            }
            if (respuesta.estado() >= 500 && respuesta.estado() <= 599 && intento == 0) continue;
            if (respuesta.estado() != 200) {
                throw new IOException("Error del proveedor: HTTP " + respuesta.estado());
            }
            TipoCambio nueva = validar(respuesta.cuerpo());
            cache = nueva;
            venceCache = reloj.instant().plus(Duration.ofHours(24));
            return nueva;
        }
        throw new IOException("No se pudo consultar la referencia.");
    }
    private Instant calcularEspera(String cabecera) {
        Instant minimo = reloj.instant().plus(Duration.ofMinutes(20));
        if (cabecera == null || cabecera.isBlank()) return minimo;
        try {
            Instant indicado;
            try { indicado = reloj.instant().plusSeconds(Long.parseLong(cabecera.trim())); }
            catch (NumberFormatException e) {
                indicado = ZonedDateTime.parse(cabecera, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
            }
            return indicado.isAfter(minimo) ? indicado : minimo;
        } catch (RuntimeException invalida) { return minimo; }
    }
    /** Analisis estricto: rechaza JSON mal formado, tasas no numericas y base distinta. */
    private TipoCambio validar(String cuerpo) throws IOException {
        if (cuerpo == null || cuerpo.length() > 1_000_000) {
            throw new IOException("Respuesta ausente o demasiado grande.");
        }
        String resultado = null, base = null;
        BigDecimal nio = null;
        boolean vioResultado = false, vioBase = false, vioRates = false;
        try (JsonReader json = new JsonReader(new StringReader(cuerpo))) {
            json.setLenient(false);
            json.beginObject();
            while (json.hasNext()) {
                String campo = json.nextName();
                switch (campo) {
                    case "result" -> {
                        if (vioResultado || json.peek() != JsonToken.STRING) throw new IOException("Resultado invalido.");
                        vioResultado = true; resultado = json.nextString();
                    }
                    case "base_code" -> {
                        if (vioBase || json.peek() != JsonToken.STRING) throw new IOException("Base invalida.");
                        vioBase = true; base = json.nextString();
                    }
                    case "rates" -> {
                        if (vioRates) throw new IOException("Tasas duplicadas.");
                        vioRates = true;
                        json.beginObject();
                        while (json.hasNext()) {
                            if ("NIO".equals(json.nextName())) {
                                if (nio != null || json.peek() != JsonToken.NUMBER) throw new IOException("Tasa NIO invalida.");
                                String numero = json.nextString();
                                if (numero.length() > 40) throw new IOException("Tasa fuera de rango.");
                                nio = new BigDecimal(numero);
                                if (nio.scale() < -100 || nio.scale() > 100) {
                                    throw new IOException("Precision de tasa fuera de rango.");
                                }
                            } else json.skipValue();
                        }
                        json.endObject();
                    }
                    default -> json.skipValue();
                }
            }
            json.endObject();
            if (json.peek() != JsonToken.END_DOCUMENT || !"success".equals(resultado)
                    || !"USD".equals(base) || nio == null || nio.signum() <= 0
                    || nio.compareTo(new BigDecimal("100000000000")) >= 0) {
                throw new IOException("Respuesta sin resultado exitoso, base USD o tasa NIO valida.");
            }
            return TipoCambio.informativa(LocalDate.now(reloj.withZone(ZoneId.of("America/Managua"))),
                    nio, "ExchangeRate-API Open Access: " + ENDPOINT,
                    reloj.instant());
        } catch (IllegalArgumentException | IllegalStateException | ArithmeticException e) {
            throw new IOException("Respuesta JSON invalida.", e);
        }
    }
    public String getAtribucion() { return "Rates By ExchangeRate-API (https://www.exchangerate-api.com)"; }
}

