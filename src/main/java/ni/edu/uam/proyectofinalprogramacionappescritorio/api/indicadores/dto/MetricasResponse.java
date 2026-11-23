package ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto;

import java.util.List;

/**
 * Métricas del panel principal. Los totales se separan por moneda y viajan
 * como texto decimal para no perder precisión en JSON.
 */
public record MetricasResponse(
        int propiedades,
        int unidades,
        int inquilinos,
        int contratosVigentes,
        List<TotalMonedaResponse> cargosPendientes,
        List<TotalMonedaResponse> pagosRegistrados) { }