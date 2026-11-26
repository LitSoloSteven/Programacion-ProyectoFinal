package ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto;

public record ResultadoRespaldoResponse(
        String archivo,
        long tamanoBytes,
        String sha256Paquete,
        String corteLocal,
        int cantidadAdjuntos,
        long ultimoNumeroRecibo,
        long duracionMs) { }
