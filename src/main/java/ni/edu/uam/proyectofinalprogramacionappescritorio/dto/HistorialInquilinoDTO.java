package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Historial consolidado de contratos, cargos y pagos de un inquilino. */
public record HistorialInquilinoDTO(
        UUID inquilinoId,
        String nombre,
        String identificacion,
        boolean activo,
        List<ContratoResumen> contratos,
        List<CargoResumen> cargos,
        List<PagoResumen> pagos) {

    public record ContratoResumen(
            UUID id,
            String unidad,
            LocalDate inicio,
            LocalDate fin,
            BigDecimal canon,
            String moneda,
            String estado) {
    }

    public record CargoResumen(
            UUID id,
            UUID contratoId,
            String tipo,
            LocalDate periodo,
            LocalDate venceEl,
            BigDecimal importe,
            String moneda,
            String estado) {
    }

    public record PagoResumen(
            UUID id,
            UUID contratoId,
            LocalDate fechaPago,
            BigDecimal montoOriginal,
            String monedaOriginal,
            BigDecimal montoContrato,
            String monedaContrato,
            String metodo,
            String estado,
            Long numeroRecibo) {
    }
}
