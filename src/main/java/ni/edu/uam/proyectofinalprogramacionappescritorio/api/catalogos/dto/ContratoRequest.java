package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

/** Cuerpo para crear o reemplazar un contrato de arrendamiento. */
public record ContratoRequest(
        String unidadId,
        String inquilinoId,
        String inicio,
        String fin,
        String canon,
        String moneda,
        Integer diaPago,
        String depositoPactado,
        String estado,
        Long version) {
}