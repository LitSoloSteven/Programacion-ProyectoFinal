package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.time.Instant;
import java.util.UUID;

/** Metadato visible de un documento o imagen adjunta controlada. */
public record DocumentoAdjuntoDTO(
        UUID id,
        String entidadTipo,
        UUID entidadId,
        String nombreOriginal,
        String mime,
        long tamanoBytes,
        String sha256,
        boolean activo,
        Instant creadoEn,
        String creadoPor,
        long version
) { }
