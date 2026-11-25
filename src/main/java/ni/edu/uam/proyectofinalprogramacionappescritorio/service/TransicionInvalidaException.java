package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

/** Error de negocio para transiciones de estado no permitidas en mantenimiento (ERR-066). */
public class TransicionInvalidaException extends IllegalArgumentException {
    public static final String CODIGO = "ERR-066";

    public TransicionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
