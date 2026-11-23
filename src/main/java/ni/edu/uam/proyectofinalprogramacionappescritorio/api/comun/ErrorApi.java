package ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun;

/**
 * Cuerpo uniforme de error: {"error":{"codigo":"...","mensaje":"..."}}.
 */
public record ErrorApi(String codigo, String mensaje) {
    public record Cuerpo(ErrorApi error) {
        public Cuerpo(String codigo, String mensaje) {
            this(new ErrorApi(codigo, mensaje));
        }
    }
}