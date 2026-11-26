package ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto;

import java.util.List;

public record ResumenGastosMantenimientoResponse(
        List<GastoMantenimientoResponse> gastos,
        String totalUsd,
        String totalNio
) { }
