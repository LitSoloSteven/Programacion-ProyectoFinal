package ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto;

public record SolicitudMantenimientoRequest(
        String unidadId,
        String descripcion,
        String prioridad,
        String responsable,
        String abiertaEl
) { }
