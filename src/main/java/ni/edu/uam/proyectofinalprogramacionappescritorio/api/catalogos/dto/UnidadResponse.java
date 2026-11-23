package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

/** Representación de una unidad en la API. */
public record UnidadResponse(
        String id,
        String propiedadId,
        String propiedadNombre,
        String codigo,
        String descripcion,
        String tipo,
        String estado,
        String canonReferencia,
        String moneda,
        long version) {
}