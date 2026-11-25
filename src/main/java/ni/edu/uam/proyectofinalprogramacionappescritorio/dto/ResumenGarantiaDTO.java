package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Resumen conciliado de la garantía de un contrato y su historial de movimientos. */
public record ResumenGarantiaDTO(
        UUID garantiaId,
        UUID contratoId,
        String moneda,
        BigDecimal montoInicial,
        BigDecimal totalAplicado,
        BigDecimal totalDeducido,
        BigDecimal totalDevuelto,
        BigDecimal saldoActual,
        String estadoDerivado,
        boolean conciliado,
        List<MovimientoGarantiaDTO> movimientos) {
}
