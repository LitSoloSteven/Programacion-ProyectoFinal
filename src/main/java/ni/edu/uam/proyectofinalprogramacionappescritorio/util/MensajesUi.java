package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;

import java.util.Locale;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/**
 * Sanitiza mensajes internos y traduce excepciones HTTP o locales en avisos claros
 * orientados al usuario y a la recuperación contextual.
 */
public final class MensajesUi {
    private static final Pattern CODIGOS_INTERNOS = Pattern.compile(
            "(?i)\\b(?:ERR-\\d+|MSG-\\d+|RN-\\d+|RF-[A-Z0-9-]+|RNF-\\d+|REP-\\d+|PAN-\\d+)\\b\\s*[:·-]?\\s*");
    private static final Pattern ETIQUETAS_H_ENTRE_PARENTESIS = Pattern.compile(
            "(?i)\\s*\\((?:H[1-6]|M05|REP-\\d+|PAN-\\d+)(?:\\s*,\\s*(?:H[1-6]|M05|REP-\\d+|PAN-\\d+))*\\)");
    private static final Pattern PREFIJO_H = Pattern.compile("(?i)\\bH[1-6]\\b\\s*[:/·-]\\s*");
    private static final Pattern SUELTO_H_O_M05 = Pattern.compile("(?i)(?:^|\\s)(?:H[1-6]|M05)(?=\\s|$|[.,;:])");
    private static final Pattern ESPACIOS = Pattern.compile("\\s{2,}");
    private static final Pattern PUNTUACION_COLGANTE = Pattern.compile("\\s+([.,;:])");
    private static final Pattern TRAZA_TECNICA = Pattern.compile(
            "(?i)\\b(SQLSTATE|StackTrace|org\\.hibernate\\.|java\\.sql\\.|java\\.lang\\.|SELECT\\s+|INSERT\\s+INTO|UPDATE\\s+\\S+\\s+SET|DELETE\\s+FROM)\\b|Exception:");

    private MensajesUi() { }

    /**
     * Limpia códigos internos (`ERR-xxx`, `H1`–`H6`, `M05`, `REP-xx`, `RN-xx`) y descarta
     * mensajes con trazas técnicas o SQL expuesto.
     */
    public static String limpiarParaUsuario(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String crudo = texto.strip();
        if (TRAZA_TECNICA.matcher(crudo).find()) {
            return "";
        }
        String limpio = ETIQUETAS_H_ENTRE_PARENTESIS.matcher(crudo).replaceAll("");
        limpio = CODIGOS_INTERNOS.matcher(limpio).replaceAll("");
        limpio = PREFIJO_H.matcher(limpio).replaceAll("");
        limpio = SUELTO_H_O_M05.matcher(limpio).replaceAll(" ");
        limpio = PUNTUACION_COLGANTE.matcher(limpio).replaceAll("$1");
        limpio = ESPACIOS.matcher(limpio).replaceAll(" ").strip();
        if (limpio.startsWith(":") || limpio.startsWith("·") || limpio.startsWith("-")) {
            limpio = limpio.substring(1).strip();
        }
        return limpio;
    }

    /**
     * Traduce errores de red, HTTP o validación local en mensajes accionables sin
     * exponer códigos técnicos ni detalles internos.
     */
    public static String traducir(Throwable error, String respaldo) {
        String baseRespaldo = (respaldo == null || respaldo.isBlank())
                ? "No se pudo completar la operación. Inténtalo de nuevo."
                : respaldo.strip();
        Throwable actual = desenvolver(error);
        if (actual == null) {
            return baseRespaldo;
        }
        if (actual instanceof ErrorClienteException api) {
            if ("INTERRUMPIDO".equals(api.getCodigo())) {
                return "La operación fue cancelada antes de completarse.";
            }
            if (api.esSinConexion()) {
                return "No se pudo conectar con el servidor de Latch. Verifica que el servicio esté activo y vuelve a intentarlo.";
            }
            if (api.esNoAutenticado()) {
                return "Tu sesión expiró. Vuelve a iniciar sesión para continuar.";
            }
            if (api.getEstado() == 403) {
                String limpio = limpiarParaUsuario(api.getMessage());
                return limpio.isBlank() ? "No tienes permiso para realizar esta acción." : limpio;
            }
            if (api.getEstado() == 503 && "ERR-057".equalsIgnoreCase(api.getCodigo())) {
                return "La fecha y hora de este equipo no coinciden con las del servidor. Ajusta el reloj del sistema y vuelve a intentarlo.";
            }
            if (api.getEstado() == 409) {
                String limpio = limpiarParaUsuario(api.getMessage());
                if (limpio.isBlank()) {
                    return "El registro cambió recientemente. Recarga los datos y vuelve a intentarlo.";
                }
                String min = limpio.toLowerCase(Locale.ROOT);
                if (min.contains("recarg") || min.contains("reintent") || min.contains("vuelve")
                        || min.contains("actualiza") || min.contains("cambia") || min.contains("clave")) {
                    return limpio;
                }
                return limpio.endsWith(".")
                        ? limpio + " Recarga los datos y vuelve a intentarlo."
                        : limpio + ". Recarga los datos y vuelve a intentarlo.";
            }
            if (api.getEstado() >= 500) {
                return "El servidor encontró un problema temporal. Tus datos siguen en pantalla; vuelve a intentarlo en unos segundos.";
            }
            String limpio = limpiarParaUsuario(api.getMessage());
            return limpio.isBlank() ? baseRespaldo : limpio;
        }
        if (actual instanceof IllegalArgumentException
                || actual instanceof SecurityException
                || actual instanceof IllegalStateException) {
            String limpio = limpiarParaUsuario(actual.getMessage());
            return limpio.isBlank() ? baseRespaldo : limpio;
        }
        return baseRespaldo;
    }

    private static Throwable desenvolver(Throwable error) {
        Throwable actual = error;
        while (actual != null && (actual instanceof ExecutionException || actual instanceof CompletionException)
                && actual.getCause() != null) {
            actual = actual.getCause();
        }
        return actual;
    }
}
