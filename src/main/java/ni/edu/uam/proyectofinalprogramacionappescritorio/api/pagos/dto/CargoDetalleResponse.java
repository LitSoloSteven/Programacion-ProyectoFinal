package ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto;

/** Detalle completo de un cargo incluyendo su versión para edición optimista. */
public record CargoDetalleResponse(
        String id,
        long version,
        String contratoId,
        String contratoInfo,
        String moneda,
        String tipo,
        String periodo,
        String fechaEmision,
        String venceEl,
        String importe,
        String saldoPendiente,
        String concepto,
        String estado) { }
