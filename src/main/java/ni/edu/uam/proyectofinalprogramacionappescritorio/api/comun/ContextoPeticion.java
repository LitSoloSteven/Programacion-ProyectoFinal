package ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun;

import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

/**
 * Datos de una petición ya autenticada. Los parámetros de ruta y de consulta
 * llegan decodificados; el cuerpo se lee de forma explícita en cada manejador.
 */
public record ContextoPeticion(
        HttpExchange intercambio,
        Map<String, String> parametros,
        Map<String, String> consulta,
        SesionApi sesion,
        String token) {

    public ContextoPeticion {
        parametros = Map.copyOf(parametros);
        consulta = Map.copyOf(consulta);
    }

    public String parametro(String nombre) {
        return parametros.get(nombre);
    }

    public String consulta(String nombre) {
        return consulta.get(nombre);
    }
}