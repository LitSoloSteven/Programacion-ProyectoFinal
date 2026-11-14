package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

/** Baja lógica de un inquilino. Si se omite {@code activo}, se marca inactivo. */
public record InquilinoEstadoRequest(Boolean activo) {
}