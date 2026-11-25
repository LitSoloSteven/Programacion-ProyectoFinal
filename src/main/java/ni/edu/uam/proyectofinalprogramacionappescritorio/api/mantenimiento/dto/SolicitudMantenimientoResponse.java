package ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto;

public record SolicitudMantenimientoResponse(
        String id,
        String unidadId,
        String unidadCodigo,
        String propiedadId,
        String propiedadNombre,
        String descripcion,
        String prioridad,
        String estado,
        String responsable,
        String abiertaEl,
        String cerradaEl,
        String motivoCancelacion,
        String detalleResolucion,
        String totalGastoUsd,
        String totalGastoNio,
        String creadoEn,
        String creadoPor,
        long version
) { }
