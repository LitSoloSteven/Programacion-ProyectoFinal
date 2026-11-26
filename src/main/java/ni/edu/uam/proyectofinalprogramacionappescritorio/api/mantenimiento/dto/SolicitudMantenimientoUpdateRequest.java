package ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto;

public record SolicitudMantenimientoUpdateRequest(
        Long version,
        String descripcion,
        String prioridad,
        String responsable
) { }
