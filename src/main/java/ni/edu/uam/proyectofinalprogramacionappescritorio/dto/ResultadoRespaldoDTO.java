package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.nio.file.Path;
import java.time.Instant;

/** Resultado verificable tras publicar un respaldo cifrado (RF-M12-06). */
public record ResultadoRespaldoDTO(
        Path archivo,
        long tamanoBytes,
        String sha256Paquete,
        Instant corteUtc,
        int cantidadAdjuntos,
        long ultimoNumeroRecibo,
        long duracionMs
) { }
