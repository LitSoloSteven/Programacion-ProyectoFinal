package ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto;

import java.util.List;

/**
 * Métricas del panel principal. Los totales y tasas se separan por moneda y
 * viajan como texto decimal para no perder precisión en JSON.
 */
public record MetricasResponse(
        int propiedades,
        int unidades,
        int unidadesActivas,
        int unidadesOcupadas,
        String porcentajeOcupacion,
        String instanteGeneracion,
        String periodo,
        int inquilinos,
        int contratosVigentes,
        int contratosPorVencer,
        int contratosVencidos,
        List<TotalMonedaResponse> cargosPendientes,
        List<TotalMonedaResponse> principalPendiente,
        List<TotalMonedaResponse> moraExigible,
        List<TotalMonedaResponse> pagosRegistrados,
        TasaResumenResponse tasaDisponible) {

    public MetricasResponse(
            int propiedades,
            int unidades,
            int inquilinos,
            int contratosVigentes,
            List<TotalMonedaResponse> cargosPendientes,
            List<TotalMonedaResponse> pagosRegistrados) {
        this(propiedades, unidades, unidades, contratosVigentes, "0.00",
                null, null, inquilinos, contratosVigentes, 0, 0,
                cargosPendientes, cargosPendientes, List.of(), pagosRegistrados,
                TasaResumenResponse.sinTasa());
    }
}
