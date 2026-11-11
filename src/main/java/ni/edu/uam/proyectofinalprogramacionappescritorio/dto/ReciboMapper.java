package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Recibo;
import java.util.Objects;

/** Obtiene una copia de datos para consultas/reportes sin exponer la entidad. */
public final class ReciboMapper {
    private ReciboMapper() { }
    public static ReciboDTO aDTO(Recibo recibo) {
        Objects.requireNonNull(recibo, "Falta el recibo.");
        return new ReciboDTO(recibo.getNumero(), recibo.getPagoId(), recibo.getContratoId(),
                recibo.getFechaPago(), recibo.getContratoInfo(), recibo.getMontoOriginal(),
                recibo.getMonedaOriginal(), recibo.getMontoContrato(), recibo.getMetodo(),
                recibo.getReferencia(), recibo.isAnulado());
    }
}
