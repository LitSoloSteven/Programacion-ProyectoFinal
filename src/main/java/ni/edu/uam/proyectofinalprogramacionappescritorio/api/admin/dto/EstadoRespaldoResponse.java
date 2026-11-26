package ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto;

public record EstadoRespaldoResponse(
        String ultimoRespaldo,
        long horasDesdeUltimo,
        boolean alertaVencida,
        String mensaje) { }
