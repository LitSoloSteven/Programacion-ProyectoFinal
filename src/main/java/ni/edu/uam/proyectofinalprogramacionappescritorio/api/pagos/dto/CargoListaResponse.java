package ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto;

public record CargoListaResponse(
        String id,
        String contratoId,
        String contratoInfo,
        String tipo,
        String concepto,
        String periodo,
        String fechaEmision,
        String venceEl,
        String importe,
        String importeFormateado,
        String estado) {
}
