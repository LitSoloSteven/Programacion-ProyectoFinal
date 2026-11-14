package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

/** Cambio de estado de un contrato. Si se omite, se marca cancelado. */
public record ContratoEstadoRequest(String estado) {
}