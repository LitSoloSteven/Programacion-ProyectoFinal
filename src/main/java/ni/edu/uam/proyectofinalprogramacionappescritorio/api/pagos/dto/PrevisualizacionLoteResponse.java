package ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto;

import java.util.List;

/** Previsualización de generación mensual de rentas (H1). */
public record PrevisualizacionLoteResponse(
        String periodo,
        int totalElegibles,
        int porGenerar,
        int omitidos,
        List<Item> items) {

    public record Item(
            String contratoId,
            String contratoInfo,
            String moneda,
            String canon,
            String periodo,
            String venceEl,
            boolean yaExistente,
            String creditoDisponible,
            String creditoAplicable,
            String saldoResultante) { }
}
