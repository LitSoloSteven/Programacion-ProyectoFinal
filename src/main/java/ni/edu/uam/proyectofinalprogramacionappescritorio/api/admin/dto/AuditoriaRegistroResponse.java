package ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto;

public record AuditoriaRegistroResponse(
        Long id,
        String actor,
        String instante,
        String correlacion,
        String accion,
        String referencia) { }
