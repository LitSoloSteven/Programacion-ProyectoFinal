package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

import java.util.List;

/** Paquete de movimientos para liquidar una garantía con clave idempotente. */
public record GarantiaLiquidacionRequest(
        String liquidacionId,
        List<MovimientoItemRequest> movimientos) {

    public record MovimientoItemRequest(
            String tipo,
            String cargoId,
            String importe,
            String fechaMovimiento,
            String metodo,
            String concepto) {
    }
}
