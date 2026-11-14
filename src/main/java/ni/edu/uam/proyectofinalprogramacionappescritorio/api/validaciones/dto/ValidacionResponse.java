package ni.edu.uam.proyectofinalprogramacionappescritorio.api.validaciones.dto;

/**
 * Resultado de una validación puntual. La petición bien formada responde 200
 * aunque el valor no sea válido; el campo {@code valido} indica el resultado.
 */
public record ValidacionResponse(boolean valido, String mensaje) {
}