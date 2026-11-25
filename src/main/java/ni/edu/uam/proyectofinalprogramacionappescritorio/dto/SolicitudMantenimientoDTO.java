package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Vista inmutable de una solicitud de mantenimiento con totales de gasto acumulados por moneda. */
public record SolicitudMantenimientoDTO(
        UUID id,
        UUID unidadId,
        String unidadCodigo,
        UUID propiedadId,
        String propiedadNombre,
        String descripcion,
        String prioridad,
        String estado,
        String responsable,
        LocalDate abiertaEl,
        LocalDate cerradaEl,
        String motivoCancelacion,
        String detalleResolucion,
        BigDecimal totalGastoUsd,
        BigDecimal totalGastoNio,
        Instant creadoEn,
        String creadoPor,
        long version
) { }
