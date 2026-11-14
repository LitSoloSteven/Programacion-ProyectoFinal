package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

/** Cambio de estado de una unidad. Si se omite, se marca inactiva. */
public record UnidadEstadoRequest(String estado) {
}