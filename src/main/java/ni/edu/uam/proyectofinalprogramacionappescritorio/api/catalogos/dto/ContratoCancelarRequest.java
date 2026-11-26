package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

/** Cuerpo para cancelar un contrato en estado Borrador con motivo obligatorio. */
public record ContratoCancelarRequest(Long version, String motivo) {
}
