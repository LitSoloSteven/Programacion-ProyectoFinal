package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

/** Representación de un contrato en la API. Importes y fechas viajan como texto. */
public record ContratoResponse(
        String id,
        String unidadId,
        String unidadInfo,
        String inquilinoId,
        String inquilinoNombre,
        String inicio,
        String fin,
        String canon,
        String moneda,
        int diaPago,
        String depositoPactado,
        String garantiaSaldo,
        String estado,
        long version) {
}