package ni.edu.uam.proyectofinalprogramacionappescritorio.api.adjuntos.dto;

public record DocumentoAdjuntoResponse(
        String id,
        String entidadTipo,
        String entidadId,
        String nombreOriginal,
        String mime,
        long tamanoBytes,
        String sha256,
        boolean activo,
        String creadoEn,
        String creadoPor,
        long version) { }
