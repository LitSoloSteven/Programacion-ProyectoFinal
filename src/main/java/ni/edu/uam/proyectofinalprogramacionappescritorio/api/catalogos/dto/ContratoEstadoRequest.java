package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

/** Cambio de estado de un contrato; {@code estado} es obligatorio. */
public record ContratoEstadoRequest(String estado) {
}
