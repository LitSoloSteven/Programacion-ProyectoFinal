package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

/**
 * Conflicto de negocio que no es un error de formato: por ejemplo, intentar
 * autorizar dos tasas para la misma fecha. La capa HTTP lo traduce a 409.
 */
public final class ConflictoDatosException extends IllegalStateException {
    public ConflictoDatosException(String mensaje) {
        super(mensaje);
    }
}