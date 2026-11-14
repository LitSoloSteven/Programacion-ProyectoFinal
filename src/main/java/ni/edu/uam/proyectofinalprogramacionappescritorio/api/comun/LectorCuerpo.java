package ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun;

import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Lectura acotada del cuerpo HTTP. Comprueba Content-Type y limita la entrada
 * a 256 KiB antes de parsear JSON.
 */
public final class LectorCuerpo {
    public static final int LIMITE_BYTES = 256 * 1024;

    private LectorCuerpo() { }

    public static String leerJson(HttpExchange intercambio) throws IOException {
        String tipo = intercambio.getRequestHeaders().getFirst("Content-Type");
        if (!esJson(tipo)) throw ExcepcionApi.contenidoNoSoportado();

        String longitud = intercambio.getRequestHeaders().getFirst("Content-Length");
        if (longitud != null && !longitud.isBlank()) {
            try {
                if (Long.parseLong(longitud.strip()) > LIMITE_BYTES) {
                    throw ExcepcionApi.cuerpoDemasiadoGrande();
                }
            } catch (NumberFormatException e) {
                throw ExcepcionApi.solicitudInvalida("El Content-Length no es válido.");
            }
        }

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        try (InputStream entrada = intercambio.getRequestBody()) {
            int leidos;
            while ((leidos = entrada.read(buffer)) != -1) {
                total += leidos;
                if (total > LIMITE_BYTES) throw ExcepcionApi.cuerpoDemasiadoGrande();
                salida.write(buffer, 0, leidos);
            }
        }

        if (total == 0) throw ExcepcionApi.solicitudInvalida("El cuerpo JSON es obligatorio.");
        return salida.toString(StandardCharsets.UTF_8);
    }

    private static boolean esJson(String contentType) {
        if (contentType == null || contentType.isBlank()) return false;
        int separador = contentType.indexOf(';');
        String base = (separador >= 0 ? contentType.substring(0, separador) : contentType).strip();
        return "application/json".equalsIgnoreCase(base);
    }
}