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
                segmentosPatron(patron), publica, manejador);
        for (Ruta existente : rutas) {
            if (existente.metodo().equals(nueva.metodo())
                    && existente.segmentos().equals(nueva.segmentos())) {
                throw new IllegalArgumentException("Ruta duplicada: " + metodo + " " + patron);
            }
        }
        rutas.add(nueva);
    }

    public RutaNormalizada normalizar(String rutaCruda) {
        try {
            return new RutaNormalizada(segmentosRuta(rutaCruda));
        } catch (IllegalArgumentException e) {
            throw ExcepcionApi.solicitudInvalida("La ruta solicitada no es válida.");
        }
    }

    public Resultado resolver(String metodo, String rutaCruda) {
        return resolver(metodo, normalizar(rutaCruda));
    }

    public Resultado resolver(String metodo, RutaNormalizada ruta) {
        List<String> partes = ruta.segmentos();

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

    public boolean esPublica(String metodo, String rutaCruda) {
        try {
            return esPublica(metodo, normalizar(rutaCruda));
        } catch (ExcepcionApi e) {
            return false;
        }
    }

    public boolean esPublica(String metodo, RutaNormalizada ruta) {
        List<String> partes = ruta.segmentos();
        for (Ruta candidata : rutas) {
            if (candidata.publica() && candidata.metodo().equalsIgnoreCase(metodo)
                    && candidata.coincidir(partes) != null) {
                return true;
            }
        }
        return false;
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

    private static List<String> segmentosPatron(String ruta) {
        String limpia = ruta == null ? "" : ruta.strip();
        if (limpia.startsWith("/")) limpia = limpia.substring(1);
        if (limpia.endsWith("/") && !limpia.isEmpty()) {
            limpia = limpia.substring(0, limpia.length() - 1);
        }
        if (limpia.isEmpty()) return List.of();
        String[] partes = limpia.split("/", -1);
        return List.of(partes);
    }

    private static List<String> segmentosRuta(String ruta) {
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
            String decodificada = URLDecoder.decode(
                    parte.replace("+", "%2B"), StandardCharsets.UTF_8);
            if (contieneEscapePendiente(decodificada)) {
                throw new IllegalArgumentException("La ruta contiene una codificación anidada.");
            }
            resultado.add(decodificada);
        }
        return List.copyOf(resultado);
    }

    private static boolean contieneEscapePendiente(String valor) {
        for (int i = 0; i + 2 < valor.length(); i++) {
            if (valor.charAt(i) == '%' && esHexadecimal(valor.charAt(i + 1))
                    && esHexadecimal(valor.charAt(i + 2))) {
                return true;
            }
        }
        return false;
    }

    private static boolean esHexadecimal(char valor) {
        return valor >= '0' && valor <= '9'
                || valor >= 'a' && valor <= 'f'
                || valor >= 'A' && valor <= 'F';
    }

    public static final class RutaNormalizada {
        private final List<String> segmentos;

        private RutaNormalizada(List<String> segmentos) {
            this.segmentos = List.copyOf(segmentos);
        }

        public List<String> segmentos() {
            return segmentos;
        }
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
