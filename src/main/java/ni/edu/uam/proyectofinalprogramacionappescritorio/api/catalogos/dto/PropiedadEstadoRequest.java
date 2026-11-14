package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

/** Baja lógica de una propiedad. Si se omite {@code activa}, se marca inactiva. */
public record PropiedadEstadoRequest(Boolean activa) {
}