package ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto;

public record ResultadoVerificacionRespaldoResponse(
        String corteLocal,
        String esquemaVersion,
        String motor,
        int adjuntosVerificados,
        long ultimoNumeroRecibo,
        String sha256Paquete,
        long duracionMs) { }
