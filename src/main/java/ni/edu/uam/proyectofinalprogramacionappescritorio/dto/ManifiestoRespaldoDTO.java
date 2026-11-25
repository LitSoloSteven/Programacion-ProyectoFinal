package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.util.List;
import java.util.Map;

/**
 * Manifiesto verificable del paquete de respaldo (M12 / RN-29).
 * Nunca incluye contraseñas, tokens ni credenciales de conexión.
 */
public record ManifiestoRespaldoDTO(
        int formatoVersion,
        String aplicacion,
        String esquemaVersion,
        String revisionCodigo,
        String motorBase,
        String corteUtc,
        String corteLocal,
        String creadoPor,
        long ultimoNumeroRecibo,
        long siguienteNumeroRecibo,
        Map<String, Long> conteosTablas,
        EntradaArchivo dump,
        List<EntradaAdjunto> adjuntos
) {
    public record EntradaArchivo(
            String ruta,
            long tamanoBytes,
            String sha256
    ) { }

    public record EntradaAdjunto(
            String ruta,
            String rutaInterna,
            long tamanoBytes,
            String sha256,
            boolean activo
    ) { }
}
