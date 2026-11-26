package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

import java.util.List;

/** Historial consolidado de contratos, cargos y pagos de un inquilino con campos serializables. */
public record HistorialInquilinoResponse(
        String inquilinoId,
        String nombre,
        String identificacion,
        boolean activo,
        List<ContratoResumen> contratos,
        List<CargoResumen> cargos,
        List<PagoResumen> pagos) {

    public record ContratoResumen(
            String id,
            String unidad,
            String inicio,
            String fin,
            String canon,
            String moneda,
            String estado) { }

    public record CargoResumen(
            String id,
            String contratoId,
            String tipo,
            String periodo,
            String venceEl,
            String importe,
            String moneda,
            String estado) { }

    public record PagoResumen(
            String id,
            String contratoId,
            String fechaPago,
            String montoOriginal,
            String monedaOriginal,
            String montoContrato,
            String monedaContrato,
            String metodo,
            String estado,
            Long numeroRecibo) { }
}
