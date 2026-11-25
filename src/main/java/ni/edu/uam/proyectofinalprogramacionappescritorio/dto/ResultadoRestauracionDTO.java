package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.nio.file.Path;

/** Resultado verificable de una restauración en entorno aislado (RF-M12-07 / RNF-037). */
public record ResultadoRestauracionDTO(
        ManifiestoRespaldoDTO manifiesto,
        Path copiaPreviaBase,
        Path copiaPreviaAdjuntos,
        Path directorioAdjuntosRestaurado,
        Path registroExterno,
        int adjuntosVerificados,
        int hashesDiscordantes,
        long duracionMs
) { }
