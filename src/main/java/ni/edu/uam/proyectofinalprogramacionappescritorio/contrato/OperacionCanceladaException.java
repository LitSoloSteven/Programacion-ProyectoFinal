package ni.edu.uam.proyectofinalprogramacionappescritorio.contrato;

/**
 * Señala que una operación cooperativa de generación o exportación de reportes (H5)
 * fue cancelada de forma segura antes de publicar el archivo final (RF-M10-12 / MSG-089).
 */
public final class OperacionCanceladaException extends RuntimeException {
    public static final String MSG_089 =
            "Operación cancelada. No se publicó ningún archivo incompleto y el archivo anterior se conservó intacto.";

    public OperacionCanceladaException() {
        super(MSG_089);
    }

    public OperacionCanceladaException(String mensaje) {
        super(mensaje == null || mensaje.isBlank() ? MSG_089 : mensaje);
    }
}
