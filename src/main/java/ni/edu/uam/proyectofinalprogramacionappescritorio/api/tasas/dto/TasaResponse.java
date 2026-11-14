package ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto;

/**
 * Representación JSON de una tasa. Fechas, instantes, importes e identificadores
 * se serializan como texto; la tasa conserva sus ocho decimales.
 */
public record TasaResponse(
        String id,
        String fecha,
        String nioPorUsd,
        String fuente,
        String registradoEn,
        boolean autorizada,
        String autorizadoPor,
        String autorizadoPorNombre,
        String motivo) { }