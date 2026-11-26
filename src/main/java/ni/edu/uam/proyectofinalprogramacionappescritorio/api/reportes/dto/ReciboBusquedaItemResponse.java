package ni.edu.uam.proyectofinalprogramacionappescritorio.api.reportes.dto;

public record ReciboBusquedaItemResponse(
        String pagoId,
        long numeroRecibo,
        String fechaPago,
        String contratoId,
        String unidadInfo,
        String inquilinoNombre,
        String moneda,
        String monto,
        boolean anulado) { }
