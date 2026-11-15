package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

/** Cambio de estado de una propiedad; {@code activa} es obligatorio. */
public record PropiedadEstadoRequest(String activa) {
}
