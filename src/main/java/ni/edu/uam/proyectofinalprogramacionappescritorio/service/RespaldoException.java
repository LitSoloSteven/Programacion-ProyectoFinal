package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import java.util.Objects;

/**
 * Error operativo o de integridad durante la generación o publicación de respaldos (M12 / RN-29).
 */
public final class RespaldoException extends RuntimeException {
    public static final String CODIGO_INTEGRIDAD = "ERR-055";

    private final String codigo;

    private RespaldoException(String codigo, String mensaje, Throwable causa) {
        super(Objects.requireNonNull(mensaje, "Falta el mensaje de respaldo."), causa);
        this.codigo = Objects.requireNonNull(codigo, "Falta el código de respaldo.");
    }

    public String getCodigo() {
        return codigo;
    }

    public static RespaldoException integridad(String detalle) {
        String base = CODIGO_INTEGRIDAD
                + ": El respaldo no superó la comprobación de integridad. Seleccione otro paquete.";
        String mensaje = (detalle == null || detalle.isBlank()) ? base : base + " " + detalle.strip();
        return new RespaldoException(CODIGO_INTEGRIDAD, mensaje, null);
    }

    public static RespaldoException operativo(String mensaje, Throwable causa) {
        String limpio = (mensaje == null || mensaje.isBlank())
                ? CODIGO_INTEGRIDAD + ": No se pudo completar el respaldo."
                : mensaje.strip();
        return new RespaldoException(CODIGO_INTEGRIDAD, limpio, causa);
    }
}
