package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

/** Cambio de estado de un inquilino; {@code activo} es obligatorio. */
public record InquilinoEstadoRequest(String activo) {
}
