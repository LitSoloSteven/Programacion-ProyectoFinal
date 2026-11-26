package ni.edu.uam.proyectofinalprogramacionappescritorio.contrato;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Set;

/** Constantes de dominio compartidas entre servidor y cliente HTTP sin dependencias de persistencia. */
public final class ConstantesLatch {
    public static final ZoneId ZONA_OPERATIVA = ZoneId.of("America/Managua");

    public static final String ROL_PROPIETARIO = "PROPIETARIO";
    public static final String ROL_ASISTENTE = "ASISTENTE";

    public static final String MONEDA_USD = "USD";
    public static final String MONEDA_NIO = "NIO";

    public static final BigDecimal BANDA_TASA_MINIMA = new BigDecimal("1.00");
    public static final BigDecimal BANDA_TASA_MAXIMA = new BigDecimal("100.00");

    public static final String EXTENSION_RESPALDO = ".lbackup";
    public static final Set<String> MIMES_IMAGEN = Set.of("image/png", "image/jpeg");

    public static final String MANT_ESTADO_ABIERTA = "Abierta";
    public static final String MANT_ESTADO_EN_PROCESO = "En proceso";
    public static final String MANT_ESTADO_RESUELTA = "Resuelta";
    public static final String MANT_ESTADO_CANCELADA = "Cancelada";

    public static final String MANT_PRIORIDAD_BAJA = "Baja";
    public static final String MANT_PRIORIDAD_MEDIA = "Media";
    public static final String MANT_PRIORIDAD_ALTA = "Alta";

    public static final String GARANTIA_TIPO_APLICACION = "Aplicacion";
    public static final String GARANTIA_TIPO_DEDUCCION = "Deduccion";
    public static final String GARANTIA_TIPO_DEVOLUCION = "Devolucion";

    public static final float ZOOM_MIN = 0.50f;
    public static final float ZOOM_MAX = 2.00f;
    public static final float ZOOM_PASO = 0.25f;
    public static final float ZOOM_INICIAL = 1.00f;

    public static float acotarZoom(float escala) {
        if (Float.isNaN(escala) || Float.isInfinite(escala)) {
            return ZOOM_INICIAL;
        }
        return Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, escala));
    }

    private ConstantesLatch() { }
}
