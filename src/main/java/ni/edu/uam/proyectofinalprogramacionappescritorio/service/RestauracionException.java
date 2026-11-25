package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import java.util.Objects;

/**
 * Error de autenticidad, integridad, conciliación u operación al restaurar un respaldo (M12 / RN-29).
 */
public final class RestauracionException extends RuntimeException {
    public static final String CODIGO_INTEGRIDAD = "ERR-055";
    public static final String CODIGO_CONCILIACION = "ERR-058";
    public static final String CODIGO_AUTENTICIDAD = "ERR-059";

    private final String codigo;

    private RestauracionException(String codigo, String mensaje, Throwable causa) {
        super(Objects.requireNonNull(mensaje, "Falta el mensaje de restauración."), causa);
        this.codigo = Objects.requireNonNull(codigo, "Falta el código de restauración.");
    }

    public String getCodigo() {
        return codigo;
    }

    public static RestauracionException autenticidadInvalida(Throwable causa) {
        return new RestauracionException(
                CODIGO_AUTENTICIDAD,
                CODIGO_AUTENTICIDAD
                        + ": No se pudo verificar la autenticidad del respaldo. Revise la contraseña o seleccione otro paquete.",
                causa);
    }

    public static RestauracionException integridadInvalida(String detalle) {
        String base = CODIGO_INTEGRIDAD
                + ": El respaldo no superó la comprobación de integridad. Seleccione otro paquete.";
        String mensaje = (detalle == null || detalle.isBlank()) ? base : base + " " + detalle.strip();
        return new RestauracionException(CODIGO_INTEGRIDAD, mensaje, null);
    }

    public static RestauracionException conciliacionRequerida(long ultimoEnRespaldo, long ultimoEnDestino) {
        return new RestauracionException(
                CODIGO_CONCILIACION,
                CODIGO_CONCILIACION
                        + ": La restauración requiere conciliar los recibos posteriores al respaldo antes de volver a operar"
                        + " (último en respaldo: " + ultimoEnRespaldo + ", último en destino: " + ultimoEnDestino + ").",
                null);
    }

    public static RestauracionException operativo(String codigo, String mensaje, Throwable causa) {
        String cod = (codigo == null || codigo.isBlank()) ? CODIGO_INTEGRIDAD : codigo.strip();
        String msg = (mensaje == null || mensaje.isBlank())
                ? cod + ": No se pudo completar la restauración."
                : mensaje.strip();
        return new RestauracionException(cod, msg, causa);
    }
}
