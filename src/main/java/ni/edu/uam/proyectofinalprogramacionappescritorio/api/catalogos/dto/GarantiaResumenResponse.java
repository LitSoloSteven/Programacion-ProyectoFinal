package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

import java.util.List;

/** Resumen de garantía de un contrato e historial de movimientos. */
public record GarantiaResumenResponse(
        String garantiaId,
        String contratoId,
        String moneda,
        String montoInicial,
        String totalAplicado,
        String totalDeducido,
        String totalDevuelto,
        String saldoActual,
        String estado,
        boolean conciliado,
        List<MovimientoGarantiaResponse> movimientos) {

    public record MovimientoGarantiaResponse(
            String id,
            String cargoId,
            String cargoResumen,
            String liquidacionId,
            int orden,
            String tipo,
            String importe,
            String fechaMovimiento,
            String metodo,
            String concepto,
            String creadoEn,
            Long creadoPor) {
    }
}
