package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.math.BigDecimal;
import java.util.List;

/** Resultado de consulta de gastos con totales separados por moneda (USD y NIO). */
public record ResumenGastosMantenimientoDTO(
        List<GastoPropiedadDTO> gastos,
        BigDecimal totalUsd,
        BigDecimal totalNio
) { }
