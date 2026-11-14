package ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto;

public record ReciboResponse(
        long numero,
        String pagoId,
        String contratoId,
        String fechaPago,
        String contratoInfo,
        String montoOriginal,
        String monedaOriginal,
        String montoContrato,
        String monedaContrato,
        String metodo,
        String referencia,
        boolean anulado) { }