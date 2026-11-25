package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

/** Condiciones del contrato sucesor Borrador creado al renovar uno vigente o por vencer. */
public record ContratoRenovarRequest(
        String inicio,
        String fin,
        String canon,
        String moneda,
        Integer diaPago,
        String depositoPactado) {
}
