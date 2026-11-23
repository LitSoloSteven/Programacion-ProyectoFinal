package ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun;

/**
 * Respuesta de una ruta. El servidor se encarga de serializar el cuerpo y de
 * escribir las cabeceras comunes.
 */
public record RespuestaJson(int estado, Object cuerpo) {
    public static RespuestaJson ok(Object cuerpo) {
        return new RespuestaJson(200, cuerpo);
    }

    public static RespuestaJson creado(Object cuerpo) {
        return new RespuestaJson(201, cuerpo);
    }

    public static RespuestaJson sinContenido() {
        return new RespuestaJson(204, null);
    }
}