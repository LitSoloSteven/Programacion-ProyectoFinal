package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

/** Resultado de verificar autenticidad e integridad de un paquete de respaldo sin aplicarlo. */
public record ResultadoVerificacionRespaldoDTO(
        ManifiestoRespaldoDTO manifiesto,
        String sha256Paquete,
        int adjuntosVerificados,
        long duracionMs
) { }
