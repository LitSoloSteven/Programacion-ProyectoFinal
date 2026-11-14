package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

/** Cuerpo para crear o reemplazar una unidad. */
public record UnidadRequest(
        String propiedadId,
        String codigo,
        String descripcion,
        String tipo,
        String estado,
        String canonReferencia,
        String moneda,
        Long version) {
}