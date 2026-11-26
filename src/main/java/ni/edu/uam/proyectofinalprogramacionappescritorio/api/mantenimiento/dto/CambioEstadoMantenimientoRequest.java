package ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto;

public record CambioEstadoMantenimientoRequest(
        Long version,
        String estado,
        String fechaCierre,
        String motivo
) { }
