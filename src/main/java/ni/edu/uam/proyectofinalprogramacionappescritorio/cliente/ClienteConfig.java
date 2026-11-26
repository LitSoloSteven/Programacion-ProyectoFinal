package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

import java.net.URI;

/**
 * Resuelve la URL base de la API HTTP para el cliente JavaFX sin acoplar puertos
 * ni rutas de servidor embebido.
 */
public final class ClienteConfig {
    public static final String PROPIEDAD_BASE_URL = "latch.api.baseUrl";
    public static final String ENV_BASE_URL = "LATCH_API_BASE_URL";
    public static final String URL_DEFECTO = "http://127.0.0.1:8080";

    private ClienteConfig() { }

    public static String resolverUrlBase() {
        String prop = System.getProperty(PROPIEDAD_BASE_URL);
        if (prop != null && !prop.isBlank()) {
            return normalizar(prop);
        }
        String env = System.getenv(ENV_BASE_URL);
        if (env != null && !env.isBlank()) {
            return normalizar(env);
        }
        return URL_DEFECTO;
    }

    public static String normalizar(String cruda) {
        if (cruda == null || cruda.isBlank()) {
            throw new IllegalArgumentException("La URL base de la API no puede estar vacía.");
        }
        String limpia = cruda.strip();
        while (limpia.endsWith("/")) {
            limpia = limpia.substring(0, limpia.length() - 1);
        }
        URI uri;
        try {
            uri = URI.create(limpia);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La URL de la API no es válida: " + cruda, e);
        }
        String esquema = uri.getScheme();
        if (esquema == null || (!"http".equalsIgnoreCase(esquema) && !"https".equalsIgnoreCase(esquema))) {
            throw new IllegalArgumentException("La URL de la API debe usar esquema http o https.");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("La URL de la API debe incluir un host válido.");
        }
        return limpia;
    }
}
