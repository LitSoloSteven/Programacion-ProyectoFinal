package ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.DatabaseConfig;

import java.nio.file.Path;
import java.util.Map;

/**
 * Configuración del servidor HTTP local. El host no se configura: la API solo
 * puede escuchar en 127.0.0.1 para no exponer datos financieros en la red.
 */
public record ApiConfig(String host, int puerto) {
    public static final String HOST_LOCAL = "127.0.0.1";

    public ApiConfig {
        if (!HOST_LOCAL.equals(host)) {
            throw new IllegalArgumentException("La API solo puede escuchar en 127.0.0.1.");
        }
        if (puerto < 0 || puerto > 65535) {
            throw new IllegalArgumentException("El puerto de la API debe estar entre 0 y 65535.");
        }
    }

    public static ApiConfig desdeArchivo(Path archivo, Map<String, String> entorno) {
        Map<String, String> valores = DatabaseConfig.leerVariables(archivo, entorno);
        String puerto = valores.get("LATCH_API_PORT");
        if (puerto == null || puerto.isBlank()) {
            throw new IllegalArgumentException("Falta configurar LATCH_API_PORT.");
        }
        try {
            int numero = Integer.parseInt(puerto.strip());
            if (numero < 1 || numero > 65535) {
                throw new IllegalArgumentException("LATCH_API_PORT debe estar entre 1 y 65535.");
            }
            return new ApiConfig(HOST_LOCAL, numero);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("LATCH_API_PORT debe ser un número entero.");
        }
    }

    /** Puerto 0 permite que el sistema operativo asigne uno efímero en pruebas. */
    public static ApiConfig paraPruebas(int puerto) {
        return new ApiConfig(HOST_LOCAL, puerto);
    }
}