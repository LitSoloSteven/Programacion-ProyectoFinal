package ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto;

public record PagoListaResponse(
        String id,
        String contratoId,
        String contratoInfo,
        String fechaPago,
        String montoOriginal,
        String monedaOriginal,
        String montoFormateado,
        String metodo,
        String referencia,
        String estado) {
}
