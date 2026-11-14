package ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Router mínimo sobre com.sun.net.httpserver. Soporta rutas exactas y
 * segmentos variables con la forma /recurso/{id}.
 */
public final class Router {
    private final List<Ruta> rutas = new ArrayList<>();

    public void registrar(String metodo, String patron, boolean publica, Manejador manejador) {
        Objects.requireNonNull(metodo, "Falta el método HTTP.");
        Objects.requireNonNull(patron, "Falta el patrón de la ruta.");
        Objects.requireNonNull(manejador, "Falta el manejador de la ruta.");
        Ruta nueva = new Ruta(metodo.strip().toUpperCase(Locale.ROOT),
                segmentos(patron), publica, manejador);
        for (Ruta existente : rutas) {
            if (existente.metodo().equals(nueva.metodo())
                    && existente.segmentos().equals(nueva.segmentos())) {
                throw new IllegalArgumentException("Ruta duplicada: " + metodo + " " + patron);
            }
        }
        rutas.add(nueva);
    }

    public Resultado resolver(String metodo, String ruta) {
        List<String> partes;
        try {
            partes = segmentos(ruta);
        } catch (IllegalArgumentException e) {
            throw ExcepcionApi.solicitudInvalida("La ruta solicitada no es válida.");
        }

        boolean rutaExistente = false;
        // Primero las rutas literales; después las que contienen variables.
        for (boolean exactas : new boolean[]{true, false}) {
            for (Ruta candidata : rutas) {
                if (candidata.esExacta() != exactas) continue;
                Map<String, String> parametros = candidata.coincidir(partes);
                if (parametros == null) continue;
                rutaExistente = true;
                if (candidata.metodo().equalsIgnoreCase(metodo)) {
                    return new Resultado(candidata, parametros);
                }
            }
        }
        if (rutaExistente) {
            throw new ExcepcionApi(405, "METODO_NO_PERMITIDO", "Método HTTP no permitido.");
        }
        throw ExcepcionApi.noEncontrado();
    }

    public static Map<String, String> analizarConsulta(String consultaCruda) {
        if (consultaCruda == null || consultaCruda.isBlank()) return Map.of();
        Map<String, String> valores = new LinkedHashMap<>();
        for (String par : consultaCruda.split("&")) {
            if (par.isEmpty()) continue;
            int separador = par.indexOf('=');
            String clave = separador < 0 ? par : par.substring(0, separador);
            String valor = separador < 0 ? "" : par.substring(separador + 1);
            try {
                valores.put(URLDecoder.decode(clave, StandardCharsets.UTF_8),
                        URLDecoder.decode(valor, StandardCharsets.UTF_8));
            } catch (IllegalArgumentException e) {
                throw ExcepcionApi.solicitudInvalida("La cadena de consulta no es válida.");
            }
        }
        return valores;
    }

    private static List<String> segmentos(String ruta) {
        String limpia = ruta == null ? "" : ruta.strip();
        if (limpia.startsWith("/")) limpia = limpia.substring(1);
        if (limpia.endsWith("/") && !limpia.isEmpty()) {
            limpia = limpia.substring(0, limpia.length() - 1);
        }
        if (limpia.isEmpty()) return List.of();
        String[] partes = limpia.split("/", -1);
        List<String> resultado = new ArrayList<>(partes.length);
        for (String parte : partes) {
            // En una ruta, '+' es un carácter literal; en la consulta sí representa espacio.
            resultado.add(URLDecoder.decode(parte.replace("+", "%2B"), StandardCharsets.UTF_8));
        }
        return List.copyOf(resultado);
    }

    public record Resultado(Ruta ruta, Map<String, String> parametros) { }

    public record Ruta(String metodo, List<String> segmentos, boolean publica, Manejador manejador) {
        public boolean esExacta() {
            return segmentos.stream().noneMatch(segmento ->
                    segmento.startsWith("{") && segmento.endsWith("}"));
        }

        Map<String, String> coincidir(List<String> partes) {
            if (partes.size() != segmentos.size()) return null;
            Map<String, String> parametros = new LinkedHashMap<>();
            for (int i = 0; i < segmentos.size(); i++) {
                String patron = segmentos.get(i);
                if (patron.startsWith("{") && patron.endsWith("}")) {
                    String nombre = patron.substring(1, patron.length() - 1);
                    if (nombre.isBlank()) return null;
                    parametros.put(nombre, partes.get(i));
                } else if (!patron.equals(partes.get(i))) {
                    return null;
                }
            }
            return parametros;
        }
    }
}